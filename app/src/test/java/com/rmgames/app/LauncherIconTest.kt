package com.rmgames.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Checks the adaptive launcher icon and splash resources (plan 09 PR 14) on the JVM.
 * Paths are relative to the module dir (Gradle's working dir for unit tests).
 */
class LauncherIconTest {

    private val res = File("src/main/res")
    private val android = "http://schemas.android.com/apk/res/android"

    private fun parse(path: String): Element {
        val file = File(res, path)
        assertTrue("missing $path", file.isFile)
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        return factory.newDocumentBuilder().parse(file).documentElement
    }

    private fun Element.child(name: String): Element? =
        (0 until childNodes.length).map { childNodes.item(it) }
            .filterIsInstance<Element>()
            .firstOrNull { it.tagName == name }

    private fun paths(drawable: String): List<Element> {
        val root = parse("drawable/$drawable.xml")
        assertEquals("vector", root.tagName)
        assertEquals("108dp", root.getAttributeNS(android, "width"))
        assertEquals("108", root.getAttributeNS(android, "viewportWidth"))
        val nodes = root.getElementsByTagName("path")
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun fills(drawable: String) =
        paths(drawable).map { it.getAttributeNS(android, "fillColor").uppercase() }

    @Test
    fun `adaptive icon and round icon have background, foreground and monochrome layers`() {
        val layers = mapOf(
            "background" to "@drawable/ic_launcher_background",
            "foreground" to "@drawable/ic_launcher_foreground",
            "monochrome" to "@drawable/ic_launcher_monochrome",
        )
        for (name in listOf("ic_launcher", "ic_launcher_round")) {
            val root = parse("mipmap-anydpi-v26/$name.xml")
            assertEquals("adaptive-icon", root.tagName)
            layers.forEach { (layer, drawable) ->
                val el = root.child(layer)
                assertTrue("$name has no $layer", el != null)
                assertEquals(drawable, el!!.getAttributeNS(android, "drawable"))
            }
        }
    }

    @Test
    fun `background is solid deep plum`() {
        assertEquals(listOf("#FF17121F"), fills("ic_launcher_background"))
    }

    @Test
    fun `foreground uses only the Direction A palette`() {
        // Dark outline + ledge, hot-pink button face, light play triangle — in draw order.
        assertEquals(listOf("#FF07050A", "#FFFF5D8F", "#FFF6F1FA"), fills("ic_launcher_foreground"))
    }

    @Test
    fun `monochrome layer is a single colour silhouette`() {
        assertEquals(1, fills("ic_launcher_monochrome").toSet().size)
    }

    @Test
    fun `mark stays inside the 66dp safe zone`() {
        for (drawable in listOf("ic_launcher_foreground", "ic_launcher_monochrome")) {
            val points = paths(drawable).flatMap { coordinates(it.getAttributeNS(android, "pathData")) }
            assertTrue(drawable, points.isNotEmpty())
            points.forEach { assertTrue("$drawable coordinate $it outside 21..87", it in 21.0..87.0) }
        }
    }

    @Test
    fun `manifest uses the launcher icons and the starting theme`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("""android:icon="@mipmap/ic_launcher""""))
        assertTrue(manifest.contains("""android:roundIcon="@mipmap/ic_launcher_round""""))
        assertTrue(manifest.contains("""android:theme="@style/Theme.RmGames.Starting""""))
    }

    @Test
    fun `starting theme shows the icon on the app background and hands over to Theme_RmGames`() {
        for (dir in listOf("values", "values-night")) {
            val text = File(res, "$dir/themes.xml").readText()
            assertTrue(dir, text.contains("""<style name="Theme.RmGames.Starting" parent="Theme.SplashScreen">"""))
            assertTrue(dir, text.contains("""<item name="windowSplashScreenBackground">@color/rm_background</item>"""))
            assertTrue(dir, text.contains("""<item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>"""))
            assertTrue(dir, text.contains("""<item name="postSplashScreenTheme">@style/Theme.RmGames</item>"""))
        }
    }

    /**
     * Absolute end-point coordinates of a path made only of M/L/H/V/A/Z commands (arc radii
     * and flags skipped). Anything else fails, so the safe-zone check cannot silently pass.
     */
    private fun coordinates(pathData: String): List<Double> {
        val tokens = Regex("""[A-Za-z]|-?\d*\.?\d+""").findAll(pathData).map { it.value }.toList()
        val out = mutableListOf<Double>()
        var cmd = ""
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            if (t[0].isLetter()) {
                cmd = t
                i++
                continue
            }
            when (cmd) {
                "M", "L" -> { out += tokens[i].toDouble(); out += tokens[i + 1].toDouble(); i += 2 }
                "H", "V" -> { out += tokens[i].toDouble(); i += 1 }
                "A" -> { out += tokens[i + 5].toDouble(); out += tokens[i + 6].toDouble(); i += 7 }
                else -> error("unsupported path command '$cmd' in $pathData")
            }
        }
        return out
    }
}
