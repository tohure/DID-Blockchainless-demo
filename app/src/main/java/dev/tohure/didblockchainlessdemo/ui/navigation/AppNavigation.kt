package dev.tohure.didblockchainlessdemo.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.tohure.didblockchainlessdemo.ui.screens.DidScreen
import dev.tohure.didblockchainlessdemo.ui.screens.HomeScreen
import dev.tohure.didblockchainlessdemo.ui.screens.RsaScreen
import dev.tohure.didblockchainlessdemo.ui.viewmodel.CredentialViewModel

enum class Screen { HOME, DID, RSA }

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    vm: CredentialViewModel = viewModel(),
) {
    NavHost(
        navController = navController,
        startDestination = Screen.HOME.name,
    ) {
        composable(Screen.HOME.name) {
            HomeScreen(
                onDid = { navController.navigate(Screen.DID.name) },
                onRsa = { navController.navigate(Screen.RSA.name) },
            )
        }
        composable(Screen.DID.name) {
            DidScreen(vm = vm, onBack = { navController.popBackStack() })
        }
        composable(Screen.RSA.name) {
            RsaScreen(vm = vm, onBack = { navController.popBackStack() })
        }
    }
}