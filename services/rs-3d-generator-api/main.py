from __future__ import annotations

import hashlib
import json
import os
import secrets
import shutil
import subprocess
import sys
import time
import uuid
from pathlib import Path
from typing import Literal

from fastapi import BackgroundTasks, Depends, FastAPI, File, Form, Header, HTTPException, UploadFile
from fastapi.responses import FileResponse
from pydantic import BaseModel

ROOT = Path(__file__).resolve().parent
REPO_ROOT = ROOT.parents[1]
WORK = Path(os.environ.get("RS3D_WORK_DIR", ROOT / "work")).resolve()
WORK.mkdir(parents=True, exist_ok=True)

CHARACTERGEN_ROOT = os.environ.get("CHARACTERGEN_ROOT", "")
TRIPOSR_ROOT = os.environ.get("TRIPOSR_ROOT", "")
UNIRIG_ROOT = os.environ.get("UNIRIG_ROOT", "")
BLENDER_BIN = os.environ.get("BLENDER_BIN", shutil.which("blender") or "")
RS3D_API_KEY = os.environ.get("RS3D_API_KEY", "")
MAX_UPLOAD_MB = int(os.environ.get("RS3D_MAX_UPLOAD_MB", "25"))
MAX_UPLOAD_BYTES = MAX_UPLOAD_MB * 1024 * 1024

ALLOWED_IMAGE_SUFFIXES = {".png", ".jpg", ".jpeg", ".webp"}
FINAL_REQUIRED_ANIMS = ["idle", "listening", "thinking", "speaking", "acknowledge"]
FINAL_REQUIRED_MORPHS = ["REST","A","E","I","O","U","FV","L","MBP","WQ","BLINK_LEFT","BLINK_RIGHT","SMILE"]

app = FastAPI(
    title="RS 3D Character Generator API",
    version="0.2.0",
    description="Self-hosted RS pipeline for image-to-3D, rigging, finishing and production GLB validation.",
)

class Capability(BaseModel):
    available: bool
    path: str = ""

class Health(BaseModel):
    status: str
    version: str
    charactergen: Capability
    triposr: Capability
    unirig: Capability
    blender: Capability

class JobResponse(BaseModel):
    job_id: str
    state: Literal["queued", "running", "completed", "failed"]
    stage: str
    backend: str
    output_glb: str | None = None
    message: str = ""
    created_at: int = 0
    updated_at: int = 0

def _now() -> int:
    return int(time.time())

def _job_dir(job_id: str) -> Path:
    if not job_id or any(c not in "0123456789abcdef" for c in job_id.lower()) or len(job_id) != 32:
        raise HTTPException(404, "Unknown job")
    job = (WORK / job_id).resolve()
    if WORK not in job.parents:
        raise HTTPException(404, "Unknown job")
    return job

def _manifest_path(job: Path) -> Path:
    return job / "job.json"

def _write_manifest(job: Path, **updates) -> dict:
    path = _manifest_path(job)
    current = {}
    if path.exists():
        try:
            current = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            current = {}
    current.update(updates)
    current.setdefault("created_at", _now())
    current["updated_at"] = _now()
    tmp = path.with_suffix(".tmp")
    tmp.write_text(json.dumps(current, indent=2, sort_keys=True), encoding="utf-8")
    tmp.replace(path)
    return current

def _read_manifest(job: Path) -> dict:
    path = _manifest_path(job)
    if not path.exists():
        raise HTTPException(404, "Unknown job")
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        raise HTTPException(500, "Job metadata is damaged")

def _job_response(data: dict) -> JobResponse:
    return JobResponse(
        job_id=data.get("job_id", ""),
        state=data.get("state", "failed"),
        stage=data.get("stage", "unknown"),
        backend=data.get("backend", ""),
        output_glb=data.get("output_glb"),
        message=data.get("message", ""),
        created_at=int(data.get("created_at", 0)),
        updated_at=int(data.get("updated_at", 0)),
    )

