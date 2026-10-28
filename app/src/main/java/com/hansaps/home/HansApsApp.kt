package com.hansaps.home

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Scaffold
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import com.hansaps.localaccount.RexLocalAccountScreen
import androidx.compose.ui.platform.LocalContext
import com.hansaps.core.AccountStore
import com.hansaps.core.AppAccount
import com.hansaps.core.UserRole
import com.hansaps.spotify.SpotifyScreen
import com.hansaps.youtube.YoutubeScreen

private enum class AppPage { HOME, SPOTIFY, YOUTUBE, LOCAL_ACCOUNT, DEVELOPER, OWNER }

@Composable
fun HansApsApp() {
    val context = LocalContext.current
    var account by remember { mutableStateOf<AppAccount?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var showPostLoginIntro by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf(AppPage.HOME) }

    LaunchedEffect(Unit) {
        AccountStore.initialize(context.applicationContext)
    }

    fun navigate(destination: AppPage) {
        history.add(destination)
    }

    fun goBack() {
        if (history.size > 1) history.removeAt(history.lastIndex)
        else history[0] = AppPage.HOME
    }

    fun switchMainPage(destination: AppPage) {
        history.clear()
        history.add(AppPage.HOME)
        if (destination != AppPage.HOME) history.add(destination)
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
                    showPostLoginIntro = true
                    history.clear()
                    history.add(AppPage.HOME)
                }
            },
        )
        return
    }

    if (showPostLoginIntro) {
        LoginIntroScreen(onFinished = { showPostLoginIntro = false })
        return
    }

    when (history.lastOrNull() ?: AppPage.HOME) {
        AppPage.HOME -> HomeScreen(
            account = signedIn,
            onOpenSpotify = { switchMainPage(AppPage.SPOTIFY) },
            onOpenYoutube = { switchMainPage(AppPage.YOUTUBE) },
            onOpenLocalAccount = { switchMainPage(AppPage.LOCAL_ACCOUNT) },
            onOpenDeveloper = { if (signedIn.role == UserRole.DEVELOPER) navigate(AppPage.DEVELOPER) },
            onOpenOwner = { if (signedIn.role == UserRole.OWNER) navigate(AppPage.OWNER) },
            onLogout = {
                account = null
                showPostLoginIntro = false
                history.clear()
                history.add(AppPage.HOME)
            },
        )
        AppPage.SPOTIFY -> SpotifyScreen(onBack = ::goBack)
        AppPage.YOUTUBE -> YoutubeScreen(onBack = ::goBack)
        AppPage.LOCAL_ACCOUNT -> Scaffold(
            containerColor = com.hansaps.core.HansColors.Background,
            bottomBar = {
                HansMainBottomNavigation(
                    selected = MainNavTab.ACCOUNT,
                    onHome = { switchMainPage(AppPage.HOME) },
                    onSpotify = { switchMainPage(AppPage.SPOTIFY) },
                    onYoutube = { switchMainPage(AppPage.YOUTUBE) },
                    onAccount = { switchMainPage(AppPage.LOCAL_ACCOUNT) },
                )
            }
        ) { padding -> RexLocalAccountScreen(padding) }
        AppPage.DEVELOPER -> if (signedIn.role == UserRole.DEVELOPER) {
            DeveloperScreen(account = signedIn, onBack = ::goBack)
        } else HomeScreen(signedIn, { switchMainPage(AppPage.SPOTIFY) }, { switchMainPage(AppPage.YOUTUBE) }, { switchMainPage(AppPage.LOCAL_ACCOUNT) }, {}, {}, { account = null })
        AppPage.OWNER -> if (signedIn.role == UserRole.OWNER) {
            OwnerScreen(account = signedIn, onBack = ::goBack)
        } else HomeScreen(signedIn, { switchMainPage(AppPage.SPOTIFY) }, { switchMainPage(AppPage.YOUTUBE) }, { switchMainPage(AppPage.LOCAL_ACCOUNT) }, {}, {}, { account = null })
    }
}
