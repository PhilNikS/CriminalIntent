package com.hfad.criminalintent

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.hfad.criminalintent.databinding.FragmentCrimeDetailBinding
import kotlinx.coroutines.launch
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.doOnLayout
import com.hfad.criminalintent.database.Crime
import java.io.File
import java.util.*
import kotlin.math.roundToInt

private const val DATE_FORMAT = "EEE,MMM,dd"

class CrimeDetailFragment : Fragment() {

    private var _binding: FragmentCrimeDetailBinding? = null
    private val binding get() = checkNotNull(_binding){"Cannot access binding because it is null. Is the view visible?"}
    private val args: CrimeDetailFragmentArgs by navArgs()
    private val crimeDetailViewModel: CrimeDetailViewModel by viewModels {
        CrimeDetailViewModelFactory(args.crimeId)
    }

    private val selectSuspect = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ){uri: Uri?->
        uri?.let {
            parseContactSelection(it)
        }
    }

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ){didTakePhoto->
        if(didTakePhoto&&photoName!=null){
            crimeDetailViewModel.updateCrime { oldCrime->
                oldCrime.copy(photoFileName = photoName)
            }
        }


    }
    private var photoName: String? = null
    private var _photoName:String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_crime_detail,menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.delete_crime->{
                viewLifecycleOwner.lifecycleScope.launch {
                    crimeDetailViewModel.deleteCrime(args.crimeId)
                    findNavController().navigate(CrimeDetailFragmentDirections.returnToCrimeList())
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentCrimeDetailBinding.inflate(inflater,container,false)

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        binding.apply {
            crimeTitle.doOnTextChanged{text,_,_,_ ->
                crimeDetailViewModel.updateCrime { oldCrime->
                    oldCrime.copy(title = text.toString())
                }
            }

            crimeSolved.setOnCheckedChangeListener{_, isChecked ->
                crimeDetailViewModel.updateCrime { oldCrime ->
                    oldCrime.copy(isSolved = isChecked)
                }
            }

            chooseSuspect.setOnClickListener{
                selectSuspect.launch(null)
            }
            val selectSuspectIntent = selectSuspect.contract.createIntent(requireContext(), null)
            chooseSuspect.isEnabled = canResolveIntent(selectSuspectIntent)


            crimeCamera.setOnClickListener{
                _photoName = "IMG_${Date()}.JPG"
                photoName = _photoName
                val photoFile = File(requireContext().applicationContext.filesDir,photoName)
                val photoUri = FileProvider.getUriForFile(requireContext(),"com.hfad.criminalintent.fileprovider",photoFile)
                takePhoto.launch(photoUri)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                crimeDetailViewModel.crime.collect{crime ->

                    crime?.let { updateUi(it) }

                }
            }
        }
        setFragmentResultListener(DataPickerFragment.REQUEST_KEY_DATE){_, bundle ->
            val newDate = bundle.getSerializable(DataPickerFragment.BUNDLE_KEY_DATE) as Date
            crimeDetailViewModel.updateCrime { it.copy(date = newDate) }
        }

    }


    private fun updateUi(crime: Crime){
        binding.apply {
            if(crimeTitle.text.toString() != crime.title) crimeTitle.setText(crime.title)
            crimeDate.text = crime.date.toString()
            crimeDate.setOnClickListener {
                findNavController().navigate(CrimeDetailFragmentDirections.selectDate(crime.date))
            }

            crimeSolved.isChecked = crime.isSolved

            crimePhoto.setOnClickListener{
                crime.photoFileName?.let {
                    findNavController().navigate(CrimeDetailFragmentDirections.zoomOnCrime(it))
                }


            }

            sendReport.setOnClickListener{
                val reportIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT,createCrimeReport(crime))
                    putExtra(Intent.EXTRA_SUBJECT,getString(R.string.crime_report_subject))
                }
                val chooserIntent = Intent.createChooser(reportIntent, getString(R.string.crime_report_subject))
                startActivity(chooserIntent)
            }

            chooseSuspect.text = crime.suspect.ifEmpty {
                getString(R.string.choose_suspect_button)
            }

            updatePhoto(crime.photoFileName)
        }
    }

    private fun createCrimeReport(crime: Crime):String{
        val solvedString = if(crime.isSolved){
            getString(R.string.crime_solved)
        }
        else{ getString(R.string.crime_unsolved)}

        val dateString = DateFormat.format(DATE_FORMAT,crime.date).toString()

        val suspectText = if(crime.suspect.isBlank()){ getString(R.string.crime_report_no_suspect) }
        else{ getString(R.string.crime_report_suspect,crime.suspect) }

        return getString(R.string.crime_report,crime.title,dateString,solvedString,suspectText)

    }

    private fun parseContactSelection(contactUri: Uri){
        val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)

        val queryCursor = requireActivity().contentResolver
            .query(contactUri,queryFields,null,null,null)

        queryCursor?.use {cursor ->
            if(cursor.moveToFirst()){
                val suspect = cursor.getString(0)
                crimeDetailViewModel.updateCrime { oldCrime->
                    oldCrime.copy(suspect = suspect)
                }
            }
        }
    }

    private fun canResolveIntent(intent:Intent):Boolean{
        val packageManager:PackageManager = requireActivity().packageManager
        val resolvedActivity: ResolveInfo?= packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolvedActivity!=null
    }
    private fun getScaledBitMap(path:String?,destWidth:Int,destHeight:Int): Bitmap {
        //Read the dimensions  of the image on disk
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path,options)

        val srcWidth = options.outWidth.toFloat()
        val srcHeight = options.outHeight.toFloat()

        //Figure out how much to scale down
        val sampleSize = if (srcHeight <= destHeight && srcWidth <= destWidth) {
            1
        } else {
            val heightScale = srcHeight / destHeight
            val widthScale = srcWidth / destWidth

            minOf(heightScale, widthScale).roundToInt()
        }
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        })
    }
    private fun updatePhoto(photoFileName:String?){
        if(binding.crimePhoto.tag!= photoFileName&&photoFileName!=null){
            val photoFile = photoFileName?.let {
                File(requireContext().applicationContext.filesDir,it)
            }
            if(photoFile?.exists() == true){
                binding.crimePhoto.doOnLayout {measuredView ->
                    val scaledBitmap= getScaledBitMap(
                        photoFile.path,
                        measuredView.width,
                        measuredView.height
                    )
                    binding.crimePhoto.setImageBitmap(scaledBitmap)
                    binding.crimePhoto.tag = photoFileName
                }
            }else{
                binding.crimePhoto.setImageBitmap(null)
                binding.crimePhoto.tag = null
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}