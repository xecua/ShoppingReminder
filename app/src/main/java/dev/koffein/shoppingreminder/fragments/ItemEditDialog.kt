package dev.koffein.shoppingreminder.fragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.activityViewModels
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.koffein.shoppingreminder.R
import dev.koffein.shoppingreminder.databinding.DialogEdititemBinding
import dev.koffein.shoppingreminder.models.Item
import dev.koffein.shoppingreminder.viewmodels.MainActivityViewModel

class ItemEditDialog : BottomSheetDialogFragment() {
    private lateinit var binding: DialogEdititemBinding

    private val viewModel by activityViewModels<MainActivityViewModel>()

    private var index: Int? = null

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        index = arguments?.getInt(ARGS_INDEX)

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), resources.getString(R.string.google_maps_key))
        }
    }

    // onCreateDialogでbindしているのでonCreateViewが不要?

    // 中身のDialogを作って返す部分
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialog)

        val inflater = LayoutInflater.from(requireContext())
        binding = DialogEdititemBinding.inflate(inflater)

        if (index != null) {
            // edit
            viewModel.items.value?.get(index!!)?.let {
                binding.editItemName.setText(it.name)
                binding.editItemDesc.setText(it.description)
            }
        }

        Log.d(TAG, "${parentFragmentManager.fragments}")

        (parentFragmentManager.findFragmentById(R.id.edit_item_place) as? AutocompleteSupportFragment)
            ?.apply {
                setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME))
                setCountry("JP")
                setOnPlaceSelectedListener(object : PlaceSelectionListener {
                    override fun onPlaceSelected(p0: Place) {
                        Log.i(TAG, "$p0")
                    }

                    override fun onError(p0: Status) {
                        Log.e(TAG, "error occurred: $p0")
                    }
                })
            }



        binding.editItemSend.setOnClickListener {
            if (index != null) {
                viewModel.setItem(
                    Item(
                        binding.editItemName.text.toString(),
                        binding.editItemDesc.text.toString()
                    ), index!!
                )
            }
            // null: 新規追加
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

        // 型安全にしたいなあ……
        const val ARGS_INDEX = "index"

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