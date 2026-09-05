package com.travelcompanion.app.data.trip

/**
 * Resolves stable asset IDs from trip.json into packaged locations.
 *
 * Screens must never build asset paths by hand; they ask for an ID and get
 * back either a resolved reference or null. A missing binary is a normal
 * state during content production (heroes still render their placeholder),
 * not an error the traveller should ever see.
 */
class AssetResolver(
    assets: List<Asset>,
    /**
     * Directory this build's content came out of. It is not decided here: it
     * arrives already resolved from [TripAssetRoot.resolve], so the file that
     * `trip.json` was read from and the files its assets resolve to are the
     * same root by construction (D087).
     */
    private val root: TripAssetRoot = TripAssetRoot.Sample,
    private val exists: (String) -> Boolean = { true },
    /**
     * Bytes a packaged file occupies, or null when this build cannot say.
     * Screen 19 states what is saved on the device and how much room it takes;
     * nothing else asks.
     */
    private val sizeOf: (String) -> Long? = { null },
) {
    private val byId: Map<String, Asset> = assets.associateBy(Asset::id)

    fun asset(assetId: String?): Asset? = assetId?.let(byId::get)

    /**
     * Packaged path relative to the app assets root, e.g. `trip/images/x.jpg`
     * — or `trip-production/images/x.jpg` on a build carrying the real trip.
     */
    fun packagedPath(assetId: String?): String? =
        asset(assetId)?.let { root.pathFor(it.path) }

    /**
     * Packaged path, but only when the binary is actually present in this
     * build. Callers fall back to the approved placeholder when it is null.
     */
    fun packagedPathIfPresent(assetId: String?): String? =
        packagedPath(assetId)?.takeIf(exists)

    /**
     * Android asset URI for a packaged file, or null when the binary is not
     * present in this build.
     */
    fun uri(assetId: String?): String? =
        packagedPathIfPresent(assetId)?.let { "$ANDROID_ASSET_SCHEME$it" }

    /**
     * Media URI for a packaged file, in the `asset:///` form Media3 reads
     * straight out of the APK. Null when the binary is not in this build, so
     * playback can say so instead of failing on a decoder error.
     */
    fun mediaUri(assetId: String?): String? =
        packagedPathIfPresent(assetId)?.let { "$MEDIA_ASSET_SCHEME$it" }

    fun isAvailableOffline(assetId: String?): Boolean = packagedPathIfPresent(assetId) != null

    /** Size on disk of a packaged file, or null when it is not in this build. */
    fun sizeInBytes(assetId: String?): Long? = packagedPathIfPresent(assetId)?.let(sizeOf)

    companion object {
        const val ANDROID_ASSET_SCHEME = "file:///android_asset/"
        const val MEDIA_ASSET_SCHEME = "asset:///"
    }
}
