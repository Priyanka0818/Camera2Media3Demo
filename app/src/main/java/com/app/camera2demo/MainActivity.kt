package com.app.camera2demo

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.camera2demo.databinding.ActivityMainBinding


/**
 * Created by Priyanka
 */
const val CAMERA_REQUEST_RESULT = 1
const val VIDEO_REQUEST_RESULT = 2

class MainActivity : AppCompatActivity(), PermissionCallback {

    private var activityMainBinding: ActivityMainBinding? = null
    val cameraPermCheck = PermissionsCheck(
        this,
        Manifest.permission.CAMERA,
        requestCode = CAMERA_REQUEST_RESULT,
        permissionCallback = this
    )
    val videoPermCheck = PermissionsCheck(
        this,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        requestCode = VIDEO_REQUEST_RESULT,
        permissionCallback = this
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding?.root)
        initListeners()
    }

    private fun initListeners() {
        activityMainBinding?.takePhotoBtn?.setOnClickListener {
            if (cameraPermCheck.checkPermission()
            ) {
                startActivity(
                    Intent(this, CaptureActivity::class.java).putExtra(
                        "type",
                        "image"
                    )
                )
            } else {
                cameraPermCheck.requestPermission()
            }
        }
        activityMainBinding?.recordVideoBtn?.setOnClickListener {
            if (videoPermCheck.checkPermission()
            ) {
                startActivity(
                    Intent(this, CaptureActivity::class.java).putExtra(
                        "type",
                        "video"
                    )
                )
            } else {
                videoPermCheck.requestPermission()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityMainBinding = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == VIDEO_REQUEST_RESULT) {
            videoPermCheck.onRequestPermissionsResult(grantResults)
        } else {
            cameraPermCheck.onRequestPermissionsResult(grantResults)
        }
    }

    override fun permissionGranted(vararg permissions: String, requestCode: Int) {
        if (requestCode == VIDEO_REQUEST_RESULT) {
            startActivity(
                Intent(this, CaptureActivity::class.java).putExtra(
                    "type",
                    "video"
                )
            )
        } else {
            startActivity(
                Intent(this, CaptureActivity::class.java).putExtra(
                    "type",
                    "image"
                )
            )
        }
    }

    override fun permissionDenied(vararg permissions: String, requestCode: Int) {
        Toast.makeText(this, "Permissions denied $permissions", Toast.LENGTH_SHORT)
            .show()
    }

}