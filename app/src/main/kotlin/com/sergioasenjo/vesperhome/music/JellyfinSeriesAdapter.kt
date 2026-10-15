package com.sergioasenjo.vesperhome.music

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import com.sergioasenjo.vesperhome.R
import com.sergioasenjo.vesperhome.databinding.ItemJellyfinSeriesBinding
import com.sergioasenjo.vesperhome.launcher.handleContainedHorizontalFocus
import com.sergioasenjo.vesperhome.settings.LauncherAppearance

internal class JellyfinSeriesAdapter(
    context: Context,
    private val recyclerView: RecyclerView,
    private val columns: Int,
    private val onSeriesClick: (JellyfinSeries) -> Unit
) : ListAdapter<JellyfinSeries, JellyfinSeriesAdapter.SeriesViewHolder>(DiffCallback) {
    private var appearance = LauncherAppearance()
    private var cardWidth = 180
    private val density = context.resources.displayMetrics.density
    private val titleAreaHeight = (TITLE_AREA_HEIGHT_DP * density).toInt()
    private val maxPosterHeight = (MAX_POSTER_HEIGHT_DP * density).toInt()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeriesViewHolder = SeriesViewHolder(
        ItemJellyfinSeriesBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        onSeriesClick
    )

    override fun onBindViewHolder(holder: SeriesViewHolder, position: Int) {
        val series = getItem(position)
        holder.itemView.layoutParams = holder.itemView.layoutParams.apply {
            height = cardHeight()
        }
        holder.bind(series, appearance, recyclerView, columns)
    }

    fun setAppearance(appearance: LauncherAppearance) {
        if (this.appearance == appearance) return
        this.appearance = appearance
        notifyItemRangeChanged(0, itemCount)
    }

    fun setCardWidth(width: Int) {
        if (cardWidth == width) return
        cardWidth = width
        notifyItemRangeChanged(0, itemCount)
    }

    fun cardHeight(): Int = minOf(
        (cardWidth / POSTER_ASPECT_RATIO).toInt(),
        maxPosterHeight
    ) + titleAreaHeight

    class SeriesViewHolder(private val binding: ItemJellyfinSeriesBinding, onSeriesClick: (JellyfinSeries) -> Unit) :
        RecyclerView.ViewHolder(binding.root) {
        private var appearance = LauncherAppearance()
        private val posterOutline = GradientDrawable()

        init {
            binding.artworkFrame.background = posterOutline
            binding.artworkFrame.clipToOutline = true
            binding.card.setOnClickListener { (it.tag as? JellyfinSeries)?.let(onSeriesClick) }
            binding.card.setOnFocusChangeListener { view, focused ->
                view.isSelected = focused
                renderFocus(focused)
            }
        }

        fun bind(series: JellyfinSeries, appearance: LauncherAppearance, recyclerView: RecyclerView, columns: Int) {
            this.appearance = appearance
            binding.card.tag = series
            binding.card.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    recyclerView.handleContainedHorizontalFocus(event, binding.card, columns)
                } else {
                    false
                }
            }
            binding.card.contentDescription = series.name
            binding.title.text = series.name
            binding.title.setTextColor(appearance.palette.primaryText)
            binding.year.text = series.productionYear?.let { year ->
                when {
                    series.continuing -> binding.root.resources.getString(R.string.series_year_current, year)

                    series.endYear != null && series.endYear > year ->
                        binding.root.resources.getString(R.string.series_year_range, year, series.endYear)

                    else -> binding.root.resources.getString(R.string.series_year, year)
                }
            }.orEmpty()
            binding.year.visibility = if (binding.year.text.isEmpty()) View.GONE else View.VISIBLE
            binding.year.setTextColor(appearance.palette.secondaryText)
            posterOutline.cornerRadius = dp(POSTER_RADIUS_DP).toFloat()
            posterOutline.setColor(appearance.palette.panel)
            binding.artwork.load(series.imageUrl)
            binding.card.isSoundEffectsEnabled = appearance.keyClickSounds
            renderFocus(binding.card.hasFocus())
        }

        private fun renderFocus(focused: Boolean) {
            val palette = appearance.palette
            val highlighted = focused && appearance.showFocusOutline
            posterOutline.setStroke(
                if (highlighted) dp(FOCUS_STROKE_DP) else 0,
                if (highlighted) palette.focus else Color.TRANSPARENT
            )
            val scale = if (focused && appearance.selectorTransitionAnimations) FOCUSED_SCALE else 1f
            binding.card.animate().cancel()
            if (appearance.selectorTransitionAnimations) {
                binding.card.animate().scaleX(scale).scaleY(scale).setDuration(FOCUS_ANIMATION_MS).start()
            } else {
                binding.card.scaleX = 1f
                binding.card.scaleY = 1f
            }
        }

        private fun dp(value: Int): Int = (value * binding.card.resources.displayMetrics.density).toInt()

        private companion object {
            const val POSTER_RADIUS_DP = 4
            const val FOCUS_STROKE_DP = 2
            const val FOCUSED_SCALE = 1.02f
            const val FOCUS_ANIMATION_MS = 140L
        }
    }

    private companion object {
        const val MAX_POSTER_HEIGHT_DP = 360
        const val TITLE_AREA_HEIGHT_DP = 42
        const val POSTER_ASPECT_RATIO = 2.0 / 3.0
        val DiffCallback = object : DiffUtil.ItemCallback<JellyfinSeries>() {
            override fun areItemsTheSame(oldItem: JellyfinSeries, newItem: JellyfinSeries): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: JellyfinSeries, newItem: JellyfinSeries): Boolean =
                oldItem == newItem
        }
    }
}
