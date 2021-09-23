package org.readium.r2.lcp.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LcpDao {

    /**
     * Retrieve passphrase
     * @return Passphrase
     */
    @Query("SELECT ${Passphrase.PASSPHRASE} FROM ${Passphrase.TABLE_NAME} WHERE ${Passphrase.PROVIDER} = :licenseId")
    suspend fun passphrase(licenseId: String): String?

    @Query("SELECT ${Passphrase.PASSPHRASE} FROM ${Passphrase.TABLE_NAME} WHERE ${Passphrase.USERID} = :userId")
    suspend fun passphrases(userId: String): List<String>

    @Query("SELECT ${Passphrase.PASSPHRASE} FROM ${Passphrase.TABLE_NAME}")
    suspend fun allPassphrases(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPassphrase(passphrase: Passphrase)

    @Query("SELECT ${License.LICENSE_ID} FROM ${License.TABLE_NAME} WHERE ${License.LICENSE_ID} = :licenseId")
    suspend fun exists(licenseId: String): String?

    @Query("SELECT ${License.REGISTERED} FROM ${License.TABLE_NAME} WHERE ${License.LICENSE_ID} = :licenseId")
    suspend fun isDeviceRegistered(licenseId: String): Boolean

    @Query("UPDATE ${License.TABLE_NAME} SET ${License.REGISTERED} = 1 WHERE ${License.LICENSE_ID} = :licenseId")
    suspend fun registerDevice(licenseId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addLicense(license: License)

    @Query("SELECT ${License.RIGHTCOPY} FROM ${License.TABLE_NAME} WHERE ${License.LICENSE_ID} = :licenseId")
    fun getCopiesLeft(licenseId: String): Int?

    @Query("UPDATE ${License.TABLE_NAME} SET ${License.RIGHTCOPY} = :quantity WHERE ${License.LICENSE_ID} = :licenseId")
    fun setCopiesLeft(quantity: Int, licenseId: String)

    @Query("SELECT ${License.RIGHTPRINT} FROM ${License.TABLE_NAME} WHERE ${License.LICENSE_ID} = :licenseId")
    fun getPrintsLeft(licenseId: String): Int?

    @Query("UPDATE ${License.TABLE_NAME} SET ${License.RIGHTPRINT} = :quantity WHERE ${License.LICENSE_ID} = :licenseId")
    fun setPrintsLeft(quantity: Int, licenseId: String)
}