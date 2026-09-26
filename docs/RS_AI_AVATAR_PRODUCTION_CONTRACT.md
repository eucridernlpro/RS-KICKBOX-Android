# RS KICKBOXING Production Digital Human Contract

This contract is mandatory for the final Sofia and Marcus SceneView assets.

## Required files
- app/src/main/assets/models/rs_ai_sofia.glb
- app/src/main/assets/models/rs_ai_marcus.glb

## Visual canon
Sofia: realistic adult athletic woman, natural skin/eyes/proportions, premium hair, athletic kickboxing trainer physique, black RS sportswear with metallic gold RS branding and restrained cyan accents.

Marcus: realistic adult athletic man, natural skin/eyes, short hair, subtle facial hair, athletic kickboxing trainer physique, black RS sportswear with metallic gold RS branding and restrained cyan accents.

Both belong to the same premium dark RS KICKBOXING studio world. Avoid cartoon, low-poly, toy, superhero, or mobile-game styling.

## Mandatory runtime contract
- GLB 2.0, self-contained.
- Humanoid skeleton/skin.
- PBR materials and embedded textures.
- <= 80,000 triangles per avatar.
- Prefer 1K-2K textures.
- No expensive transparency stacks.
- Animation clips named exactly:
  - idle
  - listening
  - thinking
  - speaking
  - acknowledge
- Facial morph targets named exactly:
  - REST
  - A
  - E
  - I
  - O
  - U
  - FV
  - L
  - MBP
  - WQ
  - BLINK_LEFT
  - BLINK_RIGHT
  - SMILE

## Performance target
- Minimum 30 fps on supported midrange Android devices.
- 60 fps preferred on high-end devices.
- SceneView/Filament must load without native/frame errors.

## Acceptance
Run:
python3 tools/validate_rs_ai_glb.py app/src/main/assets/models/rs_ai_sofia.glb app/src/main/assets/models/rs_ai_marcus.glb

Both must PASS before the app is allowed to claim FINAL 3D.
