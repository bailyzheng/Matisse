package com.zhihu.matisse.ui.imagepreview

import android.app.Activity
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.model.SelectedItemCollection
import com.zhihu.matisse.ui.imagepreview.ItemTouchHelper.IItemTouchHelperAdapter
import com.zhihu.matisse.ui.imagepreview.ItemTouchHelper.OnStartDragListener

class ImageRVAdapter(val activity: Activity, private val mSelectedCollection: SelectedItemCollection, val dragListener: OnStartDragListener, val handler: Handler?) : RecyclerView.Adapter<ImageRVAdapter.ImageHolder>(), IItemTouchHelperAdapter {
    val TAG = ImageRVAdapter::class.java.simpleName

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
        return ImageHolder(LayoutInflater.from(activity).inflate(R.layout.item_image_matisse, parent, false))
    }

    override fun getItemCount(): Int {
        return mSelectedCollection.currentMaxSelectable()
    }

    private fun getListSize(list: List<ImageInfoItem>?): Int {
        return list?.size ?: 0
    }

    override fun onBindViewHolder(holder: ImageHolder, position: Int) {
        holder.tvIndexView.text = (position + 1).toString()

        val item = if (position < mSelectedCollection.count()) mSelectedCollection.asList()[position] else null

        val options = RequestOptions()
                .transform(CenterCrop(), RoundedCorners(10))
                .override(200)
        if (item != null) {
            Log.v(TAG, "item != null")
            Glide.with(activity)
                    .load(item.uri)
                    .apply(options)
                    .into(holder.imageView)
        } else if (position == mSelectedCollection.count()) {
            Glide.with(activity)
                    .load(R.drawable.bg_to_choose_next)
                    .apply(options)
                    .into(holder.imageView)
        } else {
            Log.v(TAG, "item == null")
            Glide.with(activity)
                    .load(R.drawable.bg_to_choose)
                    .apply(options)
                    .into(holder.imageView)
        }

        if (item != null) {
            holder.btnDel.visibility = View.VISIBLE
        } else {
            holder.btnDel.visibility = View.INVISIBLE
        }

        holder.btnDel.setOnClickListener {
            mSelectedCollection.remove(item)
            notifyItemRemoved(position)
        }

//        holder.imageView.setOnClickListener {
//            val menus: MutableList<String> = mutableListOf("裁剪", "替换")
//            val listMenuWindow = QMListPopupWindow(activity, activity.window, it, menus, 100,/*TDevice.dip2px(activity, 60f)*/ ViewGroup.LayoutParams.WRAP_CONTENT, AdapterView.OnItemClickListener { parent, view, pos, id ->
//                when (pos) {
//                    0 -> {
//                        Log.v(TAG, "send crop message - position($position)")
//                        val message = Message.obtain()
//                        message.what = 0
//                        message.arg1 = position
//                        handler?.sendMessage(message)
//                    }
//                    1 -> {
//                        Log.v(TAG, "send pick message")
//                        val message = Message.obtain()
//                        message.what = 1
//                        message.arg1 = position
//                        handler?.sendMessage(message)
//                    }
//                }
//            })
//
//            listMenuWindow.show()
//        }
//        holder.imageView.setOnLongClickListener { _ ->
//            dragListener.onStartDrag(holder)
//            true
//        }
    }

    class ImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById(R.id.iv_selected_image)
        var btnDel: ImageView = itemView.findViewById(R.id.iv_selected_del)
        var tvIndexView: TextView = itemView.findViewById(R.id.tv_index)
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        Log.v(TAG, "onItemMove")
//        val idList = list.map { it.id }
//        Collections.swap(list, fromPosition, toPosition)
//        for (i in list.indices) {
//            list[i].id = idList[i]
//        }
//        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onItemDismiss(position: Int) {
        Log.v(TAG, "onItemDismiss")
    }

    override fun onSelectedChanged(actionState: Int) {
        Log.v(TAG, "onSelectedChanged")
        if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            notifyDataSetChanged()
        }
    }
}