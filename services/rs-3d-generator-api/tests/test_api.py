import importlib
import os
from pathlib import Path

from fastapi.testclient import TestClient

def load_app(tmp_path):
    os.environ["RS3D_WORK_DIR"]=str(tmp_path/"work")
    os.environ.pop("RS3D_API_KEY",None)
    import main
    importlib.reload(main)
    return main

def test_health_and_capabilities(tmp_path):
    main=load_app(tmp_path)
    c=TestClient(main.app)
    h=c.get("/v1/health")
    assert h.status_code==200
    body=h.json()
    assert body["status"]=="ok"
    assert body["version"]=="0.2.0"

    cap=c.get("/v1/capabilities")
    assert cap.status_code==200
    data=cap.json()
    assert data["paid_external_api_required"] is False
    assert "speaking" in data["production_avatar_contract"]["animations"]
    assert "MBP" in data["production_avatar_contract"]["morph_targets"]

def test_unknown_job_is_404(tmp_path):
    main=load_app(tmp_path)
    c=TestClient(main.app)
    r=c.get("/v1/jobs/"+"a"*32)
    assert r.status_code==404

def test_rejects_bad_image_type_before_generation(tmp_path):
    main=load_app(tmp_path)
    c=TestClient(main.app)
    r=c.post(
        "/v1/generate/from-image",
        files={"image":("x.txt",b"not image","text/plain")},
        data={"backend":"triposr"},
    )
    assert r.status_code==415

def test_api_key_protection(tmp_path):
    os.environ["RS3D_WORK_DIR"]=str(tmp_path/"work")
    os.environ["RS3D_API_KEY"]="secret"
    import main
    importlib.reload(main)
    c=TestClient(main.app)
    assert c.get("/v1/jobs/"+"a"*32).status_code==401
    assert c.get("/v1/jobs/"+"a"*32,headers={"X-RS-API-Key":"secret"}).status_code==404
