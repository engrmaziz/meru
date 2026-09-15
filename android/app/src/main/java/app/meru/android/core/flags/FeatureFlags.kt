package app.meru.android.core.flags

import app.meru.android.core.network.MeruApi
import app.meru.android.core.network.PublicFlagsDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Remote-backed flags (Phase 10). Defaults keep soft-launch surfaces on if offline.
 */
@Singleton
class FeatureFlags @Inject constructor(
    private val api: MeruApi,
) {
    private val _flags = MutableStateFlow(MeruFlags())
    val flags: StateFlow<MeruFlags> = _flags.asStateFlow()

    suspend fun refresh() {
        runCatching {
            val remote = api.publicFlags()
            _flags.value = MeruFlags.fromDto(remote)
        }
    }
}

data class MeruFlags(
    val softLaunchCityName: String = "Lahore",
    val softLaunchCityId: String = "pk-pb-lhr",
    val s2Leaderboards: Boolean = true,
    val s3Garage: Boolean = true,
    val s4Marketplace: Boolean = true,
    val ghostDriver: Boolean = true,
    val challengesEnabled: Boolean = true,
    val accountDeletionEnabled: Boolean = true,
    val bookingsEnabled: Boolean = true,
    val integrityCompetitiveMin: Int = 75,
) {
    companion object {
        fun fromDto(d: PublicFlagsDto) = MeruFlags(
            softLaunchCityName = d.softLaunchCityName,
            softLaunchCityId = d.softLaunchCityId,
            s2Leaderboards = d.s2Leaderboards,
            s3Garage = d.s3Garage,
            s4Marketplace = d.s4Marketplace,
            ghostDriver = d.ghostDriver,
            challengesEnabled = d.challengesEnabled,
            accountDeletionEnabled = d.accountDeletionEnabled,
            bookingsEnabled = d.bookingsEnabled,
            integrityCompetitiveMin = d.integrityCompetitiveMin,
        )
    }
}
