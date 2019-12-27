package com.zhihu.matisse.ui.imagepreview

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.zhihu.matisse.R

/**
 * Created by eezhe on 2017-11-14.
 */
private const val ARG_MESSAGE = "ARG_MESSAGE"
class TransSupportWaitDialogFragment : DialogFragment() {
    private val TAG = TransSupportWaitDialogFragment::class.java.simpleName

    private var message: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            message = it.getString(ARG_MESSAGE)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.v(TAG, "onCreateDialog")
        val builder = AlertDialog.Builder(activity!!)
        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(R.layout.matisse_dialog_trans_wait, null)
        val tvMessage = view.findViewById<TextView>(R.id.tv_message)
        message ?. let { tvMessage.text = it }

        builder.setView(view)
        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        val win = dialog?.window
        // 一定要设置Background，如果不设置，window属性设置无效
        win ?. let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val dm = DisplayMetrics()
            activity!!.windowManager.defaultDisplay.getMetrics( dm )

            val params = it.attributes
            params.gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
            params.width =  ViewGroup.LayoutParams.WRAP_CONTENT
            params.height =  ViewGroup.LayoutParams.WRAP_CONTENT
            it.attributes = params
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(message: String) =
                TransSupportWaitDialogFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_MESSAGE, message)
                    }
                }
    }
}
