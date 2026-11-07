package com.opankcs.costa.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.opankcs.costa.data.Prefs

class CostaHaptics(
    private val hapticFeedback: HapticFeedback,
    private val isEnabled: () -> Boolean,
) {
    private fun perform(type: HapticFeedbackType) {
        if (isEnabled()) {
            hapticFeedback.performHapticFeedback(type)
        }
    }

    fun tick() = perform(HapticFeedbackType.SegmentTick)

    fun toggle() = perform(HapticFeedbackType.ToggleOn)

    fun confirm() = perform(HapticFeedbackType.Confirm)

    fun reject() = perform(HapticFeedbackType.Reject)

    fun longPress() = perform(HapticFeedbackType.LongPress)
}

@Composable
fun rememberCostaHaptics(): CostaHaptics {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val prefs = remember(context) { Prefs(context) }

    return remember(hapticFeedback, prefs) {
        CostaHaptics(hapticFeedback) { prefs.hapticsEnabled }
    }
}
