package com.example.data

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import com.example.model.AudioEffectsState
import com.example.model.EqualizerBandUi
import com.example.model.EqualizerPreset

class AudioEffectsManager {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var attachedSessionId: Int = -1

    private var currentState: AudioEffectsState = AudioEffectsState()

    fun getState(): AudioEffectsState = currentState

    fun attachToSession(audioSessionId: Int): AudioEffectsState {
        if (audioSessionId <= 0) return currentState
        if (attachedSessionId == audioSessionId && equalizer != null) {
            applyCurrentToHardware()
            return currentState
        }

        release()
        attachedSessionId = audioSessionId

        val eq = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
        val bass = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
        equalizer = eq
        bassBoost = bass

        if (eq != null) {
            runCatching {
                val range = eq.bandLevelRange
                val minMb = range.getOrNull(0)?.toInt()?.coerceIn(-1500, -600) ?: -1200
                val maxMb = range.getOrNull(1)?.toInt()?.coerceIn(600, 1500) ?: 1200
                val count = eq.numberOfBands.toInt().coerceIn(1, 5)
                val existingLevels = currentState.bands.map { it.levelMb }
                val bands = (0 until count).map { idx ->
                    val centerMilliHz = runCatching { eq.getCenterFreq(idx.toShort()) }.getOrDefault(0)
                    val label = formatFrequencyLabel(centerMilliHz, idx)
                    val level = existingLevels.getOrElse(idx) { 0 }.coerceIn(minMb, maxMb)
                    EqualizerBandUi(index = idx, label = label, levelMb = level)
                }
                currentState = currentState.copy(
                    bands = bands,
                    minLevelMb = minMb,
                    maxLevelMb = maxMb,
                    hardwareAttached = true
                )
            }
        } else {
            currentState = currentState.copy(hardwareAttached = false)
        }

        applyCurrentToHardware()
        return currentState
    }

    fun applyConfig(
        enabled: Boolean,
        preset: EqualizerPreset,
        bandLevelsMb: List<Int>,
        bassBoostStrength: Int
    ): AudioEffectsState {
        val minMb = currentState.minLevelMb
        val maxMb = currentState.maxLevelMb
        val updatedBands = currentState.bands.mapIndexed { idx, band ->
            val target = bandLevelsMb.getOrElse(idx) { band.levelMb }.coerceIn(minMb, maxMb)
            band.copy(levelMb = target)
        }
        currentState = currentState.copy(
            enabled = enabled,
            preset = preset,
            bands = updatedBands,
            bassBoostStrength = bassBoostStrength.coerceIn(0, 1000)
        )
        applyCurrentToHardware()
        return currentState
    }

    fun setEnabled(enabled: Boolean): AudioEffectsState {
        currentState = currentState.copy(enabled = enabled)
        applyCurrentToHardware()
        return currentState
    }

    fun selectPreset(preset: EqualizerPreset): AudioEffectsState {
        val minMb = currentState.minLevelMb
        val maxMb = currentState.maxLevelMb
        val updatedBands = currentState.bands.mapIndexed { idx, band ->
            val target = preset.bandLevelsMb.getOrElse(idx) { 0 }.coerceIn(minMb, maxMb)
            band.copy(levelMb = target)
        }
        currentState = currentState.copy(
            enabled = true,
            preset = preset,
            bands = updatedBands,
            bassBoostStrength = preset.defaultBass.coerceIn(0, 1000)
        )
        applyCurrentToHardware()
        return currentState
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int): AudioEffectsState {
        val clamped = levelMb.coerceIn(currentState.minLevelMb, currentState.maxLevelMb)
        val updatedBands = currentState.bands.map { band ->
            if (band.index == bandIndex) band.copy(levelMb = clamped) else band
        }
        currentState = currentState.copy(
            enabled = true,
            preset = EqualizerPreset.CUSTOM,
            bands = updatedBands
        )
        applyCurrentToHardware()
        return currentState
    }

    fun setBassBoost(strength: Int): AudioEffectsState {
        val clamped = strength.coerceIn(0, 1000)
        currentState = currentState.copy(
            enabled = true,
            bassBoostStrength = clamped
        )
        applyCurrentToHardware()
        return currentState
    }

    private fun applyCurrentToHardware() {
        val eq = equalizer
        if (eq != null) {
            runCatching {
                eq.enabled = currentState.enabled
                val hwCount = eq.numberOfBands.toInt()
                currentState.bands.forEach { band ->
                    if (band.index in 0 until hwCount) {
                        val level = band.levelMb.coerceIn(currentState.minLevelMb, currentState.maxLevelMb).toShort()
                        eq.setBandLevel(band.index.toShort(), level)
                    }
                }
            }
        }
        val bass = bassBoost
        if (bass != null) {
            runCatching {
                val active = currentState.enabled && currentState.bassBoostStrength > 0
                bass.enabled = active
                if (bass.strengthSupported) {
                    bass.setStrength(currentState.bassBoostStrength.coerceIn(0, 1000).toShort())
                }
            }
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        equalizer = null
        bassBoost = null
        attachedSessionId = -1
        currentState = currentState.copy(hardwareAttached = false)
    }

    private fun formatFrequencyLabel(centerMilliHz: Int, index: Int): String {
        val hz = centerMilliHz / 1000
        return when {
            hz <= 0 -> listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz").getOrElse(index) { "${index + 1}" }
            hz >= 1000 -> {
                val khz = hz / 1000f
                if (khz >= 10f) "${khz.toInt()} kHz" else String.format("%.1f kHz", khz)
            }
            else -> "$hz Hz"
        }
    }
}
