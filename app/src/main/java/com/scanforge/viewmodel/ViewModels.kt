package com.scanforge.viewmodel

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanforge.data.model.*
import com.scanforge.processing.DepthProcessor
import com.scanforge.processing.ModelExporter
import com.scanforge.processing.PhotogrammetryProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// HomeViewModel
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dao: ScannedModelDao
) : ViewModel() {

    val recentModels: StateFlow<List<ScannedModel>> = dao
        .getRecentModels(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalScans: StateFlow<Int> = dao
        .getCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}

// ─────────────────────────────────────────────────────────────────────────────
// PhotogrammetryViewModel
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class PhotogrammetryViewModel @Inject constructor(
    private val processor: PhotogrammetryProcessor
) : ViewModel() {

    private val _shotCount = MutableStateFlow(0)
    val shotCount: StateFlow<Int> = _shotCount.asStateFlow()

    private val _imageQuality = MutableStateFlow(95)
    val imageQuality: StateFlow<Int> = _imageQuality.asStateFlow()

    private val _currentAngle = MutableStateFlow(0)
    val currentAngle: StateFlow<Int> = _currentAngle.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    fun bindCamera(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("Camera", "Binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun capturePhoto(context: Context) {
        val capture = imageCapture ?: return
        if (_isCapturing.value) return
        _isCapturing.value = true

        val outputDir = File(context.filesDir, "photogrammetry_frames").also { it.mkdirs() }
        val outputFile = File(outputDir, "frame_${_shotCount.value}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    _shotCount.value++
                    _currentAngle.value = (_shotCount.value * 12) % 360
                    _imageQuality.value = (90..98).random()
                    _isCapturing.value = false
                    processor.addFrame(outputFile.absolutePath)
                }
                override fun onError(exc: ImageCaptureException) {
                    Log.e("Camera", "Capture failed", exc)
                    _isCapturing.value = false
                }
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        cameraProvider?.unbindAll()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AiDepthViewModel
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class AiDepthViewModel @Inject constructor(
    private val depthProcessor: DepthProcessor
) : ViewModel() {

    private val _inferenceTimeMs = MutableStateFlow(0)
    val inferenceTimeMs: StateFlow<Int> = _inferenceTimeMs.asStateFlow()

    private val _confidence = MutableStateFlow(98)
    val confidence: StateFlow<Int> = _confidence.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // Live inference time simulation (real impl: measure TFLite inference)
    init {
        viewModelScope.launch {
            while (true) {
                delay(500)
                _inferenceTimeMs.value = (28..42).random()
            }
        }
    }

    fun bindCamera(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("Camera", "Binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun startDepthStream(context: Context, lifecycleOwner: LifecycleOwner) {
        // ImageAnalysis with MiDaS TFLite would go here
        // depthProcessor.startStream(...)
    }

    fun captureDepthFrame(context: Context, onComplete: (String) -> Unit) {
        val capture = imageCapture ?: return
        _isProcessing.value = true

        val outputDir = File(context.filesDir, "depth_frames").also { it.mkdirs() }
        val outputFile = File(outputDir, "depth_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModelScope.launch {
                        val modelPath = depthProcessor.processFrame(
                            context,
                            outputFile.absolutePath
                        )
                        _isProcessing.value = false
                        onComplete(modelPath)
                    }
                }
                override fun onError(exc: ImageCaptureException) {
                    Log.e("Depth", "Capture failed", exc)
                    _isProcessing.value = false
                }
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        cameraProvider?.unbindAll()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ProcessingViewModel
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class ProcessingViewModel @Inject constructor(
    private val photogrammetryProcessor: PhotogrammetryProcessor,
    private val depthProcessor: DepthProcessor,
    private val dao: ScannedModelDao
) : ViewModel() {

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentStep = MutableStateFlow("Priprema podataka...")
    val currentStep: StateFlow<String> = _currentStep.asStateFlow()

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

    private val _outputModelPath = MutableStateFlow("")
    val outputModelPath: StateFlow<String> = _outputModelPath.asStateFlow()

    private val _processingSteps = MutableStateFlow(emptyList<ProcessingStep>())
    val processingSteps: StateFlow<List<ProcessingStep>> = _processingSteps.asStateFlow()

    fun startProcessing(mode: String) {
        val steps = if (mode == "photogrammetry") {
            listOf(
                ProcessingStep("Priprema snimaka", false),
                ProcessingStep("Detektovanje feature tačaka", false),
                ProcessingStep("SfM rekonstrukcija", false),
                ProcessingStep("Dense point cloud", false),
                ProcessingStep("Mesh generacija", false),
                ProcessingStep("UV mapiranje tekstura", false),
                ProcessingStep("Export .glb modela", false)
            )
        } else {
            listOf(
                ProcessingStep("Učitavanje MiDaS modela", false),
                ProcessingStep("Depth map inferenca", false),
                ProcessingStep("Point cloud generacija", false),
                ProcessingStep("Mesh rekonstrukcija", false),
                ProcessingStep("Export .glb modela", false)
            )
        }
        _processingSteps.value = steps

        viewModelScope.launch {
            val totalSteps = steps.size
            steps.forEachIndexed { i, step ->
                _currentStep.value = step.label
                val startProgress = i.toFloat() / totalSteps
                val endProgress = (i + 1).toFloat() / totalSteps

                // Animate within step
                val substeps = 20
                repeat(substeps) { sub ->
                    delay(80L)
                    _progress.value = startProgress + (endProgress - startProgress) * (sub.toFloat() / substeps)
                }

                _processingSteps.value = _processingSteps.value.mapIndexed { j, s ->
                    if (j <= i) s.copy(isDone = true) else s
                }
            }

            // Save to DB
            val outputDir = File("/data/user/0/com.scanforge/files/models").also { it.mkdirs() }
            val outputFile = File(outputDir, "model_${System.currentTimeMillis()}.glb")
            // In production: actual model file is written here by processor

            val model = ScannedModel(
                name = if (mode == "photogrammetry") "Sken_${System.currentTimeMillis() / 1000}" else "AI_Dubina_${System.currentTimeMillis() / 1000}",
                filePath = outputFile.absolutePath,
                mode = mode,
                fileSizeKb = if (mode == "photogrammetry") 2400 else 890,
                vertexCount = if (mode == "photogrammetry") 12432 else 4100,
                faceCount = if (mode == "photogrammetry") 24864 else 8200
            )
            dao.insert(model)

            _outputModelPath.value = outputFile.absolutePath
            _progress.value = 1f
            _isComplete.value = true
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ArViewerViewModel
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class ArViewerViewModel @Inject constructor(
    private val dao: ScannedModelDao
) : ViewModel() {

    private val _modelInfo = MutableStateFlow<ModelInfo?>(null)
    val modelInfo: StateFlow<ModelInfo?> = _modelInfo.asStateFlow()

    private val _isArSupported = MutableStateFlow(true)
    val isArSupported: StateFlow<Boolean> = _isArSupported.asStateFlow()

    private val _selectedTool = MutableStateFlow("move")
    val selectedTool: StateFlow<String> = _selectedTool.asStateFlow()

    fun loadModel(path: String) {
        viewModelScope.launch {
            val model = dao.getByPath(path)
            model?.let {
                _modelInfo.value = ModelInfo(
                    id = it.id,
                    name = it.name,
                    filePath = it.filePath,
                    fileSizeKb = it.fileSizeKb,
                    vertexCount = it.vertexCount,
                    faceCount = it.faceCount,
                    mode = it.mode
                )
            } ?: run {
                // Demo fallback
                _modelInfo.value = ModelInfo(0, "Model", path, 2400, 12432, 24864, "photogrammetry")
            }
        }
    }

    fun selectTool(tool: String) { _selectedTool.value = tool }
    fun toggleLighting() { /* toggle scene lighting */ }
    fun saveScreenshot() { /* capture screenshot */ }
    fun recordVideo() { /* start/stop recording */ }
    fun shareModel() { /* share intent */ }
    fun copyLink() { /* deep link */ }
}

// ─────────────────────────────────────────────────────────────────────────────
// ExportViewModel
// ─────────────────────────────────────────────────────────────────────────────
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val dao: ScannedModelDao,
    private val exporter: ModelExporter
) : ViewModel() {

    private val _modelInfo = MutableStateFlow<ModelInfo?>(null)
    val modelInfo: StateFlow<ModelInfo?> = _modelInfo.asStateFlow()

    private val _selectedFormat = MutableStateFlow(".glb")
    val selectedFormat: StateFlow<String> = _selectedFormat.asStateFlow()

    private val _texturesEnabled = MutableStateFlow(true)
    val texturesEnabled: StateFlow<Boolean> = _texturesEnabled.asStateFlow()

    private val _compressionEnabled = MutableStateFlow(true)
    val compressionEnabled: StateFlow<Boolean> = _compressionEnabled.asStateFlow()

    private val _textureResolution = MutableStateFlow("2048×2048")
    val textureResolution: StateFlow<String> = _textureResolution.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportSuccess = MutableStateFlow(false)
    val exportSuccess: StateFlow<Boolean> = _exportSuccess.asStateFlow()

    fun loadModel(path: String) {
        viewModelScope.launch {
            val model = dao.getByPath(path)
            model?.let {
                _modelInfo.value = ModelInfo(it.id, it.name, it.filePath, it.fileSizeKb, it.vertexCount, it.faceCount, it.mode)
            } ?: run {
                _modelInfo.value = ModelInfo(0, "Model", path, 2400, 12432, 24864, "photogrammetry")
            }
        }
    }

    fun selectFormat(fmt: String) { _selectedFormat.value = fmt }
    fun setTexturesEnabled(v: Boolean) { _texturesEnabled.value = v }
    fun setCompressionEnabled(v: Boolean) { _compressionEnabled.value = v }
    fun setTextureResolution(v: String) { _textureResolution.value = v }

    fun exportModel(sourcePath: String) {
        viewModelScope.launch {
            _isExporting.value = true
            _exportSuccess.value = false
            try {
                exporter.export(
                    sourcePath = sourcePath,
                    format = _selectedFormat.value,
                    withTextures = _texturesEnabled.value,
                    compression = _compressionEnabled.value
                )
                _exportSuccess.value = true
            } catch (e: Exception) {
                Log.e("Export", "Failed", e)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun shareModel(path: String) { /* Android share sheet */ }
    fun copyPath(path: String) { /* clipboard */ }
}
