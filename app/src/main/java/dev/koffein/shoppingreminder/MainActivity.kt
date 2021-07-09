package dev.koffein.shoppingreminder

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dev.koffein.shoppingreminder.databinding.ActivityMainBinding
import dev.koffein.shoppingreminder.databinding.ItemListRowBinding
import dev.koffein.shoppingreminder.fragments.ItemEditDialog
import dev.koffein.shoppingreminder.models.Item
import dev.koffein.shoppingreminder.viewmodels.MainActivityViewModel
import net.matsudamper.viewbindingutil.bindViewBinding

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val binding by bindViewBinding<ActivityMainBinding>()

    private val viewModel by viewModels<MainActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        viewModel.items.observe(this, {
            val adapter = ItemListAdapter(it)
            adapter.setOnClickListener { index ->
                View.OnClickListener {
                    ItemEditDialog.newInstance(index)
                        .show(supportFragmentManager, ItemEditDialog::class.simpleName)
                }
            }

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
        return when (item.itemId) {
            R.id.menu_oss_license -> {
                startActivity(Intent(this, OssLicensesMenuActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}

class ItemListAdapter(private val items: List<Item>) :
    RecyclerView.Adapter<ItemListAdapter.ItemListViewHolder>() {
    private lateinit var listener: (Int) -> View.OnClickListener

    fun setOnClickListener(listener: (Int) -> View.OnClickListener) {
        this.listener = listener
    }

    class ItemListViewHolder(binding: ItemListRowBinding) : RecyclerView.ViewHolder(binding.root) {
        val nameView: TextView = binding.itemListRowName
        val descView: TextView = binding.itemListRowDesc
        val editView: AppCompatImageButton = binding.itemListRowEdit
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemListViewHolder {
        val view = ItemListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // LayoutInflater.from(parent.context).inflate(R.layout.item_list_row, parent, false)
        return ItemListViewHolder(view)
    }

    // update
    override fun onBindViewHolder(holder: ItemListViewHolder, position: Int) {
        holder.nameView.text = items.getOrNull(position)?.name ?: ""
        holder.descView.text = items.getOrNull(position)?.description ?: ""
        holder.editView.setOnClickListener(listener(position))
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
