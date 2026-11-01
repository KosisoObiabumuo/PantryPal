package com.kosi.pantrypal

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.kosi.pantrypal.data.FoodRepository
import com.kosi.pantrypal.model.StorageType
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    companion object {
        // Keys for data passed between activities in intents
        const val EXTRA_STORAGE = "com.kosi.pantrypal.EXTRA_STORAGE"
        const val EXTRA_ADDED_NAME = "com.kosi.pantrypal.EXTRA_ADDED_NAME"
    }

    private lateinit var lowStockBanner: TextView
    private lateinit var tiles: Map<StorageType, View>
    private var opening = false

    // Explicit intent that returns a result: AddItemActivity tells us what was added
    private val addItemLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val name = result.data?.getStringExtra(EXTRA_ADDED_NAME)
            if (result.resultCode == RESULT_OK && name != null) {
                Snackbar.make(findViewById(R.id.main), "$name added to your kitchen", Snackbar.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        lowStockBanner = findViewById(R.id.text_low_stock)
        tiles = mapOf(
            StorageType.FRIDGE to findViewById<View>(R.id.tile_fridge),
            StorageType.FREEZER to findViewById<View>(R.id.tile_freezer),
            StorageType.PANTRY to findViewById<View>(R.id.tile_pantry),
            StorageType.CABINET to findViewById<View>(R.id.tile_cabinet)
        )
        tiles.forEach { (type, tile) -> setUpTile(tile, type) }

        findViewById<View>(R.id.fab_add).setOnClickListener {
            addItemLauncher.launch(Intent(this, AddItemActivity::class.java))
        }
        findViewById<View>(R.id.button_share_list).setOnClickListener { shareShoppingList() }
        lowStockBanner.setOnClickListener { shareShoppingList() }
    }

    override fun onResume() {
        super.onResume()
        refreshKitchen()
    }

    private fun setUpTile(tile: View, type: StorageType) {
        (tile as MaterialCardView).setCardBackgroundColor(ContextCompat.getColor(this, type.colourRes))
        tile.findViewById<ImageView>(R.id.tile_icon).setImageResource(type.iconRes)
        tile.findViewById<TextView>(R.id.tile_label).text = type.label
        placeHandle(tile.findViewById(R.id.tile_handle), type)
        tile.cameraDistance = 10000 * resources.displayMetrics.density
        tile.setOnClickListener { openStorage(tile, type) }
    }

    /** Put the handle on the opposite side to the hinge, like a real door. */
    private fun placeHandle(handle: View, type: StorageType) {
        val lp = handle.layoutParams as FrameLayout.LayoutParams
        when (type) {
            StorageType.FREEZER -> {        // lid: handle along the bottom
                lp.width = dp(56)
                lp.height = dp(10)
                lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                lp.marginEnd = 0
                lp.bottomMargin = dp(10)
            }
            StorageType.CABINET -> {        // hinge on the right: handle on the left
                lp.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                lp.marginEnd = 0
                lp.marginStart = dp(10)
            }
            else -> Unit                    // fridge and pantry keep the handle on the right
        }
        handle.layoutParams = lp
    }

    /** Squash, swing the door on its hinge, then open StorageActivity (explicit intent). */
    private fun openStorage(tile: View, type: StorageType) {
        if (opening) return
        opening = true

        tile.animate()
            .scaleX(1.06f).scaleY(0.92f)
            .setInterpolator(DecelerateInterpolator())
            .setDuration(90)
            .withEndAction {
                val swing = tile.animate()
                    .scaleX(1f).scaleY(1f)
                    .setInterpolator(AccelerateInterpolator())
                    .setDuration(220)
                when (type) {
                    StorageType.FREEZER -> {        // lid lifts from the top
                        tile.pivotY = 0f
                        swing.rotationX(40f)
                    }
                    StorageType.CABINET -> {        // hinge on the right
                        tile.pivotX = tile.width.toFloat()
                        swing.rotationY(40f)
                    }
                    else -> {                       // fridge and pantry: hinge on the left
                        tile.pivotX = 0f
                        swing.rotationY(-40f)
                    }
                }
                swing.withEndAction {
                    val storageIntent = Intent(this, StorageActivity::class.java)
                    storageIntent.putExtra(EXTRA_STORAGE, type.name)
                    startActivity(storageIntent)
                    @Suppress("DEPRECATION")
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }.start()
            }
            .start()
    }

    /** Close all doors again and update counts and the low-stock banner. */
    private fun refreshKitchen() {
        opening = false
        tiles.forEach { (type, tile) ->
            tile.rotationX = 0f
            tile.rotationY = 0f
            tile.scaleX = 1f
            tile.scaleY = 1f
            val n = FoodRepository.getByStorage(type).size
            tile.findViewById<TextView>(R.id.tile_count).text = if (n == 1) "1 item" else "$n items"
        }

        val low = FoodRepository.getLowStock()
        if (low.isEmpty()) {
            lowStockBanner.visibility = View.GONE
        } else {
            lowStockBanner.text = "Running low: ${low.joinToString { it.name }}  (tap to share list)"
            lowStockBanner.visibility = View.VISIBLE
        }
    }

    /** IMPLICIT INTENT: any app that can send text (WhatsApp, Gmail, Messages) can receive the list. */
    private fun shareShoppingList() {
        val low = FoodRepository.getLowStock()
        if (low.isEmpty()) {
            Snackbar.make(findViewById(R.id.main), "Nothing is running low", Snackbar.LENGTH_SHORT).show()
            return
        }
        val text = buildString {
            append("Shopping list from PantryPal:\n")
            low.forEach { item ->
                append("• ").append(item.name)
                if (item.isPriority) append(" (priority)")
                append('\n')
            }
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Shopping list")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(send, "Share shopping list with"))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}