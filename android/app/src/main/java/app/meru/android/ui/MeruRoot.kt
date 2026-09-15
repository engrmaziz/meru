package app.meru.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.meru.android.core.datastore.SessionStore
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruPanel
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruVoid
import app.meru.android.engine.drive.DrivingMode
import app.meru.android.feature.arena.AdventureMapScreen
import app.meru.android.feature.arena.LeaderboardsScreen
import app.meru.android.feature.arena.ShareCardScreen
import app.meru.android.feature.auth.AuthScreen
import app.meru.android.feature.calibration.CalibrationScreen
import app.meru.android.feature.drive.DriveReadyScreen
import app.meru.android.feature.garage.GarageStubScreen
import app.meru.android.feature.home.HomeScreen
import app.meru.android.feature.more.MoreScreen
import app.meru.android.feature.progression.AchievementsScreen
import app.meru.android.feature.progression.ChallengesScreen
import app.meru.android.feature.trips.TripDetailScreen
import app.meru.android.feature.trips.TripProcessingScreen
import app.meru.android.feature.trips.TripSummaryScreen
import app.meru.android.feature.trips.TripsListScreen
import app.meru.android.feature.welcome.WelcomeScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class RootViewModel @Inject constructor(
    sessionStore: SessionStore,
    drivingMode: DrivingMode,
) : ViewModel() {
    val loggedIn = sessionStore.session
        .map { it.isLoggedIn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val driving = drivingMode.active
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}

@Composable
fun MeruRoot(
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val loggedIn by rootViewModel.loggedIn.collectAsState()
    when (loggedIn) {
        null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MeruVoid),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MeruTeal)
            }
        }
        false -> AuthGraph()
        true -> MainGraph(rootViewModel)
    }
}

