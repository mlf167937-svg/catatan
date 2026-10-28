package com.hansaps.home

import androidx.compose.runtime.Composable
import com.hansaps.core.AppAccount
import com.hansaps.core.HansColors

@Composable
fun DeveloperScreen(account: AppAccount, onBack: () -> Unit) {
    AccountManagementScreen(
        account = account,
        pageTitle = "Developer Console",
        accent = HansColors.Lime,
        onBack = onBack,
    )
}
