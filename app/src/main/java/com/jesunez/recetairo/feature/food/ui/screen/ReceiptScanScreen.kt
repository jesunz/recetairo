package com.jesunez.recetairo.feature.food.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jesunez.recetairo.core.ui.component.CategoryDropdown
import com.jesunez.recetairo.core.ui.component.DateField
import com.jesunez.recetairo.core.ui.component.NumericQuantityField
import com.jesunez.recetairo.core.ui.component.UnitDropdown
import com.jesunez.recetairo.feature.food.domain.model.FoodCategory
import com.jesunez.recetairo.feature.food.domain.model.InsertionSummary
import com.jesunez.recetairo.feature.food.domain.model.OcrFoodItem
import com.jesunez.recetairo.feature.food.ui.OcrError
import com.jesunez.recetairo.feature.food.ui.ReceiptScanUiState
import com.jesunez.recetairo.feature.food.ui.viewmodel.ReceiptScanViewModel
import com.jesunez.recetairo.ui.component.emoji
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@Composable
fun ReceiptScanScreen(
    onNavigateToAddFood: () -> Unit,
    onFinish: () -> Unit,
    viewModel: ReceiptScanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onCameraPermissionResult(granted)
    }

    // Check and request camera permission on first composition (R19)
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onCameraPermissionResult(granted)
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    ReceiptScanContent(
        state = state,
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onOpenSystemSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            )
        },
        onImageCaptured = viewModel::onImageCaptured,
        onItemEdited = viewModel::onItemEdited,
        onItemRemoved = viewModel::onItemRemoved,
        onItemCategoryChanged = viewModel::onItemCategoryChanged,
        onConfirmSelection = viewModel::onConfirmSelection,
        onEnterManually = onNavigateToAddFood,
        onFinish = onFinish
    )
}

@Composable
fun ReceiptScanContent(
    state: ReceiptScanUiState,
    onRequestPermission: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onImageCaptured: (ByteArray) -> Unit,
    onItemEdited: (Int, OcrFoodItem) -> Unit,
    onItemRemoved: (Int) -> Unit,
    onItemCategoryChanged: (Int, FoodCategory) -> Unit,
    onConfirmSelection: () -> Unit,
    onEnterManually: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            !state.isCameraPermissionGranted ->
                PermissionRationaleView(onRequestPermission, onOpenSystemSettings)
            state.insertionSummary != null ->
                InsertionSummaryView(state.insertionSummary, onFinish)
            state.items.isNotEmpty() ->
                OcrItemListView(
                    items = state.items,
                    isLoading = state.isLoading,
                    onItemEdited = onItemEdited,
                    onItemRemoved = onItemRemoved,
                    onItemCategoryChanged = onItemCategoryChanged,
                    onConfirmSelection = onConfirmSelection
                )
            else ->
                CameraCaptureView(onImageCaptured, state.isLoading, state.isClassifying)
        }

        // Error banner: R22 (no items extracted), R25 (30s timeout). Dismissible so the
        // user can see the camera again to retry; re-armed on every new capture attempt.
        var isBannerDismissed by remember { mutableStateOf(false) }
        LaunchedEffect(state.isLoading) {
            if (state.isLoading) isBannerDismissed = false
        }
        val error = state.error
        if (error != null && !isBannerDismissed && state.insertionSummary == null) {
            ReceiptScanErrorBanner(
                error = error,
                onRetry = { isBannerDismissed = true },
                onEnterManually = onEnterManually,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun PermissionRationaleView(
    onRequestPermission: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Se necesita permiso de cámara para escanear el ticket de compra.",
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
}

@Composable
private fun CameraCaptureView(
    onImageCaptured: (ByteArray) -> Unit,
    isLoading: Boolean,
    isClassifying: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        CameraPreviewWithCapture(
            onImageCaptured = onImageCaptured,
            enabled = !isLoading && !isClassifying,
            modifier = Modifier.fillMaxSize()
        )
        // R13: distinct indicator per phase — OCR (isLoading) reads the ticket text,
        // isClassifying calls the AI extractor afterward; only one is true at a time.
        if (isLoading || isClassifying) {
            val label = if (isClassifying) "Clasificando alimentos con IA" else "Leyendo el ticket"
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = label },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = if (isClassifying) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CameraPreviewWithCapture(
    onImageCaptured: (ByteArray) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(previewView) {
        try {
            val cameraProvider = context.getCameraProvider()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Timber.e(e, "Error vinculando la cámara para captura de ticket")
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Button(
            onClick = {
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            image.close()
                            onImageCaptured(bytes)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Timber.e(exception, "Error capturando la foto del ticket")
                        }
                    }
                )
            },
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .semantics { contentDescription = "Capturar foto del ticket" }
        ) {
            Text("Capturar")
        }
    }
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    withContext(Dispatchers.IO) {
        ProcessCameraProvider.getInstance(this@getCameraProvider).get()
    }

private fun isQuantityValid(quantity: String): Boolean =
    quantity.replace(',', '.').toDoubleOrNull() != null

@Composable
private fun OcrItemListView(
    items: List<OcrFoodItem>,
    isLoading: Boolean,
    onItemEdited: (Int, OcrFoodItem) -> Unit,
    onItemRemoved: (Int) -> Unit,
    onItemCategoryChanged: (Int, FoodCategory) -> Unit,
    onConfirmSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Productos detectados en el ticket",
                style = MaterialTheme.typography.titleMedium
            )
        }
        itemsIndexed(items) { index, item ->
            OcrItemRow(
                item = item,
                onItemChanged = { updated -> onItemEdited(index, updated) },
                onRemove = { onItemRemoved(index) },
                onCategoryChanged = { category -> onItemCategoryChanged(index, category) }
            )
        }
        item {
            Button(
                onClick = onConfirmSelection,
                enabled = !isLoading &&
                    items.any { it.isSelected } &&
                    items.none { it.isSelected && !isQuantityValid(it.quantity) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .semantics { contentDescription = "Confirmar selección de productos" }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("Confirmar selección")
                }
            }
        }
    }
}

