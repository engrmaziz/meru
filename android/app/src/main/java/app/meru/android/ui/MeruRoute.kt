package app.meru.android.ui

sealed class MeruRoute(val path: String) {
    data object Welcome : MeruRoute("welcome")
    data object Auth : MeruRoute("auth")
    data object Home : MeruRoute("home")
    data object Drive : MeruRoute("drive")
    data object Trips : MeruRoute("trips")
    data object Garage : MeruRoute("garage")
    data object More : MeruRoute("more")
    data object Calibration : MeruRoute("calibration")

    data object TripProcessing : MeruRoute("trip_processing/{tripId}") {
        fun create(tripId: String) = "trip_processing/$tripId"
    }

    data object TripSummary : MeruRoute("trip_summary/{tripId}") {
        fun create(tripId: String) = "trip_summary/$tripId"
    }

    data object TripDetail : MeruRoute("trip_detail/{tripId}") {
        fun create(tripId: String) = "trip_detail/$tripId"
    }

    data object Achievements : MeruRoute("achievements")
    data object Challenges : MeruRoute("challenges")
    data object Leaderboards : MeruRoute("leaderboards")
    data object AdventureMap : MeruRoute("adventure_map")
    data object ShareCard : MeruRoute("share_card")

    data object AddVehicle : MeruRoute("add_vehicle")
    data object VehiclePaywall : MeruRoute("vehicle_paywall")
    data object VehicleTimeline : MeruRoute("vehicle_timeline/{vehicleId}") {
        fun create(vehicleId: String) = "vehicle_timeline/$vehicleId"
    }
    data object AddService : MeruRoute("add_service/{vehicleId}") {
        fun create(vehicleId: String) = "add_service/$vehicleId"
    }

    data object Workshops : MeruRoute("workshops")
    data object MyBookings : MeruRoute("my_bookings")
    data object WorkshopDetail : MeruRoute("workshop/{workshopId}") {
        fun create(workshopId: String) = "workshop/$workshopId"
    }
    data object BookingConfirmed : MeruRoute("booking_confirmed")
}

val mainTabs = listOf(
    MeruRoute.Home,
    MeruRoute.Drive,
    MeruRoute.Trips,
    MeruRoute.Garage,
    MeruRoute.More,
)
