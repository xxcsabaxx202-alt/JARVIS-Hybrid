# JARVIS Hybrid v0.7

Natív Android asszisztens: helyi parancsok + választható online Gemini AI.

## Funkciók
- **Offline:** idő, dátum, köszönés, jegyzetek (`jegyezd meg: ...`, `jegyzeteim`).
- **Online:** szabad szöveges kérdések a Gemini 2.5 Flash modellhez, ha a felhasználó beállította a saját API-kulcsát.
- Magyar beszédfelismerés a készülék szolgáltatásával (internetet igényelhet), magyar TTS ha támogatott.
- Sötét, futurisztikus kezelőfelület és gyorsgombok.
- A kulcs Android Keystore AES-GCM titkosítással tárolódik a telefonon, nincs a GitHub forráskódban.

## Új funkciók v0.7
- Ingyenes magyar férfihang: **Magyar RHVoice Imre**. A hangmotor külön telepítendő a [Magyar RHVoice hangok](https://www.rhvoice.hu/hu/voices/) hivatalos oldaláról; nem része az APK-nak.
- A telefon szövegfelolvasási beállításaiban válaszd ki az RHVoice motort, majd a JARVIS Hang beállítása menüben az **Imre magyar férfihang** opciót. Az alkalmazás felismeri az elérhető `Imre` nevű magyar hangot.
- A beszédmotor módosítása után az alkalmazás újrainicializálja a TTS-t visszatéréskor.
- A teljesen új, rétegezett holografikus HUD telemetria-rácsot, több gyűrűt, állapotfüggő színeket és animált hullámsávokat tartalmaz.
- Nem szükséges fizetős beszéd API. Az RHVoice Android-alkalmazás és a hang telepítése külön felhasználói lépés.
- Az Imre hang saját karakterű, **nem azonos a film eredeti szinkronhangjával**.

## Korábbi funkciók v0.6
- Magyar beszédhang kiválasztása a telefon telepített TTS-hangjai közül, magasabb minőségű helyi hangok előnyben részesítésével.
- Mély filmes hangprofil és külön magyar hangválasztó a **JARVIS HANG BEÁLLÍTÁSA** menüben.
- A holografikus vezérlőmag külön animációval jelzi a hallgatást, a gondolkodást és a beszédet.
- Saját, filmszerű megjelenés; nem a film eredeti grafikájának vagy színészi hangjának pontos másolata.

## Korábbi funkciók v0.4
- **Beszélgetési memória:** az utolsó 12 felhasználói/AI-üzenet helyben tárolódik és a következő AI-kérdéshez elküldésre kerül. `Beszélgetési előzmények` megjeleníti; `Töröld a beszélgetést` eltávolítja. A Google a beszélgetési kontextust az AI-válaszhoz megkapja.
- **Telefonvezérlés:** `Nyisd meg a beállításokat`, `Nyisd meg a kamerát`, `Nyisd meg a böngészőt`, `Nyisd meg a térképet`, `Tárcsázd: +3612345678`. A hívást a felhasználó indítja.
- **Beszélgető mód:** a beszédhangos válasz után újra megnyitja a telefon beszédfelismerőjét, amíg a felhasználó ki nem kapcsolja. Csak nyitott alkalmazásban működik, nem ébresztőszavas háttérfigyelés.
- **Animált JARVIS-mag:** finom pulzáló vizuális visszajelzés.
- **Emlékeztető:** az `Emlékeztess` parancs megnyitja a naptár eseménylétrehozó felületét, ahol a felhasználó beállítja az időpontot és menti az eseményt.

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
- Nincs folyamatos háttérfigyelés vagy ébresztőszó; a beszélgető mód a rendszer beszédfelismerő felületét indítja újra.
- A Gemini kapcsolat kulcs és internet nélkül nem használható; az offline alapfunkciók ettől függetlenek.
- A beszélgetési memória legfeljebb 12 üzenetet őriz, egyszerű helyi tárolásban; nem titkosított napló. Ne adj meg érzékeny adatokat.
- A debug APK nem Play Store kiadás.
