package com.zhihu.matisse.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.*
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.zhihu.matisse.R
import java.lang.Exception

/**
 * Created by eagle on 2016/11/11.
 */
class FmkWebViewActivity : AppCompatActivity(), View.OnKeyListener {

    private var customView: View? = null
    private var fullscreenContainer: FrameLayout? = null
    private val COVER_SCREEN_PARAMS = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    lateinit var mWebView: WebView
    lateinit var mProgressBar: ProgressBar
    private fun bindView() {
        mWebView = findViewById(R.id.web_view)
        mProgressBar = findViewById(R.id.progress_bar)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.matisse_activity_appframework_webview)
        bindView()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initWindow()
        initWidget()
    }

    protected fun initWindow() {
        val bundle = intent.extras
        if (bundle != null) {
            val title = bundle.getString("title")
            supportActionBar?.title = title
        }
    }

    protected fun initWidget() {
        initWebView()
    }

    private fun initWebView() {
        setWebViewSettings()
        setWebView()
    }

    private fun setWebViewSettings() {
        val webSettings = mWebView.settings
        // 打开页面时， 自适应屏幕
        webSettings.useWideViewPort = true //将图片调整到适合webview的大小
        webSettings.loadWithOverviewMode = true // 缩放至屏幕的大小
        webSettings.setSupportZoom(true)

        // 便页面支持缩放
        webSettings.javaScriptEnabled = true //支持js
        webSettings.setSupportZoom(true) //支持缩放

        webSettings.domStorageEnabled = true
        webSettings.setSupportMultipleWindows(true)// 新加
        //        webSettings.setBuiltInZoomControls(true); // 放大和缩小的按钮，容易引发异常 http://blog.csdn.net/dreamer0924/article/details/34082687

        webSettings.setAppCacheEnabled(true)
        webSettings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
    }

    private fun setWebView() {
        var url = intent.extras!!.getString("url")
        if (url == null) {
            url = "http://"
        }
        Log.v(TAG, "url is $url")
        if (!url.contains("://")) {
            url = "http://$url"
        }
        mWebView!!.loadUrl(url)

        mWebView!!.webChromeClient = object : WebChromeClient() {
            fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.proceed()
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress == 100) {
                    Log.v(TAG, "load complete")
                    mProgressBar!!.visibility = View.GONE
                } else {
                    mProgressBar!!.visibility = View.VISIBLE
                    mProgressBar!!.progress = newProgress
                }
            }

            override fun getVideoLoadingProgressView(): View {
                val frameLayout = FrameLayout(this@FmkWebViewActivity)
                frameLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                return frameLayout
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                showCustomView(view, callback)
            }

            override fun onHideCustomView() {
                hideCustomView()
            }
        }

        mWebView!!.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.proceed()
            }

            override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
//                return super.shouldInterceptRequest(view, url)
                if (url.startsWith("http") || url.startsWith("https")) { //http和https协议开头的执行正常的流程
                    return super.shouldInterceptRequest(view, url)
                } else { //其他的URL则会开启一个Acitity然后去调用原生APP
                    Log.v(TAG, "other url is $url")
                    val outIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    try {
                        startActivity(outIntent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return null
                }
            }
//            @Override
//            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
//                view.loadUrl(url)
//                return true
//            }
        }
        mWebView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            Log.v(TAG, "onDownloadStart")
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            val content_url = Uri.parse(url)
            intent.data = content_url
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    /** 视频播放全屏 **/
    private fun showCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
        // if a view already exists then immediately terminate the new one
        if (customView != null) {
            callback.onCustomViewHidden()
            return
        }

        val decor = window.decorView as FrameLayout
        fullscreenContainer = FullscreenHolder(this)
        fullscreenContainer!!.addView(view, COVER_SCREEN_PARAMS)
        decor.addView(fullscreenContainer, COVER_SCREEN_PARAMS)
        customView = view
        setStatusBarVisibility(false)
        customViewCallback = callback
    }

    /** 隐藏视频全屏 */
    private fun hideCustomView() {
        if (customView == null) {
            return
        }

        setStatusBarVisibility(true)
        val decor =  window.decorView as FrameLayout
        decor.removeView(fullscreenContainer)
        fullscreenContainer = null
        customView = null
        customViewCallback!!.onCustomViewHidden()
        mWebView.visibility = View.VISIBLE
    }

    private fun setStatusBarVisibility(visible: Boolean) {
        val flag = if(visible)  0 else WindowManager.LayoutParams.FLAG_FULLSCREEN
        window.setFlags(flag, WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    //改写物理按键的返回的逻辑
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // TODO Auto-generated method stub
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mWebView!!.canGoBack()) {
                mWebView!!.goBack()//返回上一页面
                return true
            } else {
                finish()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        return false
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        Log.v(TAG, "onDestroy")
        super.onDestroy()
        mWebView.removeAllViews()
        mWebView.destroy()
    }

    companion object {
        private val TAG = FmkWebViewActivity::class.java.simpleName
    }

    class FullscreenHolder(val ctx: Context): FrameLayout(ctx) {
        init {
            setBackgroundColor(Color.BLACK)
        }

//        override fun onTouchEvent(event: MotionEvent?): Boolean {
//            return super.onTouchEvent(event)
//        }

    }

}
