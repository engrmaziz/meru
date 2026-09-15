package app.meru.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.meru.android.core.datastore.SessionStore
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruPanel
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruVoid
import app.meru.android.engine.drive.DrivingMode
import app.meru.android.feature.auth.AuthScreen
import app.meru.android.feature.calibration.CalibrationScreen
import app.meru.android.feature.drive.DriveReadyScreen
import app.meru.android.feature.garage.GarageStubScreen
import app.meru.android.feature.home.HomeScreen
import app.meru.android.feature.more.MoreScreen
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

    Scaffold(
        containerColor = MeruVoid,
        bottomBar = {
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
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = MeruRoute.Home.path,
            modifier = Modifier.padding(padding),
        ) {
            composable(MeruRoute.Home.path) { HomeScreen() }
            composable(MeruRoute.Drive.path) {
                DriveReadyScreen(
                    onOpenCalibration = { navController.navigate(MeruRoute.Calibration.path) },
                )
            }
            composable(MeruRoute.Trips.path) { TripsListScreen() }
            composable(MeruRoute.Garage.path) { GarageStubScreen() }
            composable(MeruRoute.More.path) {
                MoreScreen(
                    onOpenCalibration = { navController.navigate(MeruRoute.Calibration.path) },
                )
            }
            composable(MeruRoute.Calibration.path) {
                CalibrationScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
