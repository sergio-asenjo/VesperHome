package com.sergioasenjo.vesperhome.launcher

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import com.sergioasenjo.vesperhome.R
import com.sergioasenjo.vesperhome.applications.ApplicationSortMode
import com.sergioasenjo.vesperhome.databinding.ActivityLauncherBinding
import com.sergioasenjo.vesperhome.databinding.ViewLauncherContentBinding
import com.sergioasenjo.vesperhome.settings.LauncherAppearance

internal fun renderLauncherAppearance(
    binding: ActivityLauncherBinding,
    contentBinding: ViewLauncherContentBinding,
    appearance: LauncherAppearance
) {
    val palette = appearance.palette
    binding.title.setTextColor(palette.secondaryText)
    val buttonBackground = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()),
        intArrayOf(palette.focusedSurface, Color.TRANSPARENT)
    )
    val buttonText = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()),
        intArrayOf(palette.focusedText, palette.primaryText)
    )
    listOf(
        binding.openLauncherSettings
    ).forEach { button ->
        button.backgroundTintList = buttonBackground
        button.setTextColor(buttonText)
        button.iconTint = buttonText
    }
    binding.root.setSoundEffectsEnabledRecursively(appearance.keyClickSounds)
}

private fun View.setSoundEffectsEnabledRecursively(enabled: Boolean) {
    isSoundEffectsEnabled = enabled
    if (this is ViewGroup) {
        for (index in 0 until childCount) getChildAt(index).setSoundEffectsEnabledRecursively(enabled)
    }
}

internal val ApplicationSortMode.labelRes: Int
    get() = when (this) {
        ApplicationSortMode.MANUAL -> R.string.sort_manual
        ApplicationSortMode.ALPHABETICAL -> R.string.sort_alphabetical
        ApplicationSortMode.LAST_USED -> R.string.sort_last_used
    }
