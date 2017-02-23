package com.isapp.chips

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Parcelable
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText

class EmptyBackspaceEditText : EditText {
  var onEmptyBackspacePressed: (() -> Unit)? = null
  var onIgnoreChange: (() -> Unit)? = null

  constructor(context: Context?) : super(context)
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    val length = text.length
    val ret = super.onKeyDown(keyCode, event)
    if (keyCode == KeyEvent.KEYCODE_DEL) {
      if (onEmptyBackspacePressed != null && length == 0) {
        onEmptyBackspacePressed?.invoke()
      }
    }
    return ret
  }

  override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
    outAttrs.actionLabel = null
    outAttrs.inputType = InputType.TYPE_NULL
    val connection = object : BaseInputConnection(this, false) {
      override fun getTextBeforeCursor(ignore: Int, ignore2: Int): String {
        return " "
      }
    }
    return connection
  }

  override fun onRestoreInstanceState(state: Parcelable) {
    if (onIgnoreChange != null) {
      onIgnoreChange?.invoke()
    }
    super.onRestoreInstanceState(state)
  }
}
