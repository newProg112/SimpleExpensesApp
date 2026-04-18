package com.example.simpleexpensesapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.simpleexpensesapp.AddExpenseScreen
import com.example.simpleexpensesapp.ui.AddMileageScreen
import com.example.simpleexpensesapp.DashboardScreen
import com.example.simpleexpensesapp.DuplicateExpenseScreen
import com.example.simpleexpensesapp.DuplicateMileageScreen
import com.example.simpleexpensesapp.EditExpenseScreen
import com.example.simpleexpensesapp.ui.EditMileageScreen
import com.example.simpleexpensesapp.EmptyState
import com.example.simpleexpensesapp.Expense
import com.example.simpleexpensesapp.ExpenseCard
import com.example.simpleexpensesapp.ExpenseUi
import com.example.simpleexpensesapp.MileageCard
import com.example.simpleexpensesapp.MileageClaim
import com.example.simpleexpensesapp.MileageUi
import com.example.simpleexpensesapp.ModeSwitchChip
import com.example.simpleexpensesapp.R
import com.example.simpleexpensesapp.Screen
import com.example.simpleexpensesapp.currentMonthKey
import com.example.simpleexpensesapp.currentYearKey
import com.example.simpleexpensesapp.monthKeyFromDate
import com.example.simpleexpensesapp.nextMonthKey
import com.example.simpleexpensesapp.previousMonthKey
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun ExpensesScreen(
    modifier: Modifier = Modifier,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    analytics: FirebaseAnalytics,
    userEmail: String,
    navController: NavController,
    onEditExpense: (ExpenseUi) -> Unit,
    onEditMileage: (MileageUi) -> Unit
) {
    var expenses by remember { mutableStateOf(listOf<ExpenseUi>()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var duplicatingExpense by remember { mutableStateOf<ExpenseUi?>(null) }
    var duplicatingMileage by remember { mutableStateOf<MileageUi?>(null) }

    var mileageClaims by remember { mutableStateOf(listOf<MileageUi>()) }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    var showAddMenu by remember { mutableStateOf(false) }

    val thisMonthKey = currentMonthKey()

    val thisMonthExpenses = expenses.filter {
        monthKeyFromDate(it.expense.date) == thisMonthKey
    }

    val thisMonthTotal = thisMonthExpenses.sumOf { it.expense.net }
    val thisMonthCount = thisMonthExpenses.size

    val lastMonthKey = previousMonthKey()
    val currentYear = currentYearKey()

    val lastMonthExpenses = expenses.filter {
        monthKeyFromDate(it.expense.date) == lastMonthKey
    }

    val lastMonthTotal = lastMonthExpenses.sumOf { it.expense.net }

    val thisMonthMileageClaims = mileageClaims.filter {
        monthKeyFromDate(it.mileage.date) == thisMonthKey
    }

    val thisMonthMileageTotal = thisMonthMileageClaims.sumOf { it.mileage.total }
    val thisMonthMileageMiles = thisMonthMileageClaims.sumOf { it.mileage.miles }

    val yearToDateMileageClaims = mileageClaims.filter {
        it.mileage.date.startsWith(currentYear)
    }

    val yearToDateMileageTotal = yearToDateMileageClaims.sumOf { it.mileage.total }

    val topJourneyEntry = mileageClaims
        .groupBy { it.mileage.journey.ifBlank { "Unknown journey" } }
        .mapValues { entry -> entry.value.sumOf { it.mileage.total } }
        .maxByOrNull { it.value }

    val yearToDateExpenses = expenses.filter {
        it.expense.date.startsWith(currentYear)
    }

    val yearToDateTotal = yearToDateExpenses.sumOf { it.expense.net }

    val topSupplierEntry = expenses
        .groupBy { it.expense.supplier.ifBlank { "Unknown" } }
        .mapValues { entry -> entry.value.sumOf { it.expense.net } }
        .maxByOrNull { it.value }

    val topCategoryEntry = expenses
        .groupBy { it.expense.category.ifBlank { "Uncategorised" } }
        .mapValues { entry -> entry.value.sumOf { it.expense.net } }
        .maxByOrNull { it.value }

    val monthlyTotalsMap = expenses
        .groupBy { monthKeyFromDate(it.expense.date) }
        .filterKeys { it.isNotBlank() }
        .mapValues { entry -> entry.value.sumOf { it.expense.net } }

    val sortedMonthKeys = monthlyTotalsMap.keys.sorted()

    val filledMonthKeys = buildList {
        if (sortedMonthKeys.isNotEmpty()) {
            var current = sortedMonthKeys.first()
            val end = sortedMonthKeys.last()

            while (current <= end) {
                add(current)
                current = nextMonthKey(current)
            }
        }
    }

    val monthlyTotals = expenses
        .groupBy { monthKeyFromDate(it.expense.date) }
        .filterKeys { it.isNotBlank() }
        .mapValues { entry -> entry.value.sumOf { it.expense.net } }
        .toSortedMap()

    val monthlyTrendData = monthlyTotals.entries.map { it.key to it.value }

    val monthlyMileageMiles = mileageClaims
        .groupBy { monthKeyFromDate(it.mileage.date) }
        .filterKeys { it.isNotBlank() }
        .mapValues { entry -> entry.value.sumOf { it.mileage.miles } }
        .toSortedMap()

    val monthlyMileageMilesData = monthlyMileageMiles.entries.map { it.key to it.value }

    val categoryTotals = expenses
        .groupBy { it.expense.category.ifBlank { "Uncategorised" } }
        .mapValues { entry -> entry.value.sumOf { it.expense.net } }
        .toList()
        .sortedByDescending { it.second }

    val topFiveCategories = categoryTotals.take(5)
    val otherCategoriesTotal = categoryTotals.drop(5).sumOf { it.second }

    val donutCategoryData = buildList {
        addAll(topFiveCategories)
        if (otherCategoriesTotal > 0) {
            add("Other" to otherCategoriesTotal)
        }
    }

    var selectedCategory by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            errorMsg = "No signed-in user."
            loading = false
            onDispose { }
        } else {
            loading = true
            errorMsg = ""

            val expensesRegistration = db.collection("users")
                .document(uid)
                .collection("expenses")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        errorMsg = error.message ?: "Failed to load expenses."
                        loading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        expenses = snapshot.documents.map { doc ->
                            val expense = doc.toObject(Expense::class.java) ?: Expense()
                            ExpenseUi(
                                id = doc.id,
                                expense = expense
                            )
                        }
                    }

                    loading = false
                }

            val mileageRegistration = db.collection("users")
                .document(uid)
                .collection("mileage")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        errorMsg = error.message ?: "Failed to load mileage."
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        mileageClaims = snapshot.documents.map { doc ->
                            val mileage = doc.toObject(MileageClaim::class.java) ?: MileageClaim()
                            MileageUi(
                                id = doc.id,
                                mileage = mileage
                            )
                        }
                    }
                }

            onDispose {
                expensesRegistration.remove()
                mileageRegistration.remove()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (
                duplicatingExpense == null &&
                duplicatingMileage == null
            ) {
                Box {
                    FloatingActionButton(
                        onClick = { showAddMenu = true }
                    ) {
                        Text("Add")
                    }

                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add expense") },
                            onClick = {
                                showAddMenu = false
                                navController.navigate(Screen.AddExpense.route) {
                                    launchSingleTop = true
                                }
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Add mileage") },
                            onClick = {
                                showAddMenu = false
                                navController.navigate(Screen.AddMileage.route) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (duplicatingExpense != null) {
            DuplicateExpenseScreen(
                modifier = Modifier.padding(innerPadding),
                auth = auth,
                db = db,
                analytics = analytics,
                expenseUi = duplicatingExpense!!,
                onCancel = { duplicatingExpense = null },
                onSaved = {
                    duplicatingExpense = null
                }
            )
        } else if (duplicatingMileage != null) {
            DuplicateMileageScreen(
                modifier = Modifier.padding(innerPadding),
                auth = auth,
                db = db,
                analytics = analytics,
                mileageUi = duplicatingMileage!!,
                onCancel = { duplicatingMileage = null },
                onSaved = {
                    duplicatingMileage = null
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.simple_expenses_logo),
                        contentDescription = "Simple Expenses logo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        contentScale = ContentScale.Fit
                    )

                    Text(
                        text = "Hi, ${userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    Text(
                        text = "Track your expenses and mileage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ModeSwitchChip(
                                    label = "Expenses",
                                    selected = currentRoute == Screen.Expenses.route,
                                    onClick = {
                                        navController.navigate(Screen.Expenses.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                )

                                ModeSwitchChip(
                                    label = "Dashboard",
                                    selected = currentRoute == Screen.Dashboard.route,
                                    onClick = {
                                        navController.navigate(Screen.Dashboard.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                )

                                ModeSwitchChip(
                                    label = "Mileage",
                                    selected = currentRoute == Screen.Mileage.route,
                                    onClick = {
                                        navController.navigate(Screen.Mileage.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                )

                                ModeSwitchChip(
                                    label = "Settings",
                                    selected = currentRoute == Screen.Settings.route,
                                    onClick = {
                                        navController.navigate(Screen.Settings.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedCategory != null && currentRoute != Screen.Dashboard.route) {
                    Text(
                        text = "Filtering: $selectedCategory (tap to clear)",
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clickable { selectedCategory = null },
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                when {
                    loading -> {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
                    }

                    errorMsg.isNotBlank() -> {
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }

                    currentRoute == Screen.Dashboard.route -> {
                        DashboardScreen(
                            thisMonthTotal = thisMonthTotal,
                            thisMonthCount = thisMonthCount,
                            lastMonthTotal = lastMonthTotal,
                            yearToDateTotal = yearToDateTotal,
                            topSupplierName = topSupplierEntry?.key ?: "—",
                            topSupplierAmount = topSupplierEntry?.value ?: 0.0,
                            topCategoryName = topCategoryEntry?.key ?: "—",
                            topCategoryAmount = topCategoryEntry?.value ?: 0.0,
                            monthlyTrendData = monthlyTrendData,
                            donutCategoryData = donutCategoryData,
                            selectedCategory = selectedCategory,
                            thisMonthMileageTotal = thisMonthMileageTotal,
                            thisMonthMileageMiles = thisMonthMileageMiles,
                            yearToDateMileageTotal = yearToDateMileageTotal,
                            topJourneyName = topJourneyEntry?.key ?: "—",
                            topJourneyAmount = topJourneyEntry?.value ?: 0.0,
                            monthlyMileageMilesData = monthlyMileageMilesData,
                            onCategorySelected = { category ->
                                selectedCategory = if (selectedCategory == category) null else category
                            }
                        )
                    }

                    currentRoute == Screen.Mileage.route && mileageClaims.isEmpty() -> {
                        EmptyState(
                            title = "No mileage yet",
                            subtitle = "Tap Add to log your first journey"
                        )
                    }

                    currentRoute == Screen.Mileage.route -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(mileageClaims, key = { it.id }) { item ->
                                MileageCard(
                                    mileageUi = item,
                                    auth = auth,
                                    db = db,
                                    analytics = analytics,
                                    onEdit = {
                                        onEditMileage(item)
                                    },
                                    onDuplicate = {
                                        duplicatingMileage = item
                                    },
                                    onDeleted = { }
                                )
                            }
                        }
                    }

                    expenses.isEmpty() -> {
                        EmptyState(
                            title = "No expenses yet",
                            subtitle = "Tap Add to create your first expense"
                        )
                    }

                    else -> {
                        val filteredExpenses = if (selectedCategory == null) {
                            expenses
                        } else {
                            expenses.filter { it.expense.category == selectedCategory }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredExpenses, key = { it.id }) { item ->
                                ExpenseCard(
                                    expenseUi = item,
                                    auth = auth,
                                    db = db,
                                    analytics = analytics,
                                    onDeleted = { },
                                    onEdit = {
                                        onEditExpense(item)
                                    },
                                    onDuplicate = {
                                        duplicatingExpense = item
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}