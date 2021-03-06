package page.caffeine.shoppingreminder.fragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import page.caffeine.shoppingreminder.BuildConfig
import page.caffeine.shoppingreminder.R
import page.caffeine.shoppingreminder.databinding.DialogEdititemBinding
import page.caffeine.shoppingreminder.models.Item
import page.caffeine.shoppingreminder.viewmodels.MainActivityViewModel

class ItemEditDialog : BottomSheetDialogFragment() {
    private lateinit var binding: DialogEdititemBinding

    private val viewModel by activityViewModels<MainActivityViewModel>()

    // null == 新規作成
    private var item: Item? = null

    private var currentPlace: Place? = null

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.MAPS_API_KEY)
        }

        // むしろitemを取得する?
        val index = arguments?.getInt(ARGS_INDEX, -1)
        if (index != null && index != -1) {
            item = viewModel.items.value?.get(index)
        }
    }

    // onCreateDialogでbindしているのでonCreateViewが不要?

    // 中身のDialogを作って返す部分
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialog)

        val inflater = LayoutInflater.from(requireContext())
        binding = DialogEdititemBinding.inflate(inflater)

        item?.let {
            // edit
            binding.editItemName.setText(it.name)
            binding.editItemDesc.setText(it.description)

            if (it.placeId != "") {
                Places.createClient(requireContext()).fetchPlace(
                    FetchPlaceRequest.newInstance(it.placeId, PLACE_FIELDS)
                ).addOnSuccessListener({ response ->
                    currentPlace = response.place
                }).addOnFailureListener({ e ->
                    Log.e(TAG, "error occurred: $e")
                })
            }
        }

        Log.d(TAG, "${parentFragmentManager.fragments}")

        if (item == null) binding.editItemName.requestFocus()
        binding.editItemName.setOnEditorActionListener { _, actionId, event ->
            if ((actionId == EditorInfo.IME_ACTION_SEND)// for software keyboard
                || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) // for hardware keyboard
            ) {
                binding.editItemSend.callOnClick()
            }
            return@setOnEditorActionListener true
        }

        (parentFragmentManager.findFragmentById(R.id.edit_item_place) as? AutocompleteSupportFragment)
            ?.apply {
                setText(item?.place)
                setPlaceFields(PLACE_FIELDS)
                setCountry("JP")
                setOnPlaceSelectedListener(object : PlaceSelectionListener {
                    override fun onPlaceSelected(p0: Place) {
                        currentPlace = p0
                        Log.i(TAG, "$p0")
                    }

                    override fun onError(p0: Status) {
                        Log.e(TAG, "error occurred: $p0")
                    }
                })
            }

        binding.editItemDelete.visibility = if (item == null) View.GONE else View.VISIBLE
        binding.editItemDelete.setOnClickListener {
            val bundle = bundleOf(
                "id" to item?.id
            )
            setFragmentResult(DELETE_KEY, bundle)
            parentFragmentManager.beginTransaction().remove(this).commit()
        }

        binding.editItemSend.setOnClickListener {
            val bundle = bundleOf(
                "isNew" to (item == null),
                "name" to binding.editItemName.text.toString(),
                "description" to binding.editItemDesc.text.toString(),
                "place" to currentPlace,
                "id" to item?.id
            )
            setFragmentResult(RESULT_KEY, bundle)
            parentFragmentManager.beginTransaction().remove(this).commit()
        }

        dialog.setContentView(binding.root)

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()

        val placeFragment = parentFragmentManager.findFragmentById(R.id.edit_item_place)
        if (placeFragment != null) {
            parentFragmentManager.beginTransaction().remove(placeFragment).commit()
        }
    }

    companion object {
        const val TAG = "ItemEditDialog"

        const val DELETE_KEY = "ItemEditDialogDelete"
        const val RESULT_KEY = "ItemEditDialogResult"

        // 型安全にしたいなあ……
        const val ARGS_INDEX = "index"

        val PLACE_FIELDS = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)

        fun newInstance() = ItemEditDialog()

        fun newInstance(index: Int): ItemEditDialog {
            val dialog = ItemEditDialog()
            val args = Bundle()
            args.putInt(ARGS_INDEX, index)
            dialog.arguments = args
            return dialog
        }
    }
}