#!/usr/bin/env python3
import argparse, json, os, struct, sys

REQUIRED_ANIMS = {"idle","listening","thinking","speaking","acknowledge"}
REQUIRED_MORPHS = {"REST","A","E","I","O","U","FV","L","MBP","WQ","BLINK_LEFT","BLINK_RIGHT","SMILE"}
MAX_TRIANGLES = 80000
MAX_BYTES = 100 * 1024 * 1024

def load_glb(path):
    data=open(path,"rb").read()
    if len(data)<20: raise ValueError("file too small")
    magic,version,total=struct.unpack_from("<4sII",data,0)
    if magic!=b"glTF": raise ValueError("invalid GLB magic")
    if version!=2: raise ValueError(f"unsupported GLB version {version}; require 2")
    if total!=len(data): raise ValueError(f"header length {total} != actual {len(data)}")
    off=12
    chunks=[]
    while off+8<=len(data):
        ln,typ=struct.unpack_from("<II",data,off); off+=8
        payload=data[off:off+ln]; off+=ln
        chunks.append((typ,payload))
    js=next((p for t,p in chunks if t==0x4E4F534A),None)
    if js is None: raise ValueError("missing JSON chunk")
    return data, json.loads(js.decode("utf-8").rstrip(" \t\r\n\0"))

def target_names(doc):
    names=set()
    for mesh in doc.get("meshes",[]):
        extras=mesh.get("extras") or {}
        for n in extras.get("targetNames",[]) or []:
            if isinstance(n,str): names.add(n)
    return names

def triangle_count(doc):
    accessors=doc.get("accessors",[])
    total=0
    for mesh in doc.get("meshes",[]):
        for prim in mesh.get("primitives",[]):
            mode=prim.get("mode",4)
            if mode!=4: continue
            idx=prim.get("indices")
            if isinstance(idx,int) and idx<len(accessors):
                total += int(accessors[idx].get("count",0))//3
            else:
                pos=(prim.get("attributes") or {}).get("POSITION")
                if isinstance(pos,int) and pos<len(accessors):
                    total += int(accessors[pos].get("count",0))//3
    return total

def validate(path):
    errors=[]; warnings=[]
    if not os.path.exists(path):
        return ["MISSING"], warnings, {}
    if os.path.getsize(path)>MAX_BYTES:
        errors.append(f"file exceeds {MAX_BYTES//1024//1024} MB")
    try:
        data,doc=load_glb(path)
    except Exception as e:
        return [str(e)], warnings, {}
    if not doc.get("skins"): errors.append("no humanoid skin/skeleton found")
    if not doc.get("meshes"): errors.append("no mesh found")
    mats=doc.get("materials",[])
    if not mats: warnings.append("no material definitions found")
    anims={a.get("name","") for a in doc.get("animations",[]) if a.get("name")}
    missing_anims=sorted(REQUIRED_ANIMS-anims)
    if missing_anims: errors.append("missing animations: "+", ".join(missing_anims))
    morphs=target_names(doc)
    missing_morphs=sorted(REQUIRED_MORPHS-morphs)
    if missing_morphs: errors.append("missing facial morph targets: "+", ".join(missing_morphs))
    tris=triangle_count(doc)
    if tris==0: warnings.append("triangle count could not be determined")
    elif tris>MAX_TRIANGLES: errors.append(f"triangle budget exceeded: {tris} > {MAX_TRIANGLES}")
    images=doc.get("images",[])
    external=[i.get("uri") for i in images if isinstance(i.get("uri"),str) and not i["uri"].startswith("data:")]
    if external: errors.append("external image references found; GLB must be self-contained")
    buffers=doc.get("buffers",[])
    external_buffers=[b.get("uri") for b in buffers if isinstance(b.get("uri"),str)]
    if external_buffers: errors.append("external buffers found; GLB must be self-contained")
    info={"bytes":len(data),"triangles":tris,"animations":sorted(anims),"morph_targets":sorted(morphs),"skins":len(doc.get("skins",[])),"materials":len(mats)}
    return errors,warnings,info

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("files",nargs="+")
    ap.add_argument("--allow-missing",action="store_true")
    args=ap.parse_args()
    failed=False
    for path in args.files:
        errors,warnings,info=validate(path)
        if errors==["MISSING"] and args.allow_missing:
            print(f"[RS AI GLB] {path}: not present yet (allowed for fallback build)")
            continue
        print(f"[RS AI GLB] {path}")
        if info: print(json.dumps(info,indent=2,sort_keys=True))
        for w in warnings: print("WARNING:",w)
        for e in errors: print("ERROR:",e)
        if errors: failed=True
        else: print("PASS: production avatar contract satisfied")
    sys.exit(1 if failed else 0)

if __name__=="__main__":
    main()
