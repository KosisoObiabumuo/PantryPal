package com.kosi.pantrypal

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kosi.pantrypal.model.FoodIcons
import com.kosi.pantrypal.model.FoodItem

/** Shows food items as icons standing on shelves. */
class FoodAdapter(
    private val listener: Listener,
    @ColorInt private val shelfColour: Int
) : RecyclerView.Adapter<FoodAdapter.ViewHolder>() {

    interface Listener {
        fun onItemClick(item: FoodItem, itemView: View)
        fun onItemLongClick(item: FoodItem)
    }

    private val items = mutableListOf<FoodItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(newItems: List<FoodItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()   // replaced by ListAdapter + DiffUtil in Milestone 3
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shelf, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(h: ViewHolder, position: Int) {
        val item = items[position]
        val ctx = h.itemView.context

        h.icon.text = FoodIcons.forItem(item)
        h.icon.alpha = if (item.quantity == 0) 0.35f else 1f   // empty items look faded
        h.name.text = if (item.isPriority) "★ ${item.name}" else item.name
        h.quantity.text = "×${item.quantity}"
        h.expiry.text = shortExpiry(item)
        h.expiry.setTextColor(ContextCompat.getColor(ctx, expiryColourRes(item)))
        h.low.visibility = if (item.isLowStock) View.VISIBLE else View.INVISIBLE
        h.plank.setBackgroundColor(shelfColour)

        h.itemView.setOnClickListener { listener.onItemClick(item, it) }
        h.itemView.setOnLongClickListener {
            listener.onItemLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val icon: TextView = v.findViewById(R.id.shelf_icon)
        val quantity: TextView = v.findViewById(R.id.shelf_quantity)
        val name: TextView = v.findViewById(R.id.shelf_name)
        val expiry: TextView = v.findViewById(R.id.shelf_expiry)
        val low: TextView = v.findViewById(R.id.shelf_low)
        val plank: View = v.findViewById(R.id.shelf_plank)
    }

    companion object {
        fun shortExpiry(item: FoodItem): String {
            val days = item.daysUntilExpiry() ?: return "no date"
            return when {
                days < 0 -> "expired"
                days == 0L -> "today"
                else -> "${days}d left"
            }
        }

        fun describeExpiry(item: FoodItem): String {
            val days = item.daysUntilExpiry() ?: return "No expiry date"
            return when {
                days < -1 -> "Expired ${-days} days ago"
                days == -1L -> "Expired yesterday"
                days == 0L -> "Expires today"
                days == 1L -> "Expires tomorrow"
                else -> "Expires in $days days"
            }
        }

        fun expiryColourRes(item: FoodItem): Int {
            val days = item.daysUntilExpiry() ?: return R.color.ink_soft
            return when {
                days <= 2 -> R.color.danger
                days <= 5 -> R.color.warning
                else -> R.color.ok
            }
        }
    }
}