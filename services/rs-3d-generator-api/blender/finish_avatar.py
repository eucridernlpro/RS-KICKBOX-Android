# Run through Blender:
# blender --background --python finish_avatar.py -- --input source.glb --output result.glb --avatar sofia
import argparse
import sys
from pathlib import Path

import bpy

REQUIRED_ACTIONS=("idle","listening","thinking","speaking","acknowledge")
REQUIRED_MORPHS=("REST","A","E","I","O","U","FV","L","MBP","WQ","BLINK_LEFT","BLINK_RIGHT","SMILE")

def args():
    argv=sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else []
    ap=argparse.ArgumentParser()
    ap.add_argument("--input",required=True)
    ap.add_argument("--output",required=True)
    ap.add_argument("--avatar",choices=["sofia","marcus"],required=True)
    return ap.parse_args(argv)

def import_model(path:Path):
    ext=path.suffix.lower()
    if ext==".glb" or ext==".gltf":
        bpy.ops.import_scene.gltf(filepath=str(path))
    elif ext==".fbx":
        bpy.ops.import_scene.fbx(filepath=str(path))
    elif ext==".obj":
        bpy.ops.wm.obj_import(filepath=str(path))
    else:
        raise RuntimeError("Unsupported source format: "+ext)

def find_armature():
    return next((o for o in bpy.context.scene.objects if o.type=="ARMATURE"),None)

def mesh_objects():
    return [o for o in bpy.context.scene.objects if o.type=="MESH"]

def action_names():
    return {a.name for a in bpy.data.actions}

def morph_names():
    out=set()
    for obj in mesh_objects():
        if obj.data.shape_keys:
            out.update(k.name for k in obj.data.shape_keys.key_blocks)
    return out

def cleanup():
    # Remove cameras/lights from generated assets; SceneView owns them.
    for obj in list(bpy.context.scene.objects):
        if obj.type in {"CAMERA","LIGHT"}:
            bpy.data.objects.remove(obj,do_unlink=True)
    # Apply transforms only on non-armature meshes to preserve rig hierarchy.
    for obj in mesh_objects():
        obj.select_set(True)
        bpy.context.view_layer.objects.active=obj
        try:
            bpy.ops.object.transform_apply(location=False,rotation=True,scale=True)
        except Exception:
            pass
        obj.select_set(False)

def main():
    a=args()
    inp=Path(a.input).resolve()
    out=Path(a.output).resolve()
    out.parent.mkdir(parents=True,exist_ok=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    import_model(inp)
    cleanup()

    arm=find_armature()
    if arm is None:
        raise RuntimeError("No armature found after rigging")

    missing_actions=[x for x in REQUIRED_ACTIONS if x not in action_names()]
    missing_morphs=[x for x in REQUIRED_MORPHS if x not in morph_names()]
    if missing_actions:
        raise RuntimeError("Missing body animations before export: "+", ".join(missing_actions))
    if missing_morphs:
        raise RuntimeError("Missing facial morph targets before export: "+", ".join(missing_morphs))

    bpy.ops.export_scene.gltf(
        filepath=str(out),
        export_format="GLB",
        export_apply=True,
        export_animations=True,
        export_morph=True,
        export_skins=True,
        export_materials="EXPORT",
        export_yup=True,
    )
    print("RS avatar exported:",out)

if __name__=="__main__":
    main()
