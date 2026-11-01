package com.kosi.pantrypal.model

import java.util.Locale

/** Picks a cartoon icon for a food. Unknown foods get a generic icon for their storage. */
object FoodIcons {


    private val keywords = linkedMapOf(
        "peanut" to "🥜", "peach" to "🍑",
        "milk" to "🥛", "cheese" to "🧀", "cheddar" to "🧀", "mozzarella" to "🧀",
        "egg" to "🥚", "butter" to "🧈",
        "bread" to "🍞", "loaf" to "🍞", "toast" to "🍞", "bagel" to "🥯", "croissant" to "🥐",
        "chicken" to "🍗", "turkey" to "🍗",
        "steak" to "🥩", "beef" to "🥩", "mince" to "🥩", "lamb" to "🥩", "pork" to "🥩",
        "bacon" to "🥓", "rasher" to "🥓", "sausage" to "🌭",
        "fish" to "🐟", "salmon" to "🐟", "cod" to "🐟", "tuna" to "🐟",
        "prawn" to "🍤", "shrimp" to "🍤",
        "pasta" to "🍝", "spaghetti" to "🍝", "noodle" to "🍜", "rice" to "🍚", "pizza" to "🍕",
        "cereal" to "🥣", "oat" to "🥣", "porridge" to "🥣",
        "tomato" to "🍅", "potato" to "🥔", "chip" to "🍟", "carrot" to "🥕",
        "onion" to "🧅", "garlic" to "🧄", "broccoli" to "🥦",
        "lettuce" to "🥬", "spinach" to "🥬", "salad" to "🥗",
        "corn" to "🌽", "mushroom" to "🍄", "avocado" to "🥑",
        "apple" to "🍎", "banana" to "🍌", "orange" to "🍊", "lemon" to "🍋", "grape" to "🍇",
        "strawberry" to "🍓", "berry" to "🍓",
        "juice" to "🧃", "coffee" to "☕", "tea" to "🍵", "water" to "💧",
        "chocolate" to "🍫", "biscuit" to "🍪", "cookie" to "🍪", "cake" to "🍰",
        "honey" to "🍯", "jam" to "🍯", "soup" to "🥫", "bean" to "🥫",
        "ice" to "🍦"
    )

    fun forItem(item: FoodItem): String = forName(item.name, item.storage)

    fun forName(name: String, storage: StorageType): String {
        val words = name.lowercase(Locale.ROOT).split(Regex("[^a-z]+")).filter { it.isNotEmpty() }
        for ((keyword, icon) in keywords) {
            if (words.any { it.startsWith(keyword) }) return icon
        }
        return genericFor(storage)
    }

    /** Fallback icon for niche items. */
    fun genericFor(storage: StorageType): String = when (storage) {
        StorageType.FRIDGE -> "🥡"
        StorageType.FREEZER -> "🧊"
        StorageType.PANTRY -> "🧺"
        StorageType.CABINET -> "🥫"
    }
}