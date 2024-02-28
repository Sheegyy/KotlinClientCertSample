package com.example.myapplication

import android.content.ContentValues.TAG
import android.content.Context
import android.net.http.SslError
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.io.FileInputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate



class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        try {
            val inputStream = getP12FileInputStream(this)
            val bytes = inputStream?.readBytes()
            val encodedCertificate = Base64.encodeToString(bytes, Base64.DEFAULT)
            editor.putString("clientCert", encodedCertificate)
            editor.putString("clientCertPass", "your password")
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val webView: WebView = findViewById(R.id.webView)
        webView.webViewClient = MyWebViewClient(false)
        webView.loadUrl("your URL")

    }
    private fun getP12FileInputStream(context: Context): InputStream? {
        return try {
            context.assets.open("client1.p12")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private inner class MyWebViewClient(val isOtherWindow: Boolean) : WebViewClient() {

        private var mPrivateKey: PrivateKey? = null
        private var mCertificates = arrayOf<X509Certificate?>()

        // クライアント証明書による認証を求められた場合に呼ばれる
        override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
            // SharedPreferencesであらかじめ保存しておいた証明書とパスワードを取り出す
            val sharedPreferences = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
            val encodingCert = sharedPreferences.getString("clientCert", null)
            val clientCertPass = sharedPreferences.getString("clientCertPass", null)

            if (mPrivateKey == null || mCertificates.isEmpty() && (encodingCert != null && clientCertPass != null) ) {
                val inputStream = Base64.decode(encodingCert, 0).inputStream()
                val keyStore = KeyStore.getInstance("PKCS12")
                val password = clientCertPass!!.toCharArray()
                keyStore.load(inputStream, password)  //証明書の読み込み
                val alias = keyStore.aliases().nextElement()
                val key = keyStore.getKey(alias, password)
                // 読み込んだ証明書をX509Certificateクラスの配列として取り出す
                if (key is PrivateKey) {
                    mPrivateKey = key
                    val cert = keyStore.getCertificate(alias)
                    mCertificates[0] = cert as X509Certificate
                }
                inputStream.close()
            }
            // 証明書を利用した通信を行う
            request!!.proceed(mPrivateKey, mCertificates)
        }
    }

}