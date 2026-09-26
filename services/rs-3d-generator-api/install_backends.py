#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
from pathlib import Path

ENGINES = {
    "charactergen": {
        "url": "https://github.com/zjp-shadow/CharacterGen.git",
        "revision": "f329a835dbd5003060a5653eafd83d4d8868b043",
        "license": "Apache-2.0",
    },
    "triposr": {
        "url": "https://github.com/VAST-AI-Research/TripoSR.git",
        "revision": "107cefdc244c39106fa830359024f6a2f1c78871",
        "license": "MIT",
    },
    "unirig": {
        "url": "https://github.com/VAST-AI-Research/UniRig.git",
        "revision": "6793c6640ff01c8fb389f3993434124bb43d2933",
        "license": "MIT",
    },
}

def run(cmd, cwd=None, check=True):
    print("+", " ".join(map(str, cmd)))
    return subprocess.run(cmd, cwd=cwd, check=check)

def clone_locked(name, root: Path):
    meta=ENGINES[name]
    target=root/({"charactergen":"CharacterGen","triposr":"TripoSR","unirig":"UniRig"}[name])
    if not target.exists():
        run(["git","clone",meta["url"],str(target)])
    run(["git","fetch","--all","--tags"],cwd=target)
    run(["git","checkout","--detach",meta["revision"]],cwd=target)
    got=subprocess.check_output(["git","rev-parse","HEAD"],cwd=target,text=True).strip()
    if got!=meta["revision"]:
        raise RuntimeError(f"{name} revision mismatch: {got}")
    return target

def write_character_adapter(root: Path, service_root: Path):
    src=service_root/"templates"/"rs_charactergen_infer.py"
    dst=root/"rs_charactergen_infer.py"
    shutil.copy2(src,dst)
    print("Installed RS CharacterGen adapter:",dst)

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--root",default="/opt/rs3d")
    ap.add_argument("--engines",nargs="+",choices=sorted(ENGINES),default=sorted(ENGINES))
    ap.add_argument("--clone-only",action="store_true",help="Clone and pin source only; do not install Python dependencies/models.")
    args=ap.parse_args()

    root=Path(args.root).expanduser().resolve()
    root.mkdir(parents=True,exist_ok=True)
    service_root=Path(__file__).resolve().parent

    installed={}
    for name in args.engines:
        p=clone_locked(name,root)
        installed[name]={"path":str(p),**ENGINES[name]}
        if name=="charactergen":
            write_character_adapter(p,service_root)

    lock=root/"rs3d-engine-lock.json"
    lock.write_text(json.dumps(installed,indent=2,sort_keys=True),encoding="utf-8")
    print("\nPinned engine manifest:",lock)
    if args.clone_only:
        return 0

    print("\nSource is pinned. Engine dependency/model installation is intentionally hardware-specific.")
    print("Run hardware_probe.py next; it will choose the safe install profile for this tower.")
    return 0

if __name__=="__main__":
    raise SystemExit(main())