def _cap(path: str) -> Capability:
    return Capability(available=bool(path and Path(path).exists()), path=path)

def _run(cmd: list[str], cwd: str | Path | None = None, timeout: int = 7200) -> subprocess.CompletedProcess:
    return subprocess.run(
        cmd,
        cwd=str(cwd) if cwd else None,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        timeout=timeout,
        check=False,
        env=os.environ.copy(),
    )

def _require(path: str, name: str) -> Path:
    if not path:
        raise RuntimeError(f"{name} is not configured on this server")
    p = Path(path).resolve()
    if not p.exists():
        raise RuntimeError(f"{name} path does not exist: {p}")
    return p

def _auth(x_rs_api_key: str | None = Header(default=None)) -> None:
    if RS3D_API_KEY and not secrets.compare_digest(x_rs_api_key or "", RS3D_API_KEY):
        raise HTTPException(401, "Invalid RS 3D API key")

def _sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()

async def _save_upload(image: UploadFile, target: Path) -> int:
    total = 0
    with target.open("wb") as f:
        while True:
            chunk = await image.read(1024 * 1024)
            if not chunk:
                break
            total += len(chunk)
            if total > MAX_UPLOAD_BYTES:
                f.close()
                target.unlink(missing_ok=True)
                raise HTTPException(413, f"Image exceeds {MAX_UPLOAD_MB} MB")
            f.write(chunk)
    return total

def _discover_mesh(folder: Path) -> Path | None:
    if not folder.exists():
        return None
    candidates = list(folder.rglob("*.glb")) + list(folder.rglob("*.obj")) + list(folder.rglob("*.fbx"))
    return max(candidates, key=lambda p: p.stat().st_mtime, default=None)

def _generate_worker(job_id: str, backend: str) -> None:
    job = _job_dir(job_id)
    manifest = _read_manifest(job)
    input_path = Path(manifest["input_path"])
    raw = job / "raw"
    raw.mkdir(exist_ok=True)
    _write_manifest(job, state="running", stage="generating", message=f"Generating with {backend}")

    try:
        if backend == "triposr":
            root = _require(TRIPOSR_ROOT, "TripoSR")
            cmd = [sys.executable, "run.py", str(input_path), "--output-dir", str(raw)]
            proc = _run(cmd, cwd=root)
        else:
            root = _require(CHARACTERGEN_ROOT, "CharacterGen")
            adapter = root / "rs_charactergen_infer.py"
            if not adapter.exists():
                raise RuntimeError("CharacterGen RS CLI adapter is missing; run install_backends.py on the server")
            proc = _run([sys.executable, str(adapter), "--input", str(input_path), "--output", str(raw)], cwd=root)

        (job / "generation.log").write_text(proc.stdout or "", encoding="utf-8")
        if proc.returncode != 0:
            raise RuntimeError(f"{backend} exited with code {proc.returncode}")

        mesh = _discover_mesh(raw)
        if not mesh:
            raise RuntimeError("Backend produced no supported mesh")

        _write_manifest(
            job,
            state="completed",
            stage="generated",
            output_mesh=str(mesh),
            output_glb=str(mesh) if mesh.suffix.lower() == ".glb" else None,
            output_sha256=_sha256(mesh),
            message="Raw 3D generation completed",
        )
    except Exception as exc:
        _write_manifest(job, state="failed", stage="generating", message=str(exc)[:500])

