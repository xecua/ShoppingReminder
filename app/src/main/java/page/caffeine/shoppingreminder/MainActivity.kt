package page.caffeine.shoppingreminder

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
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import page.caffeine.shoppingreminder.databinding.ActivityMainBinding
import page.caffeine.shoppingreminder.databinding.ItemListRowBinding
import page.caffeine.shoppingreminder.fragments.ItemEditDialog
import page.caffeine.shoppingreminder.models.Item
import page.caffeine.shoppingreminder.viewmodels.MainActivityViewModel
import net.matsudamper.viewbindingutil.bindViewBinding

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val binding by bindViewBinding<ActivityMainBinding>()

    private val viewModel by viewModels<MainActivityViewModel>()

    private lateinit var geofenceClient: GeofencingClient

    private val signInLauncher =
        registerForActivityResult(FirebaseAuthUIActivityResultContract(), { res ->
            if (res.resultCode == RESULT_OK) {
                binding.addNewItem.visibility = View.VISIBLE
            } else {
                Log.d(TAG, res.idpResponse.toString())
                // user did not sign in
                Snackbar.make(
                    binding.root,
                    getString(R.string.please_login_to_use_this_app),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        geofenceClient = LocationServices.getGeofencingClient(this)

        signInLauncher.launch(createSignInIntent())

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
                                        getString(R.string.location_needed_to_use_reminder),
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
                        .setLoiteringDelay(BuildConfig.GEOFENCE_LOITERING_DELAY)
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
                        newItem.id.hashCode(),
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
                        getString(R.string.location_needed_to_use_reminder),
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

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val accountMenuItem = menu?.findItem(R.id.menu_account)
        if (Firebase.auth.currentUser == null) {
            accountMenuItem?.title = getString(R.string.sign_in)
        } else {
            accountMenuItem?.title = getString(R.string.sign_out)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_oss_license -> {
                startActivity(Intent(this, OssLicensesMenuActivity::class.java))
                true
            }
            R.id.menu_account -> {
                if (item.title.equals(getString(R.string.sign_in))) {
                    signInLauncher.launch(createSignInIntent())
                    true
                } else {
                    AuthUI.getInstance().signOut(this).addOnSuccessListener {
                        binding.addNewItem.visibility = View.GONE
                    }
                    true
                }
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun createSignInIntent(): Intent {
        return AuthUI.getInstance().createSignInIntentBuilder()
            .setAvailableProviders(arrayListOf(AuthUI.IdpConfig.GoogleBuilder().build()))
            .setIsSmartLockEnabled(!BuildConfig.DEBUG, true)
            .build()
    }

    companion object {
        const val TAG = "MainActivity"
        const val GEOFENCE_RADIUS = 100.0F // 適当
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
