package app.meru.android.core.flags

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase 1 stub — remote flags land later.
 */
@Singleton
class FeatureFlags @Inject constructor() {
    private val _flags = MutableStateFlow(
        MeruFlags(
            s2Leaderboards = false,
            s3Garage = true,
            s4Marketplace = false,
            ghostDriver = false,
        ),
    )
    val flags: StateFlow<MeruFlags> = _flags.asStateFlow()
}

data class MeruFlags(
    val s2Leaderboards: Boolean,
    val s3Garage: Boolean,
    val s4Marketplace: Boolean,
    val ghostDriver: Boolean,
)
