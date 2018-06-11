package org.readium.r2.shared.opds

import java.io.Serializable


data class IndirectAcquisition(var typeAcquisition: String):Serializable {
    var child = mutableListOf<IndirectAcquisition>()

}
