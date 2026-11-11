package com.example.app_finances.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/**
 * Campo de quantidade (parcelas, por exemplo): só aceita dígitos, até [max].
 * Vazio vale 0 — quem usa decide se 0 é aceito ao salvar. [hint] aparece embaixo.
 */
@Composable
fun CountInput(value: Int, label: String, max: Int, hint: String? = null, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = if (value == 0) "" else value.toString(),
        onValueChange = { text ->
            val digits = text.filter(Char::isDigit).take(max.toString().length)
            onChange((digits.toIntOrNull() ?: 0).coerceAtMost(max))
        },
        label = { Text(label) },
        supportingText = hint?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
