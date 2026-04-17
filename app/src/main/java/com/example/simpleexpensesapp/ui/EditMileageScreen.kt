package com.example.simpleexpensesapp.ui

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.simpleexpensesapp.MileageUi
import com.example.simpleexpensesapp.formatGBP
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

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