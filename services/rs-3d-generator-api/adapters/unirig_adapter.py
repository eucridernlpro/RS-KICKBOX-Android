#!/usr/bin/env python3
import argparse, subprocess, sys
from pathlib import Path

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

    candidates=[
        [sys.executable,"run.py","--input",str(inp),"--output",str(out)],
        [sys.executable,"inference.py","--input",str(inp),"--output",str(out)],
        [sys.executable,"predict.py","--input",str(inp),"--output",str(out)],
    ]
    for cmd in candidates:
        if not (root/cmd[1]).exists():
            continue
        p=subprocess.run(cmd,cwd=root,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        print(p.stdout or "")
        if p.returncode==0:
            return 0
    print("No supported UniRig CLI entrypoint succeeded. Update the RS adapter for the installed upstream revision.")
    return 2

if __name__=="__main__":
    raise SystemExit(main())
