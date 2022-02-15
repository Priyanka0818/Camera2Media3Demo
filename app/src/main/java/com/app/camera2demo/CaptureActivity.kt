package com.app.camera2demo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.CamcorderProfile
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Display
import android.view.Surface
import android.view.TextureView
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.hardware.display.DisplayManagerCompat
import com.app.camera2demo.databinding.ActivityImageCaptureBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

/**
 * Created by Priyanka
 */
class CaptureActivity : AppCompatActivity() {

    private var activityImageCaptureBinding: ActivityImageCaptureBinding? = null
    private lateinit var cameraId: String
    private lateinit var backgroundHandlerThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader
    private lateinit var previewSize: Size
    private lateinit var videoSize: Size
    private lateinit var file: String
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var shouldProceedWithOnResume: Boolean = true
    private var orientations: SparseIntArray = SparseIntArray(4).apply {
        append(Surface.ROTATION_0, 0)
        append(Surface.ROTATION_90, 90)
        append(Surface.ROTATION_180, 180)
        append(Surface.ROTATION_270, 270)
    }
    private var rotation: Int? = null
    private lateinit var mediaRecorder: MediaRecorder
    private var sensorOrientation: Int? = null
    private lateinit var cameraCharacteristics: CameraCharacteristics

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityImageCaptureBinding = ActivityImageCaptureBinding.inflate(layoutInflater)
        setContentView(activityImageCaptureBinding?.root)
        initCameraService()

        activityImageCaptureBinding?.ivCapture?.setOnClickListener {
            if (intent.getStringExtra("type") == "image") {
                capturePhoto()
            } else {
                activityImageCaptureBinding?.ivStop?.visibility = VISIBLE
                activityImageCaptureBinding?.ivCapture?.visibility = GONE
                captureVideo()
            }
        }
        activityImageCaptureBinding?.ivStop?.setOnClickListener {
            mediaRecorder.stop()
            mediaRecorder.reset()
            activityImageCaptureBinding?.ivCapture?.visibility = VISIBLE
            activityImageCaptureBinding?.ivStop?.visibility = GONE
            startActivity(
                Intent(this@CaptureActivity, VideoPlayActivity::class.java).putExtra(
                    "file",
                    file
                )
            )
            finish()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (activityImageCaptureBinding?.textureView?.isAvailable!! && shouldProceedWithOnResume) {
            setupCamera()
        } else if (!activityImageCaptureBinding?.textureView?.isAvailable!!) {
            activityImageCaptureBinding?.textureView?.surfaceTextureListener =
                surfaceTextureListener
        }
        shouldProceedWithOnResume = !shouldProceedWithOnResume
    }

