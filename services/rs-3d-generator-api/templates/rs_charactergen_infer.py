#!/usr/bin/env python3
"""
Stable RS CLI adapter for CharacterGen pinned revision
f329a835dbd5003060a5653eafd83d4d8868b043.

Run from the CharacterGen repository root.
"""
from __future__ import annotations

import argparse
import shutil
from pathlib import Path

from PIL import Image
from omegaconf import OmegaConf

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--input",required=True)
    ap.add_argument("--output",required=True)
    ap.add_argument("--seed",type=int,default=2333)
    ap.add_argument("--timesteps",type=int,default=40)
    ap.add_argument("--smooth-iter",type=int,default=5)
    ap.add_argument("--back-proj",action="store_true")
    ap.add_argument("--keep-background",action="store_true")
    a=ap.parse_args()

    source=Path(a.input).resolve()
    out=Path(a.output).resolve()
    out.mkdir(parents=True,exist_ok=True)

    if not source.exists():
        raise SystemExit("Input image not found: "+str(source))

    # Import after argument validation because upstream initializes CUDA/model helpers
    # at module import time.
    import webui as cg

    image=Image.open(source).convert("RGBA")
    infer2d=cg.Inference2D_API(**OmegaConf.load("./2D_Stage/configs/infer.yaml"))
    infer3d=cg.Inference3D_API()
    remove=cg.rm_bg_api()

    if not a.keep_background:
        image=remove.remove_background(
            imgs=[cg.np.array(image)],
            alpha_min=0.1,
            alpha_max=0.9,
        )[0]

    generated=infer2d.inference(
        image,
        512,
        768,
        crop=True,
        seed=a.seed,
        timestep=a.timesteps,
    )
    generated=remove.remove_background(
        imgs=generated,
        alpha_min=0.2,
        alpha_max=0.9,
    )
    if len(generated)!=4:
        raise SystemExit(f"CharacterGen returned {len(generated)} views; expected 4")

    # Upstream Gradio mapping:
    # output[0] -> right, output[1] -> back, output[2] -> left, output[3] -> front.
    right,back,left,front=generated
    view_dir=out/"views"
    view_dir.mkdir(exist_ok=True)
    for name,img in [("front",front),("back",back),("left",left),("right",right)]:
        img.save(view_dir/f"{name}.png")

    save_dir,obj_path,glb_path=infer3d.process_images(
        back,
        front,
        right,
        left,
        a.back_proj,
        a.smooth_iter,
    )
    src_glb=Path(glb_path).resolve()
    if not src_glb.exists():
        raise SystemExit("CharacterGen completed but output.glb was not found")

    final=out/"charactergen.glb"
    shutil.copy2(src_glb,final)
    src_obj=Path(obj_path).resolve()
    if src_obj.exists():
        shutil.copy2(src_obj,out/"charactergen.obj")

    (out/"source_output_dir.txt").write_text(str(Path(save_dir).resolve()),encoding="utf-8")
    print("RS CharacterGen output:",final)

if __name__=="__main__":
    main()
