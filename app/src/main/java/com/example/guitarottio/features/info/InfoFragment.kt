package com.example.guitarottio.features.info

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.guitarottio.R

class InfoFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_info, container, false)
        val coffeeButton: Button = view.findViewById(R.id.buyCoffeeButton)
        coffeeButton.setOnClickListener {
            // Substitua pela sua URL
            val url = "[https://www.buymeacoffee.com/your-username](https://www.buymeacoffee.com/your-username)"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
        return view
    }
}
