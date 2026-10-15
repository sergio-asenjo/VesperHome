package com.sergioasenjo.vesperhome.launcher

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sergioasenjo.vesperhome.databinding.ViewLauncherContentBinding
import com.sergioasenjo.vesperhome.music.JellyfinSeries
import com.sergioasenjo.vesperhome.music.JellyfinSeriesAdapter
import com.sergioasenjo.vesperhome.music.JellyfinShowsState
import com.sergioasenjo.vesperhome.settings.LauncherAppearance

internal class JellyfinShowsController(
    context: Context,
    private val binding: ViewLauncherContentBinding,
    onSeriesClick: (JellyfinSeries) -> Unit,
    private val onLoadMore: () -> Unit
) {
    private val adapter = JellyfinSeriesAdapter(context, binding.showsItems, GRID_COLUMNS, onSeriesClick)
    private var appearance = LauncherAppearance()
    private var showsState = JellyfinShowsState()
    private val horizontalSpacing = dp(context, GRID_SPACING_DP)
    private val verticalSpacing = dp(context, GRID_SPACING_DP)

    init {
        binding.showsItems.apply {
            layoutManager = GridLayoutManager(context, GRID_COLUMNS).apply {
                initialPrefetchItemCount = INITIAL_PREFETCH_COUNT
            }
            adapter = this@JellyfinShowsController.adapter
            itemAnimator = null
            addItemDecoration(GridSpacingDecoration(horizontalSpacing, verticalSpacing))
            addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
                updateItemSize(right - left)
            }
        }
        binding.root.setOnScrollChangeListener { _, _, _, _, _ ->
            loadMoreWhenNearGridEnd()
        }
        binding.showsEmpty.setOnClickListener { if (showsState.failed) onLoadMore() }
        updateAppearance(appearance)
    }

    fun render(showsState: JellyfinShowsState, appearance: LauncherAppearance) {
        updateAppearance(appearance)
        this.showsState = showsState
        val seriesCount = showsState.totalRecordCount.takeIf { it > 0 } ?: showsState.series.size
        binding.showsCount.text = binding.root.resources.getQuantityString(
            com.sergioasenjo.vesperhome.R.plurals.jellyfin_series_count,
            seriesCount,
            seriesCount
        )
        binding.showsEmpty.isVisible = showsState.series.isEmpty()
        binding.showsEmpty.text = when {
            showsState.loading -> binding.root.context.getString(
                com.sergioasenjo.vesperhome.R.string.jellyfin_series_loading
            )

            showsState.failed -> binding.root.context.getString(
                com.sergioasenjo.vesperhome.R.string.jellyfin_series_load_failed
            )

            showsState.connected -> binding.root.context.getString(
                com.sergioasenjo.vesperhome.R.string.jellyfin_series_none
            )

            else -> binding.root.context.getString(com.sergioasenjo.vesperhome.R.string.jellyfin_series_empty)
        }
        binding.showsEmpty.isFocusable = showsState.failed
        binding.showsEmpty.isClickable = showsState.failed
        binding.showsItems.isVisible = showsState.series.isNotEmpty()
        adapter.submitList(showsState.series) {
            updateItemSize(binding.showsItems.width)
            binding.root.post(::loadMoreWhenNearGridEnd)
        }
    }

    private fun updateAppearance(appearance: LauncherAppearance) {
        if (this.appearance == appearance) return
        this.appearance = appearance
        binding.showsTitle.setTextColor(appearance.palette.primaryText)
        binding.showsCount.setTextColor(appearance.palette.focus)
        adapter.setAppearance(appearance)
    }

    private fun updateItemSize(recyclerWidth: Int) {
        if (recyclerWidth <= 0) return
        val cardWidth = (recyclerWidth - horizontalSpacing * GRID_COLUMNS) / GRID_COLUMNS
        adapter.setCardWidth(cardWidth)
        val rowHeight = adapter.cardHeight() + verticalSpacing
        val rowCount = (adapter.itemCount + GRID_COLUMNS - 1) / GRID_COLUMNS
        binding.showsItems.layoutParams = binding.showsItems.layoutParams.apply { height = rowCount * rowHeight }
    }

    private fun loadMoreWhenNearGridEnd() {
        if (!showsState.hasMore || showsState.loading || showsState.failed) return
        val contentTop = binding.root.getChildAt(0)?.top ?: 0
        val gridBottom = contentTop + binding.showsSection.bottom
        if (binding.root.scrollY + binding.root.height >=
            gridBottom - dp(binding.root.context, LOAD_MORE_DISTANCE_DP)
        ) {
            onLoadMore()
        }
    }

    private class GridSpacingDecoration(private val horizontalSpacing: Int, private val verticalSpacing: Int) :
        RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            outRect.set(horizontalSpacing / 2, verticalSpacing / 2, horizontalSpacing / 2, verticalSpacing / 2)
        }
    }

    private companion object {
        const val GRID_COLUMNS = 7
        const val GRID_SPACING_DP = 12
        const val INITIAL_PREFETCH_COUNT = 6
        const val LOAD_MORE_DISTANCE_DP = 600

        fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
    }
}
