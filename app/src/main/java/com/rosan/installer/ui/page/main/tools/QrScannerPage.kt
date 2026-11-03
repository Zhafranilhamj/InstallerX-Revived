// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.ui.page.main.tools

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.rosan.installer.R
import com.rosan.installer.data.share.LocalShareManager
import com.rosan.installer.domain.share.model.LocalShareProtocol
import com.rosan.installer.ui.navigation.LocalNavigator
import java.util.concurrent.Executors
import org.koin.compose.koinInject

/** Live QR scanner that reads an InstallerX TV endpoint and returns it to Send to TV. */
@Composable
fun QrScannerPage() {
    val context = LocalContext.current
    val manager: LocalShareManager = koinInject()
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA) }

    ToolsScaffold(title = stringResource(R.string.qr_scanner_title)) {
        if (!granted) {
            Text(stringResource(R.string.qr_scanner_permission))
            return@ToolsScaffold
        }
        val navigator = LocalNavigator.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val executor = remember { Executors.newSingleThreadExecutor() }
        DisposableEffect(Unit) { onDispose { executor.shutdown() } }
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener(
                    {
                        val provider = runCatching { future.get() }.getOrNull() ?: return@addListener
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(executor) { proxy ->
                            val payload = runCatching { decodeQr(proxy) }.getOrNull()
                            val endpoint = payload?.let(LocalShareProtocol::parseQrPayload)
                            if (endpoint != null) {
                                manager.setPendingEndpoint(endpoint)
                                Handler(Looper.getMainLooper()).post { navigator.pop() }
                            }
                            proxy.close()
                        }
                        runCatching {
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                        }
                    },
                    ContextCompat.getMainExecutor(ctx),
                )
                previewView
            },
            modifier = Modifier.fillMaxWidth().height(420.dp),
        )
        Text(stringResource(R.string.qr_scanner_hint))
    }
}

@OptIn(ExperimentalGetImage::class)
private fun decodeQr(proxy: ImageProxy): String? {
    val image = proxy.image ?: return null
    val plane = image.planes[0]
    val buffer = plane.buffer
    val width = image.width
    val height = image.height
    val luminance = ByteArray(width * height)
    for (row in 0 until height) {
        val position = row * plane.rowStride
        if (position + width > buffer.capacity()) break
        buffer.position(position)
        buffer.get(luminance, row * width, width)
    }
    val source = PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false)
    val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }
    return runCatching { reader.decode(BinaryBitmap(HybridBinarizer(source))).text }.getOrNull()
}
