package com.example.pointtracker.util

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UploadDataProvider
import org.chromium.net.UploadDataSink
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.util.concurrent.Executors

class Nutritionix {
    companion object {
        private fun readJSONResponse(callback : (Map<*, *>) -> Unit) : UrlRequest.Callback {
            val bytesReceived = ByteArrayOutputStream()
            val receiveChannel = Channels.newChannel(bytesReceived)
            return object : UrlRequest.Callback() {
                override fun onRedirectReceived(
                    request: UrlRequest?,
                    info: UrlResponseInfo?,
                    newLocationUrl: String?
                ) {
                }

                override fun onResponseStarted(request: UrlRequest?, info: UrlResponseInfo?) {
                    val httpStatus = info!!.httpStatusCode
                    if (httpStatus == 200) {
                        request?.read(ByteBuffer.allocateDirect(1024))
                    }
                }

                override fun onReadCompleted(
                    request: UrlRequest?,
                    info: UrlResponseInfo?,
                    byteBuffer: ByteBuffer?
                ) {
                    byteBuffer!!.flip()
                    receiveChannel.write(byteBuffer)
                    byteBuffer.clear()
                    request!!.read(byteBuffer)
                }

                override fun onSucceeded(request: UrlRequest?, info: UrlResponseInfo?) {
                    val bodyBytes = bytesReceived.toByteArray()
                    val json = String(bodyBytes)
                    val jsonObj = Gson().fromJson(json, Map::class.java)
                    callback(jsonObj)
                }

                override fun onFailed(
                    request: UrlRequest?,
                    info: UrlResponseInfo?,
                    error: CronetException?
                ) {
                    println(error)
                }

            }
        }

        fun getSearchResults(context : Context, query: String, callback : (Map<*, *>) -> Unit) {
            val builder = CronetEngine.Builder(context)
            val cronetEngine = builder.build()
            val searchUrl = "https://trackapi.nutritionix.com/v2/search/instant?query=$query"
            val executor = Executors.newSingleThreadExecutor()
            val requestBuilder = cronetEngine.newUrlRequestBuilder(searchUrl, readJSONResponse(callback), executor)
            requestBuilder.setHttpMethod("GET")
            requestBuilder.addHeader("Content-Type", "application/json")
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val appId = preferences.getString("x-app-id", "")
            val appKey = preferences.getString("x-app-key", "")
            if (appId!!.isEmpty() || appKey!!.isEmpty()) {
                return
            }
            requestBuilder.addHeader("x-app-id", appId)
            requestBuilder.addHeader("x-app-key", appKey)
            val request = requestBuilder.build()
            request.start()
        }

        fun getNutritionalInfoFromFoodName(context : Context, foodName : String, callback : (Map<*, *>) -> Unit) {
            val builder = CronetEngine.Builder(context)
            val cronetEngine = builder.build()
            val searchUrl = "https://trackapi.nutritionix.com/v2/natural/nutrients"
            val executor = Executors.newSingleThreadExecutor()
            val requestBuilder = cronetEngine.newUrlRequestBuilder(searchUrl, readJSONResponse(callback), executor)
            requestBuilder.setHttpMethod("POST")
            requestBuilder.addHeader("Content-Type", "application/json")
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val appId = preferences.getString("x-app-id", "")
            val appKey = preferences.getString("x-app-key", "")
            if (appId!!.isEmpty() || appKey!!.isEmpty()) {
                return
            }
            requestBuilder.addHeader("x-app-id", appId)
            requestBuilder.addHeader("x-app-key", appKey)
            val body = "{\"query\":\"$foodName\"}"
            val charset = Charsets.UTF_8
            requestBuilder.setUploadDataProvider(object : UploadDataProvider() {
                override fun getLength(): Long {
                    return body.length.toLong()
                }

                override fun read(uploadDataSink: UploadDataSink?, byteBuffer: ByteBuffer?) {
                    byteBuffer!!.put(body.toByteArray(charset))
                    uploadDataSink!!.onReadSucceeded(false)
                }

                override fun rewind(uploadDataSink: UploadDataSink?) {
                    uploadDataSink!!.onRewindSucceeded()
                }
            }, executor)
            val request = requestBuilder.build()
            request.start()
        }

        private fun foodItemSearchQuery(context : Context, searchUrl : String, callback: (Map<*, *>) -> Unit) {
            val builder = CronetEngine.Builder(context)
            val cronetEngine = builder.build()
            val executor = Executors.newSingleThreadExecutor()
            val requestBuilder = cronetEngine.newUrlRequestBuilder(searchUrl, readJSONResponse(callback), executor)
            requestBuilder.setHttpMethod("GET")
            requestBuilder.addHeader("Content-Type", "application/json")
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val appId = preferences.getString("x-app-id", "")
            val appKey = preferences.getString("x-app-key", "")
            if (appId!!.isEmpty() || appKey!!.isEmpty()) {
                return
            }
            requestBuilder.addHeader("x-app-id", appId)
            requestBuilder.addHeader("x-app-key", appKey)
            val request = requestBuilder.build()
            request.start()
        }

        fun getNutritionalInfoFromNixItemId(context : Context, nixItemId : String, callback : (Map<*, *>) -> Unit) {
            foodItemSearchQuery(context, "https://trackapi.nutritionix.com/v2/search/item/?nix_item_id=$nixItemId", callback)
        }

        fun getNutritionalInfoFromUPC(context : Context, upc : String, callback : (Map<*, *>) -> Unit) {
            foodItemSearchQuery(context, "https://trackapi.nutritionix.com/v2/search/item/?upc=$upc", callback)
        }
    }
}