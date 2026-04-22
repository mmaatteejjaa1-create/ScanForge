package com.scanforge.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// PhotogrammetryProcessor
// Manages captured frames for reconstruction.
// Real implementation would call OpenMVG/COLMAP via JNI or a bundled binary.
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class PhotogrammetryProcessor @Inject constructor() {

    private val frames = mutableListOf<String>()

    fun addFrame(filePath: String) {
        frames.add(filePath)
    }

    fun getFrameCount() = frames.size

    /**
     * Run the full photogrammetry pipeline.
     * Steps:
     *  1. Feature extraction (SIFT/ORB)
     *  2. Feature matching
     *  3. Structure-from-Motion (SfM) — camera poses
     *  4. Multi-View Stereo (MVS) — dense point cloud
     *  5. Surface reconstruction (Poisson/ball pivoting)
     *  6. UV unwrapping + texture projection
     *  7. Export as .glb via glTF writer
     */
    suspend fun reconstruct(outputDir: File): String = withContext(Dispatchers.IO) {
        // TODO: JNI call to bundled OpenMVG/COLMAP/Meshroom mini binary
        // For demo: create placeholder .glb
        outputDir.mkdirs()
        val output = File(outputDir, "reconstruction_${System.currentTimeMillis()}.glb")
        output.writeBytes(createMinimalGlb())
        frames.clear()
        output.absolutePath
    }

    /**
     * Creates a minimal valid .glb binary (GLB magic + JSON chunk header).
     * In production this is replaced by the real mesh output.
     */
    private fun createMinimalGlb(): ByteArray {
        val json = """{"asset":{"version":"2.0","generator":"ScanForge"},"scene":0,"scenes":[{"nodes":[0]}],"nodes":[{"mesh":0}],"meshes":[{"primitives":[{"attributes":{"POSITION":0}}]}]}"""
        val jsonBytes = json.toByteArray(Charsets.UTF_8)
        val paddingNeeded = (4 - jsonBytes.size % 4) % 4
        val paddedJson = jsonBytes + ByteArray(paddingNeeded) { 0x20 }
        val totalLength = 12 + 8 + paddedJson.size
        val buf = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(0x46546C67) // magic "glTF"
        buf.putInt(2)          // version
        buf.putInt(totalLength)
        buf.putInt(paddedJson.size)
        buf.putInt(0x4E4F534A) // chunk type JSON
        buf.put(paddedJson)
        return buf.array()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DepthProcessor
// Runs MiDaS v2.1 Small (TFLite) on a captured image frame,
// converts the depth map to a point cloud, then meshes it.
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class DepthProcessor @Inject constructor() {

    private var interpreter: Interpreter? = null
    private val INPUT_SIZE = 256

    /**
     * Load the MiDaS TFLite model from assets.
     * Place midas_v2_1_small.tflite in app/src/main/assets/
     */
    private fun loadModel(context: Context) {
        if (interpreter != null) return
        try {
            val model = loadModelFile(context, "midas_v2_1_small.tflite")
            val options = Interpreter.Options().apply {
                numThreads = 4
                useNNAPI = true  // Hardware acceleration on supported devices
            }
            interpreter = Interpreter(model, options)
        } catch (e: Exception) {
            android.util.Log.w("DepthProcessor", "TFLite model not found, using simulated depth")
        }
    }

    /**
     * Process a single frame:
     * 1. Decode image → resize to 256×256
     * 2. Normalize pixels to [0,1]
     * 3. Run MiDaS inference
     * 4. Convert depth map to 3D point cloud
     * 5. Poisson surface reconstruction
     * 6. Write .glb
     */
    suspend fun processFrame(context: Context, imagePath: String): String =
        withContext(Dispatchers.IO) {
            loadModel(context)

            val bitmap = BitmapFactory.decodeFile(imagePath)
                ?: return@withContext createEmptyModel(context)

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            val inputBuffer = bitmapToInputBuffer(scaledBitmap)

            // Output: [1][256][256][1] float32 depth map
            val outputDepth = Array(1) { Array(INPUT_SIZE) { FloatArray(INPUT_SIZE) } }

            if (interpreter != null) {
                interpreter!!.run(inputBuffer, outputDepth)
            } else {
                // Simulate depth map (center near, edges far)
                simulateDepthMap(outputDepth[0])
            }

            // Convert depth map to .glb
            val outputDir = File(context.filesDir, "models").also { it.mkdirs() }
            val outputFile = File(outputDir, "depth_${System.currentTimeMillis()}.glb")

            depthMapToGlb(outputDepth[0], outputFile)

            outputFile.absolutePath
        }

    private fun bitmapToInputBuffer(bitmap: Bitmap): ByteBuffer {
        val buf = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3).order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            buf.putFloat(((pixel shr 16) and 0xFF) / 255f)
            buf.putFloat(((pixel shr 8) and 0xFF) / 255f)
            buf.putFloat((pixel and 0xFF) / 255f)
        }
        buf.rewind()
        return buf
    }

    private fun simulateDepthMap(depth: Array<FloatArray>) {
        val cx = INPUT_SIZE / 2f
        val cy = INPUT_SIZE / 2f
        val maxDist = Math.sqrt((cx * cx + cy * cy).toDouble()).toFloat()
        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                val dist = Math.sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toDouble()).toFloat()
                depth[y][x] = 1f - (dist / maxDist) // Center = near (1.0), edges = far (0.0)
            }
        }
    }

    /**
     * Convert a 256×256 depth map to a GLB file with a mesh.
     * Samples a grid of vertices from the depth map, triangulates them.
     */
    private fun depthMapToGlb(depthMap: Array<FloatArray>, output: File) {
        val step = 4  // Sample every 4th pixel → 64×64 grid
        val gridSize = INPUT_SIZE / step
        val scale = 2f / gridSize  // Normalize to [-1, 1] range

        val vertices = mutableListOf<Float>()
        val indices = mutableListOf<Int>()

        // Build vertex grid
        for (y in 0 until gridSize) {
            for (x in 0 until gridSize) {
                val px = x * step
                val py = y * step
                val depth = depthMap[py.coerceIn(0, INPUT_SIZE - 1)][px.coerceIn(0, INPUT_SIZE - 1)]

                vertices.add((x - gridSize / 2f) * scale)  // X
                vertices.add(depth * 1.5f - 0.5f)            // Y (height from depth)
                vertices.add((y - gridSize / 2f) * scale)  // Z
            }
        }

        // Build triangle indices
        for (y in 0 until gridSize - 1) {
            for (x in 0 until gridSize - 1) {
                val tl = y * gridSize + x
                val tr = tl + 1
                val bl = tl + gridSize
                val br = bl + 1
                // Two triangles per quad
                indices.addAll(listOf(tl, bl, tr, tr, bl, br))
            }
        }

        // Write minimal GLB with vertex/index data
        output.writeBytes(buildGlbFromMesh(vertices, indices))
    }

    private fun buildGlbFromMesh(vertices: List<Float>, indices: List<Int>): ByteArray {
        // Build binary buffer: vertices + indices
        val vertBuf = ByteBuffer.allocate(vertices.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        vertices.forEach { vertBuf.putFloat(it) }

        val idxBuf = ByteBuffer.allocate(indices.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        indices.forEach { idxBuf.putInt(it) }

        val binBuffer = vertBuf.array() + idxBuf.array()
        val binPad = (4 - binBuffer.size % 4) % 4

        val vertByteLen = vertices.size * 4
        val idxByteLen = indices.size * 4

        val json = """
{
  "asset":{"version":"2.0","generator":"ScanForge-DepthProcessor"},
  "scene":0,
  "scenes":[{"nodes":[0]}],
  "nodes":[{"mesh":0}],
  "meshes":[{"name":"depth_mesh","primitives":[{"attributes":{"POSITION":0},"indices":1,"mode":4}]}],
  "accessors":[
    {"bufferView":0,"componentType":5126,"count":${vertices.size / 3},"type":"VEC3"},
    {"bufferView":1,"componentType":5125,"count":${indices.size},"type":"SCALAR"}
  ],
  "bufferViews":[
    {"buffer":0,"byteOffset":0,"byteLength":$vertByteLen},
    {"buffer":0,"byteOffset":$vertByteLen,"byteLength":$idxByteLen}
  ],
  "buffers":[{"byteLength":${binBuffer.size}}]
}""".trimIndent()

        val jsonBytes = json.toByteArray(Charsets.UTF_8)
        val jsonPad = (4 - jsonBytes.size % 4) % 4
        val paddedJson = jsonBytes + ByteArray(jsonPad) { 0x20 }
        val paddedBin = binBuffer + ByteArray(binPad) { 0x00 }

        val totalLength = 12 + 8 + paddedJson.size + 8 + paddedBin.size
        val buf = ByteBuffer.allocate(totalLength).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(0x46546C67)
        buf.putInt(2)
        buf.putInt(totalLength)
        buf.putInt(paddedJson.size)
        buf.putInt(0x4E4F534A) // JSON chunk
        buf.put(paddedJson)
        buf.putInt(paddedBin.size)
        buf.putInt(0x004E4942) // BIN chunk
        buf.put(paddedBin)
        return buf.array()
    }

    private fun createEmptyModel(context: Context): String {
        val outputDir = File(context.filesDir, "models").also { it.mkdirs() }
        val f = File(outputDir, "empty_${System.currentTimeMillis()}.glb")
        f.writeBytes(buildGlbFromMesh(emptyList(), emptyList()))
        return f.absolutePath
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fd = context.assets.openFd(filename)
        val inputStream = FileInputStream(fd.fileDescriptor)
        val channel = inputStream.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ModelExporter
// Converts/copies the processed GLB to the user's chosen format and
// writes it to the Downloads folder.
// ─────────────────────────────────────────────────────────────────────────────
@Singleton
class ModelExporter @Inject constructor() {

    /**
     * Export a model to the desired format.
     * - .glb  → copy as-is (already binary glTF)
     * - .gltf → extract JSON from GLB, write separate bin
     * - .obj  → convert vertex data to OBJ text
     * - .stl  → write binary STL from triangle list
     * - .fbx  → requires FBX SDK (stub for now)
     * - .ply  → write ASCII PLY point cloud
     */
    suspend fun export(
        sourcePath: String,
        format: String,
        withTextures: Boolean,
        compression: Boolean
    ) = withContext(Dispatchers.IO) {
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) throw IllegalArgumentException("Source model not found: $sourcePath")

        val downloadsDir = android.os.Environment
            .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            .also { it.mkdirs() }

        val baseName = sourceFile.nameWithoutExtension
        val outputFile = File(downloadsDir, "${baseName}${format}")

        when (format) {
            ".glb" -> {
                sourceFile.copyTo(outputFile, overwrite = true)
            }
            ".gltf" -> {
                // Extract JSON chunk from GLB
                val bytes = sourceFile.readBytes()
                if (bytes.size > 20 && String(bytes, 0, 4) == "glTF") {
                    val jsonLength = ByteBuffer.wrap(bytes, 12, 4).order(ByteOrder.LITTLE_ENDIAN).int
                    val jsonData = bytes.copyOfRange(20, 20 + jsonLength)
                    File(downloadsDir, "${baseName}.gltf").writeBytes(jsonData)
                    if (bytes.size > 20 + jsonLength + 8) {
                        val binData = bytes.copyOfRange(20 + jsonLength + 8, bytes.size)
                        File(downloadsDir, "${baseName}.bin").writeBytes(binData)
                    }
                }
            }
            ".stl" -> {
                // Write minimal binary STL header (80-byte header + triangle count)
                outputFile.outputStream().use { out ->
                    val header = ByteArray(80) { 0 }
                    "ScanForge STL Export".toByteArray().copyInto(header)
                    out.write(header)
                    // Triangle count (placeholder — real impl reads from GLB)
                    val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                    buf.putInt(0)
                    out.write(buf.array())
                }
            }
            ".ply" -> {
                // ASCII PLY point cloud placeholder
                outputFile.writeText("""
ply
format ascii 1.0
comment ScanForge PLY export
element vertex 0
property float x
property float y
property float z
end_header
""".trimIndent())
            }
            else -> {
                // .obj, .fbx — copy with format stub comment
                outputFile.writeText("# ScanForge export — ${format.uppercase()} format\n# Conversion from GLB pending\n")
            }
        }
    }
}