@Composable
private fun OcrItemRow(
    item: OcrFoodItem,
    onItemChanged: (OcrFoodItem) -> Unit,
    onRemove: () -> Unit,
    onCategoryChanged: (FoodCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    // R4, R5: needsReview items get a warning border/background + "Revisar" label so the
    // user notices them, but stay fully editable/selectable like any other item.
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (item.needsReview) contentDescription = "Producto a revisar: ${item.name}"
            },
        colors = if (item.needsReview) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        },
        border = if (item.needsReview) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        } else {
            null
        }
    ) {
        Column(Modifier.padding(12.dp)) {
            if (item.needsReview) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Revisar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { checked -> onItemChanged(item.copy(isSelected = checked)) },
                    modifier = Modifier.semantics { contentDescription = "Seleccionar ${item.name}" }
                )
                Text(
                    text = item.emoji ?: item.category.emoji(),
                    fontSize = 20.sp,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .semantics { contentDescription = "Emoji del producto" }
                )
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onItemChanged(item.copy(name = it)) },
                    label = { Text("Nombre") },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Nombre del producto" }
                )
            }
            Row(modifier = Modifier.padding(top = 8.dp)) {
                val quantityInvalid = item.isSelected && !isQuantityValid(item.quantity)
                NumericQuantityField(
                    value = item.quantity,
                    onValueChange = { onItemChanged(item.copy(quantity = it)) },
                    modifier = Modifier.weight(1f),
                    isError = quantityInvalid,
                    supportingText = if (quantityInvalid) "Obligatorio" else null,
                    fieldContentDescription = "Cantidad del producto"
                )
                Spacer(Modifier.width(8.dp))
                DateField(
                    value = item.expiryDate,
                    onValueChange = { onItemChanged(item.copy(expiryDate = it)) },
                    label = "Caducidad",
                    modifier = Modifier.weight(1f),
                    fieldContentDescription = "Fecha de caducidad del producto"
                )
            }
            CategoryDropdown(
                selected = item.category,
                onCategoryChanged = onCategoryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            UnitDropdown(
                selected = item.unit,
                onUnitChanged = { unit -> onItemChanged(item.copy(unit = unit)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            TextButton(
                onClick = onRemove,
                modifier = Modifier.semantics { contentDescription = "Eliminar ${item.name} de la lista" }
            ) {
                Text("Eliminar")
            }
        }
    }
}

@Composable
private fun InsertionSummaryView(
    summary: InsertionSummary,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${summary.successCount} alimentos añadidos correctamente a la despensa.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        if (summary.failures.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No se pudieron añadir:",
                style = MaterialTheme.typography.titleSmall
            )
            summary.failures.forEach { failure ->
                Text(
                    text = "${failure.food.name}: ${failure.reason}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Finalizar" }
        ) {
            Text("Finalizar")
        }
    }
}

@Composable
private fun ReceiptScanErrorBanner(
    error: OcrError,
    onRetry: () -> Unit,
    onEnterManually: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = when (error) {
        OcrError.Timeout ->
            "El procesamiento del ticket ha superado el tiempo de espera."
        OcrError.NoItemsExtracted ->
            "No se ha podido identificar ningún producto en el ticket."
        OcrError.AiServiceUnavailable ->
            "No se ha podido contactar con el servicio de clasificación. Los resultados pueden ser menos precisos."
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Aviso: $message" }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.padding(top = 8.dp)) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.semantics { contentDescription = "Reintentar captura del ticket" }
                ) {
                    Text("Reintentar")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = onEnterManually,
                    modifier = Modifier.semantics { contentDescription = "Introducir alimentos manualmente" }
                ) {
                    Text("Introducir manualmente")
                }
            }
        }
    }
}
