# JARVIS Hybrid v0.3

Natív Android asszisztens: helyi parancsok + választható online Gemini AI.

## Funkciók
- **Offline:** idő, dátum, köszönés, jegyzetek (`jegyezd meg: ...`, `jegyzeteim`).
- **Online:** szabad szöveges kérdések a Gemini 2.5 Flash modellhez, ha a felhasználó beállította a saját API-kulcsát.
- Magyar beszédfelismerés a készülék szolgáltatásával (internetet igényelhet), magyar TTS ha támogatott.
- Sötét, futurisztikus kezelőfelület és gyorsgombok.
- A kulcs Android Keystore AES-GCM titkosítással tárolódik a telefonon, nincs a GitHub forráskódban.

## Online AI aktiválása
1. Telepítsd az új APK-t.
2. A Google AI Studio felületén hozz létre saját Gemini API-kulcsot: https://aistudio.google.com/apikey
3. Az alkalmazásban válaszd: **ONLINE AI BEÁLLÍTÁSA**, és illeszd be a kulcsot.
4. Kérdezz olyasmit, amit a helyi parancsok nem ismernek, pl. `Magyarázd el a fotoszintézist.`

A Gemini API kvótája, díjazása és elérhetősége a Google aktuális feltételeitől függ. A szabad szöveges kérdések a Google szolgáltatásához kerülnek. **Ne írj be érzékeny személyes adatokat.** A telefonra telepített alkalmazásban tárolt API-kulcs nem egyenértékű a szerveroldali kulcsvédelemmel; nyilvános terjesztéshez saját hitelesített backend ajánlott. A kulcsot nem szabad a repositoryba feltölteni.

## APK építése
GitHub Actions: https://github.com/xxcsabaxx202-alt/JARVIS-Hybrid/actions

A sikeres **Build JARVIS APK** futásnál az **Artifacts** részben található a `jarvis-hybrid-debug-apk` ZIP, benne az `app-debug.apk` fájllal.

## Jelenlegi korlátok
- A beszédfelismerés nem garantáltan offline.
- Nincs folyamatos háttérfigyelés vagy ébresztőszó.
- A Gemini kapcsolat kulcs és internet nélkül nem használható; az offline alapfunkciók ettől függetlenek.
- Nincs többkörös AI-beszélgetés előzményküldéssel.
- A debug APK nem Play Store kiadás.
