package me.tiptap.tiptap.diarywriting

import android.Manifest
import android.content.pm.PackageManager
import android.databinding.DataBindingUtil
import android.databinding.ObservableField
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.location.places.Place
import com.taskail.googleplacessearchdialog.SimplePlacesSearchDialog
import com.taskail.googleplacessearchdialog.SimplePlacesSearchDialogBuilder
import gun0912.tedbottompicker.TedBottomPicker
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import me.tiptap.tiptap.R
import me.tiptap.tiptap.TipTapApplication
import me.tiptap.tiptap.common.network.DiaryApi
import me.tiptap.tiptap.common.network.ServerGenerator
import me.tiptap.tiptap.common.rx.RxBus
import me.tiptap.tiptap.common.util.GlideApp
import me.tiptap.tiptap.data.Diary
import me.tiptap.tiptap.databinding.ActivityDiaryWritingBinding
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.util.*


class DiaryWritingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiaryWritingBinding
    private lateinit var locationManager: LocationManager
    private var checkLocationOnce = true

    val isPhotoAvailable = ObservableField<Boolean>(false)

    private var imgUri: Uri? = null //image uri
    private var diary = Diary() //diary to send

    private val service: DiaryApi = ServerGenerator.createService(DiaryApi::class.java) //service
    private val rxBus = RxBus.getInstance()
    private val disposables: CompositeDisposable = CompositeDisposable()

    private val locationCode = 0
    private val storageCode = 1


    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {

            //set latitude, longitude
            diary.run {
                latitude = location.latitude.toString()
                longitude = location.longitude.toString()
            }

            val geoCoder = Geocoder(applicationContext, Locale.getDefault())

            val listAddresses = geoCoder.getFromLocation(location.latitude, location.longitude, 1)


            if (listAddresses != null && listAddresses.size > 0) {
                val locationText = listAddresses[0].getAddressLine(0)

                //이 부분 수정 필요함.
                locationText.substring(locationText.indexOf(" ")).apply {
                    binding.textWriteLocation.text = this

                    diary.location = this.replaceFirst(" ", "")
                }
            }
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_diary_writing)

        initBind(binding)

        checkBus() //넘겨진 데이터가 있는지 확인

        checkLocationPermission() //check network permission
    }


    private fun initBind(binding: ActivityDiaryWritingBinding) {
        binding.apply {
            activity = this@DiaryWritingActivity
            date = Date()

            btnBack.setOnClickListener { finish() }

            textWriteLocation.setOnClickListener { _ ->
                SimplePlacesSearchDialogBuilder(this@DiaryWritingActivity)
                        .setSearchHint(getString(R.string.place_search_hint))
                        .setLocationListener(object : SimplePlacesSearchDialog.PlaceSelectedCallback {
                            override fun onPlaceSelected(place: Place) {
                                place.name.apply {
                                    binding.textWriteLocation.text = this
                                    this@DiaryWritingActivity.diary.location = this.toString()
                                }
                            }
                        })
                        .build()
                        .show()
            }

            editWriteDiary.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {}
                override fun beforeTextChanged(str: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(str: CharSequence, p1: Int, p2: Int, p3: Int) {

                    if (str.count() > 0) {
                        binding.textComplete.setTextColor(
                                ContextCompat.getColor(this@DiaryWritingActivity, R.color.colorMainBlack))
                    } else {
                        binding.textComplete.setTextColor(
                                ContextCompat.getColor(this@DiaryWritingActivity, R.color.colorMainGray))
                    }

                    binding.textWriteKeyboard.text = getString(R.string.text_length, str.length.toString())
                }
            })
        }
    }


    private fun checkBus() {
        rxBus
                .toObservable()
                .subscribe {
                    if (it is Pair<*, *>) { //수정
                        diary = it.second as Diary
                        binding.diary = diary
                        binding.toolbarWriteTitle.text = getString(R.string.post_count, it.first.toString())
                    } else if (it is Int) { //등록
                        binding.toolbarWriteTitle.text = getString(R.string.post_count, (it + 1).toString())
                    }
                }.dispose()
    }


    /**
     * Check LocationPermission
     */
    private fun checkLocationPermission() {
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION).run {

            //if permission is not granted, ask permission to user.
            if (this != PackageManager.PERMISSION_GRANTED || checkLocationOnce) {
                ActivityCompat.requestPermissions(this@DiaryWritingActivity,
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), locationCode)
            } else {
                //if permission is granted, find current location.
                findCurrentLocation()
            }
        }
    }


    /**
     * Find current Location with LocationManager.
     */
    private fun findCurrentLocation() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            //NetworkProvider is less accurate. And If user is stay inside, gps will be not called.
            locationManager.apply {
                requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000L, 100F, locationListener)
                requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000L, 100F, locationListener)
            }
        } catch (ex: SecurityException) {
            ex.printStackTrace()
        }
    }


    /**
     * Check Read External storage permission.
     */
    private fun checkFileAccessPermission() {
        ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE).run {

            if (this != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@DiaryWritingActivity,
                        arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), storageCode)
            } else {
                openImagePicker()
            }
        }
    }


    /**
     * Request Permission callback.
     */
    override fun onRequestPermissionsResult(reqCode: Int, permissions: Array<out String>, results: IntArray) {
        when (reqCode) {
            storageCode -> {
                if (results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) {
                    openImagePicker()
                }
            }

            locationCode -> {
                if (results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) {
                    findCurrentLocation()
                }
            }
        }
    }


    /**
     * When Pick Image button is clicked.
     */
    fun onPickImgClick() {
        //Check sdk version.
        //If version code is above m, check permission.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            checkFileAccessPermission()
        } else {
            openImagePicker()
        }
    }

    /**
     * Open bottom picker.
     */
    private fun openImagePicker() {
        // 권한 있음
        TedBottomPicker.Builder(this@DiaryWritingActivity)
                .setOnImageSelectedListener { uri ->
                    imgUri = uri //set img uri
                    isPhotoAvailable.set(true)
                    applyImage(binding.imgWriteMyPicture, uri) //apply image uri to imageView
                }
                .setImageProvider { imageView, imageUri ->
                    GlideApp
                            .with(this)
                            .load(imageUri)
                            .thumbnail(0.1f)
                            .apply(RequestOptions().centerCrop())
                            .into(imageView)
                }
                .setPreviewMaxCount(7)
                .create()
                .show(supportFragmentManager)
    }


    /**
     * Apply selected img uri to ImageWriteMyPicture.
     */
    private fun applyImage(imgView: ImageView, uri: Uri) {
        GlideApp
                .with(this@DiaryWritingActivity)
                .load(uri)
                .thumbnail(0.1f)
                .into(imgView)
    }


    /**
     * When user click Complete button.
     */
    fun onCompleteButtonClick() {
        if (binding.textComplete.isClickable) {
            if (diary.id > 0) updateDiary() else writeDiary()
        }

        binding.textComplete.isClickable = false //한번만 클릭 되도록 함.
    }


    /**
     * convert object to RequestBody
     */
    private fun toRequestBody(content: String): RequestBody =
            RequestBody.create(MediaType.parse("text/plain"), content)


    /**
     * Convert image file to MultipartBody.Part
     */
    private fun toMultipartBody(uri: Uri?): MultipartBody.Part? {
        uri?.let {
            val file = File(it.path)

            val requestFile = RequestBody.create(MediaType.parse("image/*"), file)
            return MultipartBody.Part.createFormData(diary::diaryFile.name, file.name, requestFile)
        }
        return null
    }


    /**
     * Call write diary api.
     */
    private fun writeDiary() {
        disposables.add(
                service.writeDiary(
                        TipTapApplication.getAccessToken(),
                        toRequestBody(binding.editWriteDiary.text.toString()),
                        toRequestBody(diary.location),
                        toRequestBody(diary.latitude),
                        toRequestBody(diary.longitude),
                        toMultipartBody(imgUri))

                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .doOnComplete { finish() }
                        .doOnError { e -> e.printStackTrace() }
                        .subscribe { t ->
                            t.apply {
                                if (get(getString(R.string.code)).asString != "1000") { //if not successful.
                                    Log.d(getString(R.string.desc), getAsJsonObject(getString(R.string.data)).get(getString(R.string.desc)).asString)
                                }
                            }
                        })
    }


    private fun updateDiary() {
        disposables.add(
                service.updateDiary(
                        TipTapApplication.getAccessToken(),
                        toRequestBody(binding.editWriteDiary.text.toString()),
                        toRequestBody(diary.location),
                        toRequestBody(diary.latitude),
                        toRequestBody(diary.longitude),
                        toRequestBody(diary.id.toString()),
                        toMultipartBody(imgUri))

                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .doOnComplete { finish() }
                        .doOnError { e -> e.printStackTrace() }
                        .subscribe { t ->
                            t.apply {
                                if (get(getString(R.string.code)).asString != "1000") { //if not successful.
                                    Log.d(getString(R.string.desc), getAsJsonObject(getString(R.string.data)).get(getString(R.string.desc)).asString)
                                }
                            }
                        }
        )
    }


    override fun onDestroy() {
        super.onDestroy()

        disposables.dispose()
    }
}



