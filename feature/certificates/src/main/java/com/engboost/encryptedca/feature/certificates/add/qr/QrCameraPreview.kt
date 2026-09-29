package com.engboost.encryptedca.feature.certificates.add.qr

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

/** Back camera preview that reports the texts of all QR codes in each analysed frame. Needs the camera permission. */
@Composable
internal fun QrCameraPreview(onCodes: (List<String>) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCodes = rememberUpdatedState(onCodes)
    val scanner = remember { createQrScanner() }
    DisposableEffect(scanner) { onDispose { scanner.close() } }

    AndroidView(
        factory = { viewContext ->
            val executor = ContextCompat.getMainExecutor(viewContext)
            val controller = LifecycleCameraController(context).apply {
                setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                // Dense codes need pixels: several of them in one frame don't read at the default 640x480.
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
