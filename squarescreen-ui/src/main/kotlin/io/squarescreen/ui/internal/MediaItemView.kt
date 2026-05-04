package io.squarescreen.ui.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.squarescreen.core.model.MediaType
import io.squarescreen.core.model.PlaylistItem

@Composable
internal fun MediaItemView(
    item: PlaylistItem,
    modifier: Modifier = Modifier
) {
    when (item.type) {
        MediaType.IMAGE -> ImageItemView(url = item.url, modifier = modifier)
        MediaType.VIDEO -> VideoItemView(url = item.url, thumbnailUrl = item.thumbnail, modifier = modifier)
    }
}
