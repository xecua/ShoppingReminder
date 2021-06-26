package dev.koffein.shoppingreminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.koffein.shoppingreminder.databinding.ActivityMainBinding
import dev.koffein.shoppingreminder.models.Item
import dev.koffein.shoppingreminder.viewmodels.MainActivityViewModel
import net.matsudamper.viewbindingutil.bindViewBinding

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val binding by bindViewBinding<ActivityMainBinding>()

    private val viewModel by viewModels<MainActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.items.observe(this, {
            val adapter = ItemListAdapter(it)
            binding.itemList.layoutManager = LinearLayoutManager(this)
            binding.itemList.adapter = adapter
            adapter.notifyDataSetChanged()
        })
        // for checking
        // startActivity(Intent(this, OssLicensesMenuActivity::class.java))
    }

}

class ItemListAdapter(private val items: Array<Item>) :
    RecyclerView.Adapter<ItemListAdapter.ItemListViewHolder>() {
    class ItemListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.item_list_row_name)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemListViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_list_row, parent, false)
        return ItemListViewHolder(view)
    }

    // update
    override fun onBindViewHolder(holder: ItemListViewHolder, position: Int) {
        holder.textView.text = items.getOrNull(position)?.name ?: "foo"
    }

    override fun getItemCount(): Int {
        return items.size
    }


}
