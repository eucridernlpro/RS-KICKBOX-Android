#!/usr/bin/env python3
import argparse
import subprocess
import sys
from pathlib import Path

def run(cmd,cwd):
    print("+"," ".join(map(str,cmd)),flush=True)
    return subprocess.run(cmd,cwd=cwd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--unirig-root",required=True)
    ap.add_argument("--input",required=True)
    ap.add_argument("--output",required=True)
    a=ap.parse_args()
    root=Path(a.unirig_root).resolve()
    inp=Path(a.input).resolve()
    out=Path(a.output).resolve()
    out.mkdir(parents=True,exist_ok=True)

    skeleton=out/"predicted_skeleton.fbx"
    skinned=out/"predicted_skin.fbx"
    rigged=out/"rigged.glb"

    required=[
        root/"launch/inference/generate_skeleton.sh",
        root/"launch/inference/generate_skin.sh",
        root/"launch/inference/merge.sh",
    ]
    missing=[str(p) for p in required if not p.exists()]
    if missing:
        print("Missing pinned UniRig inference scripts:",*missing,sep="\n- ")
        return 2

    stages=[
        ["bash","launch/inference/generate_skeleton.sh","--input",str(inp),"--output",str(skeleton)],
        ["bash","launch/inference/generate_skin.sh","--input",str(skeleton),"--output",str(skinned)],
        ["bash","launch/inference/merge.sh","--source",str(skinned),"--target",str(inp),"--output",str(rigged)],
    ]
    for command in stages:
        p=run(command,root)
        print(p.stdout or "")
        if p.returncode!=0:
            return p.returncode
    if not rigged.exists():
        print("UniRig stages returned success but rigged.glb was not produced.")
        return 3
    print("RS UniRig output:",rigged)
    return 0

if __name__=="__main__":
    raise SystemExit(main())
