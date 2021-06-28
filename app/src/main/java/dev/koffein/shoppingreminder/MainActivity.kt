package dev.koffein.shoppingreminder

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
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
            val layoutManager = LinearLayoutManager(this)
            val itemDecoration = DividerItemDecoration(this, layoutManager.orientation)
            binding.itemList.addItemDecoration(itemDecoration)
            binding.itemList.layoutManager = layoutManager
            binding.itemList.adapter = adapter
            adapter.notifyDataSetChanged()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.menu_oss_license -> {
               startActivity(Intent(this, OssLicensesMenuActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}

class ItemListAdapter(private val items: Array<Item>) :
    RecyclerView.Adapter<ItemListAdapter.ItemListViewHolder>() {

    class ItemListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameView: TextView = view.findViewById(R.id.item_list_row_name)
        val descView: TextView = view.findViewById(R.id.item_list_row_desc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemListViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_list_row, parent, false)
        return ItemListViewHolder(view)
    }

    // update
    override fun onBindViewHolder(holder: ItemListViewHolder, position: Int) {
        holder.nameView.text = items.getOrNull(position)?.name ?: ""
        holder.descView.text = items.getOrNull(position)?.description ?: ""
    }

    override fun getItemCount(): Int {
        return items.size
    }


}
