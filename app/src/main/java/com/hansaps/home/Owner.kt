package com.hansaps.home

import androidx.compose.runtime.Composable
import com.hansaps.core.AppAccount
import com.hansaps.core.HansColors

@Composable
fun OwnerScreen(account: AppAccount, onBack: () -> Unit) {
    AccountManagementScreen(
        account = account,
        pageTitle = "Owner Console",
        accent = HansColors.Coral,
        onBack = onBack,
    )
}
