# RS KICKBOXING — AI Production Deployment Checklist

## Supabase Edge Functions
Required production functions:
- ai-coach-chat
- ai-voice-speech
- analyze-technique (existing vision path)

## Required Supabase secrets
- OPENAI_API_KEY
- optional RS_AI_MODEL_BASIC (default gpt-6-luna)
- optional RS_AI_MODEL_PRO (default gpt-6-sol)
- optional RS_AI_MODEL_ELITE (default gpt-6-astra)
- optional RS_AI_TTS_MODEL (default gpt-4o-mini-tts)

## Deployment order
1. Confirm OPENAI_API_KEY exists in Supabase Edge Function secrets.
2. Deploy ai-coach-chat.
3. Deploy ai-voice-speech.
4. Keep analyze-technique deployed.
5. Test authenticated student and trainer calls.
6. Confirm BASIC receives device voice fallback when cloud speech endpoint returns plan restriction.
7. Confirm PRO / ELITE / trainer receives cloud voice.
8. Confirm language_code and avatar are passed through exactly.

## Android fallback guarantees
The APK must remain usable when:
- Supabase is temporarily unavailable
- OpenAI TTS fails
- ai-voice-speech is not deployed yet
- a device has no exact locale voice
- the rigged GLB files are missing

Fallback order:
premium cloud voice -> exact-locale Android TTS -> resume listening without deadlock.

## Real 3D assets
Expected paths:
- app/src/main/assets/models/rs_ai_sofia.glb
- app/src/main/assets/models/rs_ai_marcus.glb

If absent, the procedural OpenGL renderer remains active and diagnostics explicitly report PROCEDURAL FALLBACK.

## Final acceptance before test APK
- CI build success
- APK signature/package verification success
- no compile warnings promoted to fatal
- silent wake model installs and can fall back
- wake route survives biometric flow
- Sofia/Marcus switching cancels stale speech
- language change cancels stale speech
- premium voice failure falls back to device TTS
- AI route remains full-screen and swipe navigation remains active
- 3D model remains visible under holographic chat
- diagnostics never expose API keys or passwords
