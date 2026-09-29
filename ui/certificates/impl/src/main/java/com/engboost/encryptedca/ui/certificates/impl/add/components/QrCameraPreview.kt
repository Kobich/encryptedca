// Back camera preview that reports the texts of all QR codes in each frame. Needs the camera permission.
// Frames are analysed at 1920x1080: at the default 640x480 dense codes, several per frame, don't read.
package com.engboost.encryptedca.ui.certificates.impl.add.components

import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

private val QR_ONLY = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()

@Composable
internal fun QrCameraPreview(onCodes: (List<String>) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCodes = rememberUpdatedState(onCodes)
    val scanner = remember { BarcodeScanning.getClient(QR_ONLY) }
    DisposableEffect(scanner) { onDispose { scanner.close() } }

    AndroidView(
        factory = { viewContext ->
            val executor = ContextCompat.getMainExecutor(viewContext)
            val controller = LifecycleCameraController(context).apply {
                setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
                    )
                    .build()
                setImageAnalysisAnalyzer(
                    executor,
                    MlKitAnalyzer(listOf(scanner), ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL, executor) { result ->
                        val codes = result.getValue(scanner)?.mapNotNull { it.rawValue }.orEmpty()
                        if (codes.isNotEmpty()) currentOnCodes.value(codes)
                    },
                )
                bindToLifecycle(lifecycleOwner)
            }
            PreviewView(viewContext).apply { this.controller = controller }
        },
        onRelease = { view -> view.controller?.let { (it as? LifecycleCameraController)?.unbind() } },
        modifier = modifier,
    )
}
