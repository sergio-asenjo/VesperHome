package com.sergioasenjo.vesperhome.wallpaper

import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.sergioasenjo.vesperhome.settings.LauncherPalette

class WallpaperRenderer(private val root: FrameLayout, private val image: ImageView) {
    private var renderedState: WallpaperState? = null
    private var renderedPalette: LauncherPalette? = null

    fun render(state: WallpaperState, palette: LauncherPalette) {
        if (renderedState == state && renderedPalette == palette) return
        renderedState = state
        renderedPalette = palette
        image.visibility = View.GONE
        image.setImageDrawable(null)
        root.setBackgroundColor(Color.BLACK)
    }
}
