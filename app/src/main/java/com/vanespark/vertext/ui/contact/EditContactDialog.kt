package com.vanespark.vertext.ui.contact

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vanespark.vertext.data.model.Contact

/**
 * Dialog for editing contact information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContactDialog(
    contact: Contact,
    onDismiss: () -> Unit,
    onSave: (name: String, notes: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(contact.name) }
    var notes by remember { mutableStateOf(contact.notes ?: "") }

    val canSave = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(0.95f)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = "Edit Contact",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("Contact name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Phone (read-only)
                OutlinedTextField(
                    value = contact.phone,
                    onValueChange = { },
                    label = { Text("Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    readOnly = true,
                    singleLine = true
                )

                // Notes field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    placeholder = { Text("Add notes about this contact...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(name, notes) },
                        enabled = canSave,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
