# Pleromastisk

En Android-app for [Pleroma](https://pleroma.social)-instanser, med norsk grensesnitt og et
utseende hentet fra Pleroma-temaet
[gaute.space](https://gaute.space/forgejo/gaute/pleromathemes/src/branch/main/gautespace.json):
skarpe hjørner, tynn tan kantlinje, mørke blågrønne flater og et eget bakgrunnsbilde.

Appen er laget for [social.gaute.space](https://social.gaute.space), men skal fungere mot
andre Pleroma-instanser (og i prinsippet andre tjenere med Mastodon-kompatibelt API).

> **Status:** Første versjon. Koden kompilerer og API-kall er kontrollert mot
> social.gaute.space, men appen er ennå ikke systematisk testet på fysiske enheter.
> Forvent rare kanter, og meld gjerne fra om feil.

## Skjermbilder

Her er noen skjermbilder som eksempler på hvordan appen ser ut. Den finnes per nå kun for Android.

![Eksempler](Pleromastisk_screenshots.jpg)

## Funksjoner

- **Innlogging via OAuth** – du skriver inn instansadresse og godkjenner appen på instansens
  egen innloggingsside. Passordet ditt går aldri via appen.
- **Tidslinjer:** Hjem, Lokalt og Fødererte, med pull-to-refresh og automatisk «hent flere».
- **Lokal feed og egne lister** – nedtrekksmenyen «Hjem ▾» i toppen av «Hjem» bytter mellom hjemmetidslinjen, lokal feed og listene dine.
- **Innlegg:** svar, del, favoritt, bokmerke, kopier lenke, åpne i nettleser og slett egne.
- **Lenkeforhåndsvisning** – innlegg med en lenke viser bilde og sidetittel i en boks som er klikkbar. Lenken skjules i teksten når den står alene på sin egen linje. Forhåndsvisningen lages av instansen, ikke av appen.
- **Antall og avsender-app:** små tall viser antall svar, delinger og favoritter ved ikonene, og «via <app>» vises på innlegg der instansen oppgir hvilken app de ble postet fra (typisk lokale innlegg). Innlegg du selv har postet fra Pleromastisk viser alltid «via Pleromastisk».
- **Søk** – egen fane i bunnlinjen. Søk etter brukere (`@navn`, også eksterne `@navn@domene`) og emneknagger (`#emne`). Vanlig tekst søker etter begge deler.
- **Emneknagger i innlegg** – trykk på en #emneknagg for å se innlegg med den (åpnes i appen).
- **@omtaler i innlegg** – trykk på et nevnt brukernavn for å åpne profilen i appen, via instansen du er logget inn på.
- **Profilfelt** – profilens egne felt (metadata) vises i en tabell, med merke for bekreftede lenker.
- **Tråder og profiler**, inkludert følg/slutt å følge.
- **Legg til i liste** – «Lister»-knappen til høyre for «Følg» på andres profiler. Kryss av for listene personen skal være med i, eller lag en ny liste med personen i.
- **Skriv innlegg:** synlighet, innholdsvarsel, bildeopplasting (maks 4), Markdown (hvis
  instansen støtter det) og tegnteller.
- **Språk på innlegg:** velg Nynorsk (nn), Bokmål (nb) eller Engelsk (en) i skriv-dialogen. Appen husker det siste valget. Språket sendes til instansen som språkmerking av innlegget.
- **Alt-tekst:** trykk på et vedlagt bilde i skriv-skjermen for å legge inn en beskrivelse
  (grensen hentes fra instansen). Et ALT-merke på bildet viser om teksten er satt.
- **Rediger innlegg:** «mer»-menyen (⋮) på egne innlegg har «Rediger» når instansen støtter
  det. Tekst, innholdsvarsel, vedlegg og alt-tekst kan endres, ikke synligheten. Redigerte
  innlegg merkes «redigert».
- **Bildevisning med alt-tekst** – trykk på et bilde for å åpne det i full størrelse med alt-teksten under. Klyp eller dobbelttrykk for å zoome. Sveip mellom flere bilder. Bilder med alt-tekst har et ALT-merke.
- **Bilder vises fullt ut** i tidslinjene – uten beskjæring og tilpasset skjermbredden.
- **Forhåndsbilde av video** – innlegg med video viser et stillbilde fra videoen med en play-knapp over. Har ikke instansen laget et eget forhåndsbilde, henter appen et bilde fra selve videoen. Trykk for å spille av videoen i en annen app.
- **Tegngrense fra instansen** – leses fra `configuration.statuses.max_characters`, deretter
  `max_toot_chars`. Oppgir instansen ingenting, brukes **5000**.
- **Emojier fra serveren** – egendefinerte emojier vises i innlegg, navn og varsler, og det
  finnes en søkbar emoji-velger (gruppert etter kategori) i skriv-skjermen.
- **Varsler** (omtaler, delinger, favoritter, følgere, avstemninger, Pleroma-emojireaksjoner m.m.). Filterlinje øverst: **Alle** eller kun **Svar** (svar og omtaler av deg). Under «Alle» vises «X nye varsler» øverst til høyre når det har kommet varsler siden sist du var på varselsiden.
- **Avstemninger** – vis resultater og stem.
- **Navigasjon og «Skriv»-knapp nederst** på skjermen: Hjem · Føderert · Varsler · Søk · Skriv · Profil.
- **Kompakt bunnmeny** – hold inne en vilkårlig knapp i bunnmenyen for å skjule tekstene under ikonene. Hold inne igjen for å få dem tilbake. Appen husker valget.
- **Sveip mellom fanene** – sveip mot venstre eller høyre på Hjem, Føderert, Varsler, Søk og Profil for å gå til neste eller forrige fane.
- **Til toppen** – dobbelttrykk på et tomt område i toppfeltet for å rulle lista helt til toppen (tidslinjer, profiler, tråder, varsler, søk og «Om instansen»).
- **Infoboks** (ⓘ øverst til høyre på «Hjem») med opplysninger om appen og lenke til kildekoden.
- **Om instansen** – i lokal feed er ⓘ byttet ut med en «Om»-knapp. Den viser «Om»-siden til instansen du er logget inn på (instansens egen tekst og tjenestevilkår) inne i appen, med menyene oppe og nede på plass.

### Ikke med (ennå)

Søk i innleggstekst, chat, å sende emoji-reaksjoner, push-varsler, sanntidsstrøm, redigering av profil,
flere kontoer og avspilling av video i appen (åpnes i en annen app).

## Installere

1. Last ned `Pleromastisk.apk` fra siden for utgivelser i dette repoet.
2. Tillat installasjon fra ukjente kilder for appen du åpner filen med.
3. Åpne **Pleromastisk**, skriv inn instansen (f.eks. `social.gaute.space`) og trykk
   **Logg inn**. Godkjenn appen i nettleseren – du sendes automatisk tilbake til appen.

Krever Android 8.0 (API 26) eller nyere.

## Bygge selv

Du trenger JDK 17 eller nyere og Android SDK (plattform 35). Enklest er å åpne mappen i
Android Studio. Fra kommandolinjen:

```bash
echo "sdk.dir=/sti/til/Android/Sdk" > local.properties
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

`./gradlew assembleRelease` gir den lille, optimaliserte APK-en (ca. 2 MB, ikke «debuggable»).
Den er signert med debug-nøkkelen slik at den kan installeres rett på mobilen. Bytt til din
egen signeringsnøkkel i `app/build.gradle.kts` før du publiserer i en app-butikk. Vil du
oppdatere en eksisterende installasjon, må ny APK være signert med samme nøkkel som den
gamle, ellers må du avinstallere først.

## Teknisk

| | |
|---|---|
| Språk / UI | Kotlin, Jetpack Compose (Material 3 med eget tema) |
| Nettverk | OkHttp + kotlinx.serialization |
| Bilder | Coil |
| Navigasjon | Navigation Compose |
| API | Mastodon-kompatibelt API (`/api/v1/...`) som Pleroma implementerer |
| Redirect-skjema | `pleromastisk://oauth` |
| OAuth-scopes | `read write follow push` |
| Pakkenavn | `app.pleromastisk` |

Kildekoden ligger i `app/src/main/java/app/pleromastisk/`:

- `data/` – datamodeller, API-klient og lagring av innlogging
- `vm/` – ViewModels (tidslinjer, tråd, profil, varsler, lister, innlogging)
- `ui/` – skjermer, tema (`Theme.kt` har fargene fra Pleroma-temaet) og komponenter

## Personvern

Appen har ingen sporing eller analyse. Den snakker bare med instansen du velger, og henter
bilder og emojier fra adressene instansen oppgir. Tilgangstoken lagres kun lokalt på
enheten, og «Logg ut» sletter det.

## Lisens

Copyright © 2026 Gaute Holmin

Pleromastisk er fri programvare, lisensiert under
[**GNU General Public License versjon 3 eller senere (GPL-3.0-or-later)**](https://www.gnu.org/licenses/gpl-3.0.html).
Fullstendig tekst ligger i [`LICENSE`](LICENSE).

I praksis betyr det:

- ✅ Du kan bruke, kopiere, endre og dele appen og koden, også kommersielt.
- ✅ Deler du appen eller en endret versjon av den, må du gjøre **kildekoden tilgjengelig
  under samme lisens** (GPL-3.0-or-later), og beholde opphavsrettsmerknadene.
- ❌ Du kan **ikke** lage en lukket versjon av appen eller koden og distribuere den.

Programmet leveres **uten noen form for garanti**, se avsnitt 15 og 16 i lisensen.

**Bakgrunnsbildet** (`app/src/main/res/drawable-nodpi/bakgrunn.jpg`) er hentet fra
[wallhaven.cc](https://wallhaven.cc) og **er ikke dekket av denne lisensen**. Opphavsretten
tilhører kunstneren eller rettighetshaveren, ikke Pleromastisk. Bytt det ut med et bilde du
har rett til å bruke dersom du viderefordeler appen.

Tredjepartsbiblioteker (Jetpack Compose, OkHttp, Coil, kotlinx.serialization m.fl.) er
Apache-2.0-lisensiert og følger sine egne vilkår.
