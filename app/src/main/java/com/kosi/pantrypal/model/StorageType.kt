package com.kosi.pantrypal.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.kosi.pantrypal.R

/** The four places food can live, with their look. */
enum class StorageType(
    val label: String,
    @ColorRes val colourRes: Int,
    @DrawableRes val iconRes: Int
) {
    FRIDGE("Fridge", R.color.fridge, R.drawable.ic_fridge),
    FREEZER("Freezer", R.color.freezer, R.drawable.ic_freezer),
    PANTRY("Pantry", R.color.pantry, R.drawable.ic_pantry),
    CABINET("Cabinet", R.color.cabinet, R.drawable.ic_cabinet)
}