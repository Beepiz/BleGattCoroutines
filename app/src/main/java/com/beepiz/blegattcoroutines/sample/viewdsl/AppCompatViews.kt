@file:Suppress("NOTHING_TO_INLINE")

package com.beepiz.blegattcoroutines.sample.viewdsl

import android.content.Context
import android.support.v7.widget.*
import android.widget.*

/** Matches [android.support.v7.app.AppCompatViewInflater.createView] content. */

inline fun textView(ctx: Context): TextView = AppCompatTextView(ctx)
inline fun imageView(ctx: Context): ImageView = AppCompatImageView(ctx)
inline fun button(ctx: Context): Button = AppCompatButton(ctx)
inline fun editText(ctx: Context): EditText = AppCompatEditText(ctx)
inline fun spinner(ctx: Context): Spinner = AppCompatSpinner(ctx)
inline fun imageButton(ctx: Context): ImageButton = AppCompatImageButton(ctx)
inline fun checkBox(ctx: Context): CheckBox = AppCompatCheckBox(ctx)
inline fun radioButton(ctx: Context): RadioButton = AppCompatRadioButton(ctx)
inline fun checkedTextView(ctx: Context): CheckedTextView = AppCompatCheckedTextView(ctx)
inline fun autoCompleteTextView(ctx: Context): AutoCompleteTextView = AppCompatAutoCompleteTextView(ctx)
inline fun multiAutoCompleteTextView(ctx: Context): MultiAutoCompleteTextView = AppCompatMultiAutoCompleteTextView(ctx)
inline fun ratingBar(ctx: Context): RatingBar = AppCompatRatingBar(ctx)
inline fun seekBar(ctx: Context): SeekBar = AppCompatSeekBar(ctx)
