// iosMain/presentation/components/InputFieldDialogs.ios.kt
package com.mnebot.riptide.presentation.components

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@Composable
actual fun TimePickerDialogWrapper(
    initial: LocalTime?,
    onConfirm: (LocalTime?) -> Unit,
    onDismiss: () -> Unit
) {
    // TODO: implementar picker nativo iOS
    onDismiss()
}

@Composable
actual fun DatePickerDialogWrapper(
    initial: LocalDate,
    onConfirm: (LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    // TODO: implementar picker nativo iOS
    onDismiss()
}
