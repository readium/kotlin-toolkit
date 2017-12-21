package org.readium.r2.lcp

import org.readium.r2.lcp.Tables.Licenses
import org.readium.r2.lcp.Tables.Transactions

class LCPDatabase {

    val shared = LCPDatabase()
    val licenses = Licenses()
    val transactions = Transactions()
}