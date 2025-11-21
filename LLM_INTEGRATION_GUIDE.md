# On-Device LLM Integration Guide for Android

## Automatic Model Download Options (No Sign-In Required)

### TL;DR: Best Options ✅

1. **Hugging Face Hub** - Direct downloads, no auth required for public models
2. **ONNX Model Zoo** - Pre-converted models ready to use
3. **Gemini Nano** - Built into Android 14+ (AICore)
4. **MediaPipe LLM Inference** - Google's official solution

---

## Option 1: Hugging Face Hub (RECOMMENDED) 🤗

### Overview
Hugging Face hosts thousands of open-source models that can be downloaded directly without authentication for public models.

### Models Available Without Sign-In

#### 1. TinyLlama-1.1B-Chat
- **Size:** ~600MB (FP16), ~300MB (INT8 quantized)
- **License:** Apache 2.0 (permissive, no restrictions)
- **Direct Download:** YES ✅
- **URL:** `https://huggingface.co/TinyLlama/TinyLlama-1.1B-Chat-v1.0/resolve/main/`

**Files needed:**
```
model.safetensors (or model.onnx if pre-converted)
tokenizer.json
config.json
```

#### 2. Phi-2 (Microsoft)
- **Size:** ~1.4GB (FP16), ~700MB (INT8)
- **License:** MIT (permissive)
- **Direct Download:** YES ✅
- **URL:** `https://huggingface.co/microsoft/phi-2/resolve/main/`

#### 3. Gemma-2B (Google)
- **Size:** ~1.2GB (FP16)
- **License:** Gemma Terms of Use (permissive for research & commercial)
- **Direct Download:** YES ✅
- **URL:** `https://huggingface.co/google/gemma-2b/resolve/main/`

#### 4. Qwen-1.8B (Alibaba)
- **Size:** ~900MB (FP16)
- **License:** Apache 2.0
- **Direct Download:** YES ✅
- **URL:** `https://huggingface.co/Qwen/Qwen-1_8B-Chat/resolve/main/`

### Implementation Example

```kotlin
class ModelDownloadManager @Inject constructor(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val modelDir = File(context.filesDir, "models")

    suspend fun downloadTinyLlama(): Result<File> = withContext(Dispatchers.IO) {
        try {
            modelDir.mkdirs()

            // Download quantized ONNX model (no conversion needed)
            val modelUrl = "https://huggingface.co/TinyLlama/TinyLlama-1.1B-Chat-v1.0-ONNX/resolve/main/model_quantized.onnx"
            val tokenizerUrl = "https://huggingface.co/TinyLlama/TinyLlama-1.1B-Chat-v1.0-ONNX/resolve/main/tokenizer.json"

            val modelFile = downloadFile(modelUrl, "tinyllama-quantized.onnx")
            val tokenizerFile = downloadFile(tokenizerUrl, "tokenizer.json")

            Result.success(modelFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun downloadFile(url: String, filename: String): File {
        val file = File(modelDir, filename)

        if (file.exists()) {
            Timber.d("Model already exists: $filename")
            return file
        }

        val request = Request.Builder()
            .url(url)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Download failed: ${response.code}")
            }

            response.body?.let { body ->
                file.outputStream().use { output ->
                    body.byteStream().copyTo(output)
                }
            }
        }

        return file
    }
}
```

### Download with Progress Tracking

```kotlin
suspend fun downloadModelWithProgress(
    url: String,
    filename: String,
    onProgress: (percent: Int) -> Unit
): File = withContext(Dispatchers.IO) {
    val file = File(modelDir, filename)

    val request = Request.Builder().url(url).build()

    okHttpClient.newCall(request).execute().use { response ->
        val contentLength = response.body?.contentLength() ?: -1
        var downloadedBytes = 0L

        response.body?.byteStream()?.use { input ->
            file.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytes: Int

                while (input.read(buffer).also { bytes = it } != -1) {
                    output.write(buffer, 0, bytes)
                    downloadedBytes += bytes

                    if (contentLength > 0) {
                        val percent = (downloadedBytes * 100 / contentLength).toInt()
                        onProgress(percent)
                    }
                }
            }
        }
    }

    file
}
```

