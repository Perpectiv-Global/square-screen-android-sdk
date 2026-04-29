package io.squarescreen.ui.internal

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import io.squarescreen.core.model.TransitionType

private const val TRANSITION_DURATION_MS = 600

internal fun transitionSpecFor(type: TransitionType?): ContentTransform {
    return when (type) {
        TransitionType.FADE -> fadeIn(tween(TRANSITION_DURATION_MS)) togetherWith
                fadeOut(tween(TRANSITION_DURATION_MS))
        TransitionType.SLIDE -> slideInHorizontally(tween(TRANSITION_DURATION_MS)) { it } togetherWith
                slideOutHorizontally(tween(TRANSITION_DURATION_MS)) { -it }
        TransitionType.NONE, null -> EnterTransition.None togetherWith ExitTransition.None
    }
}