    private fun setupCamera() {
        val cameraIds: Array<String> = cameraManager.cameraIdList

        for (id in cameraIds) {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(id)
            sensorOrientation =
                cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

            //If we want to choose the rear facing camera instead of the front facing one
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                continue
            }

            val streamConfigurationMap: StreamConfigurationMap? =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            if (streamConfigurationMap != null) {
                previewSize =
                    cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                        .getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.height * it.width }!!
                videoSize =
                    cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                        .getOutputSizes(MediaRecorder::class.java)
                        .maxByOrNull { it.height * it.width }!!
                imageReader = ImageReader.newInstance(
                    previewSize.width,
                    previewSize.height,
                    ImageFormat.JPEG,
                    1
                )
                imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
            }
            cameraId = id
        }
    }


    private fun startBackgroundThread() {
        backgroundHandlerThread = HandlerThread("CameraVideoThread")
        backgroundHandlerThread.start()
        backgroundHandler = Handler(backgroundHandlerThread.looper)
    }

    private fun initCameraService() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private fun capturePhoto() {
        captureRequestBuilder =
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder.addTarget(imageReader.surface)
        rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val defaultDisplay: Display? =
                DisplayManagerCompat.getInstance(this@CaptureActivity)
                    .getDisplay(Display.DEFAULT_DISPLAY)
            val displayContext: Context = createDisplayContext(defaultDisplay!!)
            displayContext.display?.rotation
        } else {
            windowManager.defaultDisplay.rotation
        }
        val surfaceRotation = orientations.get(rotation!!)
        captureRequestBuilder.set(
            CaptureRequest.JPEG_ORIENTATION,
            (surfaceRotation + sensorOrientation!! + 180) % 360
        )
        cameraCaptureSession.capture(captureRequestBuilder.build(), captureCallback, null)
    }

    private fun captureVideo() {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this@CaptureActivity)
        } else {
            MediaRecorder()
        }
        setupMediaRecorder()
        startRecording()
    }

    @SuppressLint("MissingPermission")
    private fun connectCamera() {
        cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
    }

    private fun startRecording() {
        val surfaceTexture: SurfaceTexture? =
            activityImageCaptureBinding?.textureView?.surfaceTexture
        surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(surfaceTexture)
        val recordingSurface = mediaRecorder.surface
        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        captureRequestBuilder.addTarget(previewSurface)
        captureRequestBuilder.addTarget(recordingSurface)
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)


        cameraDevice.createCaptureSession(
            listOf(previewSurface, recordingSurface),
            captureStateVideoCallback,
            backgroundHandler
        )
    }


    private val captureStateVideoCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.e("TAG", "Configuration failed")
        }

        override fun onConfigured(session: CameraCaptureSession) {
            cameraCaptureSession = session
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            )
            try {
                cameraCaptureSession.setRepeatingRequest(
                    captureRequestBuilder.build(), null,
                    backgroundHandler
                )
                mediaRecorder.start()
            } catch (e: CameraAccessException) {
                e.printStackTrace()
                Log.e("TAG", "Failed to start camera preview because it couldn't access the camera")
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }

        }
    }
    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            val surfaceTexture = activityImageCaptureBinding?.textureView?.surfaceTexture
            surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
            val previewSurface = Surface(surfaceTexture)

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(previewSurface)
            val outConfig = mutableListOf<OutputConfiguration>()
            for (target in listOf(previewSurface, imageReader.surface)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    outConfig.add(OutputConfiguration(target))
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val sessionConfig = SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    outConfig,
                    cameraExecutor,
                    captureStateCallback
                )
                cameraDevice.createCaptureSession(sessionConfig)
            } else {
                cameraDevice.createCaptureSession(
                    listOf(previewSurface, imageReader.surface),
                    captureStateCallback,
                    null
                )
            }
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {

        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            val errorMsg = when (error) {
                ERROR_CAMERA_DEVICE -> "Fatal (device)"
                ERROR_CAMERA_DISABLED -> "Device policy"
                ERROR_CAMERA_IN_USE -> "Camera in use"
                ERROR_CAMERA_SERVICE -> "Fatal (service)"
                ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                else -> "Unknown"
            }
            Log.e("TAG", "Error when trying to connect camera $errorMsg")
        }
    }

    private fun getFile(type: String, dir: String, ext: String): File {
        val s = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val format = s.format(Date())
        val fileName = "$format$ext"
        return File(getExternalFilesDir(dir)!!, type + "_$fileName")
    }


    private fun setupMediaRecorder() {

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        file = getFile(
            "videos",
            Environment.DIRECTORY_MOVIES,
            ".mp4"
        ).absolutePath
        mediaRecorder.setOutputFile(file)
        val profile = CamcorderProfile.get(0, CamcorderProfile.QUALITY_HIGH)
        mediaRecorder.setVideoFrameRate(profile.videoFrameRate)
        mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight)
        mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setAudioEncodingBitRate(profile.audioBitRate)
        mediaRecorder.setAudioSamplingRate(profile.audioSampleRate)
        rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val defaultDisplay: Display? =
                DisplayManagerCompat.getInstance(this@CaptureActivity)
                    .getDisplay(Display.DEFAULT_DISPLAY)
            val displayContext: Context = createDisplayContext(defaultDisplay!!)
            displayContext.display?.rotation?.plus(90)
        } else {
            windowManager.defaultDisplay.rotation + 90
        }
        /*  when (sensorOrientation) {
              SENSOR_ORIENTATION_DEFAULT_DEGREES -> mediaRecorder.setOrientationHint(
                  orientations.get(rotation!!)
              )
              SENSOR_ORIENTATION_INVERSE_DEGREES -> mediaRecorder.setOrientationHint(
                  inverseOrientations.get(rotation!!)
              )
          }*/
        mediaRecorder.setOrientationHint(rotation!!)
        mediaRecorder.prepare()
    }

    private val captureStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {

        }

        override fun onConfigured(session: CameraCaptureSession) {
            cameraCaptureSession = session

            cameraCaptureSession.setRepeatingRequest(
                captureRequestBuilder.build(),
                null,
                backgroundHandler
            )
        }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            timestamp: Long,
            frameNumber: Long
        ) {
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {

        }
    }

    private val onImageAvailableListener =
        ImageReader.OnImageAvailableListener { reader ->
            Toast.makeText(this@CaptureActivity, "Photo Taken!", Toast.LENGTH_SHORT).show()
            val image: Image = reader.acquireLatestImage()
            saveImage(image)
            image.close()
        }


    private fun saveImage(image: Image) {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        var output: FileOutputStream? = null
        try {
            file = getFile("images", Environment.DIRECTORY_PICTURES, ".jpg").absolutePath
            startActivity(
                Intent(this@CaptureActivity, ImageShowActivity::class.java).putExtra(
                    "file",
                    file
                )
            )
            finish()
            output = FileOutputStream(getFile("images", Environment.DIRECTORY_PICTURES, ".jpg"))
            output.write(bytes)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (null != output) {
                try {
                    output.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        @SuppressLint("MissingPermission")
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            setupCamera()
            connectCamera()
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {

        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {

        }
    }

}