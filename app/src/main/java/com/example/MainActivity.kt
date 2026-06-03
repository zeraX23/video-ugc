package com.example

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.database.UgcDraft
import com.example.data.model.UgcScene
import com.example.data.model.UgcScript
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    UgcStudioApp(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun UgcStudioApp(
    modifier: Modifier = Modifier,
    viewModel: UgcViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Observe state from ViewModel
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val customPrompt by viewModel.customPrompt.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val draftsList by viewModel.draftsList.collectAsState()
    
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> viewModel.updateSelectedImageUri(uri) }
    )

    val isPlayingVideo by viewModel.isPlayingVideo.collectAsState()
    val activeSceneIndex by viewModel.activeSceneIndex.collectAsState()
    val elapsedFloat by viewModel.sceneTimeElapsedFloat.collectAsState()

    // Initialize Indonesian and Fallback Text To Speech engine inside Compose safely
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsInitialized by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var tempTts: TextToSpeech? = null
        tempTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val localeId = Locale.Builder().setLanguage("id").setRegion("ID").build()
                tempTts?.let { instantiatedTts ->
                    val result = instantiatedTts.setLanguage(localeId)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w("TTS", "Indonesian language not supported. Falling back to default.")
                        instantiatedTts.language = Locale.ENGLISH
                    }
                    ttsInitialized = true
                }
            } else {
                Log.e("TTS", "Failed to initialize TTS.")
            }
        }
        tts = tempTts
        onDispose {
            tempTts.stop()
            tempTts.shutdown()
        }
    }

    // Connect Player scene advanced state with TTS speech execution
    LaunchedEffect(isPlayingVideo, activeSceneIndex) {
        if (isPlayingVideo && ttsInitialized && tts != null) {
            val state = uiState
            if (state is UgcUiState.Success) {
                val currentScene = state.script.scenes.getOrNull(activeSceneIndex)
                if (currentScene != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        tts?.speak(
                            currentScene.voiceover,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "SCENE_VOICEOVER_$activeSceneIndex"
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        tts?.speak(currentScene.voiceover, TextToSpeech.QUEUE_FLUSH, null)
                    }
                }
            }
        } else if (!isPlayingVideo) {
            tts?.stop()
        }
    }

    // Safe track for audio download statuses
    var audioDownloadStatus by remember { mutableStateOf("") }
    if (audioDownloadStatus.isNotEmpty()) {
        LaunchedEffect(audioDownloadStatus) {
            Toast.makeText(context, audioDownloadStatus, Toast.LENGTH_LONG).show()
            audioDownloadStatus = ""
        }
    }

    // Scroll state
    val mainScrollState = rememberScrollState()

    // Outer premium dark velvet container with an elegant backdrop
    Column(
        modifier = modifier
            .background(Color(0xFF0F0E13)) // High-end studio dark tone
            .verticalScroll(mainScrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // --- 1. App Title Header & Free Badge ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "UGC Video Studio",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "Draf & Simulasi Video Pemasaran AI",
                    fontSize = 12.sp,
                    color = Color(0xFFA5A1BB)
                )
            }
            
            // Neon status badge: Token Free
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E3A31))
                    .border(1.dp, Color(0xFF00FFC4), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FFC4))
                    )
                    Text(
                        text = "GRATIS TANPA TOKEN",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FFC4)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 2. Screen Context Switch: Editor vs Player Results ---
        when (val state = uiState) {
            is UgcUiState.Idle, is UgcUiState.Loading, is UgcUiState.Error -> {
                // If there's an error, display it as a friendly alert box
                if (state is UgcUiState.Error) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1E21)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Error icon",
                                tint = Color(0xFFEF4444)
                            )
                            Text(
                                text = state.message,
                                color = Color(0xFFFCA5A5),
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Normal Input Setup Flow (Categories, Presets, Styles, Prompt Editor)
                Text(
                    text = "1. Pilih Kategori & Ide Prompt",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Category Selection Chips (Scrollable row of custom designed buttons to avoid standard FilterChip compile errors)
                val catScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(catScrollState)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UgcPresetData.categories.forEach { category ->
                        val isSelected = selectedCategory.id == category.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFF7C3AED) else Color(0xFF1B1A21))
                                .border(1.dp, if (isSelected) Color(0xFFB180FF) else Color(0xFF2E2D38), RoundedCornerShape(20.dp))
                                .clickable { viewModel.selectCategory(category) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("category_chip_${category.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = category.iconEmoji,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = category.name,
                                    color = if (isSelected) Color.White else Color(0xFFA5A1BB),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Presets Panel for Selected Category
                Text(
                    text = "Preset Skenario untuk ${selectedCategory.name}:",
                    fontSize = 13.sp,
                    color = Color(0xFFA5A1BB),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                selectedCategory.presets.forEach { preset ->
                    val isSelected = selectedPreset.title == preset.title
                    Card(
                        onClick = { viewModel.selectPreset(preset) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("preset_card_${preset.title.replace(" ", "_")}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF221A30) else Color(0xFF15141B)
                        ),
                        border = borderStroke(isSelected),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = preset.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) Color(0xFF00FFC4) else Color.White
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF00FFC4),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = preset.subText,
                                fontSize = 11.sp,
                                color = Color(0xFFA5A1BB)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Style Preset Selection ---
                Text(
                    text = "2. Pilih Gaya Pembawaan (Style)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val styleScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(styleScrollState)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UgcPresetData.styles.forEach { style ->
                        val isSelected = selectedStyle.id == style.id
                        Card(
                            onClick = { viewModel.selectStyle(style) },
                            modifier = Modifier
                                .width(150.dp)
                                .height(100.dp)
                                .testTag("style_card_${style.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF271B3A) else Color(0xFF15141B)
                            ),
                            border = borderStroke(isSelected),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = style.emoji, fontSize = 16.sp)
                                    Text(
                                        text = style.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF00FFC4) else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = style.description,
                                    fontSize = 9.sp,
                                    color = Color(0xFFA5A1BB),
                                    lineHeight = 12.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- Product Image Picker ---
                Text(
                    text = "3. Foto Produk (Opsional)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Unggah foto produk untuk dianalisis oleh AI dan disisipkan nilai visualnya ke dalam script UGC.",
                    fontSize = 11.sp,
                    color = Color(0xFFA5A1BB),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Card(
                    onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(bottom = 12.dp)
                        .testTag("image_picker_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF15141B)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF2E2D38))
                ) {
                    if (selectedImageUri != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Foto Produk Terpilih",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Remove image button
                            IconButton(
                                onClick = { viewModel.updateSelectedImageUri(null) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(28.dp)
                                    .background(Color.Black.copy(alpha=0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus foto", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Pilih Foto", tint = Color(0xFF7C3AED), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ketuk untuk Memilih Foto Produk", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // --- Custom Prompt Editor ---
                Text(
                    text = "4. Sempurnakan Detail Ide (Opsional)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Semakin detil deskripsi Anda, semakin tajam dan alami naskah video UGC yang diproduksi AI.",
                    fontSize = 11.sp,
                    color = Color(0xFFA5A1BB),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = { viewModel.updateCustomPrompt(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("prompt_editor"),
                    placeholder = { Text("Masukkan ide promosi Anda...", color = Color(0xFF535165)) },
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedBorderColor = Color(0xFF7C3AED),
                        unfocusedBorderColor = Color(0xFF2E2D38),
                        focusedContainerColor = Color(0xFF14131A),
                        unfocusedContainerColor = Color(0xFF14131A)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- Generate Action Button ---
                if (state is UgcUiState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(10.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1730))
                            .border(1.dp, Color(0xFFB180FF), RoundedCornerShape(12.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color(0xFF00FFC4),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Menulis Naskah & Merancang Visual...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "AI sedang meramu Hook, Scene, dan Voiceover promosi",
                                color = Color(0xFFA5A1BB),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.generateUgc() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C3AED)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star, // standard core replacement
                                contentDescription = "Sparkle Icon",
                                tint = Color(0xFF00FFC4)
                            )
                            Text(
                                text = "PRODUKSI NASKAH UGC",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Database Drafts History ---
                if (draftsList.isNotEmpty()) {
                    Text(
                        text = "📜 Riwayat UGC Studio Anda",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    draftsList.forEach { draft ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .testTag("draft_history_card_${draft.id}"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF15141B)),
                            border = BorderStroke(1.dp, Color(0xFF2E2D38)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = draft.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = draft.category,
                                            fontSize = 9.sp,
                                            color = Color(0xFF00FFC4),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(3.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF535165))
                                        )
                                        Text(
                                            text = draft.style,
                                            fontSize = 9.sp,
                                            color = Color(0xFFA5A1BB)
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Open Action Target
                                    IconButton(
                                        onClick = { viewModel.loadSavedDraft(draft) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("open_draft_btn_${draft.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow, // core replacement
                                            contentDescription = "Buka di Simulator",
                                            tint = Color(0xFF7C3AED),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    // Trash icon
                                    IconButton(
                                        onClick = { viewModel.deleteDraft(draft) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("delete_draft_btn_${draft.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus draf",
                                            tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is UgcUiState.Success -> {
                // UGC Generated Successfully!
                val script = state.script
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            viewModel.stopVideo()
                            // Set uiState directly to Idle so users can modify inputs again
                            viewModel.updateCustomPrompt(customPrompt) // Refresh prompt state
                            viewModel.generateUgc() // Triggers reload or go to empty
                            scope.launch {
                                // Direct back reset flow
                                viewModel.generateUgc() // Re-triggers simple layout toggle
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB180FF)),
                        modifier = Modifier.testTag("back_to_prompt_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                            Text("Sunting Prompt & Preset", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF221535))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Draft ID #${script.title.hashCode().toString().takeLast(4)}",
                            fontSize = 10.sp,
                            color = Color(0xFFB180FF),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Header details
                Text(
                    text = script.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Text(text = "🎯 ${script.targetAudience}", fontSize = 11.sp, color = Color(0xFFA5A1BB))
                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color(0xFF535165)))
                    Text(text = "🔥 Hook: \"${script.tagline}\"", fontSize = 11.sp, color = Color(0xFF00FFC4), fontWeight = FontWeight.SemiBold)
                }

                // --- THE 9:16 VERTICAL VIDEO SIMULATOR PLAYER ---
                Text(
                    text = "🖥️ Pratonton & Simulasi Video UGC",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val currentScene = script.scenes.getOrNull(activeSceneIndex) ?: script.scenes[0]

                // Vertical aspect container mimicking a smartphone format
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF07060A))
                        .border(1.dp, Color(0xFF2E2D38), RoundedCornerShape(16.dp))
                        .testTag("video_player_frame")
                ) {
                    // Backdrops based on active category
                    val backgroundGradient = getBackgroundGradientForCategory(selectedCategory.id)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundGradient)
                    )

                    // Overlay Vignette Shadow
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    // Dynamic Sound Visualizers in Background (only shows up when playing)
                    if (isPlayingVideo) {
                        SoundVisualizerOverlay()
                    }

                    // --- UGC Creator Simulated Avatar & Scene visuals info ---
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Scene Status Headers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "SCENE ${currentScene.number} / ${script.scenes.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF7C3AED).copy(alpha = 0.9f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "1080p FHD",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF00FFC4).copy(alpha = 0.9f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "LIVE RENDER",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        // Middle Visual Scene Illustration Box
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            // Render Simulated UGC Creator Avatar with some continuous soft breathing movement
                            val infiniteTransition = rememberInfiniteTransition()
                            val breathScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = if (isPlayingVideo) 1.08f else 1.02f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .drawBehind {
                                        // Draw a cute neon dynamic halo around the avatar
                                        drawCircle(
                                            color = Color(0xFF00FFC4),
                                            radius = (size.width / 2f) * breathScale,
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E1D2D))
                                    .border(1.dp, Color(0xFFB180FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person, // standard core representation
                                    contentDescription = "Simulated Talent",
                                    tint = Color.White,
                                    modifier = Modifier.size(50.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Interactive camera lens overlay marker
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "📸 Kamera: ${currentScene.visual.take(35)}...",
                                    fontSize = 9.sp,
                                    color = Color(0xFFA5A1BB),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Bottom Layout - Big Kinetic UGC Subtitles (Overlay) & Visual Prompt Details
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Overlay Text
                            Surface(
                                color = Color(0xFFFACC15), // Beautiful Yellow kinetic caption tag
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                                    .shadow(4.dp, RoundedCornerShape(8.dp))
                            ) {
                                Text(
                                    text = currentScene.overlay.uppercase(),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Spoken script info pane
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CustomVolumeUpIcon(
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF00FFC4)
                                    )
                                    Text(
                                        text = currentScene.voiceover,
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Player Progress Tracker bar
                    val duration = currentScene.durationSecs.toFloat()
                    val progressFraction = if (duration > 0) elapsedFloat / duration else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color(0xFF2E2D38))
                            .align(Alignment.BottomStart)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                                .background(Color(0xFF00FFC4))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- PLAYER CONTROLS ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Current active scene timeline tracker dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        script.scenes.forEachIndexed { idx, scene ->
                            val isActive = idx == activeSceneIndex
                            Box(
                                modifier = Modifier
                                    .size(if (isActive) 12.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) Color(0xFF00FFC4) else Color(0xFF2E2D38))
                                    .clickable { viewModel.seekToScene(idx) }
                                    .testTag("timeline_dot_$idx")
                            )
                        }
                    }

                    // Play / Pause / Stop buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play / Pause Button with Custom Pause draw graphics
                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7C3AED))
                                .testTag("player_play_pause_button")
                        ) {
                            if (isPlayingVideo) {
                                CustomPauseIcon(tint = Color.White)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White
                                )
                            }
                        }

                        // Stop Button with Custom Stop draw graphics
                        IconButton(
                            onClick = { viewModel.stopVideo() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1B1A21))
                                .border(1.dp, Color(0xFF2E2D38), CircleShape)
                                .testTag("player_stop_button")
                        ) {
                            CustomStopIcon(tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // --- 4. THE REAL DOWNLOAD / EXPORTER CENTER (BISA DI DOWNLOAD!) ---
                Text(
                    text = "📥 Exporter - Simpan & Download Gratis",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Ekspor naskah promosi dan suara narasi buatan AI langsung ke memori internal perangkat Anda untuk diedit di Tik Tok atau Cap Cut.",
                    fontSize = 11.sp,
                    color = Color(0xFFA5A1BB),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export Script TXT file
                    Card(
                        onClick = {
                            val msg = UgcExporter.exportScriptToDownloads(context, script)
                            Toast.makeText(context, msg ?: "Gagal mengekspor naskah", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_script_button"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1A21)),
                        border = BorderStroke(1.dp, Color(0xFF292835)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CustomTextSnippetIcon(tint = Color(0xFFB180FF))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Naskah Video (.txt)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Ekspor Scene & Catatan",
                                fontSize = 9.sp,
                                color = Color(0xFFA5A1BB)
                            )
                        }
                    }

                    // Export Speech Voiceover WAV file
                    Card(
                        onClick = {
                            if (!ttsInitialized || tts == null) {
                                Toast.makeText(context, "Sistem suara belum siap. Harap tunggu sebentar.", Toast.LENGTH_SHORT).show()
                                return@Card
                            }
                            UgcExporter.exportVoiceoverToDownloads(context, script, tts!!) { status ->
                                scope.launch {
                                    audioDownloadStatus = status
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_audio_button"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1A21)),
                        border = BorderStroke(1.dp, Color(0xFF292835)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CustomAudioIcon(tint = Color(0xFF00FFC4))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Suara Narasi (.wav)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Sintesis Pengisi Suara AI",
                                fontSize = 9.sp,
                                color = Color(0xFFA5A1BB)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Detailed Scene List for reading
                Text(
                    text = "📄 Naskah Lengkap & Panduan Adegan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                script.scenes.forEach { scene ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111016)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF23222E))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Adegan ${scene.number}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFB180FF)
                                )
                                Text(
                                    text = "${scene.durationSecs} detik",
                                    fontSize = 10.sp,
                                    color = Color(0xFFA5A1BB)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "VISUAL:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FFC4)
                            )
                            Text(
                                text = scene.visual,
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "TEKS OVERLAY:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFACC15)
                            )
                            Text(
                                text = scene.overlay,
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "VOICEOVER SUARA:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA5A1BB)
                            )
                            Text(
                                text = "\"${scene.voiceover}\"",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- CUSTOM HIGH-CRAFT CANVAS DRAWINGS ---

@Composable
fun CustomPauseIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Row(
        modifier = modifier.size(16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(4.dp).height(12.dp).clip(RoundedCornerShape(1.dp)).background(tint))
        Box(modifier = Modifier.width(4.dp).height(12.dp).clip(RoundedCornerShape(1.dp)).background(tint))
    }
}

@Composable
fun CustomStopIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(tint)
    )
}

@Composable
fun CustomTextSnippetIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Box(
        modifier = modifier
            .size(24.dp)
            .border(2.dp, tint, RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(2.dp).background(tint))
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(tint))
            Box(modifier = Modifier.fillMaxWidth(0.5f).height(2.dp).background(tint))
        }
    }
}

@Composable
fun CustomAudioIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier.size(24.dp)) {
        // Draw standard musical note
        drawCircle(color = tint, radius = 4.dp.toPx(), center = Offset(7.dp.toPx(), 17.dp.toPx()))
        drawLine(color = tint, start = Offset(10.dp.toPx(), 17.dp.toPx()), end = Offset(10.dp.toPx(), 7.dp.toPx()), strokeWidth = 2.dp.toPx())
        drawLine(color = tint, start = Offset(10.dp.toPx(), 7.dp.toPx()), end = Offset(18.dp.toPx(), 10.dp.toPx()), strokeWidth = 2.dp.toPx())
    }
}

@Composable
fun CustomVolumeUpIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier.size(16.dp)) {
        val path = Path().apply {
            moveTo(2.dp.toPx(), 6.dp.toPx())
            lineTo(5.dp.toPx(), 6.dp.toPx())
            lineTo(9.dp.toPx(), 2.dp.toPx())
            lineTo(9.dp.toPx(), 14.dp.toPx())
            lineTo(5.dp.toPx(), 10.dp.toPx())
            lineTo(2.dp.toPx(), 10.dp.toPx())
            close()
        }
        drawPath(path = path, color = tint)

        // Draw volume indicator sound wave arcs
        drawArc(
            color = tint,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(8.dp.toPx(), 4.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(6.dp.toPx(), 8.dp.toPx()),
            style = Stroke(width = 1.51f.dp.toPx())
        )
    }
}

// --- STANDARD STUDIO HELPERS ---

fun borderStroke(selected: Boolean): BorderStroke {
    return if (selected) {
        BorderStroke(1.5.dp, Color(0xFFB180FF))
    } else {
        BorderStroke(1.dp, Color(0xFF25242D))
    }
}

fun getBackgroundGradientForCategory(categoryId: String): Brush {
    return when (categoryId) {
        "fnd" -> Brush.radialGradient(
            colors = listOf(Color(0xFFE11D48).copy(alpha = 0.5f), Color(0xFF1E1014)),
            radius = 1200f
        )
        "beauty" -> Brush.radialGradient(
            colors = listOf(Color(0xFFEC4899).copy(alpha = 0.4f), Color(0xFF160E18)),
            radius = 1200f
        )
        "tech" -> Brush.radialGradient(
            colors = listOf(Color(0xFF2563EB).copy(alpha = 0.5f), Color(0xFF0D0F1A)),
            radius = 1200f
        )
        "health" -> Brush.radialGradient(
            colors = listOf(Color(0xFF10B981).copy(alpha = 0.45f), Color(0xFF0C1612)),
            radius = 1200f
        )
        "travel" -> Brush.radialGradient(
            colors = listOf(Color(0xFFF59E0B).copy(alpha = 0.4f), Color(0xFF19130D)),
            radius = 1200f
        )
        else -> Brush.radialGradient(
            colors = listOf(Color(0xFF7C3AED).copy(alpha = 0.4f), Color(0xFF120E1C)),
            radius = 1200f
        )
    }
}

@Composable
fun SoundVisualizerOverlay() {
    val infiniteTransition = rememberInfiniteTransition()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 40.dp)
            .offset(y = 260.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // Render animated equalizer columns
        for (i in 0 until 6) {
            val animDuration = 400 + (i * 80)
            val barHeight by infiniteTransition.animateFloat(
                initialValue = 10f,
                targetValue = 45f,
                animationSpec = infiniteRepeatable(
                    animation = tween(animDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF00FFC4).copy(alpha = 0.4f))
            )
        }
    }
}
