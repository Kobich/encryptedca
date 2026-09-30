package com.engboost.encryptedca.ui.scanner.impl.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.engboost.encryptedca.ui.scanner.impl.R
import com.engboost.encryptedca.ui.scanner.impl.ui.entity.DeviceViewState

private val ConnectableBlue = Color(0xFF1E88E5)
private val CardCorner = 12.dp

@Composable
internal fun DeviceRow(device: DeviceViewState, first: Boolean, last: Boolean, onOpen: () -> Unit) {
    Card(
        shape = RoundedCornerShape(
            topStart = if (first) CardCorner else 0.dp,
            topEnd = if (first) CardCorner else 0.dp,
            bottomStart = if (last) CardCorner else 0.dp,
            bottomEnd = if (last) CardCorner else 0.dp,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (!first) HorizontalDivider()
        Text(
            text = stringResource(R.string.device_ip, device.ip),
            style = MaterialTheme.typography.titleMedium,
            color = if (device.connectable) ConnectableBlue else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (device.connectable) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = device.connectable, onClick = onOpen)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}
