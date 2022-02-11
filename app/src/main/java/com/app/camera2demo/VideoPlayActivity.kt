package com.app.camera2demo

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import com.app.camera2demo.databinding.ActivityVideoPlayBinding
import com.google.common.util.concurrent.ListenableFuture
import java.io.File


/**
 * Created by Priyanka
 */
class VideoPlayActivity : AppCompatActivity() {

    private var activityVideoPlayBinding: ActivityVideoPlayBinding? = null
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private val controller: MediaController?
        get() = if (controllerFuture.isDone) controllerFuture.get() else null
    private val currentWindow = 0
    private var player: ExoPlayer? = null
    private val playbackPosition: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityVideoPlayBinding = ActivityVideoPlayBinding.inflate(layoutInflater)
        setContentView(activityVideoPlayBinding?.root)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        activityVideoPlayBinding?.playerView?.player = player
        player?.playWhenReady = true
        player?.seekTo(currentWindow, playbackPosition)
        player?.prepare()
        val videoUrl: Uri = Uri.fromFile(File(intent.getStringExtra("file")!!))
//        val mediaItem = MediaItem.fromUri("https://file-examples-com.github.io/uploads/2017/04/file_example_MP4_640_3MG.mp4")
        val mediaItem = MediaItem.fromUri(videoUrl)
        player?.setMediaItem(mediaItem)
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onResume() {
        super.onResume()
        activityVideoPlayBinding?.playerView?.onResume()
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onPause() {
        super.onPause()
        activityVideoPlayBinding?.playerView?.onPause()
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun onStop() {
        super.onStop()
        activityVideoPlayBinding?.playerView?.player = null
        releasePlayer()
    }

    private fun releasePlayer() {
        if (player != null) {
            player?.release()
            player = null
        }
    }
}