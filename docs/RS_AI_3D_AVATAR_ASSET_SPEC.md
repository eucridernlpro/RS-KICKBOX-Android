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


## Locked visual canon — September 2026
Sofia and Marcus must match the approved cinematic digital-human references already bundled in the Android app.
The references define identity, wardrobe, body proportions, grooming, premium RS branding, and the black / metallic-gold / restrained-cyan visual language.

### Sofia
- Adult athletic female digital human.
- Warm, confident and approachable expression.
- Natural skin detail, realistic eyes, realistic hair and human facial proportions.
- Premium black RS athletic outfit with metallic-gold RS crown/logo details and restrained cyan accents.
- Fit kickboxing-coach physique without exaggerated body proportions.

### Marcus
- Adult athletic male digital human.
- Calm, confident and approachable expression.
- Natural skin detail, realistic eyes, realistic short hair and subtle facial hair.
- Premium black RS athletic outfit with metallic-gold RS crown/logo details and restrained cyan accents.
- Athletic kickboxing-coach physique without superhero proportions.

### Shared identity rules
- Never cartoon, toy-like or low-poly in presentation.
- Sofia and Marcus must read as two distinct real people belonging to the same RS KICKBOXING brand family.
- RS branding must be tasteful and physically believable on clothing.
- No generated text baked into skin, face or facial textures.
- Keep a neutral hero stance suitable for idle, listening, thinking and speaking animation blends.

## Mandatory facial rig / lip-sync contract
Production GLBs should expose morph targets or equivalent controls that can be mapped to:
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

The Android runtime already defines the matching core viseme set in V193DigitalHumanRuntime.kt.
If an asset uses ARKit-style names, provide a deterministic mapping into this contract.

## Animation naming contract
Embedded clips should use these exact logical names where possible:
- idle
- listening
- thinking
- speaking
- acknowledge

Idle/listening/thinking/speaking must loop cleanly. Acknowledge may be one-shot.

## Mobile quality budget
Target:
- 30 fps minimum on supported mid-range phones during AI speech.
- 60 fps preferred on high-end phones.
- <= 80k triangles preferred per assistant.
- 1K–2K PBR textures preferred; use 4K only when profiling proves the memory cost is acceptable.
- Embedded textures in GLB.
- Avoid expensive transparency stacks in hair.
- Keep draw calls/material slots conservative.
- No visible texture pop, black materials, missing normals or clipping during default hero framing.

## Voice / dubbing identity contract
- Sofia is always female until the user explicitly selects Marcus/male.
- Marcus is always male until the user explicitly selects Sofia/female.
- Language switching changes language, not persona.
- Voice-style switching changes delivery style, not persona gender.
- Premium cloud styles currently supported by the app contract: natural, calm, energetic, soft, direct, deep.
- Device-TTS fallback must reject obviously opposite-gender labelled voices when possible.
- Any stale audio from the previous persona/language must be cancelled before a new response starts.

## Lip-sync acceptance
- Mouth motion starts only with the active utterance.
- Switching Sofia/Marcus cancels old speech and old lip-sync state.
- Switching language cancels old speech and old lip-sync state.
- Mouth returns to REST after speech.
- Audio and facial animation must not visibly drift over a normal mobile response.
- Blinks and idle breathing continue naturally without fighting facial speech animation.
- Speaking animation must not restart on every rendered frame.
