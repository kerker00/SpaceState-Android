package net.grafixmafia.spacestate.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.grafixmafia.spacestate.R

/** Common card surface; content grows with font size instead of fixing row heights. */
@Composable
fun SpaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isSelected: Boolean? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = BorderStroke(
        if (isSelected == true) 2.dp else 1.dp,
        if (isSelected == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
    )
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
    if (onClick == null) {
        Surface(
            modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface, border = border, content = body,
        )
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                role = Role.Button
                isSelected?.let { selected = it }
            },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface, border = border, content = body,
        )
    }
}

@Composable
fun SpaceHeading(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

/** Retained failures remain visible with the existing refresh action. */
@Composable
fun SpaceErrorCard(message: String, onRetry: () -> Unit, enabled: Boolean = true) {
    SpaceCard {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(message, color = MaterialTheme.colorScheme.onSurface)
        TextButton(onClick = onRetry, enabled = enabled) { Text(stringResource(R.string.try_again)) }
    }
}
