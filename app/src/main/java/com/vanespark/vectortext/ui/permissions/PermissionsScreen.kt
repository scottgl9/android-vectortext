package com.vanespark.vectortext.ui.permissions

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vanespark.vectortext.R
import com.vanespark.vectortext.domain.service.PermissionManager

/**
 * Permissions setup screen
 * Guides user through granting SMS permissions and setting as default app
 */
@Composable
fun PermissionsScreen(
    permissionManager: PermissionManager,
    onPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasAllPermissions by remember { mutableStateOf(permissionManager.hasAllSmsPermissions()) }
    var isDefaultSmsApp by remember { mutableStateOf(permissionManager.isDefaultSmsApp()) }

    // Permissions launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasAllPermissions = permissions.values.all { it }
        if (hasAllPermissions && isDefaultSmsApp) {
            onPermissionsGranted()
        }
    }

    // Default SMS app launcher
    val defaultSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Check if app is now default
        isDefaultSmsApp = permissionManager.isDefaultSmsApp()
        if (hasAllPermissions && isDefaultSmsApp) {
            onPermissionsGranted()
        }
    }

    // Monitor lifecycle to refresh permission status
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAllPermissions = permissionManager.hasAllSmsPermissions()
                isDefaultSmsApp = permissionManager.isDefaultSmsApp()
                if (hasAllPermissions && isDefaultSmsApp) {
                    onPermissionsGranted()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App icon
        Icon(
            imageVector = Icons.Default.Sms,
            contentDescription = null,
            modifier = Modifier
                .padding(bottom = 24.dp)
                .height(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        // Title
        Text(
            text = stringResource(R.string.setup_permissions),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = stringResource(R.string.setup_permissions_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // SMS Permissions Card
        PermissionCard(
            title = stringResource(R.string.sms_permissions),
            description = stringResource(R.string.sms_permissions_description),
            isGranted = hasAllPermissions,
            onGrantClick = {
                permissionsLauncher.launch(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionManager.REQUIRED_PERMISSIONS_API_33
                    } else {
                        PermissionManager.REQUIRED_PERMISSIONS
                    }
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Default SMS App Card
        PermissionCard(
            title = stringResource(R.string.default_sms_app),
            description = stringResource(R.string.default_sms_app_description),
            isGranted = isDefaultSmsApp,
            onGrantClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = context.getSystemService(RoleManager::class.java)
                    if (roleManager?.isRoleAvailable(RoleManager.ROLE_SMS) == true) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                        defaultSmsLauncher.launch(intent)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val intent = Intent(android.provider.Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                    intent.putExtra(android.provider.Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                    (context as? Activity)?.startActivity(intent)
                }
            },
            enabled = hasAllPermissions // Can only set as default after permissions granted
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Continue button (shown when all done)
        if (hasAllPermissions && isDefaultSmsApp) {
            Button(
                onClick = onPermissionsGranted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.continue_to_app))
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Title with checkmark
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isGranted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isGranted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(16.dp))

                if (enabled) {
                    Button(
                        onClick = onGrantClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.grant_permission))
                    }
                } else {
                    OutlinedButton(
                        onClick = { /* Disabled */ },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    ) {
                        Text(stringResource(R.string.grant_permission))
                    }
                }
            }
        }
    }
}
