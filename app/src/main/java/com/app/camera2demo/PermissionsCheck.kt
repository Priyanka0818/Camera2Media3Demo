package com.app.camera2demo

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat

/**
 * Created by Priyanka
 */
class PermissionsCheck(
    val context: Activity, private vararg val permissions: String, private val requestCode: Int,
    val permissionCallback: PermissionCallback
) {


    fun checkPermission(): Boolean =
        permissions.all {
            ActivityCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }


    private fun shouldShowRationale(vararg perms: String): Boolean {
        for (perm in perms) {
            if (context.shouldShowRequestPermissionRationale(perm)) {
                return true
            }
        }
        return false
    }

    fun requestPermission() {
        if (shouldShowRationale(*permissions)) {
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
        } else {
            ActivityCompat.requestPermissions(
                context,
                permissions,
                requestCode
            )
        }
    }

    fun onRequestPermissionsResult(
        grantResults: IntArray
    ): Boolean {
        val granted: MutableList<String> = ArrayList()
        val denied: MutableList<String> = ArrayList()
        for (i in permissions.indices) {
            val perm = permissions[i]
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                granted.add(perm)
            } else {
                denied.add(perm)
            }
        }
        if (granted.isNotEmpty() && denied.isEmpty()) {
            Toast.makeText(context, "Permissions Granted", Toast.LENGTH_SHORT)
                .show()
            permissionCallback.permissionGranted(granted.toString(),requestCode = requestCode)
        } else if (denied.isNotEmpty()) {
            Toast.makeText(
                context,
                "Permissions denied $denied",
                Toast.LENGTH_SHORT
            ).show()
            permissionCallback.permissionDenied(denied.toString(),requestCode = requestCode)
        }
        return false
    }
}