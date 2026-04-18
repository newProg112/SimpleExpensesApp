package com.example.simpleexpensesapp

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Expenses : Screen("expenses")
    object Mileage : Screen("mileage")
    object Settings : Screen("settings")
    object AddExpense : Screen("add_expense")
    object EditExpense : Screen("edit_expense")
    object AddMileage : Screen("add_mileage")
    object EditMileage : Screen("edit_mileage")
}