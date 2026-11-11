package com.dreamteam.safebus.shared.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.*
import com.dreamteam.safebus.R
import com.dreamteam.safebus.shared.data.PhotoLoader
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.*

@Composable fun PhotoChooser(photo: Photo?, onChange: (Photo?) -> Unit, enabled: Boolean = true) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var invalid by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            loading = true; invalid = false; onChange(null)
            when (val result = PhotoLoader.load(context.contentResolver, uri)) {
                is DomainResult.Success -> onChange(result.value)
                is DomainResult.Error -> invalid = true
            }
            loading = false
        }
    }
    CoreButton(R.string.core_photo_choose, "choosePhoto", enabled && !loading, secondary = true, icon = R.drawable.ic_photo_camera) {
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    Text(stringResource(R.string.core_photo_hint))
    if (photo != null) {
        PrivatePhoto(photo)
        CoreNotice(stringResource(R.string.core_photo_selected), R.drawable.ic_check_circle, Modifier.testTag("photoSelected"))
    }
    if (invalid) Text(stringResource(R.string.core_photo_invalid), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("photoError"))
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
}

@Composable fun PrivatePhoto(photo: Photo) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, photo) {
        value = withContext(Dispatchers.Default) {
            val bytes = photo.bytes
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = 4 })
        }
    }
    bitmap?.let { image -> Image(image.asImageBitmap(), stringResource(R.string.core_photo_description), Modifier.fillMaxWidth().height(dimensionResource(R.dimen.core_photo_height))) }
}

@Composable fun QrInput(value: String, onChange: (String) -> Unit, enabled: Boolean = true) {
    var scannerMissing by remember { mutableStateOf(false) }
    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.getStringExtra("SCAN_RESULT")?.let(onChange)
    }
    CoreField(R.string.core_qr_content, value, onChange, "qrContent", enabled = enabled)
    Text(stringResource(R.string.core_qr_hint))
    CoreButton(R.string.core_qr_scan, "openScanner", enabled, secondary = true, icon = R.drawable.ic_qr_code_scanner) {
        try { scanner.launch(Intent("com.google.zxing.client.android.SCAN").putExtra("SCAN_MODE", "QR_CODE_MODE")) }
        catch (_: ActivityNotFoundException) { scannerMissing = true }
    }
    if (scannerMissing) Text(stringResource(R.string.core_qr_no_scanner))
}
