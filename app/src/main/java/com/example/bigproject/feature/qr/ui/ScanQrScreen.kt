package com.example.bigproject.feature.qr.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrScreen(
    navController: NavController,
    viewModel: ScanQrViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState.validationResult != null) {
            val patientId = uiState.validationResult!!.patientId
            navController.navigate("nurse/patientDashboard/$patientId") { popUpTo("scan_qr") { inclusive = true } }
        }
        if (uiState.error != null) {
            snackbarHostState.showSnackbar(
                message = uiState.error!!,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Escanear QR do Paciente") }) }
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(it)) {
            if (hasCameraPermission) {
                CameraPreview(
                    onQrCodeScanned = { qrValue ->
                        if (uiState.isLoading) return@CameraPreview
                        viewModel.onQrCodeScanned(qrValue)
                    }
                )
                ScannerOverlay()
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("É necessária a permissão da câmera para escanear.", textAlign = TextAlign.Center)
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun CameraPreview(onQrCodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = BarcodeScanning.getClient(options)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        imageProxy.image?.let { image ->
                            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.firstOrNull()?.rawValue?.let(onQrCodeScanned)
                                }
                                .addOnFailureListener { e -> Log.e("ScanQrScreen", "Erro no scanner", e) }
                                .addOnCompleteListener { imageProxy.close() }
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("ScanQrScreen", "Falha ao ligar a câmera", e)
            }

            previewView
        }
    )
}

@Composable
fun ScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) { 
        val canvasWidth = size.width
        val canvasHeight = size.height
        val boxSize = size.minDimension * 0.7f
        val boxLeft = (canvasWidth - boxSize) / 2
        val boxTop = (canvasHeight - boxSize) / 2

        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            blendMode = BlendMode.DstOut
        )

        drawRoundRect(
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(24.dp.toPx()),
            color = Color.Transparent,
            blendMode = BlendMode.Clear
        )

        drawRoundRect(
            topLeft = Offset(boxLeft, boxTop),
            size = Size(boxSize, boxSize),
            cornerRadius = CornerRadius(24.dp.toPx()),
            color = Color.White,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
