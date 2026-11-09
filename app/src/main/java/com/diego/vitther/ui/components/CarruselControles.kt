package com.diego.vitther.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.diego.vitther.R

@Composable
fun FlechaCarrusel(anterior: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val descripcion = stringResource(if (anterior) R.string.home_anterior else R.string.home_siguiente)
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(38.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (enabled) 1f else .35f), CircleShape)
            .semantics { contentDescription = descripcion },
        enabled = enabled
    ) {
        val color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        Canvas(Modifier.size(20.dp)) {
            val direccion = if (anterior) -1 else 1
            val x = center.x + direccion * size.width * .15f
            val extremo = center.x - direccion * size.width * .1f
            drawLine(color, Offset(extremo, size.height * .25f), Offset(x, center.y), 1.6.dp.toPx(), StrokeCap.Round)
            drawLine(color, Offset(x, center.y), Offset(extremo, size.height * .75f), 1.6.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
fun IndicadoresCarrusel(total: Int, actual: Int, modifier: Modifier = Modifier) {
    val descripcion = stringResource(R.string.home_pagina, actual + 1, total)
    LazyRow(
        modifier = modifier.semantics { contentDescription = descripcion },
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
    ) {
        items(total) { indice ->
            val activo = indice == actual
            Box(
                Modifier
                    .size(width = if (activo) 18.dp else 6.dp, height = 6.dp)
                    .background(
                        if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}