def _rig_worker(job_id: str) -> None:
    job = _job_dir(job_id)
    manifest = _read_manifest(job)
    _write_manifest(job, state="running", stage="rigging", message="Auto-rigging with UniRig")
    try:
        root = _require(UNIRIG_ROOT, "UniRig")
        source = Path(manifest.get("output_mesh", ""))
        if not source.exists():
            source = _discover_mesh(job / "raw")
        if not source:
            raise RuntimeError("No generated mesh exists for this job")
        out = job / "rigged"
        out.mkdir(exist_ok=True)

        adapter = ROOT / "adapters" / "unirig_adapter.py"
        proc = _run(
            [sys.executable, str(adapter), "--unirig-root", str(root), "--input", str(source), "--output", str(out)]
        )
        (job / "rig.log").write_text(proc.stdout or "", encoding="utf-8")
        if proc.returncode != 0:
            raise RuntimeError(f"UniRig adapter exited with code {proc.returncode}")

        rigged = _discover_mesh(out)
        if not rigged:
            raise RuntimeError("UniRig produced no supported output")
        _write_manifest(
            job,
            state="completed",
            stage="rigged",
            output_mesh=str(rigged),
            output_glb=str(rigged) if rigged.suffix.lower() == ".glb" else None,
            output_sha256=_sha256(rigged),
            message="Auto-rig stage completed",
        )
    except Exception as exc:
        _write_manifest(job, state="failed", stage="rigging", message=str(exc)[:500])

def _finish_worker(job_id: str, avatar: str) -> None:
    job = _job_dir(job_id)
    manifest = _read_manifest(job)
    _write_manifest(job, state="running", stage="finishing", message="Blender mobile finishing and GLB export")
    try:
        blender = _require(BLENDER_BIN, "Blender")
        source = Path(manifest.get("output_mesh", ""))
        if not source.exists():
            source = _discover_mesh(job / "rigged") or _discover_mesh(job / "raw")
        if not source:
            raise RuntimeError("No mesh exists to finish")
        finished = job / "finished"
        finished.mkdir(exist_ok=True)
        output = finished / f"rs_ai_{avatar.lower()}.glb"
        script = ROOT / "blender" / "finish_avatar.py"
        proc = _run(
            [str(blender), "--background", "--python", str(script), "--", "--input", str(source), "--output", str(output), "--avatar", avatar],
            timeout=7200,
        )
        (job / "finish.log").write_text(proc.stdout or "", encoding="utf-8")
        if proc.returncode != 0 or not output.exists():
            raise RuntimeError(f"Blender finishing failed with code {proc.returncode}")

        validator = REPO_ROOT / "tools" / "validate_rs_ai_glb.py"
        check = _run([sys.executable, str(validator), str(output)], timeout=300)
        (job / "validation.log").write_text(check.stdout or "", encoding="utf-8")
        if check.returncode != 0:
            raise RuntimeError("Finished GLB failed RS production contract; see validation.log")

        _write_manifest(
            job,
            state="completed",
            stage="production_ready",
            output_mesh=str(output),
            output_glb=str(output),
            output_sha256=_sha256(output),
            avatar=avatar,
            message="Production GLB passed RS contract",
        )
    except Exception as exc:
        _write_manifest(job, state="failed", stage="finishing", message=str(exc)[:500])

@app.get("/v1/health", response_model=Health)
def health() -> Health:
    return Health(
        status="ok",
        version=app.version,
        charactergen=_cap(CHARACTERGEN_ROOT),
        triposr=_cap(TRIPOSR_ROOT),
        unirig=_cap(UNIRIG_ROOT),
        blender=Capability(available=bool(BLENDER_BIN and Path(BLENDER_BIN).exists()), path=BLENDER_BIN),
    )

@app.get("/v1/capabilities")
def capabilities():
    h = health()
    return {
        "image_to_human_3d": h.charactergen.available,
        "image_to_generic_3d": h.triposr.available,
        "auto_rig": h.unirig.available,
        "glb_postprocess": h.blender.available,
        "paid_external_api_required": False,
        "max_upload_mb": MAX_UPLOAD_MB,
        "production_avatar_contract": {
            "animations": FINAL_REQUIRED_ANIMS,
            "morph_targets": FINAL_REQUIRED_MORPHS,
            "max_triangles": 80000,
        },
    }

