package org.readium.r2.shared

import android.provider.ContactsContract
import java.io.Serializable


class Drm: Serializable {

    private val TAG = this::class.java.simpleName

    var brand:Brand
    var scheme:Scheme

    var profile: String? = null
    var license: DrmLicense? = null

    enum class Brand(v:String) {
        lcp("lcp")
    }
    enum class Scheme(v:String) {
        lcp("http://readium.org/2014/01/lcp")
    }

    constructor(brand: Brand){
        this.brand = brand
        when (brand) {
            Brand.lcp -> scheme = Scheme.lcp
        }
    }
}