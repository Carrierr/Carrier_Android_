package me.tiptap.tiptap.preview

import android.app.Dialog
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import android.view.*
import com.google.gson.JsonObject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import me.tiptap.tiptap.R
import me.tiptap.tiptap.TipTapApplication
import me.tiptap.tiptap.common.network.DiaryApi
import me.tiptap.tiptap.common.network.ServerGenerator
import me.tiptap.tiptap.common.rx.RxBus
import me.tiptap.tiptap.common.util.preview.PreviewDialogNavigator
import me.tiptap.tiptap.data.Diary
import me.tiptap.tiptap.data.InvalidDiary
import me.tiptap.tiptap.databinding.FragmentDialogPreviewBinding
import me.tiptap.tiptap.diarywriting.DiaryWritingActivity

class PreviewDialogFragment: DialogFragment() {

    private lateinit var binding: FragmentDialogPreviewBinding

    private val rxBus = RxBus.getInstance()
    private val service = ServerGenerator.createService(DiaryApi::class.java)
    private val disposable = CompositeDisposable()

    private lateinit var data: Diary

    var previewDialogNavi : PreviewDialogNavigator?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    private fun checkBus() {
        rxBus.toObservable().subscribe {
            if (it is Pair<*,*>) {
                binding.idx = it.first as Int
                binding.diary = it.second as Diary
                data = it.second as Diary
            }
        }.dispose()
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        //For full screen dialog.
        val root: ConstraintLayout = ConstraintLayout(activity).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        return Dialog(activity, R.style.PreviewDialogAnimation).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(root)

            window.run {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_dialog_preview, container, false)

        initToolbar()
        checkBus()

        return binding.root
    }


    private fun initToolbar() {
        (activity as AppCompatActivity).apply {
            setSupportActionBar(binding.toolbarPreview)

            supportActionBar?.apply {
                setDisplayShowTitleEnabled(false)
                setHomeAsUpIndicator(R.drawable.ic_back_white)
                setDisplayHomeAsUpEnabled(true)
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        if (menu?.size() == 0) { //메뉴가 여러 번 생성되지 않도록 방지함.
            inflater?.inflate(R.menu.menu_preview, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.menu_preview_edit -> {
                    rxBus.takeBus(Pair(binding.idx , data)) //send be edited Diary.
                    startActivity(Intent(activity, DiaryWritingActivity::class.java))
                    dialog.dismiss()
                    true
                }
                R.id.menu_preview_delete -> {
                    previewDialogNavi?.onDialogDeleteStart()
                    deleteDiaryById()
                    true
                }
                else -> false
            }


    private fun deleteDiaryById() {
        disposable.add(
                service.deleteDiaryById(TipTapApplication.getAccessToken(), InvalidDiary(mutableListOf(data.id)))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribeWith(object : DisposableObserver<JsonObject>() {
                            override fun onNext(t: JsonObject) {
                                //
                            }

                            override fun onComplete() {
                                previewDialogNavi?.onDialogDeleteComplete()
                                dialog.dismiss()
                            }

                            override fun onError(e: Throwable) {
                                e.printStackTrace()
                            }
                        })
        )
    }


    override fun onDestroy() {
        super.onDestroy()

        disposable.dispose()
    }
}