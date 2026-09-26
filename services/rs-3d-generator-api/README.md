# RS 3D Character Generator API

RS-owned, self-hosted API facade for generating and preparing 3D characters without paying a per-generation third-party API.

## Architecture

The API is ours. Inference engines remain separate open-source projects installed on the same machine:

- CharacterGen — human-specific image-to-3D.
- TripoSR — lightweight image-to-3D fallback.
- UniRig — automatic skeleton + skinning.
- Blender — local post-processing/export/animation preparation.

The service deliberately does not copy third-party source into the RS repository. This keeps upstream updates and license notices separable while giving RS KICKBOXING a stable private API.

## Endpoints

- GET /v1/health
- GET /v1/capabilities
- POST /v1/generate/from-image
- POST /v1/rig/{job_id}
- GET /v1/jobs/{job_id}/download

## Local start

python -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8787

Environment variables:
- CHARACTERGEN_ROOT
- TRIPOSR_ROOT
- UNIRIG_ROOT
- BLENDER_BIN
- RS3D_WORK_DIR

## Important

This initial API layer does not claim that the GPU backends are installed or that final facial blendshapes are solved. /v1/health reports the truth. The final RS production pipeline must additionally perform:
1. mesh cleanup/decimation;
2. humanoid rig validation;
3. five named body animation clips;
4. required RS viseme/facial morph targets;
5. mobile GLB validation;
6. SceneView test on Android.

No paid external API is required by this service itself. Compute still costs electricity and requires suitable local GPU/RAM/storage.
