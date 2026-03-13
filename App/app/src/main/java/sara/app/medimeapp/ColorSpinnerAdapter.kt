package sara.app.medimeapp

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

/**
 * Spinner adapter that shows each color name with a small colored box next to it.
 */
class ColorSpinnerAdapter(
    context: Context,
    private val colorNames: Array<String>,
    private val colorValues: IntArray
) : ArrayAdapter<String>(context, R.layout.item_color_option, colorNames) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createRow(convertView, parent, position, false)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createRow(convertView, parent, position, true)
    }

    private fun createRow(convertView: View?, parent: ViewGroup, position: Int, isDropDown: Boolean): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_color_option, parent, false)
        view.findViewById<TextView>(R.id.color_name).text = colorNames[position]
        val swatch = view.findViewById<View>(R.id.color_swatch)
        val drawable = GradientDrawable().apply {
            setColor(colorValues[position])
            cornerRadius = 4f * context.resources.displayMetrics.density
        }
        swatch.background = drawable
        return view
    }
}
