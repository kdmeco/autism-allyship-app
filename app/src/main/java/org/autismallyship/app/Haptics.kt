package org.autismallyship.app

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

// Whether to buzz at all is a settings question and stays in AppSettings.hapticsAllowed(). This
// only knows how to actually make the phone buzz, once a caller has already decided it should.
fun Context.hapticPulse(durationMs: Long = 20) {
    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (!vibrator.hasVibrator()) return
    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
}
