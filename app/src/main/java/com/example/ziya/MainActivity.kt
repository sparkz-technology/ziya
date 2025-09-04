package com.example.ziya

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val permissionsToRequest = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No need to set content view as this activity is just a launcher
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        // Reset the list
        permissionsToRequest.clear()

        // Check for Overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            Toast.makeText(this, "Please grant overlay permission to Ziya.", Toast.LENGTH_LONG).show()
            // We start this for result to know when the user comes back
            overlayPermissionLauncher.launch(intent)
            return // Wait for the user to return from settings
        }

        // Check for Microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        
        // Check for Notification permission (required for Foreground Service on API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                 permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }


        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startOverlay()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // When the user returns from the settings screen, check permissions again.
        checkAndRequestPermissions()
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startOverlay()
        } else {
            Toast.makeText(this, "All permissions are required to use Ziya.", Toast.LENGTH_LONG).show()
        }
    }

    private fun startOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
             Toast.makeText(this, "Overlay permission is still not granted. The app cannot start.", Toast.LENGTH_LONG).show()
             return
        }

        val serviceIntent = Intent(this, OverlayService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        finish() // Close the activity as we only need the service running
    }
}
