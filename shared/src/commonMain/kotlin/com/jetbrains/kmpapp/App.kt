package com.jetbrains.kmpapp

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jetbrains.kmpapp.data.AscentBackend
import com.jetbrains.kmpapp.screens.AtlasScreen
import com.jetbrains.kmpapp.screens.ChronicleScreen
import com.jetbrains.kmpapp.screens.ContractsScreen
import com.jetbrains.kmpapp.screens.ConvoyScreen
import com.jetbrains.kmpapp.screens.ExpeditionScreen
import com.jetbrains.kmpapp.screens.ExpeditionsScreen
import com.jetbrains.kmpapp.screens.MeruShell
import com.jetbrains.kmpapp.screens.RelicsScreen
import com.jetbrains.kmpapp.screens.RoadScreen
import com.jetbrains.kmpapp.screens.StoryScreen
import com.jetbrains.kmpapp.screens.SummaryScreen
import com.jetbrains.kmpapp.screens.MeruTheme
import com.jetbrains.kmpapp.screens.WelcomeScreen
import com.jetbrains.kmpapp.screens.detail.DetailScreen
import com.jetbrains.kmpapp.screens.list.ListScreen
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object WelcomeDestination

@Serializable
object ShellDestination

@Serializable
data class SummaryDestination(val driveId: String)

@Serializable
object AscentDestination

@Serializable
object AtlasDestination

@Serializable
data class RoadDestination(val roadId: String)

@Serializable
object ExpeditionsDestination

@Serializable
data class ExpeditionDestination(val expeditionId: String)

@Serializable
object RelicsDestination

@Serializable
object ChronicleDestination

@Serializable
object ConvoyDestination

@Serializable
object ContractsDestination

@Serializable
object StoryDestination

@Serializable
object ListDestination

@Serializable
data class DetailDestination(val objectId: Int)

@Composable
fun App() {
    MeruTheme {
        Surface(color = androidx.compose.material3.MaterialTheme.colorScheme.background) {
            val navController: NavHostController = rememberNavController()
            val backend = koinInject<AscentBackend>()
            NavHost(
                navController = navController,
                startDestination = if (backend.hasDriver()) ShellDestination else WelcomeDestination,
            ) {
                composable<WelcomeDestination> {
                    WelcomeScreen {
                        navController.navigate(ShellDestination) {
                            popUpTo(WelcomeDestination) { inclusive = true }
                        }
                    }
                }
                composable<ShellDestination> {
                    MeruShell(
                        onSummary = { navController.navigate(SummaryDestination(it)) },
                        onMissions = { navController.navigate(AtlasDestination) },
                        onExpeditions = { navController.navigate(ExpeditionsDestination) },
                        onRelics = { navController.navigate(RelicsDestination) },
                        onChronicle = { navController.navigate(ChronicleDestination) },
                        onConvoy = { navController.navigate(ConvoyDestination) },
                        onContracts = { navController.navigate(ContractsDestination) },
                        onStory = { navController.navigate(StoryDestination) },
                        onSignedOut = {
                            navController.navigate(WelcomeDestination) {
                                popUpTo(ShellDestination) { inclusive = true }
                            }
                        },
                    )
                }
                composable<SummaryDestination> { entry ->
                    SummaryScreen(
                        driveId = entry.toRoute<SummaryDestination>().driveId,
                        onClose = { navController.popBackStack() },
                    )
                }
                composable<AtlasDestination> {
                    AtlasScreen(
                        onBack = { navController.popBackStack() },
                        onRoad = { navController.navigate(RoadDestination(it)) },
                    )
                }
                composable<RoadDestination> { entry ->
                    RoadScreen(
                        roadId = entry.toRoute<RoadDestination>().roadId,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<ExpeditionsDestination> {
                    ExpeditionsScreen(
                        onBack = { navController.popBackStack() },
                        onOpen = { navController.navigate(ExpeditionDestination(it)) },
                    )
                }
                composable<ExpeditionDestination> { entry ->
                    ExpeditionScreen(
                        expeditionId = entry.toRoute<ExpeditionDestination>().expeditionId,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<RelicsDestination> { RelicsScreen(onBack = { navController.popBackStack() }) }
                composable<ChronicleDestination> { ChronicleScreen(onBack = { navController.popBackStack() }) }
                composable<ConvoyDestination> { ConvoyScreen(onBack = { navController.popBackStack() }) }
                composable<ContractsDestination> { ContractsScreen(onBack = { navController.popBackStack() }) }
                composable<StoryDestination> { StoryScreen(onBack = { navController.popBackStack() }) }
                composable<ListDestination> {
                    ListScreen(navigateToDetails = { objectId ->
                        navController.navigate(DetailDestination(objectId))
                    })
                }
                composable<DetailDestination> { backStackEntry ->
                    DetailScreen(
                        objectId = backStackEntry.toRoute<DetailDestination>().objectId,
                        navigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
