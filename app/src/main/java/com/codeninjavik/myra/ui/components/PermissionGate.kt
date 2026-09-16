package com.codeninjavik.myra.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.codeninjavik.myra.ui.theme.Ink
import com.codeninjavik.myra.ui.theme.RedAccent
import com.codeninjavik.myra.ui.theme.TextPrimary
import com.codeninjavik.myra.ui.theme.TextSecondary

@Composable
fun PermissionGate(
    onPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Required permissions for Stage 1 (Forwarding Module)
    val requiredPermissions = remember {
        mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                add(Manifest.permission.READ_PHONE_NUMBERS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    var permissionsGranted by remember {
        mutableStateOf(hasAllPermissions(context, requiredPermissions))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        permissionsGranted = allGranted
        if (allGranted) {
            onPermissionsGranted()
        }
    }

    if (permissionsGranted) {
        content()
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Ink)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                tint = RedAccent,
                modifier = Modifier.padding(16.dp)
            )

            Text(
                text = "MYRA Permissions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Before we configure your AI Call receptionist, MYRA needs specific access to your cellular radio to handle forwarding settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            PermissionExplanationRow(
                icon = Icons.Default.Call,
                title = "Place Forwarding MMI Calls (CALL_PHONE)",
                description = "MYRA places one call containing your carrier's forwarding code to instantly configure call routing. It does not place unsolicited calls."
            )

            Spacer(modifier = Modifier.height(16.dp))

            PermissionExplanationRow(
                icon = Icons.Default.PhoneAndroid,
                title = "Read Cellular Subscriptions (READ_PHONE_STATE)",
                description = "Necessary to read your dual-SIM status and target the correct SIM card so forwarding rules land on your desired number."
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(16.dp))
                PermissionExplanationRow(
                    icon = Icons.Default.Notifications,
                    title = "Post Live Call Alerts (POST_NOTIFICATIONS)",
                    description = "We send highly prioritized alerts whenever a call is active so you can watch live transcripts instantly."
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            PrimaryButton(
                text = "Grant Access",
                onClick = { launcher.launch(requiredPermissions) }
            )
        }
    }
}

@Composable
private fun PermissionExplanationRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "• $title",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

private fun hasAllPermissions(context: Context, permissions: Array<String>): Boolean {
    return permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
