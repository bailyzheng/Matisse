package com.zhihu.matisse.ui.imagepreview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.model.SelectedItemCollection
import com.zhihu.matisse.ui.imagepreview.ItemTouchHelper.MyItemTouchHelperCallback
import com.zhihu.matisse.ui.imagepreview.ItemTouchHelper.OnStartDragListener
import java.io.Serializable

private const val ARG_LIST = "param1"
private const val ARG_PARAM2 = "param2"

open class ImageInfoItem (
        var id: String?,
        var type: String?,
        var res: String?,
        var uriStr: String?
): Serializable

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ImagesPreviewFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ImagesPreviewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ImagesPreviewFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private val TAG: String = "ImagesPreviewFragment"
//    private lateinit var imageList: List<ImageInfoItem>
    private lateinit var mSelectedCollection: SelectedItemCollection
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null
    private var mViewContent: View? = null // 缓存视图内容
    
    lateinit var mItemTouchHelper: ItemTouchHelper
    
    lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
//            try {
//                imageList = it.getSerializable(ARG_LIST) as List<ImageInfoItem>
//            } catch (e: Exception) {
//                e.printStackTrace()
//                imageList = listOf()
//            }
//            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
//        val rootView = inflater.inflate(R.layout.fragment_images_preview, container, false)
//        recyclerView = rootView!!.findViewById(R.id.recyclerView)
//        initWidget()
//        return rootView
        if (mViewContent != null) {
            val parent: ViewGroup? = mViewContent!!.parent as ViewGroup?
            parent?.removeView(mViewContent)
        } else {
            mViewContent = inflater.inflate(R.layout.fragment_images_preview, container, false)
            initWidget()
        }

        return mViewContent
    }
    
    lateinit var imageReplaceHandler: ImageReplaceHandler

    var edit = false
    fun initWidget() {
        val btnEdit = mViewContent!!.findViewById<Button>(R.id.btn_edit)
        btnEdit.setOnClickListener {
            edit = !edit
            if (edit) {
                Toast.makeText(activity, "编辑模式", Toast.LENGTH_SHORT).show()
                btnEdit.text = "完成"
            } else {
                Toast.makeText(activity, "已退出编辑模式", Toast.LENGTH_SHORT).show()
                btnEdit.text = "编辑"
            }
        }

        recyclerView = mViewContent!!.findViewById(R.id.recyclerView)
        val layoutManager = object : LinearLayoutManager(activity,
                HORIZONTAL, false) {
            override fun canScrollVertically(): Boolean {
                return !edit
            }

            override fun canScrollHorizontally(): Boolean {
                return !edit
            }
        }
//        val layoutManager = LinearLayoutManager(context)
//        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        recyclerView.layoutManager = layoutManager
        recyclerView.setItemViewCacheSize(10)
        imageReplaceHandler = ImageReplaceHandler(this)
        val adapter = ImageRVAdapter(activity!!, this, mSelectedCollection, OnStartDragListener { viewHolder ->
            viewHolder?.let {
//                stopPreview()
                mItemTouchHelper.startDrag(it)
            }
        }, imageReplaceHandler)

        recyclerView.adapter = adapter
//        mItemTouchHelper = ItemTouchHelper(MyItemTouchHelperCallback(adapter))
//        mItemTouchHelper.attachToRecyclerView(recyclerView)
        mSelectedCollection.addChangedListener(object : SelectChangedNotifier{
            override fun onChanged() {
                recyclerView.scrollToPosition(mSelectedCollection.count())
                adapter.notifyDataSetChanged()
            }
        })
    }

    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    fun export(()->Unit) {
        (recyclerView.adapter as ImageRVAdapter).cropFragments.forEach {
            it?.cropAndSaveImage()
        }
    }

    fun getUriList() {
        (recyclerView.adapter as ImageRVAdapter).cropFragments.forEach {
            it.res
        }
    }

    override fun onResume() {
        super.onResume()
        recyclerView.adapter?.notifyDataSetChanged()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
//            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        if (resultCode == RESULT_OK) {
//            if (requestCode == UCrop.REQUEST_CROP) {
//                val uri = UCrop.getOutput(data!!) ?: return
//                try {
//                    val item = imageList[imageReplaceHandler.curReplaceIndex]
////                    item.res = FileUriUtils.getFilePathByUri(activity!!, uri)
////                    TLog.v(TAG, "new path is ${item.res}")
//                    recyclerView.adapter?.notifyDataSetChanged()
////                    delayPreview()
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                    Toast.makeText(context!!, "无法读取图片", Toast.LENGTH_SHORT).show()
//                    return
//                }
//
//            }
//        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }
    
    class ImageReplaceHandler(val fragment: ImagesPreviewFragment) : Handler() {
        val TAG = ImageReplaceHandler::class.java.simpleName
        var curReplaceIndex: Int = 0
        
        override fun handleMessage(msg: Message) {
            when (msg.what) {
//                0 -> {
//                    Log.v(TAG, "msg code is 0")
//                    val index = msg.arg1
//                    if (index < 0 || index > fragment.imageList.size - 1) {
//                        Log.w(TAG, "wrong index")
//                        return
//                    }
//
//                    curReplaceIndex = index
//                    Log.v(TAG, "position is $index")
//                    val item = fragment.imageList[index]
//
//                    val tempFile = File(fragment.context!!.cacheDir, "${item.id}_${System.currentTimeMillis()}.jpg")
////                    val tempFile = File(AppConfig.cacheDir, "${item.id}_${System.currentTimeMillis()}.jpg")
//                    val uri = if(item.uriStr != null) Uri.parse(item.uriStr)
//                            else FileProvider.getUriForFile(fragment.activity!!, fragment.activity!!.packageName + ".fileProvider", File(item.res))
//                    Log.v(TAG, "start crop")
////                    activity.stopPreview()
//                    UCrop.of(uri, Uri.fromFile(tempFile))
//                            .start(fragment.context!!, fragment)
//                }
//                1 -> {
//                    Log.v(TAG, "msg code is 1")
//                    val index = msg.arg1
//                    if (index < 0 || index > fragment.imageList.size - 1) {
//                        Log.w(TAG, "wrong index")
//                        return
//                    }
//                    curReplaceIndex = index
//                    Log.v(TAG, "position is $index")
//
//                    val intent = Intent(Intent.ACTION_GET_CONTENT)
//                    intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
//                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
//                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
//                    }
//                    try {
//                        fragment.startActivityForResult(intent, REQUEST_CODE_PICK)
//                    } catch (e: Exception) {
//                        Toast.makeText(fragment.activity!!, "打开失败", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(selectedCollection: SelectedItemCollection, param2: String) =
                ImagesPreviewFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM2, param2)
                    }
                    mSelectedCollection = selectedCollection
                }
    }
}