### UI for Download

```kotlin
@Composable
fun ModelDownloadScreen(
    viewModel: ModelDownloadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "AI Model Required",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Download TinyLlama-1.1B (~300MB) for on-device AI assistance",
            style = MaterialTheme.typography.bodyMedium
        )

        if (uiState.isDownloading) {
            LinearProgressIndicator(
                progress = { uiState.downloadProgress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text("${uiState.downloadProgress}% - ${uiState.statusMessage}")
        } else if (uiState.isDownloaded) {
            Text("✅ Model ready to use")
        } else {
            Button(onClick = { viewModel.downloadModel() }) {
                Text("Download Model (300MB)")
            }
        }
    }
}
```

---

## Option 2: ONNX Model Zoo

### Overview
Microsoft maintains a collection of pre-optimized ONNX models ready for inference.

### Available Models
- **URL:** `https://github.com/onnx/models/tree/main/validated`
- **License:** Various (mostly MIT/Apache 2.0)
- **Direct Download:** YES ✅

### Example: GPT-2 Small
```kotlin
val gpt2Url = "https://github.com/onnx/models/raw/main/validated/text/machine_comprehension/gpt-2/model/gpt2-10.onnx"

// Direct download, no auth needed
```

---

## Option 3: Gemini Nano via AICore (Android 14+) 🌟

### Overview
**Best option if targeting Android 14+** - Model is built into the OS!

### Advantages
- ✅ No download required (built into system)
- ✅ Google-maintained and updated
- ✅ Hardware-accelerated
- ✅ Extremely fast inference
- ✅ No storage impact on your app
- ✅ Privacy-preserving (on-device)

### Requirements
- Android 14 (API 34) or higher
- Device with sufficient RAM (typically 6GB+)
- Google Play Services

### Implementation

```kotlin
// Add dependency
dependencies {
    implementation("com.google.android.gms:play-services-aicore:16.3.0")
}
```

```kotlin
class GeminiNanoInference @Inject constructor(
    private val context: Context
) {
    private var generativeModel: GenerativeModel? = null

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Check if Gemini Nano is available
            val availability = AiCoreClient.create(context)
                .getAiCoreAvailability()
                .await()

            if (availability != AiCoreAvailability.AVAILABLE) {
                return@withContext Result.failure(
                    Exception("Gemini Nano not available on this device")
                )
            }

            // Initialize model
            generativeModel = GenerativeModel(
                modelName = "gemini-nano",
                context = context
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generate(prompt: String, maxTokens: Int = 200): String {
        val model = generativeModel
            ?: throw IllegalStateException("Model not initialized")

        val response = model.generateContent(prompt)
        return response.text ?: ""
    }
}
```

### Checking Availability

```kotlin
suspend fun isGeminiNanoAvailable(): Boolean {
    return try {
        val availability = AiCoreClient.create(context)
            .getAiCoreAvailability()
            .await()

        availability == AiCoreAvailability.AVAILABLE
    } catch (e: Exception) {
        false
    }
}
```

### Fallback Strategy

```kotlin
class LlmInferenceManager @Inject constructor(
    private val geminiNano: GeminiNanoInference?,
    private val onnxRuntime: OnnxRuntimeInference?,
    private val context: Context
) {
    suspend fun generate(prompt: String): String {
        // Try Gemini Nano first (fastest, no storage)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (geminiNano?.isAvailable() == true) {
                return geminiNano.generate(prompt)
            }
        }

        // Fallback to ONNX Runtime with downloaded model
        if (onnxRuntime?.isModelDownloaded() == true) {
            return onnxRuntime.generate(prompt)
        }

        // Suggest model download
        throw ModelNotAvailableException("Please download AI model in settings")
    }
}
```

---

## Option 4: MediaPipe LLM Inference API

### Overview
Google's official solution for on-device LLM inference with optimized performance.

### Features
- ✅ Direct model loading from URL or assets
- ✅ Optimized for mobile (uses GPU delegation)
- ✅ Simple API
- ✅ No auth required for public models

### Implementation

