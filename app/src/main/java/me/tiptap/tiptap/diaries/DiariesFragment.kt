package me.tiptap.tiptap.diaries

import android.content.Intent
import android.databinding.DataBindingUtil
import android.databinding.ObservableBoolean
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import me.tiptap.tiptap.R
import me.tiptap.tiptap.TipTapApplication
import me.tiptap.tiptap.common.network.DiaryApi
import me.tiptap.tiptap.common.network.ServerGenerator
import me.tiptap.tiptap.common.rx.RxBus
import me.tiptap.tiptap.data.DiariesResponse
import me.tiptap.tiptap.data.InvalidDiaries
import me.tiptap.tiptap.databinding.FragmentDiariesBinding
import me.tiptap.tiptap.diarydetail.DiaryDetailActivity
import java.util.*

class DiariesFragment : Fragment() {

    private lateinit var binding: FragmentDiariesBinding

    private val adapter = DiariesAdapter()

    private val service = ServerGenerator.createService(DiaryApi::class.java)
    private val rxBus = RxBus.getInstance()
    private val disposables = CompositeDisposable()

    private var totalPage = 0 //total page

    val isBotDialogVisible = ObservableBoolean(false)
    val isDateRangeAvailable = ObservableBoolean(false)
    val isDiaryExist = ObservableBoolean(false)


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_diaries, container, false)
        binding.fragment = this

        return binding.root
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initToolbar()
        initRecyclerView()
    }


    private fun initToolbar() {
        (activity as AppCompatActivity).apply {
            setSupportActionBar(binding.toolbarDiaries?.toolbarDiaries)

            supportActionBar?.apply {
                setDisplayShowTitleEnabled(false)
            }
        }
    }

    private fun initRecyclerView() {
        binding.recyclerDiaries.apply {
            layoutManager = LinearLayoutManager(this@DiariesFragment.context)
            adapter = this@DiariesFragment.adapter.apply {
                setHasStableIds(true)
            }

            setHasFixedSize(true)
            (itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false

            initRecyclerViewEvent()
        }
    }

    private fun initRecyclerViewEvent() {
        adapter.apply {
            disposables.addAll(
                    clickSubject.subscribe {
                        if (!this.isCheckboxAvailable.get()) {   //Go to detail page if delete mode is not running.
                            rxBus.takeBus(it) //send Diary's date to DiaryDetailActivity.
                            startActivity(Intent(this@DiariesFragment.activity, DiaryDetailActivity::class.java))
                        }
                    },
                    longClickSubject.subscribe {
                        this.changeDeleteModeState(it)
                        isBotDialogVisible.set(it) //change bottom dialog visibility
                    },
                    checkSubject.subscribe {
                        onCheckedChangeEventPublished(it)
                    }
            )
        }
    }

    private fun checkBus() {
        rxBus.toObservable()
                .subscribe {
                    if (it is ArrayList<*>) {
                        val startDate = it[0].toString()
                        val endDate = it[1].toString()

                        isDateRangeAvailable.set(true)
                        binding.layoutBotRange?.textBotRange?.text = getString(R.string.date_range, startDate, endDate)

                        getDiariesByDate(startDate, endDate)

                    } else if (it is Boolean) {
                        getDiaries(1, 2, it)
                    }
                }
                .dispose()
    }

    /**
     * Get diaries by date range.
     */
    private fun getDiariesByDate(startDate: String, endDate: String) {
        disposables.add(
                service.getDiariesByDate(
                        TipTapApplication.getAccessToken(), startDate, endDate)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnError { e -> e.printStackTrace() }
                        .subscribe { t ->
                            if (t.code == "1000") {
                                val list = t.data.list

                                if (list.isNotEmpty()) {
                                    isDiaryExist.set(true)

                                    for (monthDiary in list.iterator()) {
                                        if (monthDiary.diariesOfDay != null) {
                                            adapter.updateItems(monthDiary.diariesOfDay) //update items
                                        }
                                    }
                                }
                            }
                        }
        )
    }

    private fun getDiaries(page: Int, limit: Int, isDataNotAdded: Boolean) {
        disposables.add(
                service.getDiaries(TipTapApplication.getAccessToken(), page, limit) //한번에 하나의 달만 불러올거.
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribeWith(object : DisposableObserver<DiariesResponse>() {
                            override fun onNext(t: DiariesResponse) {
                                if (t.code == "1000") {
                                    totalPage = t.data.total

                                    val list = t.data.list

                                    if (list.isNotEmpty()) {
                                        isDiaryExist.set(true)

                                        for (monthDiary in list.iterator()) {
                                            if (monthDiary.diariesOfDay != null)
                                                if (!isDataNotAdded && adapter.itemCount != 0) { //금일에 추가된 일기가 있고, 한번 리스트를 로드한 적이 있다면,
                                                    adapter.addItemOnTop(monthDiary.diariesOfDay[0])
                                                    return
                                                } else {
                                                    adapter.addItems(monthDiary.diariesOfDay) //add items
                                                }
                                        }
                                    }
                                }
                            }

                            override fun onComplete() {
                                adapter.notifyDataSetChanged()
                            }

                            override fun onError(e: Throwable) {
                                e.printStackTrace()
                            }
                        })
        )
    }


    fun onDateFindButtonClick() {
        adapter.changeDeleteModeState(false)

        if (!isDiaryExist.get()) return else startActivity(Intent(this@DiariesFragment.activity, CalendarActivity::class.java))
    }


    fun onDateClearButtonClick() {
        isDateRangeAvailable.set(false)

        adapter.deleteAllItems()
        getDiaries(1, 2, true)
    }


    /**
     *  When click bottom dialog button
     */
    fun onBottomDialogButtonClick(view: View) {
        when (view.id) {
            R.id.text_bot_dial_cancel -> {
                //clicked cancel
            }
            R.id.text_bot_dial_delete -> {
                adapter.apply {
                    deleteCheckedItems() //delete items from adapter's dataSet
                    this@DiariesFragment.deleteDiaries(this.checkedDataSet) //call deleteDiary api
                }
            }
        }

        adapter.apply {
            isCheckboxAvailable.set(false) //change checkbox state
            changeCheckboxState(false)
        }

        isBotDialogVisible.set(false) //hide bottom dialog
    }


    /**
     * call delete diary api
     */
    private fun deleteDiaries(dataSet: MutableList<Date>) {
        val invalidDiaries = InvalidDiaries().apply {
            convertDateToString(dataSet)
        }

        disposables.add(
                service.deleteDiaryByDay(TipTapApplication.getAccessToken(), invalidDiaries)
                        .subscribeOn(Schedulers.io())
                        .doOnError { e -> e.printStackTrace() }
                        .doOnComplete { if (adapter.itemCount == 0) isDiaryExist.set(false) }
                        .subscribe { t ->
                            t.apply {
                                if (get(getString(R.string.code)).asString != "1000") { //if not successful.
                                    Log.d(getString(R.string.desc), getAsJsonObject(getString(R.string.data)).get(getString(R.string.desc)).asString)
                                }
                            }
                        })
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        if (isVisibleToUser) {
            if (!isDateRangeAvailable.get()) {
                if (disposables.size() == 0) {
                    initRecyclerViewEvent()
                }

                checkBus()
            }

        } else {
            resetAllMode()
            disposables.clear()
        }
    }


    private fun resetAllMode() {
        adapter.changeDeleteModeState(false)
        isBotDialogVisible.set(false)

        isDateRangeAvailable.set(false)
    }


    override fun onResume() {
        super.onResume()

        userVisibleHint = true

        if (disposables.size() == 0) {
            initRecyclerViewEvent()
        }
    }

    override fun onPause() {
        super.onPause()

        resetAllMode()
        disposables.clear()
    }

    override fun onDestroy() {
        super.onDestroy()

        //Dispose subjects to prevent memory leak.
        disposables.dispose()
    }
}