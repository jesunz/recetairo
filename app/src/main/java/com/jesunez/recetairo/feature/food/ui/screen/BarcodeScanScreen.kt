package com.jesunez.recetairo.feature.food.ui.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.jesunez.recetairo.feature.food.domain.model.ProductInfo
import com.jesunez.recetairo.feature.food.ui.BarcodeScanError
import com.jesunez.recetairo.feature.food.ui.BarcodeScanUiState
import com.jesunez.recetairo.feature.food.ui.viewmodel.BarcodeScanViewModel
import timber.log.Timber

@Composable
fun BarcodeScanScreen(
    onNavigateToAddFood: (ProductInfo?) -> Unit,
    viewModel: BarcodeScanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onCameraPermissionResult(granted)
    }

    // Check and request camera permission on first composition (R12)
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onCameraPermissionResult(granted)
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Handle navigation side effects: product found → pre-filled form (R15);
    // navigateToEmptyForm after error dismissed → empty form (R16, R17, R18)
    LaunchedEffect(state.productToPreFill, state.navigateToEmptyForm, state.error) {
        when {
            state.productToPreFill != null -> {
                onNavigateToAddFood(state.productToPreFill)
                viewModel.onNavigationHandled()
            }
            state.navigateToEmptyForm && state.error == null -> {
                onNavigateToAddFood(null)
                viewModel.onNavigationHandled()
            }
        }
    }

    BarcodeScanContent(
        state = state,
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onOpenSystemSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            )
        },
        onBarcodeDetected = viewModel::onBarcodeDetected,
        onErrorDismissed = viewModel::onErrorDismissed
    )
}

@Composable
fun BarcodeScanContent(
    state: BarcodeScanUiState,
    onRequestPermission: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onBarcodeDetected: (String, Int) -> Unit,
    onErrorDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (!state.isCameraPermissionGranted) {
            // R13: Show permission rationale and options when permission denied
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Se necesita permiso de cámara para escanear códigos de barras.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Conceder permiso de cámara" }
                ) {
                    Text("Conceder permiso")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenSystemSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Ir a los ajustes del sistema" }
                ) {
                    Text("Ir a ajustes")
                }
            }
        } else {
            // R14: Camera preview with ML Kit barcode analysis
            CameraPreviewWithBarcodeAnalysis(
                onBarcodeDetected = onBarcodeDetected,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading indicator while querying product service (R14)
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "Buscando producto" },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Error banner: R16 (product not found), R17 (no internet), R18 (timeout), R13 (permission)
        if (state.error != null) {
            BarcodeScanErrorBanner(
                error = state.error,
                onDismiss = onErrorDismissed,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun BarcodeScanErrorBanner(
    error: BarcodeScanError,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = when (error) {
        BarcodeScanError.CameraPermissionDenied ->
            "Permiso de cámara denegado. Concede el permiso para usar el escáner."
        BarcodeScanError.ProductNotFound ->
            "Producto no encontrado. Se abrirá el formulario vacío para entrada manual."
        BarcodeScanError.NoInternet ->
            "Sin conexión a internet. Se abrirá el formulario vacío para entrada manual."
        BarcodeScanError.ServiceTimeout ->
            "El servicio de productos no está disponible. Introduce los datos manualmente."
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Aviso: $message" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics { contentDescription = "Cerrar aviso" }
            ) {
                Text("Cerrar", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewWithBarcodeAnalysis(
    onBarcodeDetected: (rawValue: String, format: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)

    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_UPC_A
                )
                .build()
        )
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                barcodeScanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        barcodes.firstOrNull()?.let { barcode ->
                                            val rawValue = barcode.rawValue ?: return@let
                                            currentOnBarcodeDetected(rawValue, barcode.format)
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
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
                    Timber.e(e, "Error vinculando la cámara")
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
}
