package org.readium.r2.shared.opds


data class IndirectAcquisition(var typeAcquisition: String) {
    var child = mutableListOf<IndirectAcquisition>()

}
