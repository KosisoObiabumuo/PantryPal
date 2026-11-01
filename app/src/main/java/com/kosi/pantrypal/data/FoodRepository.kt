package com.kosi.pantrypal.data


import com.kosi.pantrypal.model.FoodItem
import com.kosi.pantrypal.model.StorageType
import java.time.LocalDate

object FoodRepository {

    private val items = mutableListOf<FoodItem>()

    init {
        seedSampleData()
    }

    fun getAll(): List<FoodItem> = items.toList()

    fun getByStorage(type: StorageType): List<FoodItem> = items.filter { it.storage == type }
    /** Items at or below their low stock threshold, priority items first. */
    fun getLowStock(): List<FoodItem> =
        items.filter { it.isLowStock }.sortedByDescending { it.isPriority }

    fun add(item: FoodItem) {
        items.add(item)
    }
    fun remove(item: FoodItem){
        items.remove(item)
    }

    private fun seedSampleData(){
        val today = LocalDate.now()
        add(FoodItem("Milk", 1, "L", StorageType.FRIDGE,today.plusDays(3), isPriority = true, allergens = listOf("Milk")))
        add(FoodItem("Eggs", 4, "item", StorageType.FRIDGE, today.plusDays(10), lowStockThreshold = 3, allergens = listOf("Eggs")))
        add(FoodItem("Cheddar", 1, "pack", StorageType.FRIDGE, today.plusDays(14), lowStockThreshold = 0, allergens = listOf("Milk")))
        add(FoodItem("Chicken breast", 2, "pack", StorageType.FREEZER, today.plusDays(60)))
        add(FoodItem("Frozen peas", 1, "pack", StorageType.FREEZER, today.plusDays(120), lowStockThreshold = 0))
        add(FoodItem("Bread", 1, "item", StorageType.PANTRY, today.plusDays(1), isPriority = true, allergens = listOf("Gluten")))
        add(FoodItem("Pasta", 2, "pack", StorageType.PANTRY, today.plusDays(300), allergens = listOf("Gluten")))
        add(FoodItem("Rice", 1, "kg", StorageType.PANTRY, today.plusDays(365), lowStockThreshold = 0))
        add(FoodItem("Chopped tomatoes", 3, "tin", StorageType.CABINET, today.plusDays(400)))
        add(FoodItem("Peanut butter", 1, "item", StorageType.CABINET, today.plusDays(90), lowStockThreshold = 0, allergens = listOf("Nuts")))
        add(FoodItem("Cereal", 0, "item", StorageType.CABINET, null, allergens = listOf("Gluten")))
    }
}


