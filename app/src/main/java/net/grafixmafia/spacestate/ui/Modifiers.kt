package net.grafixmafia.spacestate.ui

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/** Makes a list row act as one button for touch and accessibility. */
fun Modifier.clickableListItem(onClick: () -> Unit): Modifier = clickable(role = Role.Button, onClick = onClick)
