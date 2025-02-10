package com.example.absensipintar

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.absensipintar.databinding.FragmentAjukanIzinBinding

class AjukanIzinFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val b = FragmentAjukanIzinBinding.inflate(layoutInflater)



        return b.root
    }

}