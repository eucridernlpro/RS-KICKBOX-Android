#!/usr/bin/env python3
from __future__ import annotations
import json, os, platform, shutil, subprocess

def cmd(args):
    try:
        return subprocess.check_output(args,text=True,stderr=subprocess.STDOUT,timeout=15).strip()
    except Exception:
        return ""

def main():
    info={
        "os": platform.platform(),
        "python": platform.python_version(),
        "machine": platform.machine(),
        "cpu": platform.processor(),
        "ram_gb": None,
        "nvidia": False,
        "gpu": [],
        "nvidia_driver": "",
        "cuda_runtime": "",
        "blender": shutil.which("blender") or "",
    }
    try:
        if hasattr(os,"sysconf"):
            pages=os.sysconf("SC_PHYS_PAGES"); page=os.sysconf("SC_PAGE_SIZE")
            info["ram_gb"]=round((pages*page)/(1024**3),1)
    except Exception:
        pass

    smi=shutil.which("nvidia-smi")
    if smi:
        info["nvidia"]=True
        q=cmd([smi,"--query-gpu=name,memory.total,driver_version","--format=csv,noheader,nounits"])
        for line in q.splitlines():
            p=[x.strip() for x in line.split(",")]
            if len(p)>=3:
                info["gpu"].append({"name":p[0],"vram_mb":int(float(p[1])),"driver":p[2]})
        info["nvidia_driver"]=info["gpu"][0]["driver"] if info["gpu"] else ""
        info["cuda_runtime"]=cmd([smi])[:500]

    vram=max([g["vram_mb"] for g in info["gpu"]],default=0)
    # Only classify against hardware requirements stated by the upstream projects.
    # TripoSR documents ~6 GB VRAM for its default single-image path.
    # UniRig documents >=8 GB VRAM for generation.
    # CharacterGen does not publish a simple VRAM minimum in its README, so we
    # intentionally leave its compatibility as "probe_required" rather than guess.
    info["engine_readiness"]={
        "triposr": "candidate" if vram>=6000 else "insufficient_vram",
        "unirig": "candidate" if vram>=8000 else "insufficient_vram",
        "charactergen": "probe_required" if info["nvidia"] else "no_nvidia_gpu",
    }
    if not info["nvidia"]:
        profile="cpu_only_not_recommended"
    elif vram>=8000:
        profile="triposr_plus_unirig_then_probe_charactergen"
    elif vram>=6000:
        profile="triposr_first_rigging_needs_more_vram"
    else:
        profile="insufficient_vram_for_recommended_local_pipeline"
    info["recommended_profile"]=profile

    print(json.dumps(info,indent=2))
    return 0

if __name__=="__main__":
    raise SystemExit(main())
