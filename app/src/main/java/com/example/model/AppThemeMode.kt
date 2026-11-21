package com.example.model

enum class AppThemeMode(val key: String, val label: String) {
    NEON("NEON", "Neón"),
    NEGRO("NEGRO", "Negro"),
    BLANCO("BLANCO", "Blanco");

    companion object {
        fun fromKey(raw: String?): AppThemeMode =
            entries.firstOrNull { it.key == raw } ?: NEGRO
    }
}
