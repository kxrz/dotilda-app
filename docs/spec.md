# Dotilda: product spec

A gentle, playful journaling app for the Mudita Kompakt and Android 11+ e-readers, a little sister
of readme.club, meant to be for writing what a calm meditation app is for breathing.
Free and open source (Apache 2.0). Website: [www.dotilda.com](https://www.dotilda.com).

This document is the reference for what the app is and how it behaves. `CLAUDE.md` at the root
of the repository sums up the rules that matter for the code.

## Name and character

**Dotilda** is both the app and the character who guides the user: the ink dot who asks the
question of the day and watches over the sky of dots. The name blends *dot* and *Matilda*.
Her motto, *nulla dies sine linea* ("no day without a line", after Pliny's story of the painter
Apelles drawing at least one line a day), is background only: it never appears in the app.

**Personality:** a kind Victorian governess, a little mischievous, never preachy. Short sentences,
no exclamation marks in a row, never any guilt.

**Her voice**

| Moment | What Dotilda does | Tone |
| --- | --- | --- |
| Question of the day | Asks and signs the question | "Dotilda asks: what made you smile today?" |
| After an answer | Fills the dot, one short word | "Noted. One more dot in your sky." |
| Back after a break | Welcomes without reproach | "There you are. Your sky kept your place." |
| End of a journey | Draws the constellation and names it | "Seven days, seven dots. I'll call this one Slow." |
| Month review | Comments with affection | "A cloudy start, a sunny finish. Rather well done." |

**Her look:** an ink dot with two details, a bun and round glasses, readable at 16 px. The app
icon draws her in outline, like the Kompakt's own icons.

**Her poses.** E-ink has no smooth animation, so Dotilda changes pose in one step, like turning a
page (a single refresh). Four poses: neutral; glasses slid down her nose (she is asking); glasses
askew (another question, a little mischief); a small smile (noted). On the Done screen she pushes
her glasses up and smiles, in one step, never looping.

**Conventions:** "Dotilda" always takes a capital D, like a first name. The breathing module keeps
its Latin name, **Spira** ("breathe"), with a capital S: it is the exercise Dotilda suggests
before writing.

**A hidden game:** poke Dotilda and she changes face, one step per tap (a wink, surprise, a
glance aside, a nap, a smile); poked often enough above a question, she answers back
("That tickles."). Her asides on Today can be closed with their ×; version news fades by itself.

**Speech bubbles:** when Dotilda asks or announces something, her words sit in a light speech
bubble: a fine ink outline, round corners, white inside and a small tail pointing at her. Her
asides on Today never take more than one line.

## Concept and principles

One question a day, a short answer, and a sky of dots that fills up, one dot per day. The user
spends less than two minutes a day in the app: a ritual, not a feed.

1. **One interaction a day.** An appointment at the hour the user chooses, no endless feed.
   "Another question" changes it, at most three times a day. After that, Dotilda simply offers to
   note the inner weather, or to come back tomorrow.
2. **Nothing dies.** Skipping days breaks nothing: the day's place stays a small, quiet dot.
   No punishing streaks, no red counters.
3. **Varied formats.** Eight kinds of questions take turns so that writing stays a game, even on
   days without energy.
4. **Reward looking back.** The real highlights are the reviews (month, season, year) and the
   Mirror questions, where the user rereads themselves.
5. **Offline and private.** No account, nothing sent. Everything stays on the device and can be
   exported at any time. The one network exception is the update check, which sends nothing about
   the user and can be turned off in Settings.
6. **Made for e-ink.** A dot that fills instead of an animation; strong contrast, large type,
   no scrolling.

Spirit: the gentleness and journeys of a meditation app, the tone and design of readme.club, the
feel of paper notebooks.

## Question formats

Eight formats, each with its own input and its own place in the reviews. The picker alternates
formats so that two days in a row are never the same.

| Format | Example | Input | In the reviews |
| --- | --- | --- | --- |
| One line | What made you smile today? | Text, 140 characters max | Quoted in the month review |
| Inner weather | How's the weather inside? | 5 pictograms, sunny to stormy | Weather strip of the month |
| One word | Today in one word? | One word | Word cloud of the month |
| This or that | Tea or coffee kind of day? | 2 buttons | Playful stats at the end of the season |
| Three things | 3 small wins this week | 3 short lines | Listed in the review |
| Photo of the day | Show me your desk | Camera, dithered image | Contact sheet of the month |
| Mirror | What are you looking forward to? | Text; the same question every 30 days | "30 days ago, you wrote..." shown before the input |
| Letter to future me | Write to yourself in 6 months | Long text, sealed | Reopens on the chosen date, like a present |

Picker rules:

- **Heavy days:** if yesterday's inner weather was stormy, offer a light format (This or that,
  One word).
