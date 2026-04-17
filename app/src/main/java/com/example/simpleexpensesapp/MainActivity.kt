package com.example.simpleexpensesapp

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.simpleexpensesapp.ui.AddMileageScreen
import com.example.simpleexpensesapp.ui.ExpensesScreen
import com.example.simpleexpensesapp.ui.SettingsScreen
import com.example.simpleexpensesapp.ui.theme.SimpleExpensesAppTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import java.io.ByteArrayOutputStream
import java.io.File
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
            val navController = rememberNavController()

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
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Expenses.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Dashboard.route) {
                                ExpensesScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    auth = auth,
                                    db = db,
                                    analytics = analytics,
                                    userEmail = signedInUserEmail ?: "",
                                    navController = navController
                                )
                            }

                            composable(Screen.Expenses.route) {
                                ExpensesScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    auth = auth,
                                    db = db,
                                    analytics = analytics,
                                    userEmail = signedInUserEmail ?: "",
                                    navController = navController
                                )
                            }

                            composable(Screen.Mileage.route) {
                                ExpensesScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    auth = auth,
                                    db = db,
                                    analytics = analytics,
                                    userEmail = signedInUserEmail ?: "",
                                    navController = navController
                                )
                            }

                            composable(Screen.Settings.route) {
                                SettingsScreen(
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
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.simple_expenses_logo),
            contentDescription = "Simple Expenses logo",
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Track expenses across web and mobile",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp)
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
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var attachmentName by remember { mutableStateOf("") }

    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var vatExpanded by remember { mutableStateOf(false) }

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            attachmentUri = uri
            attachmentName = uri.lastPathSegment ?: "Selected file"
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
        }
    }

    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            attachmentUri = pendingCameraUri
            attachmentName = "photo_${System.currentTimeMillis()}.jpg"
            errorMsg = ""
            pendingCameraUri = null
        } else {
            pendingCameraUri = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
            label = { Text("Gross (£)") },
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

        Text(
            text = "Attachment",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp)
        )

        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val tempUri = createTempImageUri(context)
                    pendingCameraUri = tempUri
                    attachmentUri = tempUri
                    attachmentName = "photo_${System.currentTimeMillis()}.jpg"
                    takePhotoLauncher.launch(tempUri)
                }
            ) {
                Text("Take photo")
            }

            OutlinedButton(
                onClick = {
                    filePickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                }
            ) {
                Text("Attach image or PDF")
            }
        }

        if (attachmentName.isNotBlank()) {
            Text(
                text = "Selected: $attachmentName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            val mimeType = attachmentUri?.let { context.contentResolver.getType(it) }.orEmpty()

            if (mimeType.startsWith("image/")) {
                val previewBitmap = remember(attachmentUri) {
                    attachmentUri?.let { uri ->
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            BitmapFactory.decodeStream(input)
                        }
                    }
                }

                previewBitmap?.let { bitmap ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Attachment preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    }
                }
            } else if (mimeType == "application/pdf") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        text = "PDF selected",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            TextButton(
                onClick = {
                    attachmentUri = null
                    attachmentName = ""
                    pendingCameraUri = null
                    errorMsg = ""
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Remove attachment")
            }
        }

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
                        errorMsg = "Enter a valid gross amount."
                        return@Button
                    }

                    saving = true
                    errorMsg = ""

                    val storage = FirebaseStorage.getInstance()
                    val expenseRef = db.collection("users")
                        .document(uid)
                        .collection("expenses")

                    fun saveExpense(receiptData: Map<String, Any?>?) {
                        val payload = hashMapOf<String, Any?>(
                            "date" to date,
                            "supplier" to supplier.trim(),
                            "category" to category.trim(),
                            "net" to netValue,
                            "vatCode" to vatCode,
                            "notes" to notes.trim(),
                            "receipt" to receiptData,
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )

                        expenseRef
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
                    }

                    if (attachmentUri != null) {
                        val selectedUri = attachmentUri!!
                        val detectedMimeType = context.contentResolver.getType(selectedUri) ?: "unknown"
                        val isImage = detectedMimeType.startsWith("image/")

                        val safeName = (
                                attachmentName.ifBlank {
                                    if (isImage) "attachment.jpg" else "attachment"
                                }
                                ).replace(" ", "_")

                        val finalFileName = if (isImage && !safeName.lowercase().endsWith(".jpg") && !safeName.lowercase().endsWith(".jpeg")) {
                            safeName.substringBeforeLast(".") + ".jpg"
                        } else {
                            safeName
                        }

                        val path = "users/$uid/receipts/${System.currentTimeMillis()}_$finalFileName"
                        val fileRef = storage.reference.child(path)

                        if (isImage) {
                            val compressedBytes = compressImageFromUri(
                                context = context,
                                uri = selectedUri,
                                maxWidth = 1600,
                                jpegQuality = 80
                            )

                            if (compressedBytes == null) {
                                saving = false
                                errorMsg = "Failed to process image."
                                return@Button
                            }

                            val metadata = storageMetadata {
                                contentType = "image/jpeg"
                            }

                            fileRef.putBytes(compressedBytes, metadata)
                                .continueWithTask { task ->
                                    if (!task.isSuccessful) {
                                        throw task.exception ?: Exception("Upload failed.")
                                    }
                                    fileRef.downloadUrl
                                }
                                .addOnSuccessListener { downloadUri ->
                                    val receiptData = hashMapOf<String, Any?>(
                                        "url" to downloadUri.toString(),
                                        "path" to path,
                                        "name" to finalFileName,
                                        "type" to "image/jpeg"
                                    )

                                    saveExpense(receiptData)
                                }
                                .addOnFailureListener { e ->
                                    saving = false
                                    errorMsg = e.message ?: "Failed to upload image."
                                }
                        } else {
                            fileRef.putFile(selectedUri)
                                .continueWithTask { task ->
                                    if (!task.isSuccessful) {
                                        throw task.exception ?: Exception("Upload failed.")
                                    }
                                    fileRef.downloadUrl
                                }
                                .addOnSuccessListener { downloadUri ->
                                    val receiptData = hashMapOf<String, Any?>(
                                        "url" to downloadUri.toString(),
                                        "path" to path,
                                        "name" to safeName,
                                        "type" to detectedMimeType
                                    )

                                    saveExpense(receiptData)
                                }
                                .addOnFailureListener { e ->
                                    saving = false
                                    errorMsg = e.message ?: "Failed to upload attachment."
                                }
                        }
                    } else {
                        saveExpense(null)
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
    val context = LocalContext.current

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

            if (!expense.receipt?.url.isNullOrBlank()) {
                Text(
                    text = "📎 Receipt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable {
                            openUrl(context, expense.receipt?.url.orEmpty())
                        }
                )
            }

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
    val context = LocalContext.current

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

            if (!mileage.attachment?.url.isNullOrBlank()) {
                Text(
                    text = "📎 Evidence",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable {
                            openUrl(context, mileage.attachment?.url.orEmpty())
                        }
                )
            }

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

