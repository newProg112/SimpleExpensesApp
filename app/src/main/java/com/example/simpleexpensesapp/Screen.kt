package com.example.simpleexpensesapp

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Expenses : Screen("expenses")
    object Mileage : Screen("mileage")
    object Settings : Screen("settings")
}