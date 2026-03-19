package com.example.simpleexpensesapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.simpleexpensesapp.ui.theme.SimpleExpensesAppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import java.util.Calendar
import kotlin.math.min

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var analytics: FirebaseAnalytics
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        analytics = FirebaseAnalytics.getInstance(this)
        analytics.logEvent("android_app_opened", null)

        enableEdgeToEdge()
        setContent {
            SimpleExpensesAppTheme {
                var signedInUserEmail by remember { mutableStateOf(auth.currentUser?.email) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (signedInUserEmail == null) {
                        LoginScreen(
                            modifier = Modifier.padding(innerPadding),
                            auth = auth,
                            onLoginSuccess = { email ->
                                signedInUserEmail = email
                            }
                        )
                    } else {
                        ExpensesScreen(
                            modifier = Modifier.padding(innerPadding),
                            auth = auth,
                            db = db,
                            analytics = analytics,
                            userEmail = signedInUserEmail ?: "",
                            onSignOut = {
                                auth.signOut()
                                signedInUserEmail = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    auth: FirebaseAuth,
    onLoginSuccess: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Simple Expenses",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Track expenses across web and mobile",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMsg = ""
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMsg = ""
            },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        if (errorMsg.isNotBlank()) {
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMsg = "Enter email and password."
                    return@Button
                }

                loading = true
                errorMsg = ""

                auth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnCompleteListener { task ->
                        loading = false

                        if (task.isSuccessful) {
                            val signedInEmail = auth.currentUser?.email ?: email.trim()
                            onLoginSuccess(signedInEmail)
                        } else {
                            errorMsg = task.exception?.message ?: "Sign-in failed."
                        }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            enabled = !loading
        ) {
            if (loading) {
                CircularProgressIndicator()
            } else {
                Text("Sign in")
            }
        }

        TextButton(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMsg = "Enter email and password to create an account."
                    return@TextButton
                }

                loading = true
                errorMsg = ""

                auth.createUserWithEmailAndPassword(email.trim(), password)
                    .addOnCompleteListener { task ->
                        loading = false

                        if (task.isSuccessful) {
                            val signedInEmail = auth.currentUser?.email ?: email.trim()
                            onLoginSuccess(signedInEmail)
                        } else {
                            errorMsg = task.exception?.message ?: "Account creation failed."
                        }
                    }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Create account")
        }
    }
}

@Composable
fun ExpensesScreen(
    modifier: Modifier = Modifier,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    analytics: FirebaseAnalytics,
    userEmail: String,
    onSignOut: () -> Unit
) {
    var expenses by remember { mutableStateOf(listOf<ExpenseUi>()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    var showDashboard by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseUi?>(null) }
    var duplicatingExpense by remember { mutableStateOf<ExpenseUi?>(null) }
    var showAddMileageForm by remember { mutableStateOf(false) }
    var editingMileage by remember { mutableStateOf<MileageUi?>(null) }
    var duplicatingMileage by remember { mutableStateOf<MileageUi?>(null) }

    var mileageClaims by remember { mutableStateOf(listOf<MileageUi>()) }
    var showMileageList by remember { mutableStateOf(false) }

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
                !showAddForm &&
                !showAddMileageForm &&
                editingExpense == null &&
                editingMileage == null &&
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
                                showAddForm = true
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Add mileage") },
                            onClick = {
                                showAddMenu = false
                                showAddMileageForm = true
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (showAddForm) {
            AddExpenseScreen(
                modifier = Modifier.padding(innerPadding),
                auth = auth,
                db = db,
                analytics = analytics,
                onCancel = { showAddForm = false },
                onSaved = {
                    showAddForm = false
                }
            )
        } else if (showAddMileageForm) {
            AddMileageScreen(
                modifier = Modifier.padding(innerPadding),
                auth = auth,
                db = db,
                analytics = analytics,
                onCancel = { showAddMileageForm = false },
                onSaved = {
                    showAddMileageForm = false
                }
            )
        } else if (editingExpense != null) {
            EditExpenseScreen(
                modifier = Modifier.padding(innerPadding),
                auth = auth,
                db = db,
                analytics = analytics,
                expenseUi = editingExpense!!,
                onCancel = { editingExpense = null },
                onSaved = {
                    editingExpense = null
                }
            )
        } else if (editingMileage != null) {
            EditMileageScreen(
                modifier = Modifier.padding(innerPadding),
                auth = auth,
                db = db,
                analytics = analytics,
                mileageUi = editingMileage!!,
                onCancel = { editingMileage = null },
                onSaved = {
                    editingMileage = null
                }
            )
        } else if (duplicatingExpense != null) {
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Simple Expenses",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
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
                                    selected = !showDashboard && !showMileageList,
                                    onClick = {
                                        showDashboard = false
                                        showMileageList = false
                                    }
                                )

                                ModeSwitchChip(
                                    label = "Dashboard",
                                    selected = showDashboard,
                                    onClick = {
                                        showDashboard = true
                                        showMileageList = false
                                    }
                                )

                                ModeSwitchChip(
                                    label = "Mileage",
                                    selected = showMileageList,
                                    onClick = {
                                        showMileageList = true
                                        showDashboard = false
                                    }
                                )
                            }
                        }

                        TextButton(
                            onClick = onSignOut,
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Text("Sign out")
                        }
                    }
                }

                // Comment out "This month / Current month snapshot" from Expenses list
                /*
                if (!showDashboard && !showMileageList) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                        ) {
                            Text(
                                text = "This month",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = formatGBP(thisMonthTotal),
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(top = 10.dp)
                            )

                            Text(
                                text = "$thisMonthCount expense${if (thisMonthCount == 1) "" else "s"} recorded",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .padding(top = 14.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (thisMonthCount == 0) "No entries yet" else "Current month snapshot",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                */

                if (selectedCategory != null && !showDashboard) {
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

                    showDashboard -> {
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
                            monthlyMileageMilesData = monthlyMileageMilesData,
                            topJourneyName = topJourneyEntry?.key ?: "—",
                            topJourneyAmount = topJourneyEntry?.value ?: 0.0,
                            onCategorySelected = { category ->
                                selectedCategory = if (selectedCategory == category) null else category
                                showDashboard = false
                                showMileageList = false
                            }
                        )
                    }

                    showMileageList && mileageClaims.isEmpty() -> {
                        EmptyState(
                            title = "No mileage yet",
                            subtitle = "Tap Add to log your first journey"
                        )
                    }

                    showMileageList -> {
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
                                        editingMileage = item
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
                                        editingExpense = item
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    modifier: Modifier = Modifier,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    analytics: FirebaseAnalytics,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current

    var date by remember { mutableStateOf(todayIsoDate()) }
    var supplier by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var net by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var vatCode by remember { mutableStateOf("standard") }

    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var vatExpanded by remember { mutableStateOf(false) }

    val vatOptions = listOf(
        "standard" to "VAT 20% (Standard)",
        "reduced" to "VAT 5% (Reduced)",
        "zero" to "VAT 0% (Zero)",
        "exempt" to "Exempt / No VAT"
    )

    val calendar = Calendar.getInstance()
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val mm = (month + 1).toString().padStart(2, '0')
            val dd = dayOfMonth.toString().padStart(2, '0')
            date = "$year-$mm-$dd"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Add expense",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = date,
            onValueChange = {},
            label = { Text("Date") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            readOnly = true
        )

        TextButton(onClick = { datePickerDialog.show() }) {
            Text("Pick date")
        }

        OutlinedTextField(
            value = supplier,
            onValueChange = {
                supplier = it
                errorMsg = ""
            },
            label = { Text("Supplier") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = category,
            onValueChange = {
                category = it
                errorMsg = ""
            },
            label = { Text("Category") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true
        )

        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickCategories.forEach { quickCat ->
                TextButton(
                    onClick = {
                        category = quickCat
                        errorMsg = ""
                    }
                ) {
                    Text(quickCat)
                }
            }
        }

        OutlinedTextField(
            value = net,
            onValueChange = {
                net = it
                errorMsg = ""
            },
            label = { Text("Net (£)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        ExposedDropdownMenuBox(
            expanded = vatExpanded,
            onExpandedChange = { vatExpanded = !vatExpanded },
            modifier = Modifier.padding(top = 12.dp)
        ) {
            OutlinedTextField(
                value = vatOptions.first { it.first == vatCode }.second,
                onValueChange = {},
                readOnly = true,
                label = { Text("VAT code") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vatExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = vatExpanded,
                onDismissRequest = { vatExpanded = false }
            ) {
                vatOptions.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            vatCode = value
                            vatExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = {
                notes = it
                errorMsg = ""
            },
            label = { Text("Notes") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        if (errorMsg.isNotBlank()) {
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Row(
            modifier = Modifier.padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val uid = auth.currentUser?.uid
                    val netValue = net.toDoubleOrNull()

                    if (uid == null) {
                        errorMsg = "No signed-in user."
                        return@Button
                    }
                    if (supplier.isBlank()) {
                        errorMsg = "Enter a supplier."
                        return@Button
                    }
                    if (category.isBlank()) {
                        errorMsg = "Enter a category."
                        return@Button
                    }
                    if (netValue == null || netValue <= 0.0) {
                        errorMsg = "Enter a valid net amount."
                        return@Button
                    }

                    saving = true
                    errorMsg = ""

                    val payload = hashMapOf<String, Any?>(
                        "date" to date,
                        "supplier" to supplier.trim(),
                        "category" to category.trim(),
                        "net" to netValue,
                        "vatCode" to vatCode,
                        "notes" to notes.trim(),
                        "receipt" to null,
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )

                    db.collection("users")
                        .document(uid)
                        .collection("expenses")
                        .add(payload)
                        .addOnSuccessListener {
                            val bundle = Bundle().apply {
                                putDouble("amount", netValue)
                                putString("category", category.trim().lowercase())
                                putString("vat_code", vatCode)
                            }

                            analytics.logEvent("expense_added", bundle)

                            saving = false
                            onSaved()
                        }
                        .addOnFailureListener { e ->
                            saving = false
                            errorMsg = e.message ?: "Failed to save expense."
                        }
                },
                enabled = !saving
            ) {
                if (saving) {
                    CircularProgressIndicator()
                } else {
                    Text("Save")
                }
            }

            TextButton(
                onClick = onCancel,
                enabled = !saving
            ) {
                Text("Cancel")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseScreen(
    modifier: Modifier = Modifier,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    analytics: FirebaseAnalytics,
    expenseUi: ExpenseUi,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val original = expenseUi.expense

    var date by remember { mutableStateOf(original.date) }
    var supplier by remember { mutableStateOf(original.supplier) }
    var category by remember { mutableStateOf(original.category) }
    var net by remember { mutableStateOf(original.net.toString()) }
    var notes by remember { mutableStateOf(original.notes) }
    var vatCode by remember { mutableStateOf(original.vatCode.ifBlank { "standard" }) }

    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var vatExpanded by remember { mutableStateOf(false) }

    val vatOptions = listOf(
        "standard" to "VAT 20% (Standard)",
        "reduced" to "VAT 5% (Reduced)",
        "zero" to "VAT 0% (Zero)",
        "exempt" to "Exempt / No VAT"
    )

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val mm = (month + 1).toString().padStart(2, '0')
            val dd = dayOfMonth.toString().padStart(2, '0')
            date = "$year-$mm-$dd"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Edit expense",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = date,
            onValueChange = {},
            label = { Text("Date") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            readOnly = true
        )

        TextButton(onClick = { datePickerDialog.show() }) {
            Text("Pick date")
        }

        OutlinedTextField(
            value = supplier,
            onValueChange = {
                supplier = it
                errorMsg = ""
            },
            label = { Text("Supplier") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = category,
            onValueChange = {
                category = it
                errorMsg = ""
            },
            label = { Text("Category") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true
        )

        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickCategories.forEach { quickCat ->
                TextButton(
                    onClick = {
                        category = quickCat
                        errorMsg = ""
                    }
                ) {
                    Text(quickCat)
                }
            }
        }

        OutlinedTextField(
            value = net,
            onValueChange = {
                net = it
                errorMsg = ""
            },
            label = { Text("Net (£)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        ExposedDropdownMenuBox(
            expanded = vatExpanded,
            onExpandedChange = { vatExpanded = !vatExpanded },
            modifier = Modifier.padding(top = 12.dp)
        ) {
            OutlinedTextField(
                value = vatOptions.firstOrNull { it.first == vatCode }?.second ?: "VAT 20% (Standard)",
                onValueChange = {},
                readOnly = true,
                label = { Text("VAT code") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vatExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = vatExpanded,
                onDismissRequest = { vatExpanded = false }
            ) {
                vatOptions.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            vatCode = value
                            vatExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = {
                notes = it
                errorMsg = ""
            },
            label = { Text("Notes") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        if (errorMsg.isNotBlank()) {
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Row(
            modifier = Modifier.padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val uid = auth.currentUser?.uid
                    val netValue = net.toDoubleOrNull()

                    if (uid == null) {
                        errorMsg = "No signed-in user."
                        return@Button
                    }
                    if (supplier.isBlank()) {
                        errorMsg = "Enter a supplier."
                        return@Button
                    }
                    if (category.isBlank()) {
                        errorMsg = "Enter a category."
                        return@Button
                    }
                    if (netValue == null || netValue <= 0.0) {
                        errorMsg = "Enter a valid net amount."
                        return@Button
                    }

                    saving = true
                    errorMsg = ""

                    val payload = hashMapOf<String, Any?>(
                        "date" to date,
                        "supplier" to supplier.trim(),
                        "category" to category.trim(),
                        "net" to netValue,
                        "vatCode" to vatCode,
                        "notes" to notes.trim(),
                        "receipt" to original.receipt
                    )

                    db.collection("users")
                        .document(uid)
                        .collection("expenses")
                        .document(expenseUi.id)
                        .update(payload)
                        .addOnSuccessListener {
                            val bundle = Bundle().apply {
                                putDouble("amount", netValue)
                                putString("category", category.trim().lowercase())
                                putString("vat_code", vatCode)
                            }

                            analytics.logEvent("expense_updated", bundle)

                            saving = false
                            onSaved()
                        }
                        .addOnFailureListener { e ->
                            saving = false
                            errorMsg = e.message ?: "Failed to update expense."
                        }
                },
                enabled = !saving
            ) {
                if (saving) {
                    CircularProgressIndicator()
                } else {
                    Text("Update")
                }
            }

            TextButton(
                onClick = onCancel,
                enabled = !saving
            ) {
                Text("Cancel")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateExpenseScreen(
    modifier: Modifier = Modifier,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    analytics: FirebaseAnalytics,
    expenseUi: ExpenseUi,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val original = expenseUi.expense

    var date by remember { mutableStateOf(original.date) }
    var supplier by remember { mutableStateOf(original.supplier) }
    var category by remember { mutableStateOf(original.category) }
    var net by remember { mutableStateOf(original.net.toString()) }
    var notes by remember { mutableStateOf(original.notes) }
    var vatCode by remember { mutableStateOf(original.vatCode.ifBlank { "standard" }) }

    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var vatExpanded by remember { mutableStateOf(false) }

    val vatOptions = listOf(
        "standard" to "VAT 20% (Standard)",
        "reduced" to "VAT 5% (Reduced)",
        "zero" to "VAT 0% (Zero)",
        "exempt" to "Exempt / No VAT"
    )

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val mm = (month + 1).toString().padStart(2, '0')
            val dd = dayOfMonth.toString().padStart(2, '0')
            date = "$year-$mm-$dd"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Duplicate expense",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = date,
            onValueChange = {},
            label = { Text("Date") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            readOnly = true
        )

        TextButton(onClick = { datePickerDialog.show() }) {
            Text("Pick date")
        }

        OutlinedTextField(
            value = supplier,
            onValueChange = {
                supplier = it
                errorMsg = ""
            },
            label = { Text("Supplier") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = category,
            onValueChange = {
                category = it
                errorMsg = ""
            },
            label = { Text("Category") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true
        )

        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickCategories.forEach { quickCat ->
                TextButton(
                    onClick = {
                        category = quickCat
                        errorMsg = ""
                    }
                ) {
                    Text(quickCat)
                }
            }
        }

        OutlinedTextField(
            value = net,
            onValueChange = {
                net = it
                errorMsg = ""
            },
            label = { Text("Net (£)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        ExposedDropdownMenuBox(
            expanded = vatExpanded,
            onExpandedChange = { vatExpanded = !vatExpanded },
            modifier = Modifier.padding(top = 12.dp)
        ) {
            OutlinedTextField(
                value = vatOptions.firstOrNull { it.first == vatCode }?.second ?: "VAT 20% (Standard)",
                onValueChange = {},
                readOnly = true,
                label = { Text("VAT code") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vatExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = vatExpanded,
                onDismissRequest = { vatExpanded = false }
            ) {
                vatOptions.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            vatCode = value
                            vatExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = {
                notes = it
                errorMsg = ""
            },
            label = { Text("Notes") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        if (errorMsg.isNotBlank()) {
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Row(
            modifier = Modifier.padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val uid = auth.currentUser?.uid
                    val netValue = net.toDoubleOrNull()

                    if (uid == null) {
                        errorMsg = "No signed-in user."
                        return@Button
                    }
                    if (supplier.isBlank()) {
                        errorMsg = "Enter a supplier."
                        return@Button
                    }
                    if (category.isBlank()) {
                        errorMsg = "Enter a category."
                        return@Button
                    }
                    if (netValue == null || netValue <= 0.0) {
                        errorMsg = "Enter a valid net amount."
                        return@Button
                    }

                    saving = true
                    errorMsg = ""

                    val payload = hashMapOf<String, Any?>(
                        "date" to date,
                        "supplier" to supplier.trim(),
                        "category" to category.trim(),
                        "net" to netValue,
                        "vatCode" to vatCode,
                        "notes" to notes.trim(),
                        "receipt" to original.receipt,
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )

                    db.collection("users")
                        .document(uid)
                        .collection("expenses")
                        .add(payload)
                        .addOnSuccessListener {
                            val bundle = Bundle().apply {
                                putDouble("amount", netValue)
                                putString("category", category.trim().lowercase())
                                putString("vat_code", vatCode)
                            }

                            analytics.logEvent("expense_duplicated", bundle)

                            saving = false
                            onSaved()
                        }
                        .addOnFailureListener { e ->
                            saving = false
                            errorMsg = e.message ?: "Failed to duplicate expense."
                        }
                },
                enabled = !saving
            ) {
                if (saving) {
                    CircularProgressIndicator()
                } else {
                    Text("Save duplicate")
                }
            }

            TextButton(
                onClick = onCancel,
                enabled = !saving
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun AddMileageScreen(
    modifier: Modifier = Modifier,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    analytics: FirebaseAnalytics,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current

    var date by remember { mutableStateOf(todayIsoDate()) }
    var journey by remember { mutableStateOf("") }
    var fromLocation by remember { mutableStateOf("") }
    var toLocation by remember { mutableStateOf("") }
    var miles by remember { mutableStateOf("") }
    var ratePerMile by remember { mutableStateOf("0.45") }
    var notes by remember { mutableStateOf("") }

    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val milesValue = miles.toDoubleOrNull() ?: 0.0
    val rateValue = ratePerMile.toDoubleOrNull() ?: 0.0
    val totalValue = milesValue * rateValue

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val mm = (month + 1).toString().padStart(2, '0')
            val dd = dayOfMonth.toString().padStart(2, '0')
            date = "$year-$mm-$dd"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Add mileage claim",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = date,
            onValueChange = {},
            label = { Text("Date") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            readOnly = true
        )

        TextButton(onClick = { datePickerDialog.show() }) {
            Text("Pick date")
        }

        OutlinedTextField(
            value = journey,
            onValueChange = {
                journey = it
                errorMsg = ""
            },
            label = { Text("Journey") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = fromLocation,
            onValueChange = {
                fromLocation = it
                errorMsg = ""
            },
            label = { Text("From") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = toLocation,
            onValueChange = {
                toLocation = it
                errorMsg = ""
            },
            label = { Text("To") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = miles,
            onValueChange = {
                miles = it
                errorMsg = ""
            },
            label = { Text("Miles") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        OutlinedTextField(
            value = ratePerMile,
            onValueChange = {
                ratePerMile = it
                errorMsg = ""
            },
            label = { Text("Rate per mile (£)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Claim total",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatGBP(totalValue),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = {
                notes = it
                errorMsg = ""
            },
            label = { Text("Notes") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        if (errorMsg.isNotBlank()) {
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Row(
            modifier = Modifier.padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val uid = auth.currentUser?.uid
                    val parsedMiles = miles.toDoubleOrNull()
                    val parsedRate = ratePerMile.toDoubleOrNull()

                    if (uid == null) {
                        errorMsg = "No signed-in user."
                        return@Button
                    }
                    if (journey.isBlank()) {
                        errorMsg = "Enter a journey."
                        return@Button
                    }
                    if (fromLocation.isBlank()) {
                        errorMsg = "Enter a start location."
                        return@Button
                    }
                    if (toLocation.isBlank()) {
                        errorMsg = "Enter an end location."
                        return@Button
                    }
                    if (parsedMiles == null || parsedMiles <= 0.0) {
                        errorMsg = "Enter valid miles."
                        return@Button
                    }
                    if (parsedRate == null || parsedRate <= 0.0) {
                        errorMsg = "Enter a valid rate per mile."
                        return@Button
                    }

                    saving = true
                    errorMsg = ""

                    val total = parsedMiles * parsedRate

                    val payload = hashMapOf<String, Any?>(
                        "date" to date,
                        "journey" to journey.trim(),
                        "fromLocation" to fromLocation.trim(),
                        "toLocation" to toLocation.trim(),
                        "miles" to parsedMiles,
                        "ratePerMile" to parsedRate,
                        "total" to total,
                        "notes" to notes.trim(),
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )

                    db.collection("users")
                        .document(uid)
                        .collection("mileage")
                        .add(payload)
                        .addOnSuccessListener {
                            val bundle = Bundle().apply {
                                putDouble("miles", parsedMiles)
                                putDouble("rate_per_mile", parsedRate)
                                putDouble("claim_total", total)
                            }

                            analytics.logEvent("mileage_added", bundle)

                            saving = false
                            onSaved()
                        }
                        .addOnFailureListener { e ->
                            saving = false
                            errorMsg = e.message ?: "Failed to save mileage claim."
                        }
                },
                enabled = !saving
            ) {
                if (saving) {
                    CircularProgressIndicator()
                } else {
                    Text("Save")
                }
            }

            TextButton(
                onClick = onCancel,
                enabled = !saving
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun EditMileageScreen(
    modifier: Modifier = Modifier,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    analytics: FirebaseAnalytics,
    mileageUi: MileageUi,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val original = mileageUi.mileage

    var date by remember { mutableStateOf(original.date) }
    var journey by remember { mutableStateOf(original.journey) }
    var fromLocation by remember { mutableStateOf(original.fromLocation) }
    var toLocation by remember { mutableStateOf(original.toLocation) }
    var miles by remember { mutableStateOf(original.miles.toString()) }
    var ratePerMile by remember { mutableStateOf(original.ratePerMile.toString()) }
    var notes by remember { mutableStateOf(original.notes) }

    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val milesValue = miles.toDoubleOrNull() ?: 0.0
    val rateValue = ratePerMile.toDoubleOrNull() ?: 0.0
    val totalValue = milesValue * rateValue

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val mm = (month + 1).toString().padStart(2, '0')
            val dd = dayOfMonth.toString().padStart(2, '0')
            date = "$year-$mm-$dd"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Edit mileage claim",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = date,
            onValueChange = {},
            label = { Text("Date") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            readOnly = true
        )

        TextButton(onClick = { datePickerDialog.show() }) {
            Text("Pick date")
        }

        OutlinedTextField(
            value = journey,
            onValueChange = {
                journey = it
                errorMsg = ""
            },
            label = { Text("Journey") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = fromLocation,
            onValueChange = {
                fromLocation = it
                errorMsg = ""
            },
            label = { Text("From") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = toLocation,
            onValueChange = {
                toLocation = it
                errorMsg = ""
            },
            label = { Text("To") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = miles,
            onValueChange = {
                miles = it
                errorMsg = ""
            },
            label = { Text("Miles") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        OutlinedTextField(
            value = ratePerMile,
            onValueChange = {
                ratePerMile = it
                errorMsg = ""
            },
            label = { Text("Rate per mile (£)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Claim total",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatGBP(totalValue),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = {
                notes = it
                errorMsg = ""
            },
            label = { Text("Notes") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        if (errorMsg.isNotBlank()) {
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Row(
            modifier = Modifier.padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val uid = auth.currentUser?.uid
                    val parsedMiles = miles.toDoubleOrNull()
                    val parsedRate = ratePerMile.toDoubleOrNull()

                    if (uid == null) {
                        errorMsg = "No signed-in user."
                        return@Button
                    }
                    if (journey.isBlank()) {
                        errorMsg = "Enter a journey."
                        return@Button
                    }
                    if (fromLocation.isBlank()) {
                        errorMsg = "Enter a start location."
                        return@Button
                    }
                    if (toLocation.isBlank()) {
                        errorMsg = "Enter an end location."
                        return@Button
                    }
                    if (parsedMiles == null || parsedMiles <= 0.0) {
                        errorMsg = "Enter valid miles."
                        return@Button
                    }
                    if (parsedRate == null || parsedRate <= 0.0) {
                        errorMsg = "Enter a valid rate per mile."
                        return@Button
                    }

                    saving = true
                    errorMsg = ""

                    val total = parsedMiles * parsedRate

                    val payload = hashMapOf<String, Any?>(
                        "date" to date,
                        "journey" to journey.trim(),
                        "fromLocation" to fromLocation.trim(),
                        "toLocation" to toLocation.trim(),
                        "miles" to parsedMiles,
                        "ratePerMile" to parsedRate,
                        "total" to total,
                        "notes" to notes.trim()
                    )

                    db.collection("users")
                        .document(uid)
                        .collection("mileage")
                        .document(mileageUi.id)
                        .update(payload)
                        .addOnSuccessListener {
                            val bundle = Bundle().apply {
                                putDouble("miles", parsedMiles)
                                putDouble("rate_per_mile", parsedRate)
                                putDouble("claim_total", total)
                            }

                            analytics.logEvent("mileage_updated", bundle)

                            saving = false
                            onSaved()
                        }
                        .addOnFailureListener { e ->
                            saving = false
                            errorMsg = e.message ?: "Failed to update mileage claim."
                        }
                },
                enabled = !saving
            ) {
                if (saving) {
                    CircularProgressIndicator()
                } else {
                    Text("Update")
                }
            }

            TextButton(
                onClick = onCancel,
                enabled = !saving
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun DuplicateMileageScreen(
    modifier: Modifier = Modifier,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    analytics: FirebaseAnalytics,
    mileageUi: MileageUi,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val original = mileageUi.mileage

    var date by remember { mutableStateOf(original.date) }
    var journey by remember { mutableStateOf(original.journey) }
    var fromLocation by remember { mutableStateOf(original.fromLocation) }
    var toLocation by remember { mutableStateOf(original.toLocation) }
    var miles by remember { mutableStateOf(original.miles.toString()) }
    var ratePerMile by remember { mutableStateOf(original.ratePerMile.toString()) }
    var notes by remember { mutableStateOf(original.notes) }

    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val milesValue = miles.toDoubleOrNull() ?: 0.0
    val rateValue = ratePerMile.toDoubleOrNull() ?: 0.0
    val totalValue = milesValue * rateValue

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val mm = (month + 1).toString().padStart(2, '0')
            val dd = dayOfMonth.toString().padStart(2, '0')
            date = "$year-$mm-$dd"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Duplicate mileage claim",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = date,
            onValueChange = {},
            label = { Text("Date") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            readOnly = true
        )

        TextButton(onClick = { datePickerDialog.show() }) {
            Text("Pick date")
        }

        OutlinedTextField(
            value = journey,
            onValueChange = {
                journey = it
                errorMsg = ""
            },
            label = { Text("Journey") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = fromLocation,
            onValueChange = {
                fromLocation = it
                errorMsg = ""
            },
            label = { Text("From") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = toLocation,
            onValueChange = {
                toLocation = it
                errorMsg = ""
            },
            label = { Text("To") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = miles,
            onValueChange = {
                miles = it
                errorMsg = ""
            },
            label = { Text("Miles") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        OutlinedTextField(
            value = ratePerMile,
            onValueChange = {
                ratePerMile = it
                errorMsg = ""
            },
            label = { Text("Rate per mile (£)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Claim total",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatGBP(totalValue),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = {
                notes = it
                errorMsg = ""
            },
            label = { Text("Notes") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        if (errorMsg.isNotBlank()) {
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Row(
            modifier = Modifier.padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val uid = auth.currentUser?.uid
                    val parsedMiles = miles.toDoubleOrNull()
                    val parsedRate = ratePerMile.toDoubleOrNull()

                    if (uid == null) {
                        errorMsg = "No signed-in user."
                        return@Button
                    }
                    if (journey.isBlank()) {
                        errorMsg = "Enter a journey."
                        return@Button
                    }
                    if (fromLocation.isBlank()) {
                        errorMsg = "Enter a start location."
                        return@Button
                    }
                    if (toLocation.isBlank()) {
                        errorMsg = "Enter an end location."
                        return@Button
                    }
                    if (parsedMiles == null || parsedMiles <= 0.0) {
                        errorMsg = "Enter valid miles."
                        return@Button
                    }
                    if (parsedRate == null || parsedRate <= 0.0) {
                        errorMsg = "Enter a valid rate per mile."
                        return@Button
                    }

                    saving = true
                    errorMsg = ""

                    val total = parsedMiles * parsedRate

                    val payload = hashMapOf<String, Any?>(
                        "date" to date,
                        "journey" to journey.trim(),
                        "fromLocation" to fromLocation.trim(),
                        "toLocation" to toLocation.trim(),
                        "miles" to parsedMiles,
                        "ratePerMile" to parsedRate,
                        "total" to total,
                        "notes" to notes.trim(),
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )

                    db.collection("users")
                        .document(uid)
                        .collection("mileage")
                        .add(payload)
                        .addOnSuccessListener {
                            val bundle = Bundle().apply {
                                putDouble("miles", parsedMiles)
                                putDouble("rate_per_mile", parsedRate)
                                putDouble("claim_total", total)
                            }

                            analytics.logEvent("mileage_duplicated", bundle)

                            saving = false
                            onSaved()
                        }
                        .addOnFailureListener { e ->
                            saving = false
                            errorMsg = e.message ?: "Failed to duplicate mileage claim."
                        }
                },
                enabled = !saving
            ) {
                if (saving) {
                    CircularProgressIndicator()
                } else {
                    Text("Save duplicate")
                }
            }

            TextButton(
                onClick = onCancel,
                enabled = !saving
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun DashboardStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 14.dp)
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
fun DashboardSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
    )
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun ModeSwitchChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun DashboardPanel(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier.padding(top = 14.dp),
                content = content
            )
        }
    }
}

@Composable
fun DashboardScreen(
    thisMonthTotal: Double,
    thisMonthCount: Int,
    lastMonthTotal: Double,
    yearToDateTotal: Double,
    topSupplierName: String,
    topSupplierAmount: Double,
    topCategoryName: String,
    topCategoryAmount: Double,
    monthlyTrendData: List<Pair<String, Double>>,
    donutCategoryData: List<Pair<String, Double>>,
    selectedCategory: String?,
    thisMonthMileageTotal: Double,
    thisMonthMileageMiles: Double,
    yearToDateMileageTotal: Double,
    topJourneyName: String,
    topJourneyAmount: Double,
    monthlyMileageMilesData: List<Pair<String, Double>>,
    onCategorySelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DashboardSectionHeader("Expenses")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "This month",
                value = formatGBP(thisMonthTotal),
                subtitle = "$thisMonthCount expense${if (thisMonthCount == 1) "" else "s"}",
                icon = Icons.Default.TrendingUp
            )

            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "This month count",
                value = thisMonthCount.toString(),
                subtitle = "Entries recorded",
                icon = Icons.Default.Description
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "Last month",
                value = formatGBP(lastMonthTotal),
                subtitle = "Previous month total",
                icon = Icons.Default.CalendarMonth
            )

            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "Year to date",
                value = formatGBP(yearToDateTotal),
                subtitle = "Since January",
                icon = Icons.Default.TrendingUp
            )
        }

        DashboardSectionHeader("Mileage")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "Mileage this month",
                value = formatGBP(thisMonthMileageTotal),
                subtitle = "${"%.1f".format(thisMonthMileageMiles)} miles claimed",
                icon = Icons.Default.TrendingUp
            )

            DashboardStatCard(
                modifier = Modifier.weight(1f),
                title = "Mileage YTD",
                value = formatGBP(yearToDateMileageTotal),
                subtitle = "Claims since January",
                icon = Icons.Default.Description
            )
        }

        DashboardSectionHeader("Insights")

        DashboardPanel(
            title = "Top supplier",
            icon = Icons.Default.TrendingUp
        ) {
            Text(
                text = topSupplierName,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = formatGBP(topSupplierAmount),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        DashboardPanel(
            title = "Top category",
            icon = Icons.Default.PieChart
        ) {
            Text(
                text = topCategoryName,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = formatGBP(topCategoryAmount),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        DashboardPanel(
            title = "Top journey",
            icon = Icons.Default.TrendingUp
        ) {
            Text(
                text = topJourneyName,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = formatGBP(topJourneyAmount),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (monthlyTrendData.isNotEmpty() || monthlyMileageMilesData.isNotEmpty()) {
            DashboardSectionHeader("Trends")
        }

        if (monthlyTrendData.isNotEmpty()) {
            DashboardPanel(
                title = "Monthly spend",
                icon = Icons.Default.TrendingUp
            ) {
                MonthlyTrendChart(
                    data = monthlyTrendData,
                    lineColor = Color(0xFF0077B6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }
        }

        if (monthlyMileageMilesData.isNotEmpty()) {
            DashboardPanel(
                title = "Monthly miles",
                icon = Icons.Default.TrendingUp
            ) {
                MonthlyTrendChart(
                    data = monthlyMileageMilesData,
                    lineColor = Color(0xFF0F766E),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }
        }

        if (donutCategoryData.isNotEmpty()) {
            DashboardSectionHeader("Breakdown")

            DashboardPanel(
                title = "Category breakdown",
                icon = Icons.Default.PieChart
            ) {
                CategoryDonutChart(
                    data = donutCategoryData,
                    selectedCategory = selectedCategory,
                    onCategorySelected = onCategorySelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                )
            }
        }
    }
}

@Composable
fun MonthlyTrendChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF0077B6)
) {
    if (data.isEmpty()) return

    val values = data.map { it.second }
    val maxValue = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0

    val labels = data.map { prettyMonthLabel(it.first).take(3) }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val leftPadding = 48f
            val rightPadding = 24f
            val topPadding = 24f
            val bottomPadding = 24f

            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding

            val gridColor = androidx.compose.ui.graphics.Color(0xFFE5E7EB)
            val axisTextColor = androidx.compose.ui.graphics.Color(0xFF6B7280)

            // horizontal grid lines
            for (i in 0..3) {
                val y = topPadding + (chartHeight / 3f) * i
                drawLine(
                    color = gridColor,
                    start = Offset(leftPadding, y),
                    end = Offset(size.width - rightPadding, y),
                    strokeWidth = 1f
                )
            }

            val points = data.mapIndexed { index, entry ->
                val x = if (data.size == 1) {
                    leftPadding + chartWidth / 2f
                } else {
                    leftPadding + (chartWidth / (data.size - 1)) * index
                }

                val yRatio = (entry.second / maxValue).toFloat()
                val y = topPadding + chartHeight - (chartHeight * yRatio)

                Offset(x, y)
            }

            if (points.isNotEmpty()) {
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)

                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val current = points[i]
                        val midX = (prev.x + current.x) / 2f

                        quadraticBezierTo(
                            prev.x,
                            prev.y,
                            midX,
                            (prev.y + current.y) / 2f
                        )
                        quadraticBezierTo(
                            current.x,
                            current.y,
                            current.x,
                            current.y
                        )
                    }
                }

                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, topPadding + chartHeight)
                    lineTo(points.first().x, topPadding + chartHeight)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.22f),
                            lineColor.copy(alpha = 0.04f)
                        ),
                        startY = topPadding,
                        endY = topPadding + chartHeight
                    )
                )

                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )

                points.forEach { point ->
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color.White,
                        radius = 9f,
                        center = point
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 7f,
                        center = point
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit
) {
    if (data.isEmpty()) return

    val total = data.sumOf { it.second }.takeIf { it > 0 } ?: 1.0

    val colors = listOf(
        Color(0xFF2563EB),
        Color(0xFF3B82F6),
        Color(0xFF60A5FA),
        Color(0xFF93C5FD),
        Color(0xFF38BDF8),
        Color(0xFF94A3B8)
    )

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val diameter = min(size.width, size.height) * 0.88f
                val strokeWidth = diameter * 0.30f
                val topLeft = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f
                )
                val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

                var startAngle = -90f

                data.forEachIndexed { index, entry ->
                    val sweep = ((entry.second / total) * 360f).toFloat()

                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )

                    startAngle += sweep
                }

                drawIntoCanvas { canvas ->
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#1F2933")
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                        textSize = 42f
                    }

                    val subPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#6B7280")
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 28f
                    }

                    canvas.nativeCanvas.drawText(
                        "£${"%.0f".format(total)}",
                        size.width / 2f,
                        size.height / 2f,
                        textPaint
                    )

                    canvas.nativeCanvas.drawText(
                        "Total",
                        size.width / 2f,
                        size.height / 2f + 36f,
                        subPaint
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEachIndexed { index, entry ->
                val pct = (entry.second / total) * 100.0

                val isSelected = entry.first == selectedCategory

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else
                                Color.Transparent
                        )
                        .clickable { onCategorySelected(entry.first) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(modifier = Modifier.size(12.dp)) {
                            drawCircle(color = colors[index % colors.size])
                        }

                        Text(
                            text = entry.first,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "${"%.0f".format(pct)}% • £${"%.2f".format(entry.second)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InfoPill(text: String) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ExpenseCard(
    expenseUi: ExpenseUi,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    analytics: FirebaseAnalytics,
    onDeleted: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit
) {
    var deleting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val expense = expenseUi.expense

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
            Text(
                text = formatGBP(expense.net),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoPill(expense.category.ifBlank { "Uncategorised" })
            }

            Text(
                text = expense.supplier.ifBlank { "No supplier" },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )

            Text(
                text = expense.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (expense.notes.isNotBlank()) {
                Text(
                    text = expense.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            if (errorMsg.isNotBlank()) {
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onEdit,
                    enabled = !deleting
                ) {
                    Text("Edit")
                }

                TextButton(
                    onClick = onDuplicate,
                    enabled = !deleting
                ) {
                    Text("Duplicate")
                }

                TextButton(
                    onClick = {
                        showDeleteConfirm = true
                    },
                    enabled = !deleting
                ) {
                    if (deleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = {
                        Text("Delete expense?")
                    },
                    text = {
                        Text("This will permanently remove this expense entry.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val uid = auth.currentUser?.uid
                                if (uid == null) {
                                    errorMsg = "No signed-in user."
                                    showDeleteConfirm = false
                                    return@TextButton
                                }

                                deleting = true
                                errorMsg = ""
                                showDeleteConfirm = false

                                db.collection("users")
                                    .document(uid)
                                    .collection("expenses")
                                    .document(expenseUi.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        val bundle = Bundle().apply {
                                            putString("category", expense.category.lowercase())
                                            putString("vat_code", expense.vatCode)
                                            putDouble("amount", expense.net)
                                        }

                                        analytics.logEvent("expense_deleted", bundle)

                                        deleting = false
                                        onDeleted()
                                    }
                                    .addOnFailureListener { e ->
                                        deleting = false
                                        errorMsg = e.message ?: "Failed to delete expense."
                                    }
                            }
                        ) {
                            Text(
                                text = "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteConfirm = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MileageCard(
    mileageUi: MileageUi,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    analytics: FirebaseAnalytics,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDeleted: () -> Unit
) {
    var deleting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val mileage = mileageUi.mileage

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
            Text(
                text = formatGBP(mileage.total),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoPill("${"%.1f".format(mileage.miles)} miles")
            }

            Text(
                text = mileage.journey.ifBlank { "Mileage claim" },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )

            Text(
                text = "${mileage.date} • ${mileage.fromLocation} → ${mileage.toLocation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "Rate: £${"%.2f".format(mileage.ratePerMile)}/mile",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )

            if (mileage.notes.isNotBlank()) {
                Text(
                    text = mileage.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            if (errorMsg.isNotBlank()) {
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onEdit,
                    enabled = !deleting
                ) {
                    Text("Edit")
                }

                TextButton(
                    onClick = onDuplicate,
                    enabled = !deleting
                ) {
                    Text("Duplicate")
                }

                TextButton(
                    onClick = {
                        showDeleteConfirm = true
                    },
                    enabled = !deleting
                ) {
                    if (deleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = {
                        Text("Delete mileage claim?")
                    },
                    text = {
                        Text("This will permanently remove this mileage entry.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val uid = auth.currentUser?.uid
                                if (uid == null) {
                                    errorMsg = "No signed-in user."
                                    showDeleteConfirm = false
                                    return@TextButton
                                }

                                deleting = true
                                errorMsg = ""
                                showDeleteConfirm = false

                                db.collection("users")
                                    .document(uid)
                                    .collection("mileage")
                                    .document(mileageUi.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        val bundle = Bundle().apply {
                                            putDouble("miles", mileage.miles)
                                            putDouble("rate_per_mile", mileage.ratePerMile)
                                            putDouble("claim_total", mileage.total)
                                        }

                                        analytics.logEvent("mileage_deleted", bundle)

                                        deleting = false
                                        onDeleted()
                                    }
                                    .addOnFailureListener { e ->
                                        deleting = false
                                        errorMsg = e.message ?: "Failed to delete mileage claim."
                                    }
                            }
                        ) {
                            Text(
                                text = "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteConfirm = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

data class ExpenseUi(
    val id: String,
    val expense: Expense
)

val quickCategories = listOf(
    "Fuel",
    "Travel",
    "Food",
    "Software",
    "Parking",
    "Supplies"
)

fun currentMonthKey(): String {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    return "$year-$month"
}

fun monthKeyFromDate(date: String): String {
    return if (date.length >= 7) date.substring(0, 7) else ""
}

fun previousMonthKey(): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MONTH, -1)
    val year = calendar.get(Calendar.YEAR)
    val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    return "$year-$month"
}

fun currentYearKey(): String {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.YEAR).toString()
}

fun prettyMonthLabel(monthKey: String): String {
    val parts = monthKey.split("-")
    if (parts.size != 2) return monthKey

    val year = parts[0].toIntOrNull() ?: return monthKey
    val month = parts[1].toIntOrNull() ?: return monthKey

    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, java.util.Locale.UK) ?: return monthKey
    return "$monthName ${parts[0]}"
}

fun nextMonthKey(monthKey: String): String {
    val parts = monthKey.split("-")
    if (parts.size != 2) return monthKey

    val year = parts[0].toIntOrNull() ?: return monthKey
    val month = parts[1].toIntOrNull() ?: return monthKey

    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, 1)
    }

    val y = calendar.get(Calendar.YEAR)
    val m = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    return "$y-$m"
}

fun todayIsoDate(): String {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
    return "$year-$month-$day"
}

fun formatGBP(amount: Double): String {
    return "£${"%.2f".format(amount)}"
}

/*
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SimpleExpensesAppTheme {
        LoginScreen()
    }
}

 */