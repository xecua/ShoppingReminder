package page.caffeine.shoppingreminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
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
import dagger.hilt.android.AndroidEntryPoint
import net.matsudamper.viewbindingutil.bindViewBinding
import page.caffeine.shoppingreminder.databinding.ActivityMainBinding
import page.caffeine.shoppingreminder.databinding.ItemListRowBinding
import page.caffeine.shoppingreminder.fragments.ItemEditDialog
import page.caffeine.shoppingreminder.models.Item
import page.caffeine.shoppingreminder.viewmodels.MainActivityViewModel

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val binding by bindViewBinding<ActivityMainBinding>()

    private val viewModel by viewModels<MainActivityViewModel>()

    private lateinit var adapter: ItemListAdapter
    private lateinit var geofenceClient: GeofencingClient

    private val signInLauncher =
        registerForActivityResult(FirebaseAuthUIActivityResultContract(), { res ->
            if (res.resultCode != RESULT_OK) {
                Log.d(TAG, res.idpResponse.toString())
                // user did not sign in
                Snackbar.make(
                    binding.root,
                    getString(R.string.please_login_to_use_this_app),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.location_needed_to_use_reminder),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        geofenceClient = LocationServices.getGeofencingClient(this)

        adapter = ItemListAdapter({ index ->
            View.OnClickListener {
                ItemEditDialog.newInstance(index)
                    .show(supportFragmentManager, ItemEditDialog::class.simpleName)
            }
        })
        viewModel.items.observe(this, {
            adapter.submitList(it)
        })

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                viewModel.swapItem(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.LEFT) {
                    viewModel.items.value?.let {
                        Snackbar.make(binding.root, R.string.item_deleted, Snackbar.LENGTH_SHORT)
                            .addCallback(object : Snackbar.Callback() {
                                override fun onDismissed(
                                    transientBottomBar: Snackbar?,
                                    event: Int
                                ) {
                                    super.onDismissed(transientBottomBar, event)
                                    if (event != DISMISS_EVENT_ACTION) {
                                        viewModel.delItem(it[viewHolder.adapterPosition].id)
                                    }
                                }
                            })
                            .show()
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
                val holder = viewHolder as ItemListAdapter.ItemListViewHolder

                if (dX < 0) {
                    // swiping to left
                    // https://beightlyouch.com/blog/programming/recycler-view-swipe-delete/
                    // stick frame
                    getDefaultUIUtil().onDraw(
                        c,
                        recyclerView,
                        viewHolder.rowLayout,
                        0f,
                        0f,
                        actionState,
                        isCurrentlyActive
                    )
                    // and move foreground view
                    getDefaultUIUtil().onDraw(
                        c,
                        recyclerView,
                        viewHolder.fgLayout,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                }
            }
        }).attachToRecyclerView(binding.itemList)

        val layoutManager = LinearLayoutManager(this)
        val itemDecoration = DividerItemDecoration(this, layoutManager.orientation)
        binding.apply {
            itemList.addItemDecoration(itemDecoration)
            itemList.layoutManager = layoutManager
            itemList.adapter = adapter

            addNewItem.setOnClickListener({
                ItemEditDialog.newInstance()
                    .show(supportFragmentManager, ItemEditDialog::class.simpleName)
            })
        }


        supportFragmentManager.apply {
            setFragmentResultListener(ItemEditDialog.DELETE_KEY, this@MainActivity, { _, bundle ->
                val id = bundle.getString("id")!!
                viewModel.delItem(id)
                geofenceClient.removeGeofences(listOf(id))
            })

            setFragmentResultListener(
                ItemEditDialog.RESULT_KEY,
                this@MainActivity
            ) { _, bundle ->
                val name = bundle.getString("name")
                val description = bundle.getString("description")
                val place = bundle.get("place") as? Place
                val id = bundle.getString("id")
                val isNew = bundle.getBoolean("isNew")
                val newItem = Item.ofNullable(name, description, place?.name, place?.id, id)

                if (place != null) {
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
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    geofenceClient.addGeofences(request, pendingIntent)?.run {
                        addOnSuccessListener { Log.d(TAG, "Successfully add geofence $request") }
                        addOnFailureListener { Log.e(TAG, "Failed to add geofence $request", it) }
                    }
                }
                if (isNew) viewModel.addItem(newItem)
                else viewModel.setItem(newItem.id, newItem)
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        if (Firebase.auth.currentUser == null) {
            Snackbar.make(
                binding.root,
                R.string.please_login_to_use_this_app,
                Snackbar.LENGTH_SHORT
            ).show()
        }
        Firebase.auth.addAuthStateListener { auth ->
            binding.addNewItem.visibility =
                if (auth.currentUser == null) View.GONE else View.VISIBLE
            viewModel.updateItems()
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
                } else {
                    AuthUI.getInstance().signOut(this)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun createSignInIntent(): Intent {
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


private val diffCallback = object : DiffUtil.ItemCallback<Item>() {
    override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
        return oldItem == newItem
    }
}

class ItemListAdapter(private var listener: (Int) -> View.OnClickListener) :
    ListAdapter<Item, ItemListAdapter.ItemListViewHolder>(diffCallback) {

    fun setOnClickListener(listener: (Int) -> View.OnClickListener) {
        this.listener = listener
    }

    class ItemListViewHolder(binding: ItemListRowBinding) : RecyclerView.ViewHolder(binding.root) {
        lateinit var id: String
        val rowLayout = binding.itemListRowFrame
        val fgLayout = binding.itemListRowFg
        val nameView = binding.itemListRowName
        val descView = binding.itemListRowDesc
        val placeIconView = binding.itemListRowLocationIcon
        val placeView = binding.itemListRowPlace
        val editView = binding.itemListRowEdit
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemListViewHolder {
        val view = ItemListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // LayoutInflater.from(parent.context).inflate(R.layout.item_list_row, parent, false)
        return ItemListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemListViewHolder, position: Int) {
        getItem(position).let {
            holder.id = it.id
            holder.nameView.text = it.name
            holder.descView.text = it.description
            holder.placeIconView.visibility =
                if (it.place.isEmpty()) View.INVISIBLE else View.VISIBLE
            holder.placeView.text = it.place
            holder.editView.setOnClickListener(listener(position))
        }
    }
}
