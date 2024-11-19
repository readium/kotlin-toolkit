@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.lcp.service

import java.lang.reflect.InvocationTargetException
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpException
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOr

internal object LcpClient {

    data class Context(
        val hashedPassphrase: String,
        val encryptedContentKey: String,
        val token: String,
        val profile: String,
    ) {
        companion object {

            fun fromDRMContext(drmContext: Any): Context =
                with(Class.forName("org.readium.lcp.sdk.DRMContext")) {
                    val encryptedContentKey = getMethod("getEncryptedContentKey").invoke(drmContext) as String
                    val hashedPassphrase = getMethod("getHashedPassphrase").invoke(drmContext) as String
                    val profile = getMethod("getProfile").invoke(drmContext) as String
                    val token = getMethod("getToken").invoke(drmContext) as String
                    Context(hashedPassphrase, encryptedContentKey, token, profile)
                }
        }

        fun toDRMContext(): Any =
            Class.forName("org.readium.lcp.sdk.DRMContext")
                .getConstructor(
                    String::class.java,
                    String::class.java,
                    String::class.java,
                    String::class.java
                )
                .newInstance(hashedPassphrase, encryptedContentKey, token, profile)
    }

    private val instance: Any by lazy {
        klass.getDeclaredConstructor().newInstance()
    }

    private val klass: Class<*> by lazy {
        Class.forName("org.readium.lcp.sdk.Lcp")
    }

    fun isAvailable(): Boolean = tryOr(false) {
        instance
        true
    }

    fun createContext(jsonLicense: String, hashedPassphrases: String, pemCrl: String): Context =
        try {
            val drmContext = klass
                .getMethod(
                    "createContext",
                    String::class.java,
                    String::class.java,
                    String::class.java
                )
                .invoke(instance, jsonLicense, hashedPassphrases, pemCrl)!!

            Context.fromDRMContext(drmContext)
        } catch (e: InvocationTargetException) {
            throw mapException(e.targetException)
        }

    fun decrypt(context: Context, encryptedData: ByteArray): ByteArray =
        try {
            klass
                .getMethod(
                    "decrypt",
                    Class.forName("org.readium.lcp.sdk.DRMContext"),
                    ByteArray::class.java
                )
                .invoke(instance, context.toDRMContext(), encryptedData)
                as ByteArray
        } catch (e: InvocationTargetException) {
            throw mapException(e.targetException)
        }

    fun findOneValidPassphrase(jsonLicense: String, hashedPassphrases: List<String>): String =
        try {
            klass
                .getMethod("findOneValidPassphrase", String::class.java, Array<String>::class.java)
                .invoke(instance, jsonLicense, hashedPassphrases.toTypedArray()) as String
        } catch (e: InvocationTargetException) {
            throw mapException(e.targetException)
        }

    private fun mapException(e: Throwable): LcpException {
        val drmExceptionClass = Class.forName("org.readium.lcp.sdk.DRMException")

        if (!drmExceptionClass.isInstance(e)) {
            return LcpException(LcpError.Runtime("the Lcp client threw an unhandled exception"))
        }

        val drmError = drmExceptionClass
            .getMethod("getDrmError")
            .invoke(e)

        val errorCode = Class
            .forName("org.readium.lcp.sdk.DRMError")
            .getMethod("getCode")
            .invoke(drmError) as Int

        val error = when (errorCode) {
            // Error code 11 should never occur since we check the start/end date before calling createContext
            11 -> LcpError.Runtime("License is out of date (check start and end date).")
            101 -> LcpError.LicenseIntegrity.CertificateRevoked
            102 -> LcpError.LicenseIntegrity.InvalidCertificateSignature
            111 -> LcpError.LicenseIntegrity.InvalidLicenseSignatureDate
            112 -> LcpError.LicenseIntegrity.InvalidLicenseSignature
            // Error code 121 seems to be unused in the C++ lib.
            121 -> LcpError.Runtime("The drm context is invalid.")
            131 -> LcpError.Decryption.ContentKeyDecryptError
            141 -> LcpError.LicenseIntegrity.InvalidUserKeyCheck
            151 -> LcpError.Decryption.ContentDecryptError
            else -> LcpError.Unknown(e)
        }

        return LcpException(error)
    }
}
