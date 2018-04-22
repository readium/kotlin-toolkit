package org.readium.r2.shared.opds


public class IndirectAcquisition {
    public var typeAcquisition: String
    public var child = mutableListOf<IndirectAcquisition>()

    public constructor(typeAcquisition: String) {
        this.typeAcquisition = typeAcquisition
    }
}
