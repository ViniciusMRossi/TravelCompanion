package com.travelcompanion.app.feature.city

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.travelcompanion.app.domain.explore.ExploreCities

/**
 * Explorar's city cursor lives here, and nowhere else.
 *
 * It starts on the city the day is spent in every time the screen opens, and
 * it is **not** the value screens 02, 03, 17 and 20 read. Those ask where the
 * traveller sleeps and answer with the base; this one asks where the day
 * happens. On sixteen of the twenty days the two agree, and a value that
 * usually equals another is not a guard, it is a coincidence (D089) — so the
 * two are computed apart and only this screen may move.
 *
 * `remember` and deliberately not `rememberSaveable`: the bottom bar restores
 * saved state when a tab is re-entered (`restoreState = true`), so a saveable
 * cursor would bring back a city chosen before the traveller walked somewhere
 * else. The cost is that rotating the phone returns to the day's own city,
 * which is the same place the screen opens on anyway.
 */
@Composable
fun rememberExploreCityCursor(cities: ExploreCities): MutableState<String?> =
    remember(cities) { mutableStateOf(cities.openAtCityId) }
