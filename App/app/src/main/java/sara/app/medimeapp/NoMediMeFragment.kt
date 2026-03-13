package sara.app.medimeapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import sara.app.medimeapp.databinding.FragmentNoMedimeBinding

/**
 * Page shown when the user taps "I don't have MediMe".
 * Use the toolbar back arrow to return.
 */
class NoMediMeFragment : Fragment() {

    private var _binding: FragmentNoMedimeBinding? = null
    private val binding get() = _binding!!

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
        binding.buttonAddNoMedime.setOnClickListener { showAddBox() }
    }

    private fun showAddBox() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_box, null)
        setupSpinner(dialogView.findViewById(R.id.dialog_add_time_spinner), R.array.dialog_time_options)
        setupSpinner(dialogView.findViewById(R.id.dialog_add_till_when_spinner), R.array.dialog_till_when_options)
        setupColorSpinner(dialogView.findViewById(R.id.dialog_add_color_spinner))
        MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
        val colorValues = intArrayOf(
            0xFFF44336.toInt(), // Red
            0xFF2196F3.toInt(), // Blue
            0xFF4CAF50.toInt(), // Green
            0xFFFFEB3B.toInt(), // Yellow
            0xFFFF9800.toInt(), // Orange
            0xFF9C27B0.toInt(), // Purple
            0xFFE91E63.toInt(), // Pink
            0xFF009688.toInt(), // Teal
        )
        spinner.adapter = ColorSpinnerAdapter(requireContext(), colorNames, colorValues)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
