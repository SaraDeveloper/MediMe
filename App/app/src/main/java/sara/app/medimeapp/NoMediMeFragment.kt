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
        setupSpinner(dialogView.findViewById(R.id.dialog_add_color_spinner), R.array.dialog_color_options)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
