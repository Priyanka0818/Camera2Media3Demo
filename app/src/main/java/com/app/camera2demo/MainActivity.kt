package com.app.camera2demo

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.app.camera2demo.databinding.ActivityMainBinding


/**
 * Created by Priyanka
 */
const val CAMERA_REQUEST_RESULT = 1
const val VIDEO_REQUEST_RESULT = 2

class MainActivity : AppCompatActivity() {

    private var activityMainBinding: ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding?.root)
        initListeners()
    }

    private fun initListeners() {
        activityMainBinding?.takePhotoBtn?.setOnClickListener {
            if (PermissionsCheck(
                    this@MainActivity, arrayOf(Manifest.permission.CAMERA),
                    CAMERA_REQUEST_RESULT
                ).checkPermissionGiven()
            ) {
                startActivity(
                    Intent(this, CaptureActivity::class.java).putExtra(
                        "type",
                        "image"
                    )
                )
            } else {
                PermissionsCheck(
                    this@MainActivity, arrayOf(Manifest.permission.CAMERA),
                    CAMERA_REQUEST_RESULT
                ).requestPermission()
            }
        }
        activityMainBinding?.recordVideoBtn?.setOnClickListener {
            if (PermissionsCheck(
                    this@MainActivity, arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    VIDEO_REQUEST_RESULT
                ).checkPermissionGiven()
            ) {
                startActivity(
                    Intent(this, CaptureActivity::class.java).putExtra(
                        "type",
                        "video"
                    )
                )
            } else {
                PermissionsCheck(
                    this@MainActivity, arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    VIDEO_REQUEST_RESULT
                ).requestPermission()
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
        if (PermissionsCheck(
                this@MainActivity, arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
                ),
                VIDEO_REQUEST_RESULT
            ).onRequestPermissionsResult(grantResults)
        ) {
            if (requestCode == VIDEO_REQUEST_RESULT)
                startActivity(Intent(this, CaptureActivity::class.java))
        } else if (PermissionsCheck(
                this@MainActivity, arrayOf(
                    Manifest.permission.CAMERA
                ),
                CAMERA_REQUEST_RESULT
            ).onRequestPermissionsResult(grantResults)
        ) {
            if (requestCode == VIDEO_REQUEST_RESULT)
                startActivity(Intent(this, CaptureActivity::class.java))
        }
    }
}