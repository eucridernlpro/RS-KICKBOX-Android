#!/usr/bin/env python3
"""
RS adapter boundary for CharacterGen.

CharacterGen upstream is primarily demo/web oriented and its internal Python API
may change. This adapter intentionally refuses to guess at incompatible revisions.
For the pinned revision, install the model dependencies per upstream README and
then map its documented inference entry point here after the hardware profile is known.

Keeping this adapter explicit prevents the RS server from silently producing the
wrong mesh when upstream APIs change.
"""
import argparse
from pathlib import Path

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--input",required=True)
    ap.add_argument("--output",required=True)
    a=ap.parse_args()
    Path(a.output).mkdir(parents=True,exist_ok=True)
    raise SystemExit(
        "CharacterGen source is pinned, but GPU/model-specific inference wiring "
        "must be finalized after hardware_probe.py reports the tower GPU/VRAM."
    )

if __name__=="__main__":
    main()
