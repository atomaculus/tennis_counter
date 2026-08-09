package com.playce.shared.training

/**
 * A completed training session outside of a scored match (paredón or 21).
 *
 * The paredón series are intentionally not modeled here yet — they get added
 * once that mode exists; for now a paredón session only carries duration,
 * best streak and count fields it can meaningfully fill.
 */
data class TrainingSession(
    val id: String,               // UUID string
    val modality: String,         // MODALITY_PAREDON | MODALITY_VEINTIUNO
    val createdAt: Long,          // epoch millis
    val durationSeconds: Long,
    val targetPoints: Int,        // 21 para veintiuno (campo pedido por producto desde el día uno); 0 para paredón
    val finalCount: Int,          // veintiuno: conteo alcanzado en el mejor intento
    val bestStreak: Int,          // mejor racha de la sesión
    val attempts: Int             // veintiuno: cantidad de intentos (misses + 1)
) {
    companion object {
        const val MODALITY_PAREDON = "paredon"
        const val MODALITY_VEINTIUNO = "veintiuno"
    }
}
