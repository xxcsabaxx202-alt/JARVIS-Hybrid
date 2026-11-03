# JARVIS Hybrid v0.1

Magyar nyelvű, natív Android asszisztens prototípus.

## Jelenlegi funkciók
- Szöveges parancsok: idő, dátum, köszönés.
- Helyi jegyzetek: `jegyezd meg: ...`, `jegyzeteim`.
- Android beszédfelismerő indítása mikrofon gombbal (a telefon szolgáltatásától függ; internetet igényelhet).
- Magyar szövegfelolvasás, ha a készüléken elérhető magyar TTS-hang.

**Még nincs online AI-modell, ébresztőszó, háttérben futó figyelés vagy telefonvezérlés.** Ne tölts fel API-kulcsokat a repositoryba.

## APK fordítása GitHubon
1. Nyisd meg a [GitHub Actions](https://github.com/xxcsabaxx202-alt/JARVIS-Hybrid/actions) oldalt.
2. Válaszd a **Build JARVIS APK** munkafolyamatot.
3. Kattints a **Run workflow** gombra (ha elérhető), vagy ellenőrizd a legutóbbi push által indított futást.
4. Sikeres futás után az **Artifacts** résznél töltsd le a `jarvis-hybrid-debug-apk` csomagot.
5. A ZIP-ben található `app-debug.apk` telepíthető Androidra, a készülék telepítési engedélyeinek megfelelően.

A GitHub Actions workflow Gradle 8.9-et, JDK 17-et és Android SDK-t használ. A build még nincs igazoltan sikeresnek ellenőrizve. Ez tesztelésre szánt debug APK, nem Play Áruház kiadás.

## Forráskód
- `app/src/main/java/com/jarvis/hybrid/MainActivity.kt`: képernyő, hangvezérlés, parancsok.
- `app/src/main/AndroidManifest.xml`: Android konfiguráció.
- `.github/workflows/android-apk.yml`: automatizált APK-fordítás.

## Következő verzió
Biztonságos backendhez kapcsolódó online AI, jobb kezelőfelület és megbízhatóbb helyi adattárolás.
