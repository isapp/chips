package com.isapp.chips

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.widget.ImageView
import java.util.*

class ChipsActivity : AppCompatActivity() {
  private lateinit var chips: ChipsView
  private lateinit var fab: FloatingActionButton
  private var flexBox = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_chips)
    val toolbar = findViewById(R.id.toolbar) as Toolbar
    setSupportActionBar(toolbar)

    chips = findViewById(R.id.chips_view) as ChipsView

    chips.setListener(object : ChipsListener {
      override fun onLoadIcon(chip: Chip, imageView: ImageView) {
        imageView.setImageResource(R.mipmap.ic_launcher)
      }

      override fun onChipClicked(chip: Chip) {
        Snackbar.make(fab, "${chip.text} clicked", Snackbar.LENGTH_SHORT).show()
      }

      override fun onChipDeleted(chip: Chip) {
        chips.removeChip(chip)
      }
    })

    chips.setTextAppearance(R.style.Base_TextAppearance_AppCompat_Body2)

    fab = findViewById(R.id.fab) as FloatingActionButton
    fab.setOnClickListener({ view ->
      chips.addChip(Chip(UUID.randomUUID().toString().take(Random().nextInt(32 - 2 + 1) + 2), deletable = randomBool(), icon = randomBool()))
    })

    fab.setOnLongClickListener {
      if(flexBox) {
        chips.useFlexboxScrollingLayout()
      }
      else {
        chips.useHorizontalScrollingLayout()
      }
      flexBox = !flexBox
      true
    }

    Snackbar.make(fab, "Long press the FAB to switch between flexbox chips and horizontal chips.", Snackbar.LENGTH_LONG).show()
  }

}

private fun randomBool() = Math.random() >= .5
