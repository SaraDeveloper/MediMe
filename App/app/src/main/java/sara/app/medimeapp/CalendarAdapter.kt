package sara.app.medimeapp

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class CalendarAdapter(
    private val onDayClick: (year: Int, month: Int, day: Int) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    private data class DayCell(val year: Int, val month: Int, val day: Int)

    private var cells: List<DayCell?> = emptyList()
    private var selectedYear = -1
    private var selectedMonth = -1
    private var selectedDay = -1
    private val markedDates = mutableMapOf<String, Int>()

    fun setMonth(year: Int, month: Int) {
        val cal = Calendar.getInstance().apply { set(year, month, 1) }
        val firstDow = cal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val list = mutableListOf<DayCell?>()
        repeat(firstDow) { list.add(null) }
        for (d in 1..daysInMonth) list.add(DayCell(year, month, d))
        while (list.size < 42) list.add(null)
        cells = list
        notifyDataSetChanged()
    }

    fun setSelectedDate(year: Int, month: Int, day: Int) {
        selectedYear = year
        selectedMonth = month
        selectedDay = day
        notifyDataSetChanged()
    }

    fun markDate(year: Int, month: Int, day: Int, color: Int) {
        markedDates[dateKey(year, month, day)] = color
        notifyDataSetChanged()
    }

    override fun getItemCount() = cells.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = DayViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
    )

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val cell = cells[position]
        val tv = holder.dayText

        if (cell == null) {
            tv.text = ""
            tv.background = null
            tv.isClickable = false
            tv.setOnClickListener(null)
            return
        }

        tv.text = cell.day.toString()

        val today = Calendar.getInstance()
        val isToday = cell.year == today.get(Calendar.YEAR) &&
                cell.month == today.get(Calendar.MONTH) &&
                cell.day == today.get(Calendar.DAY_OF_MONTH)
        tv.setTypeface(null, if (isToday) Typeface.BOLD else Typeface.NORMAL)

        val isSelected = cell.year == selectedYear &&
                cell.month == selectedMonth &&
                cell.day == selectedDay

        val markedColor = markedDates[dateKey(cell.year, cell.month, cell.day)]
        val dp = tv.context.resources.displayMetrics.density

        tv.background = when {
            markedColor != null -> GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (isSelected) 0x332196F3 else Color.TRANSPARENT)
                setStroke((2.5f * dp).toInt(), markedColor)
            }
            isSelected -> GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x332196F3)
            }
            else -> null
        }

        tv.setOnClickListener {
            setSelectedDate(cell.year, cell.month, cell.day)
            onDayClick(cell.year, cell.month, cell.day)
        }
    }

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayText: TextView = view.findViewById(R.id.day_text)
    }

    private fun dateKey(y: Int, m: Int, d: Int) = "$y-$m-$d"
}
