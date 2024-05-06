package com.wutiancheng.videoapp.page.publish

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.media.MediaScannerConnection
import android.net.Uri
import android.nfc.Tag
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Vibrator
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import android.util.Log
import android.util.Size
import android.util.TimeUtils
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.Camera
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.OutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.constraintlayout.widget.ConstraintSet.Motion
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.util.MimeTypes
import com.wutiancheng.videoapp.R
import com.wutiancheng.videoapp.databinding.ActivityLayoutCaptureBinding
import com.wutiancheng.videoapp.ext.invokeViewBinding
import com.wutiancheng.videoapp.ext.setVisibility
import com.wutiancheng.videoapp.pluginruntime.NavDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class CaptureActivity : AppCompatActivity() {
    private val viewBinding: ActivityLayoutCaptureBinding by invokeViewBinding()
    private lateinit var imageCapture: ImageCapture
    private var videoCapture: VideoCapture<Recorder>? = null
    private var videoRecording: Recording? = null
    private var camera: Camera? = null
    private var outputFilePath: String? = null
    private var outputFileWidth: Int = 0
    private var outputFileHeight: Int = 0
    private var outputFileMimeType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        // 申请获取权限
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            // deniedPermission用于存储用户未允许的权限，在下次请求权限时就只用请求未允许的权限
            val deniedPermission = mutableListOf<String>()
            // 遍历所有的权限获取结果，并根据结果将权限添加到deniedPermission中
            for (i in permissions.indices) {
                val permission = permissions[i]
                val result = grantResults[i]
                if (result != PackageManager.PERMISSION_GRANTED) {
                    deniedPermission.add(permission)
                }
            }
            if (deniedPermission.isEmpty()) {
                //  如果权限都允许了，则启动相机
                startCamera()
            } else {
                AlertDialog.Builder(this).setMessage(getString(R.string.capture_permission_message))
                    .setNegativeButton(getString(R.string.capture_permission_no)) { dialog, _ ->
                        // 如果用户依然拒绝，则关闭拍摄dialog和activity
                        dialog.dismiss()
                        this@CaptureActivity.finish()
                    }.setPositiveButton(getString(R.string.capture_permission_ok)) { dialog, _ ->
                        // 如果用户允许，则重新提出权限申请，并关闭dialog
                        ActivityCompat.requestPermissions(
                            this@CaptureActivity, deniedPermission.toTypedArray(),
                            PERMISSION_CODE
                        )
                        dialog.dismiss()
                    }.create().show()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(kotlinx.coroutines.Runnable {
            // 图片或视频的拍摄需要基于CameraProvider完成
            val cameraProvider = cameraProviderFuture.get()

            // 获取前置或后置摄像头
            val cameraSelector = when {
                cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                else -> throw IllegalStateException("Back and Front camera are unavailable")
            }

            // 旋转角度，如果手机横向了，那么预览和图片拍摄的结果都要跟着旋转
            val displayRotation = viewBinding.previewView.display.rotation

            // preview 拍摄预览
            val preview = Preview.Builder().setCameraSelector(cameraSelector)
                .setTargetRotation(displayRotation)
                .build().also {
                    it.setSurfaceProvider(viewBinding.previewView.surfaceProvider)
                }

            // imageCapture 图片拍摄
            imageCapture = ImageCapture.Builder()
                // ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY:图片拍摄结果进行压缩
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // 图片质量0-100
                .setJpegQuality(90)
                // 设置期望的最大分辨率，这个方法和setTargetAspectRatio不能同时用
                .setResolutionSelector(
                    ResolutionSelector.Builder().setMaxResolution(Size(1920, 1080)).build()
                )
                .build()

            val useCase = mutableListOf(preview, imageCapture)

            if(isSupportCombinedUsages(cameraProvider,cameraSelector)){
                val recorder = Recorder.Builder()
                    .setQualitySelector(getQualitySelector(cameraProvider, cameraSelector)).build()

                videoCapture = VideoCapture.withOutput(recorder)
                useCase.add(videoCapture!!)
            }

            try {
                cameraProvider.unbindAll()
                // kotlin中vararg可以通过在数组前加*，对数组进行展开传递
                camera =
                    cameraProvider.bindToLifecycle(this, cameraSelector, *useCase.toTypedArray())
                bindUI()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun isSupportCombinedUsages(
        cameraProvider: CameraProvider,
        cameraSelector: CameraSelector
    ): Boolean {
        val level = cameraSelector.filter(cameraProvider.availableCameraInfos).firstOrNull()?.let {
            Camera2CameraInfo.from(it)
        }?.getCameraCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            ?: return false
        return level >= CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
    }

    @SuppressLint("RestrictedApi")
    private fun getQualitySelector(
        cameraProvider: CameraProvider,
        cameraSelector: CameraSelector
    ): QualitySelector {
        // 获取相机信息
        val cameraInfo = cameraProvider.availableCameraInfos.filter {
            it.lensFacing == cameraSelector.lensFacing
        }
        // 获取受支持的质量列表
        val supportQualities = QualitySelector.getSupportedQualities(cameraInfo[0]).filter {
            listOf(Quality.FHD, Quality.HD, Quality.SD).contains(it)
        }
        return QualitySelector.from(supportQualities[0])
    }

    @SuppressLint("RestrictedApi", "ClickableViewAccessibility")
    private fun bindUI() {
        viewBinding.captureTips.setText(R.string.capture_tips_take_picture)
        viewBinding.recordView.setOnClickListener {
            takePicture()
        }
        viewBinding.recordView.setOnLongClickListener {
            captureVideo()
            true
        }
        videoCapture?.run {
            viewBinding.captureTips.setText(R.string.capture_tips)
            viewBinding.recordView.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP && videoRecording?.isClosed == false) {
                    // 如果松开了拍摄键且视频录制已经关闭，则停止录制
                    videoRecording?.stop()
                }
                false
            }
        }
        viewBinding.previewView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // 获取点击的坐标
                val meteringPointFactory = viewBinding.previewView.meteringPointFactory
                val point = meteringPointFactory.createPoint(event.x, event.y)
                // FLAG_AE表示自动曝光
                // FLAG_AWB表示自动白平衡
                //FocusMeteringAction.FLAG_AF表示自动对焦
                val focusMeteringAction =
                    FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF).build()
                camera?.cameraControl?.startFocusAndMetering(focusMeteringAction)
                showFocusPoint(event.x, event.y)
            }
            true
        }
    }

    private fun showFocusPoint(x: Float, y: Float) {
        val focusView = viewBinding.focusPoint
        val alphaAnim = SpringAnimation(focusView, DynamicAnimation.ALPHA, 1f).apply {
            spring.stiffness = SPRING_STIFFNESS
            spring.dampingRatio = SPRING_DAMPING_RATIO
            addEndListener { _, _, _, _ ->
                SpringAnimation(focusView, DynamicAnimation.ALPHA, 0f).apply {
                    spring.stiffness = SPRING_STIFENESS_ALPHA_OUT
                    spring.dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                }.start()
            }
        }

        val scaleXAnim =
            SpringAnimation(focusView, DynamicAnimation.SCALE_X, 1f).apply {
                spring.stiffness = SPRING_STIFFNESS
                spring.dampingRatio = SPRING_DAMPING_RATIO
            }

        val scaleYAnim = SpringAnimation(focusView, DynamicAnimation.SCALE_Y, 1f).apply {
            spring.stiffness = SPRING_STIFFNESS
            spring.dampingRatio = SPRING_DAMPING_RATIO
        }

        focusView.setVisibility(true)
        focusView.alpha = 0f
        // x和y是框的左上角，不是中心点，因此不能直接设置
//        focusView.x=x
//        focusView.y=y
        focusView.translationX = x - focusView.width / 2
        focusView.translationY = y - focusView.height / 2
        focusView.alpha = 0f
        focusView.scaleX = 1.5f
        focusView.scaleY = 1.5f

        alphaAnim.start()
        scaleXAnim.start()
        scaleYAnim.start()
    }

    private fun captureVideo() {
        val vibrator = getSystemService(Vibrator::class.java) as Vibrator
        vibrator.vibrate(200)
        viewBinding.captureTips.setVisibility(true)
        // 当按住拍摄键时放大拍摄键
        viewBinding.recordView.scaleX = 1.2f
        viewBinding.recordView.scaleY = 1.2f

        val fileName =
            SimpleDateFormat(FILENAME, Locale.CHINA).format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, VIDEO_TYPE)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH_VIDEO)
            }
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues)
            // 设置视频录制的市场限制
            .setDurationLimitMillis(10 * 1000)
            .build()

        videoRecording = videoCapture!!.output.prepareRecording(this, outputOptions).apply {
            if (PermissionChecker.checkSelfPermission(
                    this@CaptureActivity,
                    Manifest.permission.RECORD_AUDIO
                ) == PermissionChecker.PERMISSION_GRANTED
            ) {
                withAudioEnabled()
            }
        }.start(ContextCompat.getMainExecutor(this@CaptureActivity)) {
            when (it) {
                is VideoRecordEvent.Start -> {
                    // 开始录制视频
                    viewBinding.captureTips.setText(R.string.capture_tips_stop_recording)
                }

                is VideoRecordEvent.Status -> {
                    // 录制中，设置录制进度条
                    val recordMills =
                        TimeUnit.NANOSECONDS.toMillis(it.recordingStats.recordedDurationNanos)
                    viewBinding.recordView.progress =
                        (recordMills * 1.0f / (10 * 1000) * 100).roundToInt()
                    if ((recordMills * 1.0f / (10 * 1000) * 100).roundToInt() >= 100) {
                        videoRecording?.stop()
                    }
                }

                is VideoRecordEvent.Finalize -> {
                    // 结束录制
                    if (it.hasError()) {
                        videoRecording?.close()
                        videoRecording = null
                    } else {
                        val savedUri = it.outputResults.outputUri
                        onFileSaved(savedUri)
                        Log.d(TAG, "captureVideo success:${savedUri}")
                    }

                    // 结束录制时拍摄键变回原大小
                    viewBinding.recordView.scaleX = 1.0f
                    viewBinding.recordView.scaleY = 1.0f
                    viewBinding.recordView.progress = 0
                    viewBinding.captureTips.setText(R.string.capture_tips)
                }
            }
        }

    }

    private fun takePicture() {
        // 拍照时需要震动反馈
        val vibrator = getSystemService(Vibrator::class.java) as Vibrator
        vibrator.vibrate(200)

        // 定义文件名，用拍摄时的当前时间作为文件名
        val fileName = SimpleDateFormat(FILENAME, Locale.CHINA).format(System.currentTimeMillis())

        // 指定文件存放位置
        val contentValues = ContentValues().apply {
            // 文件名
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            // 文件类型
            put(MediaStore.MediaColumns.MIME_TYPE, PHOTO_TYPE)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                // 如果系统版本大于android 9，指定存放路径
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH_PICTURE)
            }
        }

        // 指定文件输出的配置
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    Log.d(TAG, "onImageSaved: capture success:$savedUri")
                    onFileSaved(savedUri!!)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@CaptureActivity,
                        exception.imageCaptureError,
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })
    }

    private fun onFileSaved(savedUri: Uri) {
        lifecycleScope.launch {
            // 查询图片或视频的数据，宽，高，类型
            val cursor = contentResolver.query(
                savedUri,
                arrayOf(
                    MediaStore.MediaColumns.DATA,
                    MediaStore.MediaColumns.WIDTH,
                    MediaStore.MediaColumns.HEIGHT,
                    MediaStore.MediaColumns.MIME_TYPE
                ),
                null,
                null,
                null
            ) ?: return@launch

            cursor.moveToFirst()

            outputFilePath =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
            outputFileMimeType =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))
            outputFileWidth =
                cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH))
            outputFileHeight =
                cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT))

            cursor.close()

            // 在Android 7及以下部分设备中录制的视频和拍摄的照片不会出现在相册中
            MediaScannerConnection.scanFile(
                this@CaptureActivity,
                arrayOf(outputFilePath!!),
                arrayOf(outputFileMimeType),
                null
            )

            withContext(Dispatchers.Main) {
                // 判断是否是视频类型
                val video = MimeTypes.isVideo(outputFileMimeType)
                // 启动预览界面
                PreviewActivity.startActivityForResult(
                    this@CaptureActivity,
                    outputFilePath!!,
                    video,
                    getString(R.string.preview_ok)
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PreviewActivity.REQ_PREVIEW && resultCode == RESULT_OK){
            val intent=Intent()
            intent.putExtra(RESULT_FILE_PATH,outputFilePath)
            intent.putExtra(RESULT_FILE_WIDTH,outputFileWidth)
            intent.putExtra(RESULT_FILE_HEIGHT,outputFileHeight)
            intent.putExtra(RESULT_FILE_TYPE,outputFileMimeType)
            // 将文件数据在CaptureActivity结束时传递回PublishActivity
            setResult(RESULT_OK,intent)
            finish()
        }
    }

    companion object {
        private const val TAG = "CaptureActivity"

        // 动态权限申请
        private val PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) Manifest.permission.WRITE_EXTERNAL_STORAGE else null
        ).filterNotNull().toTypedArray()

        // spring 动画参数配置
        private const val SPRING_STIFENESS_ALPHA_OUT = 100f
        private const val SPRING_STIFFNESS = 800f
        private const val SPRING_DAMPING_RATIO = 0.35f

        // 图片/视频文件名称，存放位置
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-sss"
        private const val PHOTO_TYPE = "image/jpeg"
        private const val VIDEO_TYPE = "video/mp4"
        private const val RELATIVE_PATH_PICTURE = "Pictures/VideoApp"
        private const val RELATIVE_PATH_VIDEO = "Movies/VideoApp"

        // request code
        internal const val REQ_CAPTURE = 10001
        private const val PERMISSION_CODE = 1000

        // output file information
        internal const val RESULT_FILE_PATH = "file_path"
        internal const val RESULT_FILE_HEIGHT = "file_height"
        internal const val RESULT_FILE_WIDTH = "file_width"
        internal const val RESULT_FILE_TYPE = "file_type"

        fun startActivityForResult(activity: Activity) {
            val intent = Intent(activity, CaptureActivity::class.java)
            activity.startActivityForResult(intent, REQ_CAPTURE)
        }
    }
}
