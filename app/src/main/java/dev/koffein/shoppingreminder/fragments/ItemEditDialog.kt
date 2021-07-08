package dev.koffein.shoppingreminder.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.koffein.shoppingreminder.R
import dev.koffein.shoppingreminder.databinding.DialogEdititemBinding
import dev.koffein.shoppingreminder.viewmodels.MainActivityViewModel

class ItemEditDialog : BottomSheetDialogFragment() {
    private lateinit var binding: DialogEdititemBinding

    private val viewModel by activityViewModels<MainActivityViewModel>()

    private var index: Int? = null

    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        index = arguments?.getInt(ARGS_INDEX)
    }

    // onCreateDialogでbindしているのでonCreateViewが不要?

    // 中身のDialogを作って返す部分
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialog)

        val inflater = LayoutInflater.from(requireContext())
        binding = DialogEdititemBinding.inflate(inflater)

        if (index == null) {
            // create
        } else {
            // edit
            viewModel.items.value?.get(index!!)?.let {
                binding.editItemName.setText(it.name)
                binding.editItemDesc.setText(it.description)
            }
        }

        dialog.setContentView(binding.root)

        return dialog
    }

    companion object {
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