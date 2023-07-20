package com.hfad.criminalintent

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.hfad.criminalintent.database.Crime
import com.hfad.criminalintent.databinding.ListItemCrimeBinding
import java.util.UUID

class CrimeHolder(
    private val binding: ListItemCrimeBinding
    ): RecyclerView.ViewHolder(binding.root){
        fun bind(crime: Crime, onCrimeClicked:(crimeId:UUID)->Unit){
            binding.crimeTitle.text = crime.title
            binding.crimeDate.text = crime.date.toString()

            binding.root.setOnClickListener {
                onCrimeClicked(crime.id)
            }
            binding.crimeSolved.visibility = if(crime.isSolved) {
                View.VISIBLE
            }
            else{
                View.GONE
            }
        }
}