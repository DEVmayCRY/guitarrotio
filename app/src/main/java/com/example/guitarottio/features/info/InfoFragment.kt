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
import android.widget.Toast


class InfoFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_info, container, false)
        val coffeeButton: Button = view.findViewById(R.id.buyCoffeeButton)
        coffeeButton.setOnClickListener {

            val urlString = "https://www.paypal.com/donate/?hosted_button_id=GSFW5XEUS525N"
            val webpage = Uri.parse(urlString)
            val intent = Intent(Intent.ACTION_VIEW, webpage)

            val chooser = Intent.createChooser(intent, "Open link with:")

            try {
                startActivity(chooser)
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to open link.", Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }
}