package com.travelcompanion.app.data.trip

/**
 * Which packaged directory this build reads trip content out of.
 *
 * There are two, and the difference is what Git does with them. `trip/` is the
 * sample package, committed and required by `tools/check_repo.py`.
 * `trip-production/` is the real trip, ignored since the bootstrap, and it
 * holds the traveller's own documents: insurance policies, a vaccination
 * certificate, boarding passes with a name and a seat, vouchers with an
 * address, and booking locators in `trip.json` itself. That directory was
 * declared ignored and never wired to anything, so real content had nowhere to
 * go but the tracked one (D087).
 *
 * The root is **one decision**, taken in [resolve] and carried from there.
 * Content and assets cannot disagree about it, because there is nothing for
 * them to disagree with: both read the [directory] of the same value.
 */
enum class TripAssetRoot(val directory: String) {
    /** The real trip. Present only on a machine that has promoted content. */
    Production("trip-production"),

    /** The sample package this repository ships. Always present. */
    Sample("trip"),
    ;

    /** Where `trip.json` sits for this root, relative to the assets root. */
    val tripJsonPath: String get() = pathFor("trip.json")

    /** A packaged file's path under this root, relative to the assets root. */
    fun pathFor(assetPath: String): String = "$directory/${assetPath.trimStart('/')}"

    companion object {
        /**
         * [Production] when its `trip.json` is packaged in this build,
         * [Sample] otherwise.
         *
         * The presence of the file is the whole signal — no build flag, no
         * variant, no dependency — on the same terms as `google-services.json`
         * deciding whether Firebase is configured (D034).
         */
        fun resolve(exists: (String) -> Boolean): TripAssetRoot =
            if (exists(Production.tripJsonPath)) Production else Sample
    }
}
