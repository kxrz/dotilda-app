package com.dotilda.app.data

/** How big the dots are, as a share of their cell (in the Galaxy, and on the month's line). */
enum class DotSize(val galaxy: Float, val line: Float) {
    Fine(0.22f, 0.28f),
    Classic(0.3f, 0.36f),
    Bold(0.38f, 0.44f),
}

/** Neat rows, or a night sky where each dot sits a little off its place (always the same place). */
enum class SkyLayout { Rows, NightSky }

data class SkyStyle(val dots: DotSize = DotSize.Classic, val layout: SkyLayout = SkyLayout.Rows)
