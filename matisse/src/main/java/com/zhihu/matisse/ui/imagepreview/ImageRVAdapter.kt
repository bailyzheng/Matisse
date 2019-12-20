package com.zhihu.matisse.ui.imagepreview

import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.model.SelectedItemCollection
import com.zhihu.matisse.ui.imagepreview.ItemTouchHelper.IItemTouchHelperAdapter
import com.zhihu.matisse.ui.imagepreview.ItemTouchHelper.OnStartDragListener
import java.io.File

class ImageRVAdapter(val activity: FragmentActivity, val fragment: Fragment, private val mSelectedCollection: SelectedItemCollection, val dragListener: OnStartDragListener, val handler: Handler?) : RecyclerView.Adapter<ImageRVAdapter.ImageHolder>()/*, IItemTouchHelperAdapter*/ {
    val TAG = ImageRVAdapter::class.java.simpleName

    val cropFragments = arrayOfNulls<Fragment>(mSelectedCollection.currentMaxSelectable())
    val views = arrayOfNulls<View>(mSelectedCollection.currentMaxSelectable())

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
            if (cropFragments[position] == null) {
                val uCrop = UCrop.of(item.contentUri, Uri.fromFile(File(activity.cacheDir, "ucropout")))
                val options = UCrop.Options()
                options.setHideBottomControls(true)
                options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL)
                uCrop.withOptions(options)
                cropFragments[position] = uCrop.getFragment(uCrop.getIntent(activity).extras)

                holder.fragment = cropFragments[position]
            } else {
                holder.fragment = cropFragments[position]
            }
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
        var flContainer: FrameLayout = itemView.findViewById(R.id.fragment_container)
        var fragment: Fragment? = null
    }

//    override fun onViewDetachedFromWindow(holder: ImageHolder) {
//        super.onViewDetachedFromWindow(holder)
//        val position = holder.adapterPosition
//        Log.e(TAG, "onViewDetachedFromWindow: position is ${position}, id is ${holder.flContainer.id}")
//        if (position < 0) {
//            return
//        }
//        cropFragments[position] ?. let {
//            if (!it.isAdded) {
//                fragment.childFragmentManager.beginTransaction()
//                        .remove(it)
//                        .commitAllowingStateLoss()
//            }
//        }
//
//    }

    override fun onViewAttachedToWindow(holder: ImageHolder) {
        super.onViewAttachedToWindow(holder)

        val position = holder.adapterPosition
        if (position < 0) {
            return
        }

        Log.e(TAG, "onViewAttachedToWindow: position is ${position}, id is ${holder.flContainer.id}")
        if (holder.fragment == null) {
            Log.e(TAG, "null position is ${position}")
            val item = if (position < mSelectedCollection.count()) mSelectedCollection.asList()[position] else null
            if (item != null) {
//                val uCrop = UCrop.of(item.contentUri, Uri.fromFile(File(activity.cacheDir, "ucropout")))
//                val options = UCrop.Options()
//                options.setHideBottomControls(true)
//                options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL)
//                uCrop.withOptions(options)
//                holder.fragment = uCrop.getFragment(uCrop.getIntent(activity).extras)
                holder.fragment = cropFragments[position]
                if (holder.fragment!!.isAdded) {
                    Log.e(TAG, "is Added")
                    holder.flContainer.id = View.generateViewId()
                    fragment.childFragmentManager.beginTransaction()
                            .remove(holder.fragment!!)
                            .replace(holder.flContainer.id, holder.fragment!!, "111")
                            .commitAllowingStateLoss()
                } else {
                    holder.flContainer.id = View.generateViewId()
                    fragment.childFragmentManager.beginTransaction()
                            .replace(holder.flContainer.id, holder.fragment!!, "111")
                            .commitAllowingStateLoss()
                }
            }
        } else {
            Log.e(TAG, "position is ${holder.adapterPosition}")
            if (holder.adapterPosition >= mSelectedCollection.count()) {
                fragment.childFragmentManager.beginTransaction()
                        .remove(holder.fragment!!)
                        .commitAllowingStateLoss()
                holder.fragment = null
            }
        }

//        if (holder.fragment == null) {
//            Log.e(TAG, "null position is ${position}")
//            val item = if (position < mSelectedCollection.count()) mSelectedCollection.asList()[position] else null
//            if (item != null && cropFragments[position] != null) {
//                holder.fragment = cropFragments[position]
//                if (holder.fragment!!.isAdded) {
//                    Log.e(TAG, "is Added")
//                    fragment.childFragmentManager.beginTransaction()
//                            .remove(holder.fragment!!)
//                            .commitAllowingStateLoss()
//                }
//                holder.flContainer.id = View.generateViewId()
//                fragment.childFragmentManager.beginTransaction()
//                        .add(holder.flContainer.id, holder.fragment!!, "111")
//                        .commitAllowingStateLoss()
//            }
//        } else {
//            Log.e(TAG, "position is ${holder.adapterPosition}")
//            if (holder.adapterPosition >= mSelectedCollection.count()) {
//                fragment.childFragmentManager.beginTransaction()
//                        .remove(holder.fragment!!)
//                        .commitAllowingStateLoss()
//                holder.fragment = null
//            }
//        }
    }

//    override fun onItemMove(fromPosition: Int, toPosition: Int) {
//        Log.v(TAG, "onItemMove")
////        val idList = list.map { it.id }
////        Collections.swap(list, fromPosition, toPosition)
////        for (i in list.indices) {
////            list[i].id = idList[i]
////        }
////        notifyItemMoved(fromPosition, toPosition)
//    }
//
//    override fun onItemDismiss(position: Int) {
//        Log.v(TAG, "onItemDismiss")
//    }
//
//    override fun onSelectedChanged(actionState: Int) {
//        Log.v(TAG, "onSelectedChanged")
////        if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
////            notifyDataSetChanged()
////        }
//    }
}