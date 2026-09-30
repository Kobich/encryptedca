package com.engboost.encryptedca.ui.webpanel.impl.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.ui.webpanel.impl.R
import com.engboost.encryptedca.ui.webpanel.impl.domain.entity.WebPanelProblem
import com.engboost.encryptedca.ui.webpanel.impl.ui.entity.ImageMessage

@Composable
internal fun ProblemText(problem: WebPanelProblem) {
    Text(
        text = stringResource(
            when (problem) {
                WebPanelProblem.NO_PROFILE -> R.string.web_panel_no_profile
                WebPanelProblem.PROFILE_UNAVAILABLE -> R.string.web_panel_profile_unavailable
                WebPanelProblem.UNTRUSTED_SERVER -> R.string.web_panel_untrusted_server
            },
        ),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(16.dp),
    )
}

@Composable
internal fun ImageMessageSnackbar(
    message: ImageMessage?,
    snackbar: SnackbarHostState,
    onShown: (ImageMessage) -> Unit,
) {
    val context = LocalContext.current
    val savedText = stringResource(R.string.web_panel_image_saved)
    val openLabel = stringResource(R.string.web_panel_image_open)
    val failedText = stringResource(R.string.web_panel_image_failed)
    LaunchedEffect(message) {
        when (message) {
            null -> return@LaunchedEffect
            is ImageMessage.Saved -> {
                val result = snackbar.showSnackbar(savedText, openLabel, duration = SnackbarDuration.Long)
                if (result == SnackbarResult.ActionPerformed) {
                    openExternally(context, Uri.parse(message.uri), type = "image/*")
                }
            }
            is ImageMessage.Failed -> snackbar.showSnackbar(failedText)
        }
        onShown(message)
    }
}
