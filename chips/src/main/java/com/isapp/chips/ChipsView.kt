package com.isapp.chips

import android.content.Context
import android.graphics.Rect
import android.support.annotation.StyleRes
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
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

  private val spacingDecoration = GridSpacingItemDecoration(10)

  private var adapter: ChipsAdapter = ChipsAdapter()

  fun setListener(listener: ChipsListener) {
    adapter.listener = listener
  }

  fun setTextAppearance(@StyleRes textAppearance: Int) {
    adapter.textAppearance = textAppearance
  }

  init {
    useFlexboxScrollingLayout()
  }

  fun addChip(chip: Chip) { synchronized(this) {
    adapter.apply {
      chips.add(chip)
      val index = chips.size - 1
      notifyItemInserted(index)
      post {
        scrollToPosition(itemCount - 1)
      }
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

  fun useFlexboxScrollingLayout() = synchronized(this) {
    layoutManager = FlexboxLayoutManager(FlexDirection.ROW)
    adapter = ChipsAdapter(adapter)
    addItemDecoration(spacingDecoration)
    setAdapter(adapter)
  }

  fun useHorizontalScrollingLayout() = synchronized(this) {
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false).apply {
      stackFromEnd = true
    }
    adapter = ChipsAdapter(adapter)
    removeItemDecoration(spacingDecoration)
    setAdapter(adapter)
  }
}

data class Chip(val data: Any, val text: String = data.toString(), val deletable: Boolean = false, val icon: Boolean = false)

private class ChipsAdapter() : RecyclerView.Adapter<ChipsViewHolder>() {
  companion object {
    const val JUST_TEXT = 0
    const val DELETABLE = 1
    const val ICON = 2
    const val DELETABLE_ICON = 3
  }

  constructor(adapter: ChipsAdapter) : this() {
    chips.addAll(adapter.chips)
    textAppearance = adapter.textAppearance
    listener = adapter.listener
  }
  
  internal val chips: MutableList<Chip> = ArrayList()
  internal var textAppearance: Int = 0
  internal var listener: ChipsListener? = null

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipsViewHolder {
    return ChipsViewHolder(
        when(viewType) {
          JUST_TEXT -> LayoutInflater.from(parent.context).inflate(R.layout.chip, parent, false)
          DELETABLE -> LayoutInflater.from(parent.context).inflate(R.layout.deletable_chip, parent, false)
          ICON ->  LayoutInflater.from(parent.context).inflate(R.layout.icon_chip, parent, false)
          else -> LayoutInflater.from(parent.context).inflate(R.layout.deletable_icon_chip, parent, false)
        }
    ).apply {
      if(textAppearance > 0) {
        TextViewCompat.setTextAppearance(text, textAppearance)
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