@app.post("/v1/generate/from-image", response_model=JobResponse, dependencies=[Depends(_auth)])
async def generate_from_image(
    background_tasks: BackgroundTasks,
    image: UploadFile = File(...),
    backend: Literal["charactergen", "triposr"] = Form("charactergen"),
):
    suffix = Path(image.filename or "input.png").suffix.lower()
    if suffix not in ALLOWED_IMAGE_SUFFIXES:
        raise HTTPException(415, "Supported input images: PNG, JPG/JPEG, WEBP")

    if backend == "triposr":
        _require(TRIPOSR_ROOT, "TripoSR")
    else:
        _require(CHARACTERGEN_ROOT, "CharacterGen")

    job_id = uuid.uuid4().hex
    job = WORK / job_id
    job.mkdir(parents=True, exist_ok=False)
    input_path = job / f"input{suffix}"
    size = await _save_upload(image, input_path)
    data = _write_manifest(
        job,
        job_id=job_id,
        state="queued",
        stage="generation_queued",
        backend=backend,
        input_path=str(input_path),
        input_bytes=size,
        input_sha256=_sha256(input_path),
        message="Generation queued",
    )
    background_tasks.add_task(_generate_worker, job_id, backend)
    return _job_response(data)

@app.get("/v1/jobs/{job_id}", response_model=JobResponse, dependencies=[Depends(_auth)])
def job_status(job_id: str):
    job = _job_dir(job_id)
    if not job.exists():
        raise HTTPException(404, "Unknown job")
    return _job_response(_read_manifest(job))

@app.post("/v1/rig/{job_id}", response_model=JobResponse, dependencies=[Depends(_auth)])
def rig(job_id: str, background_tasks: BackgroundTasks):
    _require(UNIRIG_ROOT, "UniRig")
    job = _job_dir(job_id)
    if not job.exists():
        raise HTTPException(404, "Unknown job")
    current = _read_manifest(job)
    if current.get("state") == "running":
        raise HTTPException(409, "Job is already running")
    data = _write_manifest(job, state="queued", stage="rig_queued", message="Rigging queued")
    background_tasks.add_task(_rig_worker, job_id)
    return _job_response(data)

@app.post("/v1/finish/{job_id}", response_model=JobResponse, dependencies=[Depends(_auth)])
def finish(
    job_id: str,
    background_tasks: BackgroundTasks,
    avatar: Literal["sofia", "marcus"] = Form(...),
):
    _require(BLENDER_BIN, "Blender")
    job = _job_dir(job_id)
    if not job.exists():
        raise HTTPException(404, "Unknown job")
    current = _read_manifest(job)
    if current.get("state") == "running":
        raise HTTPException(409, "Job is already running")
    data = _write_manifest(job, state="queued", stage="finish_queued", avatar=avatar, message="Finishing queued")
    background_tasks.add_task(_finish_worker, job_id, avatar)
    return _job_response(data)

@app.post("/v1/validate/{job_id}", dependencies=[Depends(_auth)])
def validate(job_id: str):
    job = _job_dir(job_id)
    if not job.exists():
        raise HTTPException(404, "Unknown job")
    source = _discover_mesh(job / "finished") or _discover_mesh(job / "rigged") or _discover_mesh(job / "raw")
    if not source or source.suffix.lower() != ".glb":
        raise HTTPException(409, "No GLB exists for validation")
    validator = REPO_ROOT / "tools" / "validate_rs_ai_glb.py"
    proc = _run([sys.executable, str(validator), str(source)], timeout=300)
    return {
        "job_id": job_id,
        "valid": proc.returncode == 0,
        "file": str(source),
        "sha256": _sha256(source),
        "report": (proc.stdout or "")[-12000:],
    }

@app.get("/v1/jobs/{job_id}/download", dependencies=[Depends(_auth)])
def download(job_id: str):
    job = _job_dir(job_id)
    if not job.exists():
        raise HTTPException(404, "Unknown job")
    manifest = _read_manifest(job)
    preferred = Path(manifest.get("output_glb", "")) if manifest.get("output_glb") else None
    source = preferred if preferred and preferred.exists() else _discover_mesh(job / "finished")
    if not source or source.suffix.lower() != ".glb":
        raise HTTPException(404, "No GLB is ready")
    return FileResponse(source, filename=source.name, media_type="model/gltf-binary")
