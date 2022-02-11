package com.app.camera2demo

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Created by Priyanka
 */
class PermissionsCheck(
    val context: Activity,
    val permissions: Array<String>,
    val requestCode: Int
) {

    fun checkPermissionGiven(): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                permissions[0]
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }

        return false
    }


    fun requestPermission() {
        ActivityCompat.requestPermissions(
            context,
            permissions,
            requestCode
        )
    }

    fun onRequestPermissionsResult(
        grantResults: IntArray
    ): Boolean {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestPermission()
            return true
        } else {
            if (!context.shouldShowRequestPermissionRationale(permissions[0])) {
                val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                builder.setMessage(
                    "Permission required"
                )
                builder.setCancelable(false)
                builder.setPositiveButton(
                    "Settings"
                ) { dialog, _ ->
                    dialog.dismiss()
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri: Uri = Uri.fromParts("package", context.packageName, null)
                    intent.data = uri
                    context.startActivity(intent)
                }
                builder.setNegativeButton("Cancel", null)
                builder.show()
            }
        }
        return false
    }
}