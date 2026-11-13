package com.example.app_finances.feature.transactions.form

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.example.app_finances.R
import com.example.app_finances.domain.SpokenText

/**
 * "mercado 45,90 nubank" + Preencher, ou o microfone: completa valor, categoria, cartão, parcelas e data.
 * A frase falada aparece no campo já escrita ("45 reais e 90" → "45,90"), para conferir.
 * [startWithVoice]: aberto pelo atalho "Lançamento por voz", já começa ouvindo.
 */
@Composable
fun QuickEntryField(onApply: (String) -> Unit, startWithVoice: Boolean) {
    var text by rememberSaveable { mutableStateOf("") }
    var voiceMissing by rememberSaveable { mutableStateOf(false) }
    val apply = { onApply(text) }
    val listen = rememberVoiceInput(
        listenOnOpen = startWithVoice,
        onText = { spoken -> text = SpokenText.toWritten(spoken); onApply(text) },
        onUnavailable = { voiceMissing = true },
    )
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(stringResource(R.string.form_quick)) },
        placeholder = { Text(stringResource(R.string.form_quick_hint)) },
        supportingText = if (voiceMissing) ({ Text(stringResource(R.string.form_voice_unavailable)) }) else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { apply() }),
        trailingIcon = {
            Row {
                IconButton(onClick = listen) { Icon(Icons.Outlined.Mic, stringResource(R.string.form_voice)) }
                IconButton(onClick = apply, enabled = text.isNotBlank()) {
                    Icon(Icons.Outlined.AutoFixHigh, stringResource(R.string.form_quick_apply))
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Abre o reconhecimento de voz do Android em português (offline quando o aparelho permitir)
 * e entrega a frase reconhecida. Quem ouve é o serviço de voz do sistema: o app não pede o microfone.
 * Com [listenOnOpen], já abre uma vez assim que aparece (não de novo ao girar a tela).
 */
@Composable
private fun rememberVoiceInput(listenOnOpen: Boolean, onText: (String) -> Unit, onUnavailable: () -> Unit): () -> Unit {
    val currentOnUnavailable by rememberUpdatedState(onUnavailable)
    val prompt = stringResource(R.string.form_voice_prompt)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.takeIf { it.isNotBlank() }?.let(onText)
    }
    val listen = remember(launcher, prompt) {
        {
            try {
                launcher.launch(speechIntent(prompt))
            } catch (_: ActivityNotFoundException) {
                currentOnUnavailable()
            }
        }
    }
    var listenedOnOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (listenOnOpen && !listenedOnOpen) {
            listenedOnOpen = true
            listen()
        }
    }
    return listen
}

private fun speechIntent(prompt: String): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
    .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
    .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
    .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
