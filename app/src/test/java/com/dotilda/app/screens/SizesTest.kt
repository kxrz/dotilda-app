package com.dotilda.app.screens

import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/*
 * The same tour as ScreensTest on other Android devices, to see what a screen that never scrolls
 * does with less room (or more). Images go to screenshots/<prefix>.
 */

/** A small phone: 320 × 480 dp. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w320dp-h480dp-port-mdpi")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SmallPhoneTest : ScreensTest() {
    override val prefix = "small/"
}

/** A large phone: 412 × 915 dp. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w412dp-h915dp-port-xxhdpi")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LargePhoneTest : ScreensTest() {
    override val prefix = "large/"
}

/** The Kompakt with the system text size at 130 %. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = KOMPAKT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class LargeTextTest : ScreensTest() {
    override val prefix = "text130/"

    @Before
    fun largerText() {
        RuntimeEnvironment.setFontScale(1.3f)
    }
}