```kotlin
// Add dependency
dependencies {
    implementation("com.google.mediapipe:tasks-genai:0.10.14")
}
```

```kotlin
class MediaPipeLlmInference @Inject constructor(
    private val context: Context
) {
    private var llmInference: LlmInference? = null

    suspend fun initialize(modelPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(256)
                .setTemperature(0.7f)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generate(prompt: String): String = withContext(Dispatchers.Default) {
        val inference = llmInference
            ?: throw IllegalStateException("Model not initialized")

        inference.generateResponse(prompt)
    }
}
```

### Download + Load Model

```kotlin
class MediaPipeModelManager @Inject constructor(
    private val context: Context,
    private val downloader: ModelDownloadManager
) {
    suspend fun downloadAndLoadGemma2B(): Result<String> {
        // Download from Hugging Face (converted to MediaPipe format)
        val modelUrl = "https://huggingface.co/google/gemma-2b-it-mediapipe/resolve/main/gemma-2b-it-cpu-int8.bin"

        val modelFile = downloader.downloadFile(modelUrl, "gemma-2b.bin")

        return Result.success(modelFile.absolutePath)
    }
}
```

---

## Option 5: llama.cpp for Android

### Overview
Port of llama.cpp that runs efficiently on Android with GGUF models.

### Advantages
- ✅ Extremely fast (optimized C++)
- ✅ Many quantization options (2-bit to 16-bit)
- ✅ Large community and model availability
- ✅ Direct downloads from Hugging Face

### Implementation with llama.cpp Android

```kotlin
// Use TheBloke's quantized models on Hugging Face
val modelUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"

// Download GGUF model (much smaller than ONNX)
// TinyLlama Q4 quantized: ~600MB → ~180MB
```

```kotlin
class LlamaCppInference @Inject constructor(
    private val context: Context
) {
    private external fun initModel(modelPath: String): Long
    private external fun generate(contextPtr: Long, prompt: String, maxTokens: Int): String
    private external fun freeModel(contextPtr: Long)

    private var modelContext: Long = 0

    suspend fun initialize(modelPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            modelContext = initModel(modelPath)
            if (modelContext == 0L) {
                return@withContext Result.failure(Exception("Failed to load model"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generate(prompt: String, maxTokens: Int = 200): String =
        withContext(Dispatchers.Default) {
            generate(modelContext, prompt, maxTokens)
        }

    fun release() {
        if (modelContext != 0L) {
            freeModel(modelContext)
            modelContext = 0
        }
    }
}
```

### Quantization Options (Trade-off Matrix)

| Quantization | Size | Quality | Speed | RAM Usage |
|--------------|------|---------|-------|-----------|
| Q2_K | ~200MB | Poor | Very Fast | Low |
| Q4_K_M | ~400MB | Good | Fast | Medium |
| Q5_K_M | ~500MB | Very Good | Medium | Medium |
| Q8_0 | ~800MB | Excellent | Slower | Higher |
| F16 | ~1.2GB | Perfect | Slowest | Highest |

**Recommended for mobile:** Q4_K_M (best balance)

---

## Complete Download Manager Implementation