- **Rhythm:** at least one Mirror a month and one Letter a season.
- **Question bank:** more than 200 questions, tagged by format, theme and journey, shipped as JSON
  in the app (`app/src/main/assets/questions.json`), so the user never feels they are going round
  in circles.
- **Changing the question:** at most 3 times a day, never landing on a question already offered
  that day. After that, only the inner weather is offered.

## The three loops

Dots reward showing up, journeys draw constellations, reviews reward looking back.
None of them punishes absence.

**The sky (daily loop)**

- One dot per day, neatly arranged: 12 rows (the months) of 31 columns (the days). The whole year
  fits on one screen, like a year in pixels, but in dots.
- The dot takes the shape of the answer. The inner weather becomes a moon phase (full for sunny, a
  ring for stormy). Photo: a circled dot. Mirror: a double ring. Letter: a sealed diamond that
  opens on its date.
- A day without an entry is a tiny grey dot: a breath in the grid, never a hole or an alert.
- Dots stay free: no line for a complete week or month. Regularity shows in how dense the sky is,
  not in a line that would reward a streak. Not even constellations use lines.
- Dots are spaced so that a run of days never merges into a bar; today is marked by a small caret.
- Years pile up in the **Galaxy**, one page per year; touching a dot reopens that day's entry.

**Journeys (weekly loop)**

- Themed series of 5 to 10 questions, one a day: "7 days to slow down", "Gratitude",
  "Season review", "Unplugged".
- A finished journey becomes a constellation, named after the journey: its days light up as stars
  (four short rays around each dot), with no line between them, and its name shows under the sky.
- One journey at a time; outside a journey, the question of the day comes from the general bank.

**Reviews (long loops)**

- **Month:** a generated page with the weather strip, the word cloud, a quote of oneself and the
  photo contact sheet.
- **Season:** the quarter in dots, This or that stats, and last season's sealed letter reopening.
- **Year:** the whole journal exported as an **EPUB book**, with the year's sky on the cover, to
  read on an e-reader.

Guardrails: no nagging notifications, no rankings, no built-in social sharing. The only reminder
is the daily appointment the user chose.

## Screens

Flat navigation: Today leads everywhere, and every screen goes back to Today in one gesture or
one button.

| Screen | Content | Actions |
| --- | --- | --- |
| Today | The month's line of dots, the question of the day, the current journey | Answer, another question (3 max), open the notebook |
| Answer | The question and its input; for a Mirror, the answer from 30 days ago | Save, breathe with Spira first |
| Done | The day's dot filling in; the constellation if a journey ends | Back to Today |
| Notebook | Entries by month, one page per day, paginated | Read, filter by format or journey |
| Journeys | Available, current and finished journeys | Start a journey |
| Galaxy | Every year in dots, with the journeys' constellations | Page through the years, touch a dot to reread the day |
| Review | Month or season review | Read, export as an image |
| Spira | Breathing minute, timer, stopwatch | Start, set |
| Settings | Reminder time, backup, EPUB export, updates, about | Export, import |

Onboarding: a welcome screen where Dotilda introduces herself in three short promises (reachable
later from Settings > About Dotilda), the choice of the reminder time, then the first question
right away.

## Spira

Spira fills a real gap on the Kompakt, which has no timer or stopwatch out of the box, and serves
as a pause before writing. It lives inside Dotilda; a standalone app may follow if it is used.

- **Breathing minute:** a circle that changes size in steps (breathe in 4 s, out 6 s), with no
  smooth animation: 3 fixed states, e-ink friendly. A light vibration at each change.
- **Timer:** duration set with large +1 / +5 min buttons, an alarm at the end, works with the
  screen off.
- **Stopwatch:** start, stop, laps; the display refreshes every second in fast mode, every minute
  otherwise to save battery.
- **Link to the journal:** from Answer, "breathe first" runs one minute and comes back to the
  question.

## E-ink and Kompakt constraints

Main target: Mudita Kompakt (MuditaOS K, based on AOSP 12, no Google services).
Secondary: Android 11+ e-readers.

- **MMD theme:** use Mudita Mindful Design (Compose, Apache 2.0) for a native Kompakt look;
  ripples off, monochrome palette.
- **No animation:** instant transitions, no scrolling; pagination everywhere, like an e-reader.
- **Drawing:** dots are drawn with Compose Canvas, pure black, no gradients; the sky is redrawn
  once per entry, never animated.
- **Photos:** Floyd-Steinberg dithering to 4 greys on the device, stored already dithered.
- **Refresh:** a full refresh after each saved entry and at each month change in the notebook, to
  clear ghosting.
