package com.shouvick.mapup.feature.location.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun Navigation() {
    val navController = rememberNavController()
    NavHost(
        startDestination = Routes.MainScreen,
        navController = navController
    ) {
        composable <Routes.MainScreen>{
            MainScreen(navigateToNextScreen = {
                navController.navigate(Routes.SessionDetails(it))
            })
        }
        composable <Routes.SessionDetails>{
            val id = it.savedStateHandle.toRoute<Routes.SessionDetails>().id
            SessionDetailScreen(id)
        }

    }
}

@Serializable
sealed class Routes {
    @Serializable
    data object MainScreen : Routes()
    @Serializable
    data class SessionDetails(
        val id: Long
    ) : Routes()
}