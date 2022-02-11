package com.app.camera2demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.app.camera2demo.databinding.ActivityImageShowBinding
import com.bumptech.glide.Glide
import java.io.File

/**
 * Created by Priyanka
 */
class ImageShowActivity : AppCompatActivity() {

    private var activityImageShowBinding: ActivityImageShowBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityImageShowBinding = ActivityImageShowBinding.inflate(layoutInflater)
        setContentView(activityImageShowBinding?.root)
        Glide.with(this@ImageShowActivity)
            .load(File(intent.getStringExtra("file")!!))
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .into(activityImageShowBinding?.img!!)
    }
}