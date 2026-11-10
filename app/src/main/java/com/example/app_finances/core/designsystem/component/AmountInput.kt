package com.example.app_finances.core.designsystem.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.example.app_finances.core.format.Money

/**
 * Campo de valor em reais no estilo "caixa eletrônico": o usuário só digita
 * números e o valor é sempre exibido formatado (R$ 0,00 → R$ 0,01 → R$ 0,12…).
 */
@Composable
fun AmountInput(
    cents: Long,
    onCentsChange: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val text = Money.format(cents)
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        // Cursor sempre no fim: a digitação acontece da direita para a esquerda.
        value = TextFieldValue(text, selection = TextRange(text.length)),
        onValueChange = { onCentsChange(Money.parseTypedDigits(it.text)) },
        label = { Text(label) },
        textStyle = MaterialTheme.typography.headlineSmall,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        singleLine = true,
        modifier = modifier,
    )
}
