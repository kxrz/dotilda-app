# Changelog

Every user-visible change, and every change to how the app is built or released, is recorded
here in the same commit. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow `appVersion` in `app/build.gradle.kts`.

The release workflow publishes the section of the tagged version as the GitHub release notes
(and in the update manifest the app reads), and refuses to release a version without one.

## [0.6.0] - Unreleased

### Added
- Dotilda speaks Polish: on a phone set to Polish, every screen, the 229 questions and the four
  journeys are in Polish, and so are the dates.
- The season in review, on three pages: the weather of three months, your words and your
  This-or-that tallies, a line of yours and your photos. From the Galaxy (Jan–Mar to Oct–Dec), or
  from Dotilda's note in the first days of January, April, July and October.
- Your year as a book: export it as an EPUB to read on your e-reader, with the year's galaxy on
  the cover, one chapter per month, your photos and your constellations.
- Your sky, your way: dots Fine, Classic or Bold, and the Galaxy in neat rows or as a night sky.

### Changed
- Dates (months, days in the Notebook, letters) follow the app's language instead of always
  being in English.
- Settings now fit on two pages: reminder and updates, then your journal and your sky.

## [0.5.0] - 2026-10-31

### Added
- Journeys: 7 days to slow down, Gratitude, Unplugged and Season review. One question a day;
  finish a journey and its days light up as a constellation in your Galaxy, with its name.
- The month in review, on two pages: Dotilda's word on your weather, the weather as a row of
  moons, your words, a line of yours and your photos. Touch a month's initial in the Galaxy,
  or follow Dotilda's note in the first days of a month.
- Photo of the day, taken with your camera app and kept small, in four greys.
- Letters to your future self, sealed for one to twelve months. Dotilda tells you when one
  is ready to open. A letter comes once a season.
- New dots: a little square for a photo, a diamond for a letter (hollow while sealed).
- Export and import now carry your photos and constellations too.

## [0.4.0] - 2026-10-30

### Added
- Spira: a breathing minute (in for four, out for six, with a light tap at each step), a timer
  that rings even with the screen off, and a stopwatch. "Breathe first" before writing.
- Export your whole journal to a zip file, and import it on another device.
- A gentler start: after the welcome page, choose when Dotilda comes by, then answer your
  first question straight away.

### Changed
- Clearer dots: the inner weather now reads like the real moon (full, gibbous, half, crescent,
  new), dots have room around them so a run of days never turns into a bar, and today is marked
  by a small caret instead of an empty ring.
- An airier welcome page: three short promises, each led by a dot.
- In the Galaxy, the buttons name the year they lead to.
- The app is much lighter to download: about 3 MB instead of 22 MB (code and resources are now shrunk).

## [0.3.0] - 2026-10-30

### Added
- One question a day, signed by Dotilda, from a bank of 229 questions.
- Answer in a line, a word, three short lines, a tap between two choices, or the inner weather.
- Change the question up to 3 times a day, then just tell the weather inside.
- Mirror questions come back every 30 days and show what you wrote last time.
- Each answer fills the day's dot. Read your days in the Notebook, see the year in the Galaxy.
- The volume buttons turn the pages of the Notebook and the Galaxy.
- A daily reminder at the time of your choice.
- Dotilda changes pose: peering, curious, pleased.
- Dotilda tells you herself when a new version is ready, and installs it from within the app.
  Nothing about you is ever sent.
