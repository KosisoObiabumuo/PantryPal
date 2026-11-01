package com.kosi.pantrypal

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kosi.pantrypal.data.FoodRepository
import com.kosi.pantrypal.model.FoodIcons
import com.kosi.pantrypal.model.FoodItem
import com.kosi.pantrypal.model.StorageType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AddItemActivity : AppCompatActivity() {

    private lateinit var nameLayout: TextInputLayout
    private lateinit var quantityLayout: TextInputLayout
    private lateinit var nameInput: TextInputEditText
    private lateinit var quantityInput: TextInputEditText
    private lateinit var thresholdInput: TextInputEditText
    private lateinit var unitSpinner: Spinner
    private lateinit var storageSpinner: Spinner
    private lateinit var expiryButton: MaterialButton
    private lateinit var priorityCheck: CheckBox
    private lateinit var allergenGroup: ChipGroup
    private lateinit var previewIcon: TextView
    private var selectedExpiry: LocalDate? = null   // null means no expiry date

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_item)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            // Include the keyboard (ime) so the form scrolls above it
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.title = "Log food"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        nameLayout = findViewById(R.id.layout_name)
        quantityLayout = findViewById(R.id.layout_quantity)
        nameInput = findViewById(R.id.input_name)
        quantityInput = findViewById(R.id.input_quantity)
        thresholdInput = findViewById(R.id.input_threshold)
        unitSpinner = findViewById(R.id.spinner_unit)
        storageSpinner = findViewById(R.id.spinner_storage)
        expiryButton = findViewById(R.id.button_expiry)
        priorityCheck = findViewById(R.id.check_priority)
        allergenGroup = findViewById(R.id.chip_group_allergens)
        previewIcon = findViewById(R.id.text_preview_icon)

        setUpSpinners()

        // Live icon preview while typing
        nameInput.doAfterTextChanged { updatePreview() }

        findViewById<View>(R.id.button_scan).setOnClickListener {
            Toast.makeText(this, "Barcode scanning arrives in Milestone 2", Toast.LENGTH_SHORT).show()
        }
        expiryButton.setOnClickListener { pickExpiryDate() }
        findViewById<View>(R.id.button_save).setOnClickListener { saveItem() }

        updatePreview()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setUpSpinners() {
        val units = ArrayAdapter.createFromResource(this, R.array.units, android.R.layout.simple_spinner_item)
        units.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        unitSpinner.adapter = units

        val storages = ArrayAdapter(this, android.R.layout.simple_spinner_item, StorageType.entries.map { it.label })
        storages.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        storageSpinner.adapter = storages

        // If opened from a storage screen, preselect that storage (explicit intent extra)
        intent.getStringExtra(MainActivity.EXTRA_STORAGE)?.let {
            storageSpinner.setSelection(StorageType.valueOf(it).ordinal)
        }

        storageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = updatePreview()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun selectedStorage(): StorageType = StorageType.entries[storageSpinner.selectedItemPosition]

    private fun textOf(input: TextInputEditText): String = input.text?.toString()?.trim().orEmpty()

    /** Show the icon this item will get; pop it when it changes. */
    private fun updatePreview() {
        val name = textOf(nameInput)
        val icon = if (name.isEmpty()) FoodIcons.genericFor(selectedStorage())
        else FoodIcons.forName(name, selectedStorage())
        if (icon != previewIcon.text.toString()) {
            previewIcon.text = icon
            previewIcon.scaleX = 0.5f
            previewIcon.scaleY = 0.5f
            previewIcon.animate().scaleX(1f).scaleY(1f)
                .setInterpolator(OvershootInterpolator(3f))
                .setDuration(300)
                .start()
        }
    }

    private fun pickExpiryDate() {
        val start = selectedExpiry ?: LocalDate.now().plusDays(7)
        val dialog = DatePickerDialog(this, { _, year, month, day ->
            val date = LocalDate.of(year, month + 1, day)   // DatePicker months start at 0
            selectedExpiry = date
            expiryButton.text = "Expires ${date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}"
        }, start.year, start.monthValue - 1, start.dayOfMonth)
        dialog.datePicker.minDate = System.currentTimeMillis() - 1000
        dialog.show()
    }

    private fun saveItem() {
        val name = textOf(nameInput)
        if (name.isEmpty()) {
            nameLayout.error = "Give it a name"
            return
        }
        nameLayout.error = null

        val quantity = textOf(quantityInput).toIntOrNull() ?: 0
        if (quantity < 1) {
            quantityLayout.error = "Enter 1 or more"
            return
        }
        quantityLayout.error = null

        val threshold = textOf(thresholdInput).toIntOrNull() ?: 1
        val allergens = allergenGroup.checkedChipIds.map { findViewById<Chip>(it).text.toString() }

        val item = FoodItem(
            name = name,
            quantity = quantity,
            unit = unitSpinner.selectedItem.toString(),
            storage = selectedStorage(),
            expiryDate = selectedExpiry,
            isPriority = priorityCheck.isChecked,
            lowStockThreshold = threshold,
            allergens = allergens
        )
        FoodRepository.add(item)

        // Send the result back to whichever screen opened us
        setResult(RESULT_OK, Intent().putExtra(MainActivity.EXTRA_ADDED_NAME, name))
        finish()
    }
}