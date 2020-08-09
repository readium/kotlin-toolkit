/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.persistence

import org.jetbrains.anko.db.*
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.LcpException
import org.readium.r2.lcp.service.DeviceRepository
import org.readium.r2.lcp.service.LicensesRepository

internal object LicensesTable {
    const val NAME = "Licenses"
    const val ID = "id"
    const val PRINTSLEFT = "printsLeft"
    const val COPIESLEFT = "copiesLeft"
    const val REGISTERED = "registered"
}

internal class Licenses(var database: LcpDatabaseOpenHelper) : DeviceRepository, LicensesRepository {

    private fun exists(license: LicenseDocument): Boolean {
        return database.use {
            select(LicensesTable.NAME, LicensesTable.ID)
                    .whereArgs("${LicensesTable.ID} = {id}","id" to license.id)
                    .limit(1)
                    .exec {
                        val parser = rowParser { result: String ->
                            return@rowParser result
                        }
                        parseList(parser)
                    }
        }.isNotEmpty()
    }

    private fun get(column: String, licenseId: String): Int? {
        return database.use {
            select(LicensesTable.NAME, column)
                    .whereArgs("${LicensesTable.ID} = {id}", "id" to licenseId)
                    .limit(1)
                    .exec {
                        val parser = rowParser { result: Int ->
                            return@rowParser result
                        }
                        try {
                            if (parseList(parser).isNullOrEmpty()) {
                                null
                            } else {
                                parseList(parser)[0]
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
        }
    }

    private fun set(column: String, value: Int, licenseId: String) {
        database.use {
            update(LicensesTable.NAME,
                    column to value)
                    .whereArgs("${LicensesTable.ID} = {id}", "id" to licenseId)
                    .exec()
        }
    }

    override fun isDeviceRegistered(license: LicenseDocument): Boolean {
        if (!exists(license)) {
            throw LcpException.Runtime("The LCP License doesn't exist in the database")
        }
        return database.use {
            select(LicensesTable.NAME, LicensesTable.REGISTERED)
                    .whereArgs("${LicensesTable.ID} = {id} AND ${LicensesTable.REGISTERED} = {registered}",
                            "id" to license.id, "registered" to true)
                    .limit(1)
                    .exec {
                        val parser = rowParser { result: Int ->
                            return@rowParser result
                        }
                        parseList(parser)
                    }
        }.isNotEmpty()
    }

    override fun registerDevice(license: LicenseDocument) {
        if (!exists(license)) {
            throw LcpException.Runtime("The LCP License doesn't exist in the database")
        }
        database.use {
            update(LicensesTable.NAME,
                    LicensesTable.REGISTERED to true)
                    .whereArgs("${LicensesTable.ID} = {id}", "id" to license.id)
                    .exec()
        }
    }

    override fun addLicense(license: LicenseDocument) {
        if (exists(license)) {
            return
        }
        database.use {
            insert(LicensesTable.NAME,
                    LicensesTable.ID to license.id,
                    LicensesTable.PRINTSLEFT to license.rights.print,
                    LicensesTable.COPIESLEFT to license.rights.copy)
        }
    }

    override fun copiesLeft(licenseId: String): Int? {
        return get(LicensesTable.COPIESLEFT, licenseId)
    }

    override fun setCopiesLeft(quantity: Int, licenseId: String) {
        set(LicensesTable.COPIESLEFT, quantity, licenseId)
    }

    override fun printsLeft(licenseId: String): Int? {
        return get(LicensesTable.PRINTSLEFT, licenseId)
    }

    override fun setPrintsLeft(quantity: Int, licenseId: String) {
        set(LicensesTable.PRINTSLEFT, quantity, licenseId)
    }

}