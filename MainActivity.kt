package com.oilchange.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDb.get(this).dao()
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { App(dao) }
            }
        }
    }
}

@Composable
fun App(dao: AppDao) {
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val route = back?.destination?.route
    val tabs = listOf("home" to ("🏠" to "خانه"), "customers" to ("👥" to "مشتریان"), "vehicles" to ("🚗" to "خودروها"), "reports" to ("📊" to "گزارش"), "settings" to ("⚙️" to "تنظیمات"))
    Scaffold(bottomBar = {
        if (route in tabs.map { it.first }) NavigationBar {
            tabs.forEach { (r, il) ->
                NavigationBarItem(selected = route == r,
                    onClick = { nav.navigate(r) { popUpTo("home"); launchSingleTop = true } },
                    icon = { Text(il.first) }, label = { Text(il.second, fontSize = 10.sp) })
            }
        }
    }) { pad ->
        NavHost(nav, "home", Modifier.padding(pad)) {
            composable("home") { HomeScreen(dao, nav) }
            composable("customers") { CustomersScreen(dao, nav) }
            composable("vehicles") { VehiclesScreen(dao, nav) }
            composable("reports") { ReportsScreen(dao, nav) }
            composable("settings") { SettingsScreen(dao) }
            composable("customer/{id}", listOf(navArgument("id") { type = NavType.LongType })) { CustomerScreen(dao, nav, it.arguments!!.getLong("id")) }
            composable("vehicle/{id}", listOf(navArgument("id") { type = NavType.LongType })) { VehicleScreen(dao, nav, it.arguments!!.getLong("id")) }
            composable("service/{id}", listOf(navArgument("id") { type = NavType.LongType })) { ServiceFormScreen(dao, nav, it.arguments!!.getLong("id")) }
        }
    }
}
