package com.example.app_finances.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.app_finances.R

/**
 * Corpo rolável padrão de uma aba: respeita o espaço da barra inferior,
 * margens laterais de 16 dp e título grande no topo.
 */
@Composable
fun ScreenColumn(
    title: String,
    scaffoldPadding: PaddingValues,
    onBack: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    // Em cima, o conteúdo para abaixo da barra de status (não rola por baixo do relógio).
    // Embaixo, o espaço da barra inferior e do "+" é margem do conteúdo: a lista rola até
    // a barra em vez de ser cortada antes dela.
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = scaffoldPadding.calculateTopPadding()),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = scaffoldPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenTitle(title, onBack) }
        content()
    }
}

@Composable
private fun ScreenTitle(title: String, onBack: (() -> Unit)?) {
    Row(
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.padding(end = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
            }
        }
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
    }
}
