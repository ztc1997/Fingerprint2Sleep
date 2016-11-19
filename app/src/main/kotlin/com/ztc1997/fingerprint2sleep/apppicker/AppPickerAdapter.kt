package com.ztc1997.fingerprint2sleep.apppicker

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.ztc1997.fingerprint2sleep.R
import kotlinx.android.synthetic.main.item_app_picker.view.*
import org.jetbrains.anko.image
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.onClick

class AppPickerAdapter(val ctx: Context, apps: List<AppPickerDialog.AppInfo>, var selected: String = "", val callback: (String) -> Unit)
    : ArrayAdapter<AppPickerDialog.AppInfo>(ctx, R.layout.item_app_picker, apps) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: ctx.layoutInflater.inflate(R.layout.item_app_picker, parent, false)

        val ai = getItem(position)

        view.tv_name.text = ai.appName

        view.iv_icon.image = ai.appIcon

        view.btn_radio.isChecked = ai.value == selected

        view.onClick {
            selected = ai.value
            notifyDataSetChanged()
            callback(selected)
        }

        return view
    }
}