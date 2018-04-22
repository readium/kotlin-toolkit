package org.readium.r2.shared.opds


public class Price {
    public var currency: String
    public var value: Double

    public constructor(currency: String, value: Double) {
        this.currency = currency
        this.value = value
    }
}
