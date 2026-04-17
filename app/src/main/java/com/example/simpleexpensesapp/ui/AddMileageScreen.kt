package com.example.simpleexpensesapp.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.simpleexpensesapp.compressImageFromUri
import com.example.simpleexpensesapp.createTempImageUri
import com.example.simpleexpensesapp.formatGBP
import com.example.simpleexpensesapp.isImageMimeType
import com.example.simpleexpensesapp.resolveMimeType
import com.example.simpleexpensesapp.todayIsoDate
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import java.util.Calendar

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

    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var attachmentName by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

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
            pendingCameraUri = null
            errorMsg = ""
        } else {
            pendingCameraUri = null
        }
    }

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

            val mimeType = attachmentUri?.let { uri ->
                resolveMimeType(context, uri, attachmentName)
            }.orEmpty()

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
                    val storage = FirebaseStorage.getInstance()
                    val mileageRef = db.collection("users")
                        .document(uid)
                        .collection("mileage")

                    fun saveMileage(receiptData: Map<String, Any?>?) {
                        val payload = hashMapOf<String, Any?>(
                            "date" to date,
                            "journey" to journey.trim(),
                            "fromLocation" to fromLocation.trim(),
                            "toLocation" to toLocation.trim(),
                            "miles" to parsedMiles,
                            "ratePerMile" to parsedRate,
                            "total" to total,
                            "notes" to notes.trim(),
                            "attachment" to receiptData,
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )

                        mileageRef
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
                    }

                    if (attachmentUri != null) {
                        val selectedUri = attachmentUri!!
                        val detectedMimeType = resolveMimeType(context, selectedUri, attachmentName)
                        val isImage = isImageMimeType(detectedMimeType)

                        val safeName = (
                                attachmentName.ifBlank {
                                    if (isImage) "attachment.jpg" else "attachment"
                                }
                                ).replace(" ", "_")

                        val finalFileName = if (
                            isImage &&
                            !safeName.lowercase().endsWith(".jpg") &&
                            !safeName.lowercase().endsWith(".jpeg")
                        ) {
                            safeName.substringBeforeLast(".") + ".jpg"
                        } else {
                            safeName
                        }

                        val path = "users/$uid/mileage_receipts/${System.currentTimeMillis()}_$finalFileName"
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

                                    saveMileage(receiptData)
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

                                    saveMileage(receiptData)
                                }
                                .addOnFailureListener { e ->
                                    saving = false
                                    errorMsg = e.message ?: "Failed to upload attachment."
                                }
                        }
                    } else {
                        saveMileage(null)
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