package com.travelcompanion.app.data.trip

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/**
 * Which directory the app reads its trip out of (D087).
 *
 * This decides where every screen's content and every document's file come
 * from, and it is invisible once the app is running: a build that loaded the
 * real `trip.json` but resolved its PDFs under `trip/` would show twenty-seven
 * documents and open none of them, and look exactly like a build with no
 * documents packaged at all. So both branches are driven here, and the pair
 * that could disagree — the file opened and the root handed to the resolver —
 * is asserted to be one answer.
 */
class TripAssetRootTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    /** The sample package, read from the module's own assets. */
    private val sampleTripJson: String =
        File("src/main/assets/trip/trip.json").readText()

    /**
     * An asset directory that holds exactly the paths given, each serving the
     * sample package's bytes. Anything else is absent, the way
     * `AssetManager.open` is absent — by failing.
     */
    private fun assetsHolding(vararg paths: String): Pair<(String) -> InputStream, (String) -> Boolean> {
        val present = paths.toSet()
        val open: (String) -> InputStream = { path ->
            if (path in present) {
                ByteArrayInputStream(sampleTripJson.toByteArray())
            } else {
                error("no such packaged file: $path")
            }
        }
        return open to { path -> path in present }
    }

    @Test
    fun `with a production trip packaged, that is the root`() {
        val (open, exists) = assetsHolding("trip-production/trip.json", "trip/trip.json")

        assertEquals(TripAssetRoot.Production, TripAssetRoot.resolve(exists))

        val content = buildContent(json, open, exists, sizeOf = { null })

        assertEquals(
            "trip-production/images/cities/sarajevo.jpg",
            content.assets.packagedPath("img.sarajevo.hero"),
        )
    }

    @Test
    fun `with no production trip packaged, the sample is the root`() {
        val (open, exists) = assetsHolding("trip/trip.json")

        assertEquals(TripAssetRoot.Sample, TripAssetRoot.resolve(exists))

        val content = buildContent(json, open, exists, sizeOf = { null })

        assertEquals(
            "trip/images/cities/sarajevo.jpg",
            content.assets.packagedPath("img.sarajevo.hero"),
        )
    }

    /**
     * The failure this whole type exists to make impossible: reading content
     * from one root and its files from another. Driven by recording which path
     * was actually opened and comparing it to where the assets resolved.
     */
    @Test
    fun `the file opened and the assets resolved share one root`() {
        TripAssetRoot.entries.forEach { root ->
            val packaged = listOfNotNull(
                root.tripJsonPath,
                TripAssetRoot.Sample.tripJsonPath.takeIf { root != TripAssetRoot.Sample },
            )
            val (open, exists) = assetsHolding(*packaged.toTypedArray())
            val opened = mutableListOf<String>()

            val content = buildContent(
                json,
                open = { path -> opened += path; open(path) },
                exists = exists,
                sizeOf = { null },
            )

            assertEquals("one trip.json is opened, not two", 1, opened.size)
            assertEquals(root.tripJsonPath, opened.single())
            assertTrue(
                "assets must resolve under the root the content was read from",
                content.assets.packagedPath("img.sarajevo.hero")!!
                    .startsWith("${root.directory}/"),
            )
        }
    }

    @Test
    fun `a root names its own trip json`() {
        assertEquals("trip-production/trip.json", TripAssetRoot.Production.tripJsonPath)
        assertEquals("trip/trip.json", TripAssetRoot.Sample.tripJsonPath)
    }

    /** Manifest paths are written without a leading slash, but survive one. */
    @Test
    fun `a leading slash in a manifest path does not double up`() {
        assertEquals("trip/images/x.jpg", TripAssetRoot.Sample.pathFor("/images/x.jpg"))
        assertEquals("trip/images/x.jpg", TripAssetRoot.Sample.pathFor("images/x.jpg"))
    }
}
