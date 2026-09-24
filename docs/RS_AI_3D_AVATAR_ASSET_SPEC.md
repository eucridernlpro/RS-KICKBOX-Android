# RS KICKBOXING — Sofia / Marcus Production 3D Asset Spec

## Goal
Replace the procedural fallback with two production-ready, rigged, textured humanoid GLB avatars that run inside the SceneView/Filament bridge.

## Required file names
- app/src/main/assets/models/rs_ai_sofia.glb
- app/src/main/assets/models/rs_ai_marcus.glb

## Technical target
- Format: GLB 2.0
- Humanoid skeleton
- PBR materials
- Prefer <= 80k triangles per avatar for mobile
- Prefer 1K–2K textures
- No external texture files: embed textures in GLB
- No transparent hair cards unless tested on Android/Filament
- Neutral standing pose
- Centered origin
- Feet near Y=0
- Forward axis consistent between both avatars
- Real-world scale approximately 1.65–1.90 m
- Keep head/face topology clean enough for close mobile framing

## Animation expectation
Preferred embedded clips:
1. idle
2. listening
3. thinking
4. speaking / talking
5. subtle acknowledge / nod

The current bridge can safely render a GLB without these clips. Missing clips fall back to model idle / procedural state treatment.

## Facial target
Best case:
- blend shapes / morph targets for mouth open, smile, blink and basic visemes
- separate eye materials or geometry
- natural eyelid topology

Fallback:
- body/head animation only while the existing TTS amplitude layer controls visual speech feedback.

## Sofia design prompt
Professional adult female virtual kickboxing coach, realistic human proportions, confident but approachable, premium black athletic training suit with subtle metallic gold RS-style accents and restrained cyan holographic light details, clean modern hair, athletic build, neutral standing pose, hands relaxed near guard position, realistic face, high-end sports technology aesthetic, no weapons, no text printed on skin or face, symmetrical full-body character, game-ready PBR materials, dark futuristic coaching studio aesthetic.

## Marcus design prompt
Professional adult male virtual kickboxing coach, realistic human proportions, calm confident expression, premium black athletic training suit with subtle metallic gold RS-style accents and restrained cyan holographic light details, athletic kickboxing build, neutral standing pose, hands relaxed near guard position, realistic face, high-end sports technology aesthetic, no weapons, no text printed on skin or face, symmetrical full-body character, game-ready PBR materials, dark futuristic coaching studio aesthetic.

## Production acceptance
Before replacing fallback:
- GLB loads on Android without crash
- full body is visible at default camera
- no inverted normals
- no missing textures
- no black/white material corruption
- no huge memory spike
- model remains smooth during phone-parallax movement
- Sofia and Marcus are visually distinct
- both avatars remain readable behind holographic chat overlays
- idle animation loops cleanly
- speaking state does not restart animation every frame
- no visible clipping through clothing
