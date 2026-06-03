package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.database.DatabaseProvider
import com.example.data.database.UgcDraft
import com.example.data.model.UgcScript
import com.example.data.repository.UgcRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UgcUiState {
    object Idle : UgcUiState
    object Loading : UgcUiState
    data class Success(val script: UgcScript) : UgcUiState
    data class Error(val message: String) : UgcUiState
}

class UgcViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "UgcViewModel"

    // Initialize Room Database & Repository
    private val database = DatabaseProvider.getDatabase(application)
    private val repository = UgcRepository(database.ugcDraftDao())

    // Expose saved drafts reactively to the UI
    val draftsList: StateFlow<List<UgcDraft>> = repository.allDrafts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selection States
    private val _selectedCategory = MutableStateFlow(UgcPresetData.categories[0])
    val selectedCategory: StateFlow<UgcCategory> = _selectedCategory.asStateFlow()

    private val _selectedPreset = MutableStateFlow(UgcPresetData.categories[0].presets[0])
    val selectedPreset: StateFlow<UgcPresetPrompt> = _selectedPreset.asStateFlow()

    private val _selectedStyle = MutableStateFlow(UgcPresetData.styles[2]) // Default: Casual & Otentik
    val selectedStyle: StateFlow<UgcStyle> = _selectedStyle.asStateFlow()

    private val _customPrompt = MutableStateFlow(UgcPresetData.categories[0].presets[0].promptText)
    val customPrompt: StateFlow<String> = _customPrompt.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<android.net.Uri?>(null)
    val selectedImageUri: StateFlow<android.net.Uri?> = _selectedImageUri.asStateFlow()

    // API Generation UI State
    private val _uiState = MutableStateFlow<UgcUiState>(UgcUiState.Idle)
    val uiState: StateFlow<UgcUiState> = _uiState.asStateFlow()

    // Simulator Playback States
    private val _isPlayingVideo = MutableStateFlow(false)
    val isPlayingVideo: StateFlow<Boolean> = _isPlayingVideo.asStateFlow()

    private val _activeSceneIndex = MutableStateFlow(0)
    val activeSceneIndex: StateFlow<Int> = _activeSceneIndex.asStateFlow()

    private val _sceneTimeElapsed = MutableStateFlow(0f) // Current scene elapsed time in seconds
    val sceneTimeElapsed: StateFlow<String> = MutableStateFlow("0.0").apply {
        // Formatted value updated from job
    }.asStateFlow() // wait, simpler and safer to just expose Float
    
    private val _sceneTimeElapsedFloat = MutableStateFlow(0f)
    val sceneTimeElapsedFloat: StateFlow<Float> = _sceneTimeElapsedFloat.asStateFlow()

    private var playerJob: Job? = null

    init {
        // Default startup logs
        Log.d(TAG, "UgcViewModel Initialized")
    }

    fun selectCategory(category: UgcCategory) {
        _selectedCategory.value = category
        if (category.presets.isNotEmpty()) {
            selectPreset(category.presets[0])
        }
    }

    fun selectPreset(preset: UgcPresetPrompt) {
        _selectedPreset.value = preset
        _customPrompt.value = preset.promptText
    }

    fun selectStyle(style: UgcStyle) {
        _selectedStyle.value = style
    }

    fun updateCustomPrompt(text: String) {
        _customPrompt.value = text
    }

    fun updateSelectedImageUri(uri: android.net.Uri?) {
        _selectedImageUri.value = uri
    }

    /**
     * Executes UGC Script Generation via free-tier Gemini API
     */
    fun generateUgc() {
        val category = _selectedCategory.value.name
        val style = _selectedStyle.value.name + " (" + _selectedStyle.value.emoji + ")"
        val prompt = _customPrompt.value.trim()
        val imageUri = _selectedImageUri.value

        if (prompt.isEmpty()) {
            _uiState.value = UgcUiState.Error("Prompt deskripsi ide tidak boleh kosong!")
            return
        }

        stopVideo()
        _uiState.value = UgcUiState.Loading

        viewModelScope.launch {
            try {
                var base64Img: String? = null
                var mimeType: String = "image/jpeg"
                if (imageUri != null) {
                    val app = getApplication<Application>()
                    val contentResolver = app.contentResolver
                    mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"
                    val inputStream = contentResolver.openInputStream(imageUri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    if (bytes != null) {
                        base64Img = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                    }
                }

                // Call raw Gemini client
                val rawJsonResult = GeminiClient.generateUgcScript(category, style, prompt, base64Img, mimeType)
                
                // Parse the returned response
                val script = UgcScript.fromJson(rawJsonResult)
                
                // Set UI success
                _uiState.value = UgcUiState.Success(script)

                // Auto-save this magnificent generated campaign to history
                val draft = UgcDraft(
                    title = script.title,
                    category = category,
                    style = style,
                    prompt = prompt,
                    generationJson = rawJsonResult
                )
                repository.insertDraft(draft)
                Log.d(TAG, "Successfully Auto-saved generated draft to Room")

            } catch (e: Exception) {
                Log.e(TAG, "Error generating UGC script", e)
                _uiState.value = UgcUiState.Error(e.localizedMessage ?: "Terjadi kegagalan jaringan atau parsing.")
            }
        }
    }

    /**
     * Deletes a draft from local history database
     */
    fun deleteDraft(draft: UgcDraft) {
        viewModelScope.launch {
            repository.deleteDraft(draft)
        }
    }

    /**
     * Loads a previously generated draft into the current success view state
     */
    fun loadSavedDraft(draft: UgcDraft) {
        stopVideo()
        try {
            val script = UgcScript.fromJson(draft.generationJson)
            _uiState.value = UgcUiState.Success(script)
            
            // Re-sync UI selectors
            _customPrompt.value = draft.prompt
            
            // Try to find matching Category in presets
            val matchedCat = UgcPresetData.categories.find { it.name == draft.category }
            if (matchedCat != null) {
                _selectedCategory.value = matchedCat
            }
            
            // Try to find matching Style in presets
            val matchedStyle = UgcPresetData.styles.find { draft.style.contains(it.name) }
            if (matchedStyle != null) {
                _selectedStyle.value = matchedStyle
            }
            
            Log.d(TAG, "Loaded draft into UI state: ${script.title}")
        } catch (e: Exception) {
            _uiState.value = UgcUiState.Error("Gagal membuka dokumen tersimpan: ${e.localizedMessage}")
        }
    }

    /**
     * Controls Play / Pause of the UGC Player
     */
    fun togglePlayPause() {
        if (_isPlayingVideo.value) {
            pauseVideo()
        } else {
            playVideo()
        }
    }

    fun playVideo() {
        val ui = _uiState.value
        val script = if (ui is UgcUiState.Success) ui.script else return

        _isPlayingVideo.value = true
        playerJob?.cancel()

        playerJob = viewModelScope.launch {
            while (_isPlayingVideo.value) {
                val currentIndex = _activeSceneIndex.value
                if (currentIndex >= script.scenes.size) {
                    // Script ended, reset player
                    _isPlayingVideo.value = false
                    _activeSceneIndex.value = 0
                    _sceneTimeElapsedFloat.value = 0f
                    break
                }

                val currentScene = script.scenes[currentIndex]
                val sceneDuration = currentScene.durationSecs.toFloat()
                
                // Interval increment loop for smooth progress ticks
                val tickIntervalMs = 50L
                var elapsedMs = (_sceneTimeElapsedFloat.value * 1000).toLong()
                val totalMs = (sceneDuration * 1000).toLong()

                while (elapsedMs < totalMs && _isPlayingVideo.value) {
                    delay(tickIntervalMs)
                    if (!_isPlayingVideo.value) break
                    elapsedMs += tickIntervalMs
                    _sceneTimeElapsedFloat.value = elapsedMs.toFloat() / 1000f
                }

                if (_isPlayingVideo.value) {
                    // Advance Scene
                    _sceneTimeElapsedFloat.value = 0f
                    if (currentIndex + 1 < script.scenes.size) {
                        _activeSceneIndex.value = currentIndex + 1
                    } else {
                        // Finished last scene
                        _isPlayingVideo.value = false
                        _activeSceneIndex.value = 0
                        break
                    }
                }
            }
        }
    }

    fun pauseVideo() {
        _isPlayingVideo.value = false
        playerJob?.cancel()
    }

    fun stopVideo() {
        _isPlayingVideo.value = false
        _activeSceneIndex.value = 0
        _sceneTimeElapsedFloat.value = 0f
        playerJob?.cancel()
    }

    fun seekToScene(index: Int) {
        val ui = _uiState.value
        val script = if (ui is UgcUiState.Success) ui.script else return
        if (index in 0 until script.scenes.size) {
            _activeSceneIndex.value = index
            _sceneTimeElapsedFloat.value = 0f
            if (_isPlayingVideo.value) {
                playVideo()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerJob?.cancel()
    }
}
