# JARVIS on-device AI roadmap (v1.3)

Goal: run an open GGUF language model entirely on Android using llama.cpp, without a paid API or a remote inference service.

## Implemented in v1.3
- Android Storage Access Framework GGUF file picker.
- Background copy to private app storage with GGUF magic header validation.
- Offline-only AI mode enabled by default. In this mode JARVIS does not send free-form questions to Gemini.
- Explicit optional switch back to the legacy online Gemini mode.
- Offline battery, diagnostics, date/time and the existing Open-Meteo weather feature remain separate from local LLM inference.

## Not implemented yet
- Native llama.cpp library and JNI bridge.
- Token streaming, context-window management, cancellation, memory-budget handling.
- Local speech recognition: the current Android speech recognition intent may use a network service, depending on the installed recognizer.
- Bundled Hungarian male neural TTS model. The installed Android TTS engine is still used.
- Build and on-device performance validation for GGUF inference.

## Planned implementation
1. Pin a specific upstream llama.cpp release, vendor/build its Android ARM64 native library using CMake/NDK, and expose model load, token generation and unload through JNI.
2. Add a background inference worker with strict memory caps, model-size checks and cancellation.
3. Test a small multilingual instruction-tuned GGUF model (e.g. 1B-3B class, appropriately quantized) on the target phone; verify Hungarian quality and RAM usage.
4. Add on-device ASR and optional Hungarian male TTS where licensing and availability allow.
5. Test release builds and full voice-to-response flows on physical Android devices.

## Important
Importing a GGUF file is not equivalent to running it. The local inference engine must be integrated and tested before this application can answer arbitrary questions offline. The Open-Meteo weather request still requires an internet connection. Model licenses vary; users should only import models they are permitted to use.
