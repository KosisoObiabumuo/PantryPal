package com.kosi.pantrypal.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

class FoodItem(
    var name:String,
    quantity: Int,
    var unit: String,
    var storage: StorageType,
    var expiryDate: LocalDate? = null,   //null when unknown
    var isPriority: Boolean = false,     //e.g. milk, bread: always keep in stock
    var lowStockThreshold: Int = 1,      //"running low" when quantity <= this
    var barcode: String? = null,         // filled by the scanner later
    val allergens: List<String> = emptyList()

) {
    //Temporary ID
    val id: Int = nextId++

    /**Never goes below zero. */
    var quantity: Int = quantity.coerceAtLeast(0)
        set(value) {
            field = value.coerceAtLeast(0)

        }
    val isLowStock: Boolean
        get() = quantity <= lowStockThreshold
    /** Days from today until expiry.*/
    fun daysUntilExpiry(): Long? =
        expiryDate.let {
            ChronoUnit.DAYS.between(LocalDate.now(), it)
        }
    companion object{
        private var nextId = 1
    }
}