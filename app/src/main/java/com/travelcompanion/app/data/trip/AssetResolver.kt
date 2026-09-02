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
    private val exists: (String) -> Boolean = { true },
) {
    private val byId: Map<String, Asset> = assets.associateBy(Asset::id)

    fun asset(assetId: String?): Asset? = assetId?.let(byId::get)

    /**
     * Packaged path relative to the app assets root, e.g. `trip/images/x.jpg`.
     */
    fun packagedPath(assetId: String?): String? =
        asset(assetId)?.let { "$TRIP_ASSET_ROOT/${it.path.trimStart('/')}" }

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

    fun isAvailableOffline(assetId: String?): Boolean = packagedPathIfPresent(assetId) != null

    companion object {
        const val TRIP_ASSET_ROOT = "trip"
        const val ANDROID_ASSET_SCHEME = "file:///android_asset/"
    }
}
