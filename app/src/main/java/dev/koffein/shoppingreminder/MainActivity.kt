package dev.koffein.shoppingreminder

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.snackbar.Snackbar
import dev.koffein.shoppingreminder.databinding.ActivityMainBinding
import dev.koffein.shoppingreminder.databinding.ItemListRowBinding
import dev.koffein.shoppingreminder.fragments.ItemEditDialog
import dev.koffein.shoppingreminder.models.Item
import dev.koffein.shoppingreminder.viewmodels.MainActivityViewModel
import net.matsudamper.viewbindingutil.bindViewBinding

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val binding by bindViewBinding<ActivityMainBinding>()

    private val viewModel by viewModels<MainActivityViewModel>()

    private lateinit var geofenceClient: GeofencingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        geofenceClient = LocationServices.getGeofencingClient(this)

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

        binding.addNewItem.setOnClickListener {
            ItemEditDialog.newInstance()
                .show(supportFragmentManager, ItemEditDialog::class.simpleName)
        }

        supportFragmentManager.apply {
            setFragmentResultListener(ItemEditDialog.DELETE_KEY, this@MainActivity) { _, bundle ->
                val id = bundle.getString("id")!!
                viewModel.delItem(id)
                geofenceClient.removeGeofences(listOf(id))
            }
            setFragmentResultListener(
                ItemEditDialog.RESULT_KEY,
                this@MainActivity
            ) { _, bundle ->
                val name = bundle.getString("name")
                val description = bundle.getString("description")
                val place = bundle.get("place") as? Place
                val id = bundle.getString("id")
                val isNew = bundle.getBoolean("isNew")

                val newItem = Item.ofNullable(name, description, place?.name, id)

                if (place != null) {
                    // 通知を設定
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (ActivityCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                                if (!isGranted) {
                                    Snackbar.make(
                                        binding.root,
                                        "リマインダ機能を利用するためには、位置情報を有効にする必要があります",
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                            }.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    }

                    val geofence = Geofence.Builder()
                        .setRequestId(newItem.id)
                        .setCircularRegion(
                            place.latLng!!.latitude,
                            place.latLng!!.longitude,
                            GEOFENCE_RADIUS
                        )
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                        .setLoiteringDelay(GEOFENCE_LOITERING_DELAY)
                        .build()
                    val request = GeofencingRequest.Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
                        .addGeofence(geofence)
                        .build()
                    val intent = Intent(this@MainActivity, GeofenceBroadcastReceiver::class.java)
                    intent.putExtra("name", newItem.name)
                    intent.putExtra("description", newItem.description)
                    intent.putExtra("id", newItem.id)
                    val pendingIntent = PendingIntent.getBroadcast(
                        this@MainActivity,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    geofenceClient.addGeofences(request, pendingIntent)?.run {
                        addOnSuccessListener { Log.d(TAG, "Successfully add geofence $request") }
                        addOnFailureListener { Log.e(TAG, "Failed to add geofence $request", it) }
                    }
                }
                if (isNew) viewModel.addItem(newItem)
                else viewModel.setItem(newItem.id, newItem)
                // index持ってるとnotifyItemChangedが飛ばせそう?
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (!isGranted) {
                    Snackbar.make(
                        binding.root,
                        "リマインダ機能を利用するためには、位置情報を有効にする必要があります",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
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

    companion object {
        const val TAG = "MainActivity"
        const val GEOFENCE_RADIUS = 100.0F // 適当
        const val GEOFENCE_LOITERING_DELAY = 1000 * 60 * 1 // 1分
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
        items.getOrNull(position)?.let {
            holder.nameView.text = it.name
            holder.descView.text = it.description
        }
        holder.editView.setOnClickListener(listener(position))
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
