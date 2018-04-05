package org.readium.r2.lcp

import android.content.Context
import android.preference.PreferenceManager
import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject

/**
 * Created by aferditamuriqi on 3/22/18.
 */
open class R2StringRequest : StringRequest {


    private var context: Context? = null
    private var params : MutableMap<String, String> =  mutableMapOf()

//    constructor(context: Context, url: String, listener: Response.Listener<String>, errorListener: Response.ErrorListener) : super(Request.Method.GET, url, listener, errorListener) {
//
//        this.context = context
//
//    }

    constructor(context: Context, method: Int, url: String, params : MutableMap<String, String>, listener: Response.Listener<String>, errorListener: Response.ErrorListener) : super(method, url, listener, errorListener) {

        this.context = context
        this.params = params
    }

    override fun getParams(): MutableMap<String, String> {
        return params
    }


    override fun parseNetworkResponse(response: NetworkResponse): Response<String> {

        responseCode = response.statusCode
        responseStatus = Integer.toString(response.statusCode)

        return super.parseNetworkResponse(response)

    }

//    @Throws(AuthFailureError::class)
//    override fun getHeaders(): Map<String, String> {
//
//        val preferences =
//                //      context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
//                PreferenceManager.getDefaultSharedPreferences(context)
//
//        val params = hashMapOf<String,String>().apply {
//            put("Accept", "application/json")
//            put("Authorization", "Bearer sdfjsdfhsdj")
//
//            put("userID", preferences.getString("userID", null))
//            put("sessionKey", preferences.getString("sessionKey", null))
//        }
//
//        return params
//    }

    companion object {


        lateinit var responseStatus: String
        var responseCode: Int = 0

        fun statusCode(): Int {
            return responseCode
        }

        fun status(): String {
            return responseStatus
        }
    }
}
