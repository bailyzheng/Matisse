package com.zhihu.matisse.ui.imagepreview

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.PopupWindow
import com.zhihu.matisse.R

/**
 * Created by zzw on 2017/3/30.
 */

class QMListPopupWindow(protected var context: Context, val topWindow: Window, val ancherView: View, val datas: List<String>, w: Int, h: Int, val listener: AdapterView.OnItemClickListener?) {
    val TAG = QMListPopupWindow::class.java.simpleName
    var contentView: View
        protected set
    var popupWindow: PopupWindow
        protected set
    lateinit var dataList: ListView

    init {
        contentView = LayoutInflater.from(context).inflate(R.layout.popup_list_matisse, null)
        initView()
        initEvent()
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        popupWindow = PopupWindow(contentView, w, h, true)

        initWindow()
    }

    fun initView() {
        val view = contentView
        dataList = view.findViewById(R.id.data_list) as ListView
        dataList.adapter = ArrayAdapter(context, R.layout.item_popup_list_matisse, datas)
    }
    fun initEvent() {
        dataList.onItemClickListener = listener
        dataList.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            listener?.onItemClick(parent, view, position, id)
            popupWindow.dismiss()
        }
    }
    protected fun initWindow() {
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true
        popupWindow.isTouchable = true
        val instance = popupWindow
        instance.setOnDismissListener {
            val lp = topWindow.attributes
            lp.alpha = 1.0f
            topWindow.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            topWindow.attributes = lp
        }
    }

    fun showBashOfAnchor(anchor: View, layoutGravity: LayoutGravity, xmerge: Int, ymerge: Int) {
        val offset = layoutGravity.getOffset(anchor, popupWindow)
        popupWindow.showAsDropDown(anchor, offset[0] + xmerge, offset[1] + ymerge)
    }

    fun showAsDropDown(anchor: View, xoff: Int, yoff: Int) {
        popupWindow.showAsDropDown(anchor, xoff, yoff)
    }

    fun showAtLocation(parent: View, gravity: Int, x: Int, y: Int) {
        popupWindow.showAtLocation(parent, gravity, x, y)
        dimTopWindow()
    }

    fun showAncherTopLocation(ancherView: View) {
        val location: IntArray = intArrayOf(0, 0)
        ancherView.getLocationOnScreen(location)

        popupWindow.showAtLocation(ancherView, Gravity.NO_GRAVITY, location[0], location[1] - contentView.measuredHeight * 2)
    }

    fun dimTopWindow() {
        val lp = topWindow.attributes
        lp.alpha = 0.3f
        topWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        topWindow.attributes = lp
    }

    fun show() {
        val win = popupWindow
        win.animationStyle = R.style.animTranslate
        showAncherTopLocation(ancherView)
//        showAtLocation(contentView, Gravity.BOTTOM, 0, 0)
    }

    class LayoutGravity(var layoutGravity: Int) {

        val horiParam: Int
            get() {
                var i = 0x1
                while (i <= 0x100) {
                    if (isParamFit(i))
                        return i
                    i = i shl 2
                }
                return ALIGN_LEFT
            }

        val vertParam: Int
            get() {
                var i = 0x2
                while (i <= 0x200) {
                    if (isParamFit(i))
                        return i
                    i = i shl 2
                }
                return TO_BOTTOM
            }

        fun setHoriGravity(gravity: Int) {
            layoutGravity = layoutGravity and 0x2 + 0x8 + 0x20 + 0x80 + 0x200
            layoutGravity = layoutGravity or gravity
        }

        fun setVertGravity(gravity: Int) {
            layoutGravity = layoutGravity and 0x1 + 0x4 + 0x10 + 0x40 + 0x100
            layoutGravity = layoutGravity or gravity
        }

        fun isParamFit(param: Int): Boolean {
            return layoutGravity and param > 0
        }

        fun getOffset(anchor: View, window: PopupWindow): IntArray {
            val anchWidth = anchor.width
            val anchHeight = anchor.height

            var winWidth = window.width
            var winHeight = window.height
            val view = window.contentView
            if (winWidth <= 0)
                winWidth = view.width
            if (winHeight <= 0)
                winHeight = view.height

            var xoff = 0
            var yoff = 0

            when (horiParam) {
                ALIGN_LEFT -> xoff = 0
                ALIGN_RIGHT -> xoff = anchWidth - winWidth
                TO_LEFT -> xoff = -winWidth
                TO_RIGHT -> xoff = anchWidth
                CENTER_HORI -> xoff = (anchWidth - winWidth) / 2
                else -> {
                }
            }
            when (vertParam) {
                ALIGN_ABOVE -> yoff = -anchHeight
                ALIGN_BOTTOM -> yoff = -winHeight
                TO_ABOVE -> yoff = -anchHeight - winHeight
                TO_BOTTOM -> yoff = 0
                CENTER_VERT -> yoff = (-winHeight - anchHeight) / 2
                else -> {
                }
            }
            return intArrayOf(xoff, yoff)
        }

        companion object {
            // waring, don't change the order of these constants!
            val ALIGN_LEFT = 0x1
            val ALIGN_ABOVE = 0x2
            val ALIGN_RIGHT = 0x4
            val ALIGN_BOTTOM = 0x8
            val TO_LEFT = 0x10
            val TO_ABOVE = 0x20
            val TO_RIGHT = 0x40
            val TO_BOTTOM = 0x80
            val CENTER_HORI = 0x100
            val CENTER_VERT = 0x200
        }
    }
}
