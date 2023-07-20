package com.hfad.criminalintent

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hfad.criminalintent.database.Crime
import com.hfad.criminalintent.databinding.ListItemCrimeBinding
import java.util.*

class CrimeListAdapter(
    private val crimes: List<Crime>,
    private val onCrimeClicked:(crimeId: UUID)-> Unit
): RecyclerView.Adapter<CrimeHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemCrimeBinding.inflate(inflater,parent,false)
        return CrimeHolder(binding)

    }
    override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
        val crime = crimes[position]

        holder.bind(crime,onCrimeClicked)
    }

    override fun getItemCount() = crimes.size



}