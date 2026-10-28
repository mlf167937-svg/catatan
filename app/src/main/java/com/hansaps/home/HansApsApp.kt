package com.hansaps.home

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.hansaps.core.AccountStore
import com.hansaps.core.AppAccount
import com.hansaps.core.PlaybackConnector
import com.hansaps.core.UserRole
import com.hansaps.spotify.SpotifyScreen
import com.hansaps.youtube.YoutubeScreen

private enum class AppPage { HOME, SPOTIFY, YOUTUBE, DEVELOPER, OWNER }

@Composable
fun HansApsApp() {
    val context = LocalContext.current
    var account by remember { mutableStateOf<AppAccount?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }
    val history = remember { mutableStateListOf(AppPage.HOME) }

    LaunchedEffect(Unit) {
        AccountStore.initialize(context.applicationContext)
        PlaybackConnector.connect(context.applicationContext)
    }

    fun navigate(destination: AppPage) {
        history.add(destination)
    }

    fun goBack() {
        if (history.size > 1) history.removeAt(history.lastIndex)
        else history[0] = AppPage.HOME
    }

    BackHandler(enabled = account != null && history.size > 1) { goBack() }

    val signedIn = account
    if (signedIn == null) {
        LoginScreen(
            errorMessage = loginError,
            onClearError = { loginError = null },
            onLogin = { username, password ->
                val found = AccountStore.login(context, username, password)
                if (found == null) loginError = "Username atau password salah."
                else {
                    account = found
                    loginError = null
                    history.clear()
                    history.add(AppPage.HOME)
                }
            },
        )
        return
    }

    when (history.lastOrNull() ?: AppPage.HOME) {
        AppPage.HOME -> HomeScreen(
            account = signedIn,
            onOpenSpotify = { navigate(AppPage.SPOTIFY) },
            onOpenYoutube = { navigate(AppPage.YOUTUBE) },
            onOpenDeveloper = { if (signedIn.role == UserRole.DEVELOPER) navigate(AppPage.DEVELOPER) },
            onOpenOwner = { if (signedIn.role == UserRole.OWNER) navigate(AppPage.OWNER) },
            onLogout = {
                account = null
                history.clear()
                history.add(AppPage.HOME)
            },
        )
        AppPage.SPOTIFY -> SpotifyScreen(onBack = ::goBack)
        AppPage.YOUTUBE -> YoutubeScreen(onBack = ::goBack)
        AppPage.DEVELOPER -> if (signedIn.role == UserRole.DEVELOPER) {
            DeveloperScreen(account = signedIn, onBack = ::goBack)
        } else HomeScreen(signedIn, { navigate(AppPage.SPOTIFY) }, { navigate(AppPage.YOUTUBE) }, {}, {}, { account = null })
        AppPage.OWNER -> if (signedIn.role == UserRole.OWNER) {
            OwnerScreen(account = signedIn, onBack = ::goBack)
        } else HomeScreen(signedIn, { navigate(AppPage.SPOTIFY) }, { navigate(AppPage.YOUTUBE) }, {}, {}, { account = null })
    }
}
