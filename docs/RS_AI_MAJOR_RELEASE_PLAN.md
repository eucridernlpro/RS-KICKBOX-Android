# RS KICKBOXING — AI Major Release Plan

## Release goal
Make Sofia / Marcus a platform-level assistant rather than a chat-only feature.

## Non-negotiable quality gates
- Sofia never intentionally selects a male-labelled voice when a female-labelled exact-locale voice is available.
- Marcus never intentionally selects a female-labelled voice when a male-labelled exact-locale voice is available.
- New AI output, TTS, recognition and status copy stay in the active AI language.
- Simple voice commands execute without cluttering chat history.
- Wake listening is silent.
- Wake from background restores the requested route; expired trusted sessions require biometric/device credential when available.
- AI route always renders in immersive mode.
- Major preview is released only after compile, signature verification, route audit, language audit, voice audit and visual QC.

## Architecture
1. On-device wake-word engine for low-power silent detection.
2. Android foreground service owns wake lifecycle and can survive the app task being minimized.
3. Android STT / online AI handles post-wake commands and conversation.
4. Supabase Edge Function protects the OpenAI API key and handles AI requests.
5. Real-time OpenGL assistant renderer owns Sofia / Marcus visual state.
6. App-wide command router resolves navigation and platform actions.

## Voice interaction modes
### Sleeping
Only the wake engine listens.

### Awake command mode
Short commands execute immediately:
- open route
- music controls
- minimize
- close task
- logout
- language/avatar switch

These responses do not create normal AI chat history.

### Conversation mode
Natural conversation continues without requiring the AI page. Longer explanations can be persisted when useful.

### Immersive AI room
Used when the user explicitly opens Sofia / Marcus, asks for detailed coaching, technique analysis, or rich visual guidance.

## Membership feature expansion proposal

### RS BASIC
- Standard Sofia / Marcus assistant
- App navigation by voice
- Basic kickboxing Q&A
- Basic page explanations
- Standard local TTS fallback
- Limited daily AI usage
- Standard AI room

### RS PRO
Everything in BASIC plus:
- richer AI coaching
- technique/combo breakdowns
- image/video-assisted coaching where enabled
- personal training suggestions
- saved AI coaching notes
- deeper progress explanations
- advanced voice control across RS features
- enhanced AI room effects and selectable installed voice profiles
- higher daily AI allowance

### RS ELITE
Everything in PRO plus:
- highest-quality AI model tier when enabled by owner
- priority/expanded AI allowance
- advanced video technique analysis
- personalized training-plan generation using trainer-approved context
- longitudinal progress insights
- premium real-time voice mode when backend is enabled
- richer 3D assistant room, additional avatar/environment customization
- advanced trainer/student AI workflows

## Owner / Trainer
- AI policy controls
- AI usage/quota overview
- choose which AI capabilities each membership plan receives
- enable/disable video analysis, wake voice, advanced coaching and premium voice
- trainer-approved reference content remains authoritative context
- audit/status panel for AI backend availability and device voice capability

## Future premium integrations
### Wake word
Preferred: Picovoice Porcupine with custom RS wake phrases.
Reason: dedicated on-device wake-word detection avoids repeated Android SpeechRecognizer restart tones and improves background reliability.

### Natural realtime voice
Existing OpenAI backend can later be extended to an OpenAI Realtime voice session while preserving Android TTS as fallback.

## Visual QC checklist
- true real-time renderer visible; no fake video/GIF dependency in AI room
- face/body proportions consistent on Sofia / Marcus
- no chat panels hide the model unnecessarily
- chat glass contrast remains readable over light/dark areas
- phone tilt creates subtle room depth, never nausea-inducing movement
- listening/thinking/speaking colors and animations are consistent
- small-screen layout does not crop avatar controls or composer

## Critic pass before major APK
1. Functional critic: identify broken routes/actions and state races.
2. Language critic: test all 9 supported languages for new output paths.
3. Voice critic: inspect exact locale and avatar/voice lock.
4. UX critic: remove duplicate controls and noisy feedback.
5. Visual critic pass 1: hierarchy, alignment, spacing, clipping.
6. Visual critic pass 2: themes, small phone, dark overlays, 3D visibility.
7. Build/signature/package verification.
8. Physical-device test request only after all automated gates pass.
