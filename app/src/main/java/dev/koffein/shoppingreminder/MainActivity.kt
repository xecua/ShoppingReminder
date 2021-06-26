package dev.koffein.shoppingreminder

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dev.koffein.shoppingreminder.databinding.ActivityMainBinding
import dev.koffein.shoppingreminder.viewmodels.MainActivityViewModel
import net.matsudamper.viewbindingutil.bindViewBinding

class MainActivity  : AppCompatActivity(R.layout.activity_main) {
    private val binding by bindViewBinding<ActivityMainBinding>()

    private val viewModel by viewModels<MainActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.mainText.text = "foo"
    }

}