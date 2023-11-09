package de.tomcory.heimdall.ui.apps.appDetailScreen

import androidx.compose.animation.scaleIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Floating Action Button for [AppDetailScreen] to issue a re-scan.
 * Currently not used.
 */
@Preview
@Composable
fun RescanFloatingActionButton(onClick: ()->Unit = {}) {
    ExtendedFloatingActionButton(
        text = { Text(text = "Rescan App") },
        icon = { Icon(Icons.Filled.Refresh, contentDescription = null)},
        onClick = {
            scaleIn()
            onClick()
        },
    )
}