```kotlin
@Singleton
class UniversalModelDownloader @Inject constructor(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val modelDir = File(context.filesDir, "ai_models")

    enum class ModelType {
        TINYLLAMA_ONNX,
        TINYLLAMA_GGUF,
        GEMMA_2B_MEDIAPIPE,
        PHI_2_ONNX
    }

    data class ModelInfo(
        val name: String,
        val url: String,
        val filename: String,
        val size: Long,
        val license: String,
        val description: String
    )

    private val availableModels = mapOf(
        ModelType.TINYLLAMA_ONNX to ModelInfo(
            name = "TinyLlama 1.1B (ONNX)",
            url = "https://huggingface.co/TinyLlama/TinyLlama-1.1B-Chat-v1.0-ONNX/resolve/main/model_quantized.onnx",
            filename = "tinyllama-1.1b-q8.onnx",
            size = 300_000_000, // 300MB
            license = "Apache 2.0",
            description = "Fast and efficient for mobile devices"
        ),
        ModelType.TINYLLAMA_GGUF to ModelInfo(
            name = "TinyLlama 1.1B (GGUF)",
            url = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            filename = "tinyllama-1.1b-q4.gguf",
            size = 180_000_000, // 180MB
            license = "Apache 2.0",
            description = "Smallest size with good quality"
        ),
        ModelType.GEMMA_2B_MEDIAPIPE to ModelInfo(
            name = "Gemma 2B (MediaPipe)",
            url = "https://huggingface.co/google/gemma-2b-it-mediapipe/resolve/main/gemma-2b-it-cpu-int8.bin",
            filename = "gemma-2b-int8.bin",
            size = 700_000_000, // 700MB
            license = "Gemma License",
            description = "High quality, optimized for MediaPipe"
        ),
        ModelType.PHI_2_ONNX to ModelInfo(
            name = "Phi-2 (ONNX)",
            url = "https://huggingface.co/microsoft/phi-2-onnx/resolve/main/model_quantized.onnx",
            filename = "phi-2-q8.onnx",
            size = 700_000_000, // 700MB
            license = "MIT",
            description = "Best reasoning for mobile"
        )
    )

    suspend fun downloadModel(
        modelType: ModelType,
        onProgress: (percent: Int) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            modelDir.mkdirs()

            val modelInfo = availableModels[modelType]!!
            val modelFile = File(modelDir, modelInfo.filename)

            // Check if already downloaded
            if (modelFile.exists() && modelFile.length() > 0) {
                Timber.d("Model already exists: ${modelInfo.name}")
                return@withContext Result.success(modelFile)
            }

            // Download with progress
            Timber.d("Downloading model: ${modelInfo.name} from ${modelInfo.url}")

            val request = Request.Builder()
                .url(modelInfo.url)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Download failed: ${response.code}")
                }

                val contentLength = response.body?.contentLength() ?: modelInfo.size
                var downloadedBytes = 0L

                response.body?.byteStream()?.use { input ->
                    modelFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytes: Int

                        while (input.read(buffer).also { bytes = it } != -1) {
                            output.write(buffer, 0, bytes)
                            downloadedBytes += bytes

                            val percent = (downloadedBytes * 100 / contentLength).toInt()
                            onProgress(percent)
                        }
                    }
                }
            }

            Timber.d("Download complete: ${modelInfo.name}")
            Result.success(modelFile)

        } catch (e: Exception) {
            Timber.e(e, "Failed to download model: $modelType")
            Result.failure(e)
        }
    }

    fun getModelInfo(modelType: ModelType): ModelInfo? {
        return availableModels[modelType]
    }

    fun isModelDownloaded(modelType: ModelType): Boolean {
        val modelInfo = availableModels[modelType] ?: return false
        val modelFile = File(modelDir, modelInfo.filename)
        return modelFile.exists() && modelFile.length() > 0
    }

    fun deleteModel(modelType: ModelType): Boolean {
        val modelInfo = availableModels[modelType] ?: return false
        val modelFile = File(modelDir, modelInfo.filename)
        return modelFile.delete()
    }

    fun getAllModels(): List<Pair<ModelType, ModelInfo>> {
        return availableModels.entries.map { it.key to it.value }
    }
}
```

---

## Model Selection UI

```kotlin
@Composable
fun ModelSelectionScreen(
    viewModel: ModelSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "AI Model Selection",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Choose a model for on-device AI assistance",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        items(uiState.availableModels) { (modelType, modelInfo) ->
            ModelCard(
                modelInfo = modelInfo,
                isDownloaded = uiState.downloadedModels.contains(modelType),
                isDownloading = uiState.downloadingModel == modelType,
                downloadProgress = if (uiState.downloadingModel == modelType)
                    uiState.downloadProgress else 0,
                onDownload = { viewModel.downloadModel(modelType) },
                onDelete = { viewModel.deleteModel(modelType) },
                onSelect = { viewModel.selectModel(modelType) },
                isSelected = uiState.selectedModel == modelType
            )
        }
    }
}

@Composable
fun ModelCard(
    modelInfo: ModelInfo,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Int,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    isSelected: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = modelInfo.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = modelInfo.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip(text = "${modelInfo.size / 1_000_000}MB")
                Chip(text = modelInfo.license)
            }

            when {
                isDownloading -> {
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Downloading: $downloadProgress%")
                }
                isDownloaded -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isSelected) {
                            Button(onClick = onSelect) {
                                Text("Use This Model")
                            }
                        }
                        OutlinedButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                }
                else -> {
                    Button(onClick = onDownload) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Download")
                    }
                }
            }
        }
    }
}

@Composable
fun Chip(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
```

