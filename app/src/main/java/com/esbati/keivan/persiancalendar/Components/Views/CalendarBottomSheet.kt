package com.esbati.keivan.persiancalendar.Components.Views

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.FloatingActionButton
import android.support.v4.widget.NestedScrollView
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import com.esbati.keivan.persiancalendar.POJOs.CalendarDay
import com.esbati.keivan.persiancalendar.POJOs.UserEvent
import com.esbati.keivan.persiancalendar.R
import com.esbati.keivan.persiancalendar.Utils.AndroidUtilities
import com.esbati.keivan.persiancalendar.Utils.Constants
import java.util.*

/**
 * Created by asus on 11/25/2016.
 */

class CalendarBottomSheet @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null
    ) : FrameLayout(context, attrs) {

    var mBottomSheetMode = Mode.SHEET_MODE_DATE
    var onEventListener: OnEventListener? = null

    private var mPreviousBottomSheetState: Int = 0
    private var mShouldUpdateBottomSheet: Boolean = false
    private var mShouldExpandBottomSheet: Boolean = false
    private lateinit var mSelectedDay: CalendarDay
    private var mSelectedEvent: UserEvent? = null

    //Views
    private var mBottomSheet: NestedScrollView
    private var mBottomSheetContainer: LinearLayout
    var mPersianDate: TextView
    var mGregorianDate: TextView
    private val mBottomSheetBehavior by lazy {
        BottomSheetBehavior.from<CalendarBottomSheet>(this).apply {
            setBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback(){
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    //Change BottomSheet Content if Needed
                    if(isCollapsed() && mShouldUpdateBottomSheet){
                        mShouldUpdateBottomSheet = false
                        setBottomSheetMode(mBottomSheetMode)
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        }
    }
    lateinit var eventActionBtn: FloatingActionButton

    //Event Sheet
    private lateinit var mEventTitle: TextView
    private lateinit var mEventDesc: TextView

    enum class Mode{
        SHEET_MODE_DATE,
        SHEET_MODE_EDIT_EVENT,
        SHEET_MODE_VIEW_EVENT
    }

    init {
        View.inflate(this.context, R.layout.component_bottom_sheet, this)

        mBottomSheet = findViewById(R.id.scroll_view) as NestedScrollView
        mBottomSheetContainer = findViewById(R.id.bottom_sheet_content_container) as LinearLayout
        mPersianDate = findViewById(R.id.date_shamsi) as TextView
        mGregorianDate = findViewById(R.id.date_miladi) as TextView
    }

    fun isCollapsed(): Boolean {
        return mBottomSheetContainer.height == 0 || mBottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED
    }

    fun collapse() {
        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun showDate(day: CalendarDay, expandSheet: Boolean) {
        mShouldExpandBottomSheet = expandSheet
        mSelectedDay = day

        proceedToSetupBottomSheet(Mode.SHEET_MODE_DATE)
    }

    fun showEvent(event: UserEvent) {
        mShouldExpandBottomSheet = true
        mSelectedEvent = event

        proceedToSetupBottomSheet(Mode.SHEET_MODE_VIEW_EVENT)
    }

    fun editEvent(event: UserEvent?, isEditable: Boolean) {
        mShouldExpandBottomSheet = true
        mSelectedEvent = event

        proceedToSetupBottomSheet(if (isEditable) Mode.SHEET_MODE_EDIT_EVENT else CalendarBottomSheet.Mode.SHEET_MODE_VIEW_EVENT)
    }

    @SuppressLint("SetTextI18n")
    private fun setDateSheet(day: CalendarDay, onEventClick: (UserEvent) -> Unit) {
        //Set Date
        mPersianDate.text = day.mPersianDate.persianLongDate
        val gregorianCalendar = GregorianCalendar().apply {
            time = day.mPersianDate.time
        }
        mGregorianDate.text = Constants.weekdays_en[gregorianCalendar.get(Calendar.DAY_OF_WEEK) - 1] + ", " +
                Constants.months_en[gregorianCalendar.get(Calendar.MONTH)] + " " +
                gregorianCalendar.get(Calendar.DAY_OF_MONTH) + " " +
                gregorianCalendar.get(Calendar.YEAR)

        //Set Google Calendar Events
        mBottomSheetContainer.removeAllViews()
        if (day.mEvents != null)
            for (googleEvent in day.mEvents) {
                val eventView = LayoutInflater.from(context).inflate(R.layout.cell_bottom_sheet_day, mBottomSheetContainer, false)
                val eventTitle = eventView.findViewById(R.id.event_title) as TextView

                eventView.setBackgroundResource(R.drawable.bg_calendar_today)
                if (!TextUtils.isEmpty(googleEvent.title))
                    eventTitle.text = googleEvent.title
                else
                    eventTitle.setText(R.string.event_no_title)

                eventView.setOnClickListener { onEventClick(googleEvent)}
                mBottomSheetContainer.addView(eventView)
            }

        //Set Calendar Events
        if (day.mRemarks != null && day.mRemarks.size > 0) {
            //Add header
            val eventHeader = LayoutInflater.from(context).inflate(R.layout.cell_bottom_sheet_header, mBottomSheetContainer, false)
            (eventHeader.findViewById(R.id.header_title) as TextView).text = "رویداد های روز:"
            mBottomSheetContainer.addView(eventHeader)

            //Add events
            for (calendarEvent in day.mRemarks) {
                val eventView = LayoutInflater.from(context).inflate(R.layout.cell_bottom_sheet_day, mBottomSheetContainer, false)
                val eventTitle = eventView.findViewById(R.id.event_title) as TextView

                eventTitle.text = calendarEvent.mTitle
                eventView.setBackgroundResource(if (calendarEvent.isHoliday) R.drawable.bg_calendar_holiday else R.drawable.bg_calendar_today)
                mBottomSheetContainer.addView(eventView)
            }
        }
    }

    private fun setShowEventSheet(event: UserEvent, onDeleteEvent: () -> Unit) {
        //Set Bottom Sheet
        mBottomSheetContainer.removeAllViews()

        //Set Event Title
        val eventTitle = LayoutInflater.from(context).inflate(R.layout.cell_bottom_sheet_day, mBottomSheetContainer, false)
        val eventTitleText = eventTitle.findViewById(R.id.event_title) as TextView
        val eventTitleIcon = eventTitle.findViewById(R.id.event_icon) as ImageView

        eventTitle.setBackgroundResource(R.drawable.bg_calendar_today)
        if (!TextUtils.isEmpty(event.title))
            eventTitleText.text = event.title
        else
            eventTitleText.setText(R.string.event_no_title)

        eventTitleIcon.visibility = View.VISIBLE
        eventTitleIcon.setOnClickListener {
            val dialog = AlertDialog.Builder(context)
                    //.setView(mDialogView)
                    .setTitle(resources.getString(R.string.dialog_delete_event_title))
                    .setMessage(resources.getString(R.string.dialog_delete_event_body))
                    .setNegativeButton(resources.getString(R.string.dialog_button_return), null)
                    .setPositiveButton(resources.getString(R.string.dialog_button_confirm)) { _, _ ->
                        onDeleteEvent()
                    }.create()
            AndroidUtilities.showRTLDialog(dialog)
        }

        //Set Event Description
        val eventDescription = LayoutInflater.from(context).inflate(R.layout.cell_bottom_sheet_day, mBottomSheetContainer, false)
        val eventDescTV = eventDescription.findViewById(R.id.event_title) as TextView

        eventDescription.setBackgroundResource(R.drawable.bg_calendar_today)
        if (!TextUtils.isEmpty(event.description))
            eventDescTV.text = event.description
        else
            eventDescTV.setText(R.string.event_no_desc)

        mBottomSheetContainer.addView(eventTitle)
        mBottomSheetContainer.addView(eventDescription)
    }

    private fun setEditEventSheet(event: UserEvent) {
        mBottomSheetContainer.removeAllViews()

        val eventSheet = LayoutInflater.from(context).inflate(R.layout.cell_event_sheet, mBottomSheetContainer, false)
        mEventTitle = eventSheet.findViewById(R.id.event_title) as TextView
        mEventDesc = eventSheet.findViewById(R.id.event_description) as TextView

        //Set Event in Case of Updating Available Event
        if (!TextUtils.isEmpty(event.title))
            mEventTitle.text = event.title
        else
            mEventTitle.setHint(R.string.event_no_title)

        if (!TextUtils.isEmpty(event.description))
            mEventDesc.text = event.description
        else
            mEventDesc.setHint(R.string.event_no_desc)

        mBottomSheetContainer.addView(eventSheet)
    }

    private fun proceedToSetupBottomSheet(mode: Mode) {
        //Save Current state to restore later if moving from main mBottomSheetMode
        if (mode !== Mode.SHEET_MODE_DATE && mBottomSheetMode == Mode.SHEET_MODE_DATE)
            mPreviousBottomSheetState = mBottomSheetBehavior.state

        mBottomSheetMode = mode

        //If Sheet is Flat or Collapsed Set it Up
        if (isCollapsed()) {
            if (mBottomSheetBehavior.state != BottomSheetBehavior.STATE_COLLAPSED)
                mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            setBottomSheetMode(mode)
        } else {
            //FIXME Sometimes if Item is in Settling Mode It won't Change Mode to Collapsed
            if (mBottomSheetBehavior.state == BottomSheetBehavior.STATE_SETTLING)
                Handler().postDelayed({
                    //Force Update if BottomSheet got Stuck
                    if (mShouldUpdateBottomSheet)
                        setBottomSheetMode(mode)
                }, 300)

            //If Sheet is not Collapsed, Collapse it then Set it Up
            mShouldUpdateBottomSheet = true
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun setBottomSheetMode(mode: CalendarBottomSheet.Mode) {
        when (mode) {
            CalendarBottomSheet.Mode.SHEET_MODE_DATE -> {
                setDateSheet(mSelectedDay) { googleEvent ->
                    showEvent(googleEvent)
                }

                eventActionBtn.setImageResource(R.drawable.ic_calendar_plus_white_24dp)
                eventActionBtn.setOnClickListener { editEvent(null, true) }
            }

            CalendarBottomSheet.Mode.SHEET_MODE_VIEW_EVENT -> {
                mSelectedEvent?.let {
                    setShowEventSheet(it) {
                        onEventListener?.onEventDeleted(it)
                    }
                }

                eventActionBtn.setImageResource(R.drawable.ic_pencil_white_24dp)
                eventActionBtn.setOnClickListener { editEvent(mSelectedEvent, true) }
            }

            CalendarBottomSheet.Mode.SHEET_MODE_EDIT_EVENT -> {
                val tempEvent = mSelectedEvent?.copy() ?: UserEvent(dtStart = mSelectedDay.mPersianDate.timeInMillis)
                setEditEventSheet(tempEvent)

                eventActionBtn.setImageResource(R.drawable.ic_check_white_24dp)
                eventActionBtn.setOnClickListener { view ->
                    AndroidUtilities.hideSoftKeyboard(view)
                    onEventListener?.onEventEdited(tempEvent.copy(
                            title = mEventTitle.text.toString(),
                            description = mEventDesc.text.toString()
                    ))
                }
            }
        }

        //Expand View If Needed
        Handler().postDelayed({
            //If BottomSheet is in the Date mBottomSheetMode restore any previous state if available, else just expand it
            if (mBottomSheetMode == Mode.SHEET_MODE_DATE && mPreviousBottomSheetState > 0) {
                //If BottomSheet is Stuck in Settling Set it to Collapse
                if (mPreviousBottomSheetState == BottomSheetBehavior.STATE_SETTLING)
                    mPreviousBottomSheetState = BottomSheetBehavior.STATE_COLLAPSED

                mBottomSheetBehavior.state = mPreviousBottomSheetState
                mPreviousBottomSheetState = 0
            } else if (mShouldExpandBottomSheet) {
                mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }, 200)
    }

    interface OnEventListener{
        fun onEventDeleted(deletedEvent: UserEvent)
        fun onEventEdited(editedEvent: UserEvent)
    }
}
