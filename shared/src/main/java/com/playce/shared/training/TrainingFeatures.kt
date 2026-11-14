package com.playce.shared.training

/**
 * Gates for training modes that are not ready to ship.
 *
 * Paredón screens and strings stay in the project so the mode can be finished
 * later. While [PAREDON_ENABLED] is false the phone and Wear selectors must
 * not offer it — Play reviewers reject placeholder features.
 */
object TrainingFeatures {
    const val PAREDON_ENABLED = false
}
