package com.isapp.chips

import android.content.Context
import android.graphics.Rect
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.view.ViewCompat
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import com.isapp.chips.library.R
import java.util.*

interface ChipsListener {
  fun onLoadIcon(chip: Chip, imageView: ImageView) {}
  fun onChipClicked(chip: Chip) {}
  fun onChipDeleted(chip: Chip) {}
  fun onTextChanged(text: String) {}
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

      adapter.focusChipInput()
    }

  var chipsListener: ChipsListener?
    get() = adapter.listener
    set(value) { adapter.listener = value }

  var chipInputEnabled: Boolean
    get() = adapter.chipInputEnabled
    set(value) { adapter.chipInputEnabled = value }

  var chipTextAppearance: Int
    get() = adapter.chipTextAppearance
    set(value) {
      adapter.chipTextAppearance = value
      recycledViewPool.clear()
    }

  var chipInputTextAppearance: Int
    get() = adapter.chipInputTextAppearance
    set(value) {
      adapter.chipInputTextAppearance = value
      recycledViewPool.clear()
    }

  val chipInputText: String
    get() = adapter.chipInputText

  fun getChips(): List<Chip> = synchronized(this) { ArrayList(adapter.chips) }

  fun addChip(chip: Chip, clearEditText: Boolean = false) { synchronized(this) {
    adapter.apply {
      if(clearEditText) {
        clearChipInput()
      }

      chips.add(chip)
      val index = chips.lastIndex
      notifyItemInserted(index)
      scrollToPosition(itemCount - 1)

      focusChipInput()
    }
  }}

  fun removeChip(chip: Chip) { synchronized(this) {
    adapter.apply {
      val index = chips.indexOf(chip)
      if(index >= 0) {
        chips.remove(chip)
        notifyItemRemoved(index)
      }

      focusChipInput()
    }
  }}

  fun clearChips() { synchronized(this) {
    adapter.apply {
      val chipCount = chips.size
      chips.clear()
      notifyItemRangeRemoved(0, chipCount)

      focusChipInput()
    }
  }}

  private fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()

  private fun useFreeFormScrollingLayout(maxColumns: Int) {
    addItemDecoration(gridPaddingDecoration)

    layoutManager = GridLayoutManager(context, maxColumns).apply {
      spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
          if(position == itemCount - 1) {
            return 1
          }

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

  private fun useHorizontalScrollingLayout() {
    removeItemDecoration(gridPaddingDecoration)
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
  }
}

data class Chip(val data: Any, val text: String = data.toString(), val deletable: Boolean = false, val icon: Boolean = false)

private class ChipsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
  companion object {
    const val JUST_TEXT = 0
    const val DELETABLE = 1
    const val ICON = 2
    const val DELETABLE_ICON = 3
    const val EDIT_TEXT = 4
  }
  
  internal val chips: MutableList<Chip> = ArrayList()
  internal var chipTextAppearance: Int = R.style.DefaultTextAppearance
  internal var chipInputTextAppearance: Int = chipTextAppearance
    set(value) {
      field = value
      chipInput?.apply { TextViewCompat.setTextAppearance(this, chipInputTextAppearance) }
    }

  internal var listener: ChipsListener? = null

  internal var chipInputEnabled: Boolean = true
    get() = field
    set(enabled) {
      val previousVal = field
      field = enabled
      if(enabled && !previousVal) {
        notifyItemInserted(itemCount - 1)
      }
      else if(!enabled && previousVal) {
        chipInput?.hideKeyboard()
        notifyItemRemoved(itemCount)
      }
    }

  private var chipInput: EmptyBackspaceEditText? = null

  internal val chipInputText: String
    get() = chipInput?.text?.toString() ?: ""

  private val textWatcher = object : TextWatcher {
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
      (chipInput?.parent as? View)?.apply {
        if(s.isNullOrEmpty()) {
          if(background != null) {
            ViewCompat.setBackground(this, null)
          }
        }
        else {
          if(background == null) {
            ViewCompat.setBackground(this, ResourcesCompat.getDrawable(resources, R.drawable.chip_background, context.theme))
          }
        }
      }

      if(s != null) listener?.onTextChanged(s.toString())
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun afterTextChanged(s: Editable?) {}
  }

  init {
    chipInputEnabled = true
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    return if(viewType == EDIT_TEXT) {
      val chipInputContainer = LayoutInflater.from(parent.context).inflate(R.layout.chip_input, parent, false)
      ChipInputViewHolder(chipInputContainer).apply {
        initializeChipInput(chipInput, parent as RecyclerView)
      }
    }
    else {
      ChipsViewHolder(
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
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    if(holder !is ChipsViewHolder) {
      if(holder is ChipInputViewHolder) {
        with(holder.chipInput) {
          showKeyboard()
          requestFocus()
        }
      }
      return
    }

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

  override fun getItemCount() = chips.size + (if(chipInputEnabled) 1 else 0)

  override fun getItemViewType(position: Int): Int {
    if(position >= chips.size) {
      return EDIT_TEXT
    }

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

  private fun initializeChipInput(newChipInput: EmptyBackspaceEditText, parent: RecyclerView) {
    if(this.chipInput != null) {
      destroyChipInput()
    }

    this.chipInput = newChipInput.apply {
      TextViewCompat.setTextAppearance(this, chipInputTextAppearance)
      onEmptyBackspacePressed = {
        val index = chips.lastIndex
        if(index >= 0) {
          val chip = chips.removeAt(index)
          notifyItemRemoved(index)
          setText(chip.text)
          setSelection(chip.text.length)
          parent.scrollToPosition(itemCount - 1)
        }
      }

      addTextChangedListener(textWatcher)
    }
  }

  private fun destroyChipInput() {
    chipInput = chipInput?.run {
      onEmptyBackspacePressed = null
      removeTextChangedListener(textWatcher)
      null
    }
  }

  internal fun clearChipInput() {
    chipInput?.setText("")
  }

  internal fun focusChipInput() {
    chipInput?.requestFocus()
  }
}

private class ChipsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
  val icon = view.findViewById(android.R.id.icon1) as? ImageView
  val text = view.findViewById(android.R.id.text1) as TextView
  val delete = view.findViewById(android.R.id.icon2) as? ImageView
}

private class ChipInputViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  val chipInput: EmptyBackspaceEditText = itemView.findViewById(android.R.id.edit) as EmptyBackspaceEditText

}

class GridSpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
  override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
    outRect.bottom = spacing // item top
  }
}

private fun View.showKeyboard() {
  val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

private fun View.hideKeyboard() {
  val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  imm.hideSoftInputFromWindow(windowToken, 0)
}