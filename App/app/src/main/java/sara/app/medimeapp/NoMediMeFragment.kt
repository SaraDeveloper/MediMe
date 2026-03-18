package sara.app.medimeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import sara.app.medimeapp.databinding.FragmentNoMedimeBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MedEntry(
    val name: String,
    val frequencyIndex: Int,
    val tillWhenIndex: Int,
    val colorIndex: Int,
    val startYear: Int,
    val startMonth: Int,
    val startDay: Int
)

class NoMediMeFragment : Fragment() {

    private var _binding: FragmentNoMedimeBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarAdapter: CalendarAdapter
    private val displayCalendar: Calendar = Calendar.getInstance()

    private var selectedYear = -1
    private var selectedMonth = -1
    private var selectedDay = -1

    private val entries = mutableListOf<MedEntry>()
    private val dateToEntries = mutableMapOf<String, MutableList<MedEntry>>()

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

            val entriesForDate = dateToEntries["$year-$month-$day"]
            if (entriesForDate != null && entriesForDate.size > 1) {
                showMedicationPicker(entriesForDate)
            } else if (entriesForDate != null && entriesForDate.size == 1) {
                showEditBox(entriesForDate[0])
            }
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

    // ---- Add new entry ----

    private fun showAddBox() {
        if (selectedDay == -1) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_box, null)
        val nameInput: EditText = dialogView.findViewById(R.id.dialog_add_name_input)
        val freqSpinner: Spinner = dialogView.findViewById(R.id.dialog_add_time_spinner)
        val tillSpinner: Spinner = dialogView.findViewById(R.id.dialog_add_till_when_spinner)
        val colorSpinner: Spinner = dialogView.findViewById(R.id.dialog_add_color_spinner)

        setupSpinner(freqSpinner, R.array.dialog_time_options)
        setupSpinner(tillSpinner, R.array.dialog_till_when_options)
        setupColorSpinner(colorSpinner)

        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val entry = MedEntry(
                    name = nameInput.text.toString(),
                    frequencyIndex = freqSpinner.selectedItemPosition,
                    tillWhenIndex = tillSpinner.selectedItemPosition,
                    colorIndex = colorSpinner.selectedItemPosition,
                    startYear = selectedYear,
                    startMonth = selectedMonth,
                    startDay = selectedDay
                )
                entries.add(entry)
                rebuildAll()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ---- Edit existing entry ----

    private fun showEditBox(entry: MedEntry) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_box, null)
        val nameInput: EditText = dialogView.findViewById(R.id.dialog_add_name_input)
        val freqSpinner: Spinner = dialogView.findViewById(R.id.dialog_add_time_spinner)
        val tillSpinner: Spinner = dialogView.findViewById(R.id.dialog_add_till_when_spinner)
        val colorSpinner: Spinner = dialogView.findViewById(R.id.dialog_add_color_spinner)

        setupSpinner(freqSpinner, R.array.dialog_time_options)
        setupSpinner(tillSpinner, R.array.dialog_till_when_options)
        setupColorSpinner(colorSpinner)

        nameInput.setText(entry.name)
        freqSpinner.setSelection(entry.frequencyIndex)
        tillSpinner.setSelection(entry.tillWhenIndex)
        colorSpinner.setSelection(entry.colorIndex)

        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setView(dialogView)
            .setPositiveButton(R.string.change) { _, _ ->
                entries.remove(entry)
                val updated = MedEntry(
                    name = nameInput.text.toString(),
                    frequencyIndex = freqSpinner.selectedItemPosition,
                    tillWhenIndex = tillSpinner.selectedItemPosition,
                    colorIndex = colorSpinner.selectedItemPosition,
                    startYear = entry.startYear,
                    startMonth = entry.startMonth,
                    startDay = entry.startDay
                )
                entries.add(updated)
                rebuildAll()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ---- Show list of overlapping medications ----

    private fun showMedicationPicker(medEntries: List<MedEntry>) {
        val names = medEntries.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle(R.string.medications_on_date)
            .setItems(names) { _, which ->
                showEditBox(medEntries[which])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ---- Rebuild all calendar marks from the entries list ----

    private fun rebuildAll() {
        dateToEntries.clear()
        val marks = mutableMapOf<String, Int>()

        for (entry in entries) {
            val color = colorValues[entry.colorIndex]
            val interval = dayIntervalForFrequency(entry.frequencyIndex)
            val totalDays = daysForDuration(entry.tillWhenIndex)

            forEachDate(entry.startYear, entry.startMonth, entry.startDay, interval, totalDays) { y, m, d ->
                val key = "$y-$m-$d"
                marks[key] = color
                dateToEntries.getOrPut(key) { mutableListOf() }.add(entry)
            }
        }

        val overlaps = dateToEntries.filter { it.value.size > 1 }.keys
        calendarAdapter.setMarkedDates(marks, overlaps)
    }

    private inline fun forEachDate(
        startY: Int, startM: Int, startD: Int,
        interval: Int, totalDays: Int,
        action: (Int, Int, Int) -> Unit
    ) {
        if (interval <= 0) {
            action(startY, startM, startD)
            return
        }
        val cal = Calendar.getInstance().apply { set(startY, startM, startD) }
        var elapsed = 0
        while (elapsed <= totalDays) {
            action(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            cal.add(Calendar.DAY_OF_MONTH, interval)
            elapsed += interval
        }
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

    // ---- Spinner helpers ----

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
