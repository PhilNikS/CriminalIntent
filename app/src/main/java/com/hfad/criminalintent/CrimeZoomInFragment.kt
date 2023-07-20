package com.hfad.criminalintent

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.hfad.criminalintent.databinding.FragmentCrimeZoomInBinding
import java.io.File
import kotlin.math.roundToInt

class CrimeZoomInFragment (): Fragment() {
    private val args: CrimeZoomInFragmentArgs by navArgs()
    private var _binding:FragmentCrimeZoomInBinding? = null
    private val binding get() = checkNotNull(_binding){"Is binding exist?"}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentCrimeZoomInBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val path = args.photoPath
        val photoFile = path.let {
            File(requireContext().applicationContext.filesDir,it)
        }
        if(photoFile.exists()) {
            binding.crimeImage.doOnLayout { measure ->
                val photoBitmap = createBitmap(photoFile.path, measure.width, measure.height)
                binding.crimeImage.setImageBitmap(photoBitmap)
            }
        }



    }
    private fun createBitmap(path:String, destWidth:Int,destHeight:Int): Bitmap {

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path,options)

        val srcWidth = options.outWidth.toFloat()
        val srcHeight = options.outHeight.toFloat()

        val sampleSize = if(srcWidth<=destWidth&&srcHeight<=destHeight){
            1
        }else{
            val heightScale = srcHeight/destHeight
            val widthScale = srcWidth/destWidth

            minOf(heightScale,widthScale).roundToInt()
        }
        return BitmapFactory.decodeFile(path,BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        })

    }

}