@Composable
private fun AuthGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = MeruRoute.Welcome.path) {
        composable(MeruRoute.Welcome.path) {
            WelcomeScreen(
                onCreateAccount = { navController.navigate(MeruRoute.Auth.path) },
                onSignIn = { navController.navigate(MeruRoute.Auth.path) },
            )
        }
        composable(MeruRoute.Auth.path) {
            AuthScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MainGraph(rootViewModel: RootViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val driving by rootViewModel.driving.collectAsState()
    val hideBottomBar = current?.startsWith("trip_") == true ||
        current == MeruRoute.Calibration.path ||
        current == MeruRoute.Achievements.path ||
        current == MeruRoute.Challenges.path ||
        current == MeruRoute.Leaderboards.path ||
        current == MeruRoute.AdventureMap.path ||
        current == MeruRoute.ShareCard.path

    Scaffold(
        containerColor = MeruVoid,
        bottomBar = {
            if (!hideBottomBar) {
                NavigationBar(containerColor = MeruPanel) {
                    mainTabs.forEach { route ->
                        val selected = current == route.path
                        val locked = driving && route == MeruRoute.More
                        NavigationBarItem(
                            selected = selected,
                            enabled = !locked,
                            onClick = {
                                navController.navigate(route.path) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = when (route) {
                                        MeruRoute.Home -> Icons.Outlined.Home
                                        MeruRoute.Drive -> Icons.Outlined.DirectionsCar
                                        MeruRoute.Trips -> Icons.Outlined.Route
                                        MeruRoute.Garage -> Icons.Outlined.Garage
                                        else -> Icons.Outlined.MoreHoriz
                                    },
                                    contentDescription = route.path,
                                )
                            },
                            label = {
                                Text(
                                    text = when (route) {
                                        MeruRoute.Home -> "Home"
                                        MeruRoute.Drive -> "Drive"
                                        MeruRoute.Trips -> "Trips"
                                        MeruRoute.Garage -> "Garage"
                                        else -> if (locked) "Locked" else "More"
                                    },
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MeruTeal,
                                selectedTextColor = MeruTeal,
                                unselectedIconColor = MeruMuted,
                                unselectedTextColor = MeruMuted,
                                disabledIconColor = MeruMuted.copy(alpha = 0.4f),
                                disabledTextColor = MeruMuted.copy(alpha = 0.4f),
                                indicatorColor = MeruVoid,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = MeruRoute.Home.path,
            modifier = Modifier.padding(padding),
        ) {
            composable(MeruRoute.Home.path) {
                HomeScreen(
                    onOpenAchievements = { navController.navigate(MeruRoute.Achievements.path) },
                    onOpenChallenges = { navController.navigate(MeruRoute.Challenges.path) },
                    onOpenLeaderboards = {
                        if (!driving) navController.navigate(MeruRoute.Leaderboards.path)
                    },
                    onOpenAdventureMap = {
                        if (!driving) navController.navigate(MeruRoute.AdventureMap.path)
                    },
                    driving = driving,
                )
            }
            composable(MeruRoute.Drive.path) {
                DriveReadyScreen(
                    onOpenCalibration = { navController.navigate(MeruRoute.Calibration.path) },
                    onTripEnded = { tripId ->
                        navController.navigate(MeruRoute.TripProcessing.create(tripId))
                    },
                )
            }
            composable(MeruRoute.Trips.path) {
                TripsListScreen(
                    onOpenTrip = { id -> navController.navigate(MeruRoute.TripDetail.create(id)) },
                )
            }
            composable(MeruRoute.Garage.path) { GarageStubScreen() }
            composable(MeruRoute.More.path) {
                MoreScreen(
                    onOpenCalibration = { navController.navigate(MeruRoute.Calibration.path) },
                    onOpenAchievements = { navController.navigate(MeruRoute.Achievements.path) },
                    onOpenChallenges = { navController.navigate(MeruRoute.Challenges.path) },
                    onOpenLeaderboards = {
                        if (!driving) navController.navigate(MeruRoute.Leaderboards.path)
                    },
                    onOpenAdventureMap = {
                        if (!driving) navController.navigate(MeruRoute.AdventureMap.path)
                    },
                    driving = driving,
                )
            }
            composable(MeruRoute.Calibration.path) {
                CalibrationScreen(onBack = { navController.popBackStack() })
            }
            composable(MeruRoute.Achievements.path) {
                AchievementsScreen(onBack = { navController.popBackStack() })
            }
            composable(MeruRoute.Challenges.path) {
                ChallengesScreen(onBack = { navController.popBackStack() })
            }
            composable(MeruRoute.Leaderboards.path) {
                if (driving) {
                    LockedBoardsPlaceholder(onBack = { navController.popBackStack() })
                } else {
                    LeaderboardsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAdventureMap = { navController.navigate(MeruRoute.AdventureMap.path) },
                        onOpenShare = { navController.navigate(MeruRoute.ShareCard.path) },
                    )
                }
            }
            composable(MeruRoute.AdventureMap.path) {
                if (driving) {
                    LockedBoardsPlaceholder(onBack = { navController.popBackStack() })
                } else {
                    AdventureMapScreen(onBack = { navController.popBackStack() })
                }
            }
            composable(MeruRoute.ShareCard.path) {
                ShareCardScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = MeruRoute.TripProcessing.path,
                arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
            ) { entry ->
                val tripId = entry.arguments?.getString("tripId") ?: return@composable
                TripProcessingScreen(
                    tripId = tripId,
                    onReady = { id ->
                        navController.navigate(MeruRoute.TripSummary.create(id)) {
                            popUpTo(MeruRoute.Drive.path) { inclusive = false }
                        }
                    },
                )
            }
            composable(
                route = MeruRoute.TripSummary.path,
                arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
            ) {
                TripSummaryScreen(
                    onOpenDetail = { id -> navController.navigate(MeruRoute.TripDetail.create(id)) },
                    onDone = {
                        navController.navigate(MeruRoute.Drive.path) {
                            popUpTo(MeruRoute.Home.path) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = MeruRoute.TripDetail.path,
                arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
            ) {
                TripDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun LockedBoardsPlaceholder(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Driving Mode", color = MeruTeal, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Leaderboards and adventure map stay locked until you end the drive.",
                color = MeruMuted,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Back",
                color = MeruTeal,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(12.dp),
            )
        }
    }
}
