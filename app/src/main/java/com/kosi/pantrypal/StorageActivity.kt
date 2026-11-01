package com.kosi.pantrypal

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kosi.pantrypal.data.FoodRepository
import com.kosi.pantrypal.model.FoodIcons
import com.kosi.pantrypal.model.FoodItem
import com.kosi.pantrypal.model.StorageType
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

class StorageActivity : AppCompatActivity(), FoodAdapter.Listener {

    private lateinit var storageType: StorageType
    private lateinit var adapter: FoodAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var doorOverlay: View
    private lateinit var doorLeft: View
    private lateinit var doorRight: View
    private var sortMode = 0
    private var closing = false

    private val addItemLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val name = result.data?.getStringExtra(MainActivity.EXTRA_ADDED_NAME)
            if (result.resultCode == RESULT_OK && name != null) {
                Snackbar.make(findViewById(R.id.main), "$name added", Snackbar.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_storage)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // Which storage to show comes from the explicit intent extra
        storageType = intent.getStringExtra(MainActivity.EXTRA_STORAGE)
            ?.let { StorageType.valueOf(it) } ?: StorageType.FRIDGE
        val colour = ContextCompat.getColor(this, storageType.colourRes)

        setUpToolbar(colour)
        setUpShelves(colour)
        setUpSortSpinner()
        setUpDoors(colour)

        findViewById<View>(R.id.fab_add).setOnClickListener {
            val addIntent = Intent(this, AddItemActivity::class.java)
            addIntent.putExtra(MainActivity.EXTRA_STORAGE, storageType.name)
            addItemLauncher.launch(addIntent)
        }

        // Back closes the doors before leaving
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = closeAndFinish()
        })

        if (savedInstanceState == null) {
            // Wait until the doors have been measured, then swing them open
            doorOverlay.post { animateDoors(open = true) { revealItems() } }
        } else {
            // Screen rotated: skip the show and just display the shelves
            doorOverlay.visibility = View.GONE
            recycler.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        loadItems()
    }

    override fun onSupportNavigateUp(): Boolean {
        closeAndFinish()
        return true
    }

    //Setup

    private fun setUpToolbar(colour: Int) {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = storageType.label
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setBackgroundColor(colour)
    }

    private fun setUpShelves(colour: Int) {
        findViewById<View>(R.id.interior).setBackgroundColor(ColorUtils.blendARGB(colour, Color.WHITE, 0.6f))
        val shelfColour = ColorUtils.blendARGB(colour, Color.BLACK, 0.3f)
        recycler = findViewById(R.id.recycler_items)
        recycler.layoutManager = GridLayoutManager(this, 3)
        adapter = FoodAdapter(this, shelfColour)
        recycler.adapter = adapter
        emptyText = findViewById(R.id.text_empty)
    }

    private fun setUpSortSpinner() {
        val sort = findViewById<Spinner>(R.id.spinner_sort)
        val sortAdapter = ArrayAdapter.createFromResource(
            this, R.array.sort_options, android.R.layout.simple_spinner_item
        )
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sort.adapter = sortAdapter
        sort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sortMode = position
                loadItems()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    //Doors

    private fun setUpDoors(colour: Int) {
        doorOverlay = findViewById(R.id.door_overlay)
        doorLeft = findViewById(R.id.door_left)
        doorRight = findViewById(R.id.door_right)

        doorLeft.background = makeDoor(colour)
        doorRight.background = makeDoor(colour)
        val camera = 12000 * resources.displayMetrics.density
        doorLeft.cameraDistance = camera
        doorRight.cameraDistance = camera

        when (storageType) {
            StorageType.FRIDGE -> {         // one door, hinge on the left
                doorRight.visibility = View.GONE
            }
            StorageType.CABINET -> {        // one door, hinge on the right
                doorLeft.visibility = View.GONE
            }
            StorageType.FREEZER -> {        // lid that lifts up, handle along the bottom
                doorRight.visibility = View.GONE
                findViewById<View>(R.id.handle_left).layoutParams = FrameLayout.LayoutParams(
                    dp(120), dp(16), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                ).apply { bottomMargin = dp(48) }
            }
            StorageType.PANTRY -> Unit      // double doors, both visible
        }
    }

    private fun makeDoor(colour: Int) = GradientDrawable().apply {
        setColor(colour)
        cornerRadius = dp(18).toFloat()
        setStroke(dp(5), ContextCompat.getColor(this@StorageActivity, R.color.ink))
    }

    /** open = true swings the doors away; false swings them shut with a bounce. */
    private fun animateDoors(open: Boolean, endAction: (() -> Unit)? = null) {
        doorOverlay.visibility = View.VISIBLE
        doorLeft.pivotX = 0f
        doorLeft.pivotY = 0f
        doorRight.pivotX = doorRight.width.toFloat()

        val motion = if (open) AccelerateDecelerateInterpolator() else BounceInterpolator()
        val duration = if (open) 550L else 650L
        val done = Runnable {
            if (open) doorOverlay.visibility = View.GONE
            endAction?.invoke()
        }

        if (storageType == StorageType.FREEZER) {
            doorLeft.animate().rotationX(if (open) 90f else 0f)
                .setInterpolator(motion).setDuration(duration).withEndAction(done).start()
            return
        }

        var endActionUsed = false
        if (doorLeft.visibility == View.VISIBLE) {
            doorLeft.animate().rotationY(if (open) -90f else 0f)
                .setInterpolator(motion).setDuration(duration).withEndAction(done).start()
            endActionUsed = true
        }
        if (doorRight.visibility == View.VISIBLE) {
            val right = doorRight.animate().rotationY(if (open) 90f else 0f)
                .setInterpolator(motion).setDuration(duration)
            if (!endActionUsed) right.withEndAction(done)
            right.start()
        }
    }

    private fun revealItems() {
        recycler.visibility = View.VISIBLE
        recycler.scheduleLayoutAnimation()   // items pop onto the shelves
    }

    private fun closeAndFinish() {
        if (closing) return
        closing = true
        animateDoors(open = false) {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    //Items

    private fun loadItems() {
        val items = FoodRepository.getByStorage(storageType)
        val sorted = when (sortMode) {
            1 -> items.sortedBy { it.name.lowercase(Locale.ROOT) }
            2 -> items.sortedBy { it.quantity }
            else -> items.sortedWith(compareBy(nullsLast<LocalDate>()) { item: FoodItem -> item.expiryDate })
        }
        adapter.submit(sorted)
        emptyText.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
    }

    /** Tap: the item hops like a Crossy Road character, then its sheet opens. */
    override fun onItemClick(item: FoodItem, itemView: View) {
        itemView.animate()
            .translationY(-dp(14).toFloat())
            .setInterpolator(DecelerateInterpolator())
            .setDuration(110)
            .withEndAction {
                itemView.animate()
                    .translationY(0f)
                    .setInterpolator(BounceInterpolator())
                    .setDuration(260)
                    .withEndAction { showItemSheet(item) }
                    .start()
            }
            .start()
    }

    override fun onItemLongClick(item: FoodItem) = confirmDelete(item)

    private fun showItemSheet(item: FoodItem) {
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.sheet_item, null)
        sheet.setContentView(view)

        val quantity = view.findViewById<TextView>(R.id.sheet_quantity)
        val expiry = view.findViewById<TextView>(R.id.sheet_expiry)
        view.findViewById<TextView>(R.id.sheet_icon).text = FoodIcons.forItem(item)
        view.findViewById<TextView>(R.id.sheet_name).text = item.name
        expiry.text = FoodAdapter.describeExpiry(item)
        expiry.setTextColor(ContextCompat.getColor(this, FoodAdapter.expiryColourRes(item)))
        view.findViewById<TextView>(R.id.sheet_allergens).text =
            if (item.allergens.isEmpty()) "No allergens logged"
            else "Contains: ${item.allergens.joinToString()}"

        fun showQuantity() {
            quantity.text = "${item.quantity} ${item.unit}"
        }
        showQuantity()

        view.findViewById<View>(R.id.sheet_minus).setOnClickListener {
            item.quantity -= 1
            showQuantity()
            pop(quantity)
            loadItems()
        }
        view.findViewById<View>(R.id.sheet_plus).setOnClickListener {
            item.quantity += 1
            showQuantity()
            pop(quantity)
            loadItems()
        }
        view.findViewById<View>(R.id.sheet_recipes).setOnClickListener { searchRecipes(item) }
        view.findViewById<View>(R.id.sheet_reminder).setOnClickListener { addCalendarReminder(item) }
        view.findViewById<View>(R.id.sheet_remove).setOnClickListener {
            sheet.dismiss()
            confirmDelete(item)
        }
        sheet.show()
    }

    private fun pop(v: View) {
        v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(80)
            .withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(120).start() }
            .start()
    }

    private fun confirmDelete(item: FoodItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove ${item.name}?")
            .setMessage("It will be taken out of your ${storageType.label.lowercase(Locale.ROOT)}.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ ->
                FoodRepository.remove(item)
                loadItems()
                Snackbar.make(findViewById(R.id.main), "${item.name} removed", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        FoodRepository.add(item)
                        loadItems()
                    }
                    .show()
            }
            .show()
    }

    //Implicit intents

    /** IMPLICIT INTENT: let any browser show recipes for this item. */
    private fun searchRecipes(item: FoodItem) {
        val uri = Uri.parse("https://www.google.com/search?q=" + Uri.encode("${item.name} recipes"))
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show()
        }
    }

    /** IMPLICIT INTENT: let the calendar app create an all-day reminder the day before expiry. */
    private fun addCalendarReminder(item: FoodItem) {
        val expiry = item.expiryDate
        if (expiry == null) {
            Toast.makeText(this, "Set an expiry date first", Toast.LENGTH_SHORT).show()
            return
        }
        val start = expiry.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Use up your ${item.name}")
            putExtra(CalendarContract.Events.DESCRIPTION, "Expires on $expiry")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        }
        try {
            startActivity(calendarIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No calendar app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}