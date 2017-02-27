package com.isapp.chips

import android.content.Context
import android.graphics.Rect
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.isapp.chips.library.R
import java.util.*

interface ChipsListener {
  fun onLoadIcon(chip: Chip, imageView: ImageView) {}
  fun onChipClicked(chip: Chip) {}
  fun onChipDeleted(chip: Chip) {}
}

class ChipsView : RecyclerView {
  constructor(context: Context?) : super(context)
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

  companion object {
    const val HORIZONTAL = RecyclerView.HORIZONTAL
    const val VERTICAL = RecyclerView.VERTICAL

    private val gridPaddingDecoration = GridSpacingItemDecoration(10)
  }

  private var adapter = ChipsAdapter()

  init {
    orientation = HORIZONTAL
    setAdapter(adapter)
  }

  /**
   * Can be either [ChipsView.VERTICAL] or [ChipsView.HORIZONTAL]
   */
  var orientation: Int
    get() = field
    set(value) {
      field = value
      if(value == VERTICAL) {
        useFreeFormScrollingLayout(4)
      }
      else if(value == HORIZONTAL) {
        useHorizontalScrollingLayout()
      }
    }

  var chipsListener: ChipsListener?
    get() = adapter.listener
    set(value) { adapter.listener = value }

  var chipTextAppearance: Int
    get() = adapter.chipTextAppearance
    set(value) {
      adapter.chipTextAppearance = value
      recycledViewPool.clear()
    }

  fun getChips(): List<Chip> = synchronized(this) { ArrayList(adapter.chips) }

  fun addChip(chip: Chip) { synchronized(this) {
    adapter.apply{
      chips.add(chip)
      val index = chips.lastIndex
      notifyItemInserted(index)
      scrollToPosition(index)
    }
  }}

  fun removeChip(chip: Chip) { synchronized(this) {
    adapter.apply {
      val index = chips.indexOf(chip)
      if(index >= 0) {
        chips.remove(chip)
        notifyItemRemoved(index)
      }
    }
  }}

  fun clearChips() { synchronized(this) {
    adapter.apply {
      val chipCount = chips.size
      chips.clear()
      notifyItemRangeRemoved(0, chipCount)
    }
  }}

  private fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()

  private fun useFreeFormScrollingLayout(maxColumns: Int) = synchronized(this) {
    addItemDecoration(gridPaddingDecoration)

    layoutManager = GridLayoutManager(context, maxColumns).apply {
      spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
          val parentWidth = width

          val holder = getChildAt(position)?.let {
            getChildViewHolder(it)  as? ChipsViewHolder
          }

          val textPaint = if(holder == null) {
            TextView(context).let {
              TextViewCompat.setTextAppearance(it, adapter.chipTextAppearance)
              it.paint
            }
          }
          else {
            holder.text.paint
          }

          val chip = adapter.chips[position]

          val text = chip.text
          val rect = Rect()
          textPaint.getTextBounds(text, 0, text.length, rect)

          // add padding and margins + icon widths
          val childWidth = if(chip.deletable && chip.icon) {
            (rect.width() + context.dip(80)).toFloat()
          }
          else if(chip.deletable && !chip.icon) {
            (rect.width() + context.dip(55)).toFloat()
          }
          else if(!chip.deletable && chip.icon) {
            (rect.width() + context.dip(60)).toFloat()
          }
          else {
            (rect.width() + context.dip(35)).toFloat()
          }

          val widthPerSpan = parentWidth.toFloat() / spanCount.toFloat()
          return Math.ceil(childWidth / widthPerSpan.toDouble()).toInt()
        }
      }

      spanSizeLookup.isSpanIndexCacheEnabled = true
    }
  }

  private fun useHorizontalScrollingLayout() = synchronized(this) {
    removeItemDecoration(gridPaddingDecoration)
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
  }
}

data class Chip(val data: Any, val text: String = data.toString(), val deletable: Boolean = false, val icon: Boolean = false)

private class ChipsAdapter : RecyclerView.Adapter<ChipsViewHolder>() {
  companion object {
    const val JUST_TEXT = 0
    const val DELETABLE = 1
    const val ICON = 2
    const val DELETABLE_ICON = 3
  }
  
  internal val chips: MutableList<Chip> = ArrayList()
  internal var chipTextAppearance: Int = R.style.DefaultTextAppearance
  internal var listener: ChipsListener? = null

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipsViewHolder {
    return ChipsViewHolder(
        when(viewType) {
          JUST_TEXT -> LayoutInflater.from(parent.context).inflate(R.layout.chip, parent, false)
          DELETABLE -> LayoutInflater.from(parent.context).inflate(R.layout.deletable_chip, parent, false)
          ICON ->  LayoutInflater.from(parent.context).inflate(R.layout.icon_chip, parent, false)
          DELETABLE_ICON -> LayoutInflater.from(parent.context).inflate(R.layout.deletable_icon_chip, parent, false)
          else -> throw UnsupportedOperationException("Trying to create an unsupported view type")
        }
    ).apply {
      if(chipTextAppearance > 0) {
        TextViewCompat.setTextAppearance(text, chipTextAppearance)
      }
    }
  }

  override fun onBindViewHolder(holder: ChipsViewHolder, position: Int) {
    val chip = chips[position]
    holder.itemView.setOnClickListener {
      listener?.onChipClicked(chips[holder.adapterPosition])
    }
    holder.text.text = chip.text
    holder.icon?.let {
      listener?.onLoadIcon(chip, it)
    }
    holder.delete?.setOnClickListener {
      listener?.onChipDeleted(chips[holder.adapterPosition])
    }
  }

  override fun getItemCount() = chips.size

  override fun getItemViewType(position: Int): Int {
    val chip = chips[position]
    return if(!chip.deletable and chip.icon) {
      ICON
    }
    else if(chip.deletable and !chip.icon) {
      DELETABLE
    }
    else if(chip.deletable and chip.icon) {
      DELETABLE_ICON
    }
    else {
      JUST_TEXT
    }
  }
}

private class ChipsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
  val icon = view.findViewById(android.R.id.icon1) as? ImageView
  val text = view.findViewById(android.R.id.text1) as TextView
  val delete = view.findViewById(android.R.id.icon2) as? ImageView
}

class GridSpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
  override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
    outRect.bottom = spacing // item top
  }
}