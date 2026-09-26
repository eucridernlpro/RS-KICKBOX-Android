from __future__ import annotations

import os
import shutil
import subprocess
import uuid
from pathlib import Path
from typing import Literal

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.responses import FileResponse
from pydantic import BaseModel

ROOT = Path(__file__).resolve().parent
WORK = Path(os.environ.get("RS3D_WORK_DIR", ROOT / "work")).resolve()
WORK.mkdir(parents=True, exist_ok=True)

CHARACTERGEN_ROOT = os.environ.get("CHARACTERGEN_ROOT", "")
TRIPOSR_ROOT = os.environ.get("TRIPOSR_ROOT", "")
UNIRIG_ROOT = os.environ.get("UNIRIG_ROOT", "")
BLENDER_BIN = os.environ.get("BLENDER_BIN", shutil.which("blender") or "")

app = FastAPI(
    title="RS 3D Character Generator API",
    version="0.1.0",
    description="Self-hosted RS pipeline for image-to-3D, rigging, animation preparation and GLB export.",
)

class Capability(BaseModel):
    available: bool
    path: str = ""

class Health(BaseModel):
    status: str
    charactergen: Capability
    triposr: Capability
    unirig: Capability
    blender: Capability

class JobResponse(BaseModel):
    job_id: str
    state: Literal["queued", "running", "completed", "failed"]
    backend: str
    output_glb: str | None = None
    message: str = ""

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
    )

def _require(path: str, name: str) -> Path:
    if not path:
        raise HTTPException(503, f"{name} is not configured on this server")
    p = Path(path)
    if not p.exists():
        raise HTTPException(503, f"{name} path does not exist: {p}")
    return p

@app.get("/v1/health", response_model=Health)
def health() -> Health:
    return Health(
        status="ok",
        charactergen=_cap(CHARACTERGEN_ROOT),
        triposr=_cap(TRIPOSR_ROOT),
        unirig=_cap(UNIRIG_ROOT),
        blender=Capability(available=bool(BLENDER_BIN), path=BLENDER_BIN),
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
        "production_avatar_contract": {
            "animations": ["idle", "listening", "thinking", "speaking", "acknowledge"],
            "morph_targets": ["REST","A","E","I","O","U","FV","L","MBP","WQ","BLINK_LEFT","BLINK_RIGHT","SMILE"],
            "max_triangles": 80000,
        },
    }

@app.post("/v1/generate/from-image", response_model=JobResponse)
async def generate_from_image(
    image: UploadFile = File(...),
    backend: Literal["charactergen", "triposr"] = Form("charactergen"),
):
    job_id = uuid.uuid4().hex
    job = WORK / job_id
    job.mkdir(parents=True, exist_ok=False)
    suffix = Path(image.filename or "input.png").suffix.lower() or ".png"
    input_path = job / f"input{suffix}"
    with input_path.open("wb") as f:
        while chunk := await image.read(1024 * 1024):
            f.write(chunk)

    if backend == "triposr":
        root = _require(TRIPOSR_ROOT, "TripoSR")
        proc = _run(["python", "run.py", str(input_path), "--output-dir", str(job / "raw")], cwd=root)
    else:
        root = _require(CHARACTERGEN_ROOT, "CharacterGen")
        # CharacterGen's upstream repo is web-UI oriented. Our installer creates
        # rs_charactergen_infer.py as a stable local CLI adapter when installed.
        adapter = root / "rs_charactergen_infer.py"
        if not adapter.exists():
            raise HTTPException(
                503,
                "CharacterGen is installed but the RS CLI adapter is not ready. Run the RS backend installer.",
            )
        proc = _run(["python", str(adapter), "--input", str(input_path), "--output", str(job / "raw")], cwd=root)

    (job / "generation.log").write_text(proc.stdout or "", encoding="utf-8")
    if proc.returncode != 0:
        return JobResponse(job_id=job_id, state="failed", backend=backend, message="Generation failed; see server log.")

    candidates = list((job / "raw").rglob("*.glb")) + list((job / "raw").rglob("*.obj"))
    if not candidates:
        return JobResponse(job_id=job_id, state="failed", backend=backend, message="Backend produced no supported mesh.")

    mesh = candidates[0]
    return JobResponse(job_id=job_id, state="completed", backend=backend, output_glb=str(mesh), message="Raw 3D generation completed.")

@app.post("/v1/rig/{job_id}", response_model=JobResponse)
def rig(job_id: str):
    job = (WORK / job_id).resolve()
    if not job.exists() or WORK not in job.parents:
        raise HTTPException(404, "Unknown job")
    root = _require(UNIRIG_ROOT, "UniRig")
    meshes = list((job / "raw").rglob("*.glb")) + list((job / "raw").rglob("*.obj"))
    if not meshes:
        raise HTTPException(409, "No generated mesh exists for this job")
    out = job / "rigged"
    out.mkdir(exist_ok=True)
    proc = _run(["python", "run.py", "--input", str(meshes[0]), "--output", str(out)], cwd=root)
    (job / "rig.log").write_text(proc.stdout or "", encoding="utf-8")
    if proc.returncode != 0:
        return JobResponse(job_id=job_id, state="failed", backend="unirig", message="Rigging failed; see server log.")
    results = list(out.rglob("*.glb")) + list(out.rglob("*.fbx")) + list(out.rglob("*.obj"))
    return JobResponse(
        job_id=job_id,
        state="completed" if results else "failed",
        backend="unirig",
        output_glb=str(results[0]) if results else None,
        message="Auto-rig stage completed." if results else "UniRig produced no supported output.",
    )

@app.get("/v1/jobs/{job_id}/download")
def download(job_id: str):
    job = (WORK / job_id).resolve()
    if not job.exists() or WORK not in job.parents:
        raise HTTPException(404, "Unknown job")
    candidates = list(job.rglob("*.glb"))
    if not candidates:
        raise HTTPException(404, "No GLB is ready")
    return FileResponse(candidates[-1], filename=f"rs-character-{job_id}.glb", media_type="model/gltf-binary")
