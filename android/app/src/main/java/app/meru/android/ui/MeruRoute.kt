package app.meru.android.ui

sealed class MeruRoute(val path: String) {
    data object Welcome : MeruRoute("welcome")
    data object Auth : MeruRoute("auth")
    data object Home : MeruRoute("home")
    data object Drive : MeruRoute("drive")
    data object Trips : MeruRoute("trips")
    data object Garage : MeruRoute("garage")
    data object More : MeruRoute("more")
}

val mainTabs = listOf(
    MeruRoute.Home,
    MeruRoute.Drive,
    MeruRoute.Trips,
    MeruRoute.Garage,
    MeruRoute.More,
)
