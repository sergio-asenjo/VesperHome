package com.sergioasenjo.vesperhome.profiles

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.sergioasenjo.vesperhome.R
import com.sergioasenjo.vesperhome.databinding.DialogProfilesBinding
import com.sergioasenjo.vesperhome.settings.LauncherAppearance

internal class ProfilePanel(
    context: Context,
    onCreate: () -> Unit,
    onSelected: (LayoutProfile) -> Unit,
    private val onDismissed: () -> Unit
) {
    private val binding = DialogProfilesBinding.inflate(android.view.LayoutInflater.from(context))
    private val adapter = ProfileAdapter(onSelected)
    private val dialog = Dialog(context, R.style.Theme_VesperHome_SettingsPanel).apply {
        setContentView(binding.root)
        setCanceledOnTouchOutside(true)
        window?.apply {
            setLayout(dp(context, PANEL_WIDTH_DP), ViewGroup.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.START)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { dimAmount = BACKGROUND_DIM_AMOUNT }
        }
        setOnShowListener { binding.createProfile.post { binding.createProfile.requestFocus() } }
        setOnDismissListener { onDismissed() }
    }

    init {
        binding.profiles.layoutManager = LinearLayoutManager(context)
        binding.profiles.adapter = adapter
        binding.profiles.itemAnimator = null
        binding.createProfile.setOnClickListener { onCreate() }
    }

    val isShowing: Boolean
        get() = dialog.isShowing

    fun show(state: ProfileState, busy: Boolean, appearance: LauncherAppearance) {
        render(state, busy, appearance)
        if (!dialog.isShowing) dialog.show()
    }

    fun render(state: ProfileState, busy: Boolean, appearance: LauncherAppearance) {
        adapter.render(state.activeProfileId, appearance)
        adapter.submitList(state.profiles)
        binding.progress.visibility = if (busy || state.loading) View.VISIBLE else View.GONE
        binding.profiles.visibility = if (busy || state.loading) View.INVISIBLE else View.VISIBLE
        binding.createProfile.isEnabled = !busy
        applyAppearance(appearance)
    }

    fun release() {
        dialog.setOnDismissListener(null)
        dialog.dismiss()
    }

    private fun applyAppearance(appearance: LauncherAppearance) {
        val palette = appearance.palette
        binding.root.setBackgroundColor(palette.panel)
        binding.title.setTextColor(palette.primaryText)
        binding.description.setTextColor(palette.secondaryText)
        renderButton(binding.createProfile, appearance)
    }

    private fun renderButton(button: MaterialButton, appearance: LauncherAppearance) {
        val palette = appearance.palette
        val states = arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf())
        button.backgroundTintList = ColorStateList(states, intArrayOf(palette.focusedSurface, palette.surface))
        button.setTextColor(ColorStateList(states, intArrayOf(palette.focusedText, palette.primaryText)))
        button.iconTint = ColorStateList(states, intArrayOf(palette.focusedText, palette.primaryText))
    }

    private companion object {
        const val PANEL_WIDTH_DP = 470
        const val BACKGROUND_DIM_AMOUNT = 0.62f

        fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
    }
}