fun openUrl(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

fun createTempImageUri(context: Context): Uri {
    val imageFile = File.createTempFile(
        "expense_photo_${System.currentTimeMillis()}",
        ".jpg",
        context.cacheDir
    )

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

fun compressImageFromUri(
    context: Context,
    uri: Uri,
    maxWidth: Int = 1600,
    jpegQuality: Int = 80
): ByteArray? {
    val originalBitmap = context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input)
    } ?: return null

    val resizedBitmap = resizeBitmapIfNeeded(originalBitmap, maxWidth)

    return ByteArrayOutputStream().use { output ->
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, output)
        output.toByteArray()
    }
}

private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxWidth: Int): Bitmap {
    if (bitmap.width <= maxWidth) return bitmap

    val ratio = maxWidth.toFloat() / bitmap.width.toFloat()
    val targetHeight = (bitmap.height * ratio).toInt()

    return Bitmap.createScaledBitmap(
        bitmap,
        maxWidth,
        targetHeight,
        true
    )
}

fun resolveMimeType(
    context: Context,
    uri: Uri,
    attachmentName: String
): String {
    val resolverType = context.contentResolver.getType(uri)
    if (!resolverType.isNullOrBlank()) return resolverType

    val name = attachmentName.lowercase()
    val uriString = uri.toString().lowercase()

    return when {
        name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                uriString.endsWith(".jpg") || uriString.endsWith(".jpeg") -> "image/jpeg"

        name.endsWith(".png") ||
                uriString.endsWith(".png") -> "image/png"

        name.endsWith(".heic") || name.endsWith(".heif") ||
                uriString.endsWith(".heic") || uriString.endsWith(".heif") -> "image/heic"

        name.endsWith(".pdf") ||
                uriString.endsWith(".pdf") -> "application/pdf"

        else -> "application/octet-stream"
    }
}

fun isImageMimeType(mimeType: String): Boolean {
    return mimeType.startsWith("image/")
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