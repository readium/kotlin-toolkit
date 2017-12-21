package org.readium.r2.navigator.UserSettings

import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import org.readium.r2.navigator.R

class UpDownPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {

    var value = 0.01f
    var step = 0.01f
    var min = 0.01f
    var max = 0.01f
    var prefKey = ""
    var attrs: AttributeSet

    init {
        this.attrs = attrs
        dialogLayoutResource = R.layout.preference_up_down
        setPositiveButtonText("Ok")
        setDialogIcon(null)
        for (i in 0..attrs.attributeCount - 1){
            when (attrs.getAttributeName(i).toLowerCase()) {
                "step" -> step = attrs.getAttributeFloatValue(i, 10.0f)
                "min" -> min = attrs.getAttributeFloatValue(i, 100.0f)
                "max" -> max = attrs.getAttributeFloatValue(i, 150.0f)
                "prefkey" -> prefKey = attrs.getAttributeValue(i)
            }
        }
    }

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)
        value = preferenceManager.sharedPreferences.getString(prefKey, "100.0").toFloat()
        val buttonUp = view!!.findViewById<Button>(R.id.upButton)
        buttonUp.setOnClickListener {
            if (value + step <= max){
                value += step
                preferenceManager.sharedPreferences.edit().putString(prefKey, value.toString()).apply()
            }
            //Toast.makeText(context, value.toString(), Toast.LENGTH_SHORT).show()
        }
        val buttonDown = view.findViewById<Button>(R.id.downButton)
        buttonDown.setOnClickListener{
            if (value - step >= min){
                value -= step
                preferenceManager.sharedPreferences.edit().putString(prefKey, value.toString()).apply()
            }
            //Toast.makeText(context, value.toString(), Toast.LENGTH_SHORT).show()
        }
    }

}