---

## Summary: Best Approach for VectorText

### Recommended Strategy: Tiered Approach

```kotlin
class AdaptiveLlmManager @Inject constructor(
    private val context: Context,
    private val geminiNano: GeminiNanoInference?,
    private val downloadedModelInference: OnnxRuntimeInference?,
    private val downloader: UniversalModelDownloader
) {
    suspend fun initialize(): LlmBackend {
        // Tier 1: Try Gemini Nano (Android 14+, no download)
        if (Build.VERSION.SDK_INT >= 34) {
            if (geminiNano?.isAvailable() == true) {
                Timber.d("Using Gemini Nano (built-in)")
                return LlmBackend.GEMINI_NANO
            }
        }

        // Tier 2: Check if user downloaded a model
        if (downloader.isModelDownloaded(ModelType.TINYLLAMA_GGUF)) {
            Timber.d("Using downloaded TinyLlama")
            return LlmBackend.TINYLLAMA
        }

        // Tier 3: Offer to download
        Timber.d("No model available, need to download")
        return LlmBackend.NONE
    }
}
```

### User Flow

1. **First launch:**
   - Check if Gemini Nano available (Android 14+)
   - If yes: Use it immediately (best UX!)
   - If no: Show "Download AI Model" in settings

2. **Settings → AI Assistant:**
   - Show available models (TinyLlama, Phi-2, etc.)
   - Display size, features, license
   - One-tap download button
   - Progress indicator during download

3. **After download:**
   - Model loads automatically
   - AI assistant becomes available
   - No further user action needed

### Licenses Summary (All Safe to Use)

| Model | License | Commercial Use | Attribution | Restrictions |
|-------|---------|----------------|-------------|--------------|
| TinyLlama | Apache 2.0 | ✅ Yes | Optional | None |
| Phi-2 | MIT | ✅ Yes | Optional | None |
| Gemma | Gemma ToU | ✅ Yes | Required | Must display attribution |
| Qwen | Apache 2.0 | ✅ Yes | Optional | None |
| Gemini Nano | Google ToS | ✅ Yes | N/A | Built-in only |

**All models listed can be used commercially without sign-in or license acceptance dialogs.**

---

## Storage Requirements

### App Size Impact
- **Without model:** ~15MB APK
- **With bundled model:** 15MB + model size
- **Recommended:** Download on demand (keep APK small)

### User Storage
- TinyLlama Q4: ~180-300MB
- Gemini Nano: 0MB (system model)
- Phi-2: ~700MB
- Gemma 2B: ~700MB

### Cache Strategy
```kotlin
// Clean up old models when storage is low
fun cleanupOldModels() {
    if (getAvailableStorage() < 500_000_000) { // 500MB threshold
        // Delete unused models
        downloader.deleteModel(ModelType.PHI_2_ONNX)
    }
}
```

---

## Final Recommendation for VectorText ⭐

**Implementation Plan:**

1. **Phase 1: Gemini Nano Support (Android 14+)**
   - Zero download, best performance
   - Fallback to rule-based for older devices
   - **Effort:** 2-3 days

2. **Phase 2: Add Download Option (All Devices)**
   - TinyLlama GGUF (Q4_K_M) - 180MB
   - One-tap download from Hugging Face
   - **Effort:** 3-5 days

3. **Phase 3: Multiple Model Support**
   - Let users choose based on device storage
   - Add Phi-2 for better quality (optional)
   - **Effort:** 1-2 days

**Total effort:** 1-2 weeks for full implementation

**User experience:**
- Android 14+ users: Instant AI (Gemini Nano)
- Older devices: One-time 180MB download
- No sign-ins, no license dialogs
- All open-source, permissive licenses
