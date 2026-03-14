package com.example.simpleexpensesapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.simpleexpensesapp.ui.theme.SimpleExpensesAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

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
    userEmail: String,
    onSignOut: () -> Unit
) {
    var expenses by remember { mutableStateOf(listOf<ExpenseUi>()) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            errorMsg = "No signed-in user."
            loading = false
            return@LaunchedEffect
        }

        loading = true
        errorMsg = ""

        db.collection("users")
            .document(uid)
            .collection("expenses")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                expenses = result.documents.map { doc ->
                    val expense = doc.toObject<Expense>() ?: Expense()
                    ExpenseUi(
                        id = doc.id,
                        expense = expense
                    )
                }
                loading = false
            }
            .addOnFailureListener { e ->
                errorMsg = e.message ?: "Failed to load expenses."
                loading = false
            }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (!showAddForm) {
                FloatingActionButton(onClick = { showAddForm = true }) {
                    Text("Add")
                }
            }
        }
    ) { innerPadding ->
        if (showAddForm) {
            AddExpenseScreen(
                modifier = Modifier.padding(innerPadding),
                auth = auth,
                db = db,
                onCancel = { showAddForm = false },
                onSaved = {
                    showAddForm = false
                    reloadKey++
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Simple Expenses",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Button(onClick = onSignOut) {
                        Text("Sign out")
                    }
                }

                Spacer(modifier = Modifier.padding(8.dp))

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

                    expenses.isEmpty() -> {
                        Text(
                            text = "No expenses yet.",
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(expenses, key = { it.id }) { item ->
                                ExpenseCard(expense = item.expense)
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

@Composable
fun ExpenseCard(expense: Expense) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "£${"%.2f".format(expense.net)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = expense.category,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${expense.date} • ${expense.supplier}",
                style = MaterialTheme.typography.bodySmall
            )
            if (expense.notes.isNotBlank()) {
                Text(
                    text = expense.notes,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

data class ExpenseUi(
    val id: String,
    val expense: Expense
)

fun todayIsoDate(): String {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
    val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
    return "$year-$month-$day"
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