- **Input:** short questions, wide fields, compatible with the Kompakt keyboard and FUTO.
- **Kompakt screen** (MuditaOS K 1.6.1, API 31): 480 × 800 px, 213 dpi (density ×1.33), which
  gives the app **360 × 564 dp**.
- **Hardware keys:** volume up = previous page (`KEYCODE_VOLUME_UP`, 24), volume down = next page
  (`KEYCODE_VOLUME_DOWN`, 25), only on paginated screens; elsewhere volume keeps its role. The back
  key under the screen sends `KEYCODE_BACK` (4), the standard system back.
- **Battery:** no background service; one daily reminder scheduled with AlarmManager, plus the
  Spira timer when the user starts it.

## Architecture and data

Kotlin, Jetpack Compose with MMD (`com.mudita:MMD`), Room as the single source of truth, minSdk 30.
Application id: `com.dotilda.app`.

- **UI:** Compose + MMD, flat Compose navigation.
- **Data:** Room (SQLite). The question bank and journeys are JSON assets, synced at every launch
  (an app update can fix or add questions). Preferences in DataStore.
- **Photos:** taken with the system camera app (no CameraX, no camera permission), shrunk to the
  Kompakt's width, dithered, then stored in the app's private folder.
- **Reminder:** exact AlarmManager alarm, re-armed after reboot.
- **Export:** a local EPUB 3 (XHTML and images in a zip), and a backup zip (JSON and photos),
  through the Android file picker.

| Table | Main fields | Role |
| --- | --- | --- |
| Question | id, type, text, options, tags, journeyId?, mirrorGroup? | Question bank |
| Entry | id, date, questionId, type, payload (JSON), photoPath? | Answers; the day's first one gives its dot a shape |

Journeys are not tables: they live in `assets/journeys.json`, and their questions are synced into
the Question table with their journeyId. The journey under way, the finished constellations
(journey, name, dates) and the letters already opened live in DataStore and travel in the backup
zip. So far the database schema has not changed since the first public release: no migration.

The JSON payload depends on the format (text, word, weather index, choice, three lines, letter
with its unsealing date), which keeps a single Entry table that is simple to export. The dot's
shape is computed from the day's entry: no cache table. Preferences (reminder time, number of
question changes today) live in DataStore.

## Updates and distribution

Dotilda is **completely free**: no purchase, no subscription, no ads. Every journey, review and
export is included. The code is open under Apache 2.0, like MMD.

**In-app updates** (the same mechanism as the readme.club app): at launch, the app reads
`manifest.json` from the latest GitHub release (version, APK URL, SHA-256, notes from the
CHANGELOG). If a newer version exists, Today mentions it quietly; the update screen shows the
notes, downloads the APK, checks its checksum and opens Android's installer, which asks for
confirmation. Test builds (`com.dotilda.app.debug`) do not update. Releases must be downloadable
without a GitHub account.

**Where to get it:** the APK on GitHub Releases, installed with Mudita Center or Obtainium, and
[www.dotilda.com](https://www.dotilda.com).

## Milestones

Short steps, each one usable on a real Kompakt. Every push builds a debug APK on GitHub Actions.

1. ✅ **Foundation:** Kotlin + Compose + MMD project, Room, question import, a Diagnostics screen
   to read the Kompakt's key codes and resolution.
   Done when the app installs with Mudita Center and shows a question from the bank.
2. ✅ **Ritual:** Today, Answer and Done; One line, One word, Inner weather and This or that;
   the daily reminder.
   Done when a week of real use passes without a bug and the reminder arrives every day on time.
3. ✅ **Notebook and sky:** paginated Notebook, the year's sky and the month's line, dot shapes;
   Three things and Mirror.
   Done when every entry fills the right dot with the right shape, and empty days stay quiet.
4. ✅ **Spira and backup** (0.4.0): the full Spira module, zip export and import, onboarding,
   public release.
   Done when a whole journal is restored on a second device.
5. ✅ **Depth** (0.5.0): journeys and constellations, the month review, Photo of the day with dithering,
   Letter to future me.
   Done when a 7-day journey ends by drawing its constellation.
6. ✅ **Looking back** (0.6.0): season reviews, the yearly EPUB export, dot and sky styles.
   Done when the generated EPUB opens cleanly in KOReader.

A milestone is done when the app installs through Mudita Center, runs on a Kompakt for a full day
of use without a crash, and meets the criteria above.

## Languages

The interface speaks English and Polish, following the phone's language; on a phone in another
language it stays in English. A language needs `res/values-<code>/strings.xml` (with its
`app_locale` tag, which dates and questions follow), and translated `questions-<code>.json` and
`journeys-<code>.json` that keep every id, type and tag.
