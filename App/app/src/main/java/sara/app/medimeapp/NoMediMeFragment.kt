package sara.app.medimeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import sara.app.medimeapp.databinding.FragmentNoMedimeBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NoMediMeFragment : Fragment() {

    private var _binding: FragmentNoMedimeBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarAdapter: CalendarAdapter
    private val displayCalendar: Calendar = Calendar.getInstance()

    private var selectedYear = -1
    private var selectedMonth = -1
    private var selectedDay = -1

    private val colorValues = intArrayOf(
        0xFFF44336.toInt(),
        0xFF2196F3.toInt(),
        0xFF4CAF50.toInt(),
        0xFFFFEB3B.toInt(),
        0xFFFF9800.toInt(),
        0xFF9C27B0.toInt(),
        0xFFE91E63.toInt(),
        0xFF009688.toInt(),
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoMedimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCalendar()
        binding.buttonAddNoMedime.setOnClickListener { showAddBox() }
    }

    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter { year, month, day ->
            selectedYear = year
            selectedMonth = month
            selectedDay = day
        }

        binding.rvCalendarDays.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rvCalendarDays.adapter = calendarAdapter

        val today = Calendar.getInstance()
        selectedYear = today.get(Calendar.YEAR)
        selectedMonth = today.get(Calendar.MONTH)
        selectedDay = today.get(Calendar.DAY_OF_MONTH)
        calendarAdapter.setSelectedDate(selectedYear, selectedMonth, selectedDay)

        updateMonthDisplay()

        binding.btnPrevMonth.setOnClickListener {
            displayCalendar.add(Calendar.MONTH, -1)
            updateMonthDisplay()
        }
        binding.btnNextMonth.setOnClickListener {
            displayCalendar.add(Calendar.MONTH, 1)
            updateMonthDisplay()
        }
    }

    private fun updateMonthDisplay() {
        val fmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvMonthYear.text = fmt.format(displayCalendar.time)
        calendarAdapter.setMonth(
            displayCalendar.get(Calendar.YEAR),
            displayCalendar.get(Calendar.MONTH)
        )
    }

    private fun showAddBox() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_box, null)
        setupSpinner(dialogView.findViewById(R.id.dialog_add_time_spinner), R.array.dialog_time_options)
        setupSpinner(dialogView.findViewById(R.id.dialog_add_till_when_spinner), R.array.dialog_till_when_options)
        val colorSpinner: Spinner = dialogView.findViewById(R.id.dialog_add_color_spinner)
        setupColorSpinner(colorSpinner)

        val freqSpinner: Spinner = dialogView.findViewById(R.id.dialog_add_time_spinner)
        val tillSpinner: Spinner = dialogView.findViewById(R.id.dialog_add_till_when_spinner)

        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (selectedDay != -1) {
                    val color = colorValues[colorSpinner.selectedItemPosition]
                    val dayInterval = dayIntervalForFrequency(freqSpinner.selectedItemPosition)
                    val totalDays = daysForDuration(tillSpinner.selectedItemPosition)
                    markRecurringDates(selectedYear, selectedMonth, selectedDay, dayInterval, totalDays, color)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun dayIntervalForFrequency(index: Int): Int = when (index) {
        8    -> 2   // Every other day
        9    -> 7   // Once a week
        10   -> 30  // Once a month
        11   -> 0   // As needed – single date only
        else -> 1   // All daily / hourly frequencies
    }

    private fun daysForDuration(index: Int): Int = when (index) {
        0 -> 7
        1 -> 14
        2 -> 30
        3 -> 90
        4 -> 180
        5 -> 365
        else -> 365 // Ongoing
    }

    private fun markRecurringDates(year: Int, month: Int, day: Int, interval: Int, totalDays: Int, color: Int) {
        val cal = Calendar.getInstance().apply { set(year, month, day) }
        if (interval <= 0) {
            calendarAdapter.markDate(year, month, day, color)
            return
        }
        var elapsed = 0
        while (elapsed <= totalDays) {
            calendarAdapter.markDate(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
                color
            )
            cal.add(Calendar.DAY_OF_MONTH, interval)
            elapsed += interval
        }
    }

    private fun setupSpinner(spinner: Spinner, optionsResId: Int) {
        val options = resources.getStringArray(optionsResId)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = adapter
    }

    private fun setupColorSpinner(spinner: Spinner) {
        val colorNames = resources.getStringArray(R.array.dialog_color_options)
        spinner.adapter = ColorSpinnerAdapter(requireContext(), colorNames, colorValues)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
