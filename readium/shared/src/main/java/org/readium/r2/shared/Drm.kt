package org.readium.r2.shared

import android.provider.ContactsContract


class Drm {
    val brand = Brand.lcp
    val scheme = "http://readium.org/2014/01/lcp"

    var profile: String? = ""
    var license: DrmLicense? = null

    enum class Brand {
        lcp
    }

    fun initDrmLicense(){

    }
}