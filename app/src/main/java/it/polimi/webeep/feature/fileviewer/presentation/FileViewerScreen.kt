package it.polimi.webeep.feature.fileviewer.presentation

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.compose.runtime.LaunchedEffect
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.EmptyStateView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import it.polimi.webeep.feature.fileviewer.presentation.components.PdfViewer
import it.polimi.webeep.feature.fileviewer.presentation.components.MediaViewer
import it.polimi.webeep.feature.fileviewer.presentation.components.CodeViewer
import java.io.File
import kotlin.math.roundToInt

import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    viewModel: FileViewerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pageSubtitle = when (val state = uiState) {
        is FileViewerUiState.Pdf -> androidx.compose.ui.res.pluralStringResource(R.plurals.file_viewer_pdf_pages, state.pageCount, state.pageCount)
        is FileViewerUiState.Image -> stringResource(R.string.file_viewer_image)
        is FileViewerUiState.Web -> stringResource(R.string.file_viewer_web)
        else -> null
    }
    
    var showTopBar by remember { mutableStateOf(true) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            if (!isLandscape) {
                AnimatedVisibility(
                    visible = showTopBar,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = viewModel.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                pageSubtitle?.let { subtitle ->
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ),
                        scrollBehavior = scrollBehavior
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        if (uiState is FileViewerUiState.Image || uiState is FileViewerUiState.Media || !showTopBar) 
                            PaddingValues(0.dp) 
                        else 
                            innerPadding
                    )
            ) {
                when (val state = uiState) {
                    FileViewerUiState.Loading -> FileViewerLoading()
                    is FileViewerUiState.Image -> ImagePreviewContent(
                        url = state.url,
                        title = viewModel.title,
                        onToggleFullScreen = { showTopBar = !showTopBar }
                    )
                    is FileViewerUiState.Pdf -> PdfViewer(
                        file = state.file,
                        pageCount = state.pageCount,
                        onToggleFullScreen = { showTopBar = !showTopBar }
                    )
                    is FileViewerUiState.Media -> MediaViewer(url = state.url)
                    is FileViewerUiState.Code -> CodeViewer(content = state.content)
                    is FileViewerUiState.Web -> WebContent(url = state.url)
                    is FileViewerUiState.CustomTab -> CustomTabLauncher(
                        url = state.url,
                        onLaunched = onBackClick
                    )
                    is FileViewerUiState.ExternalViewer -> ExternalFileContent(
                        file = state.file,
                        mimeType = state.mimeType,
                        title = viewModel.title
                    )
                    is FileViewerUiState.Error -> EmptyStateView(
                        icon = Icons.Default.Description,
                        title = stringResource(R.string.file_viewer_error_title),
                        subtitle = state.message,
                        actionLabel = stringResource(R.string.retry),
                        onAction = viewModel::load,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            
            if (isLandscape || !showTopBar) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 40.dp, start = 16.dp)
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun FileViewerLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
            text = stringResource(R.string.file_viewer_loading),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ImagePreviewContent(
    url: String,
    title: String,
    modifier: Modifier = Modifier,
    onToggleFullScreen: () -> Unit = {}
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleFullScreen() },
                    onDoubleTap = { tapOffset ->
                        val targetScale = if (scale > 1f) 1f else 3f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val relativeTap = tapOffset - center
                        val maxX = (size.width * (targetScale - 1f)) / 2f
                        val maxY = (size.height * (targetScale - 1f)) / 2f
                        val targetOffsetX = if (targetScale == 1f) 0f else (-relativeTap.x * (targetScale - 1f)).coerceIn(-maxX, maxX)
                        val targetOffsetY = if (targetScale == 1f) 0f else (-relativeTap.y * (targetScale - 1f)).coerceIn(-maxY, maxY)

                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            } else {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                        } catch (_: Exception) {}

                        coroutineScope.launch {
                            val startScale = scale
                            val startOffsetX = offsetX
                            val startOffsetY = offsetY

                            animate(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 300)
                            ) { progress, _ ->
                                scale = startScale + (targetScale - startScale) * progress
                                offsetX = startOffsetX + (targetOffsetX - startOffsetX) * progress
                                offsetY = startOffsetY + (targetOffsetY - startOffsetY) * progress
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val oldScale = scale
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale == 1f || scale == 5f) {
                        if (oldScale != scale) {
                            try {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                                } else {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                    val maxX = (size.width * (scale - 1)) / 2
                    val maxY = (size.height * (scale - 1)) / 2
                    offsetX = (offsetX + pan.x * scale).coerceIn(-maxX, maxX)
                    offsetY = (offsetY + pan.y * scale).coerceIn(-maxY, maxY)
                }
            }
    ) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = title,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit,
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            },
            error = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.file_viewer_image_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            success = { SubcomposeAsyncImageContent() }
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
            tonalElevation = 2.dp
        ) {
            Text(
                text = stringResource(R.string.file_viewer_pinch_hint),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExternalFileContent(
    file: File,
    mimeType: String,
    title: String
) {
    val context = LocalContext.current

    val launchIntent = {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.file_viewer_open_with)))
        } catch (_: Exception) {
            // handled by empty state
        }
    }

    LaunchedEffect(file) {
        launchIntent()
    }

    EmptyStateView(
        icon = Icons.Default.Description,
        title = stringResource(R.string.file_viewer_external_title),
        subtitle = stringResource(R.string.file_viewer_external_subtitle, title),
        actionLabel = stringResource(R.string.file_viewer_open_again),
        onAction = launchIntent,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun CustomTabLauncher(
    url: String,
    onLaunched: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(url) {
        CustomTabsIntent.Builder()
            .build()
            .launchUrl(context, Uri.parse(url))
        onLaunched()
    }
    FileViewerLoading()
}

@android.annotation.SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebContent(
    url: String,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { androidx.compose.runtime.mutableStateOf(true) }
    var webView by remember { androidx.compose.runtime.mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                webViewClient = WebViewClient()
                webChromeClient = android.webkit.WebChromeClient()
                loadUrl("about:blank")
                destroy()
            }
            webView = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }
                            
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val requestUrl = request?.url ?: return true
                                val scheme = requestUrl.scheme ?: return true
                                if (scheme == "http" || scheme == "https") {
                                    return false // loads inside the webview
                                }
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, requestUrl)
                                    view?.context?.startActivity(intent)
                                } catch (_: Exception) {}
                                return true
                            }
                        }
                        
                        webChromeClient = android.webkit.WebChromeClient()
                        
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            mediaPlaybackRequiresUserGesture = false
                            javaScriptCanOpenWindowsAutomatically = true
                        }

                        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)

                        loadUrl(MoodleFileUrlBuilder.stripAuthFromUrl(url))
                    }
                },
                update = { webView ->
                    val cleanUrl = MoodleFileUrlBuilder.stripAuthFromUrl(url)
                    if (webView.url != cleanUrl) {
                        webView.loadUrl(cleanUrl)
                    }
                }
            )
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
