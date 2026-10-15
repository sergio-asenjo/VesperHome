package com.sergioasenjo.vesperhome.inputs

import android.content.ActivityNotFoundException
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sergioasenjo.vesperhome.R
import com.sergioasenjo.vesperhome.databinding.ViewLauncherContentBinding
import kotlinx.coroutines.launch

internal class HdmiInputCardsController(
    private val activity: AppCompatActivity,
    private val binding: ViewLauncherContentBinding,
    private val repository: TvInputRepository
) {
    private val cards = listOf(binding.hdmiInputOne, binding.hdmiInputTwo, binding.hdmiInputThree)
    private val cardGap = dp(activity, CARD_GAP_DP)

    init {
        cards.forEach { card ->
            card.setOnClickListener { (it.tag as? TvInput)?.let(::openInput) }
        }
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeInputs().collect(::render)
            }
        }
    }

    private fun render(inputs: List<TvInput>) {
        val connectedHdmiInputs = inputs
            .filter { it.type == TvInputType.HDMI && it.connected }
            .take(MAX_VISIBLE_INPUTS)
        binding.hdmiInputsSection.visibility = if (connectedHdmiInputs.isEmpty()) View.GONE else View.VISIBLE
        cards.forEachIndexed { index, card ->
            val input = connectedHdmiInputs.getOrNull(index)
            card.tag = input
            card.text = input?.label.orEmpty()
            card.contentDescription = input?.label
            card.visibility = if (input == null) View.GONE else View.VISIBLE
            (card.layoutParams as LinearLayout.LayoutParams).apply {
                marginEnd = if (input != null && index < connectedHdmiInputs.lastIndex) cardGap else 0
                card.layoutParams = this
            }
        }
    }

    private fun openInput(input: TvInput) {
        try {
            activity.startActivity(repository.createSwitchIntent(input))
        } catch (_: ActivityNotFoundException) {
            showOpenFailure()
        } catch (_: SecurityException) {
            showOpenFailure()
        }
    }

    private fun showOpenFailure() {
        Toast.makeText(activity, R.string.tv_input_open_failed, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val CARD_GAP_DP = 12
        const val MAX_VISIBLE_INPUTS = 3

        fun dp(activity: AppCompatActivity, value: Int): Int =
            (value * activity.resources.displayMetrics.density).toInt()
    }
}
