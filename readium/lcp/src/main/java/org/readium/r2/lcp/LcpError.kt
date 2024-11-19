/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.lcp

import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import org.readium.r2.lcp.service.NetworkException
import org.readium.r2.shared.util.DebugError
import org.readium.r2.shared.util.Error
import org.readium.r2.shared.util.ErrorException
import org.readium.r2.shared.util.Instant
import org.readium.r2.shared.util.ThrowableError
import org.readium.r2.shared.util.Url

public sealed class LcpError(
    override val message: String,
    override val cause: Error? = null,
) : Error {

    public object MissingPassphrase :
        LcpError("Passphrase is not available.")

    /** The interaction is not available with this License. */
    public object LicenseInteractionNotAvailable :
        LcpError("This interaction is not available.")

    /** This License's profile is not supported by liblcp. */
    public object LicenseProfileNotSupported :
        LcpError(
            "This License has a profile identifier that this app cannot handle, the publication cannot be processed"
        )

    /** Failed to retrieve the Certificate Revocation List. */
    public object CrlFetching :
        LcpError("Can't retrieve the Certificate Revocation List")

    /** A network request failed with the given exception. */
    public class Network(override val cause: Error?) :
        LcpError("NetworkError", cause = cause) {

        internal constructor(throwable: Throwable) : this(ThrowableError(throwable))
    }

    /**
     * An unexpected LCP exception occurred. Please post an issue on r2-lcp-kotlin with the error
     * message and how to reproduce it.
     */
    public class Runtime(message: String) :
        LcpError("Unexpected LCP error", DebugError(message))

    /** An unknown low-level exception was reported. */
    public class Unknown(override val cause: Error?) :
        LcpError("Unknown LCP error") {

        internal constructor(throwable: Throwable) : this(ThrowableError(throwable))
    }

    /**
     * Errors while checking the status of the License, using the Status Document.
     *
     * The app should notify the user and stop there. The message to the user must be clear about
     * the status of the license: don't display "expired" if the status is "revoked". The date and
     * time corresponding to the new status should be displayed (e.g. "The license expired on 01
     * January 2018").
     */
    public sealed class LicenseStatus(
        message: String,
        cause: Error? = null,
    ) : LcpError(message, cause) {

        public class Cancelled(public val date: Instant) :
            LicenseStatus("This license was cancelled on $date")

        public class Returned(public val date: Instant) :
            LicenseStatus("This license has been returned on $date")

        public class NotStarted(public val start: Instant) :
            LicenseStatus("This license starts on $start")

        public class Expired(public val end: Instant) :
            LicenseStatus("This license expired on $end")

        /**
         * If the license has been revoked, the user message should display the number of devices which
         * registered to the server. This count can be calculated from the number of "register" events
         * in the status document. If no event is logged in the status document, no such message should
         * appear (certainly not "The license was registered by 0 devices").
         */
        public class Revoked(public val date: Instant, public val devicesCount: Int) :
            LicenseStatus(
                "This license was revoked by its provider on $date. It was registered by $devicesCount device(s)."
            )
    }

    /**
     * Errors while renewing a loan.
     */
    public sealed class Renew(
        message: String,
        cause: Error? = null,
    ) : LcpError(message, cause) {

        /** Your publication could not be renewed properly. */
        public object RenewFailed :
            Renew("Publication could not be renewed properly")

        /** Incorrect renewal period, your publication could not be renewed. */
        public class InvalidRenewalPeriod(public val maxRenewDate: Instant?) :
            Renew("Incorrect renewal period, your publication could not be renewed")

        /** An unexpected error has occurred on the licensing server. */
        public object UnexpectedServerError :
            Renew("An unexpected error has occurred on the server")
    }

    /**
     * Errors while returning a loan.
     */
    public sealed class Return(
        message: String,
        cause: Error? = null,
    ) : LcpError(message, cause) {

        /** Your publication could not be returned properly. */
        public object ReturnFailed :
            Return("Publication could not be returned properly")

        /** Your publication has already been returned before or is expired. */

        public object AlreadyReturnedOrExpired :
            Return("Publication has already been returned before or is expired")

        /** An unexpected error has occurred on the licensing server. */
        public object UnexpectedServerError :
            Return("An unexpected error has occurred on the server")
    }

    /**
     * Errors while parsing the License or Status JSON Documents.
     */
    public sealed class Parsing(
        message: String,
        cause: Error? = null,
    ) : LcpError(message, cause) {

        /** The JSON is malformed and can't be parsed. */
        public object MalformedJSON :
            Parsing("The JSON is malformed and can't be parsed")

        /** The JSON is not representing a valid License Document. */
        public object LicenseDocument :
            Parsing("The JSON is not representing a valid License Document")

        /** The JSON is not representing a valid Status Document. */
        public object StatusDocument :
            Parsing("The JSON is not representing a valid Status Document")

        /** Invalid Link. */
        public object Link :
            Parsing("The JSON is not representing a valid document")

        /** Invalid Encryption. */
        public object Encryption :
            Parsing("The JSON is not representing a valid document")

        /** Invalid License Document Signature. */
        public object Signature :
            Parsing("The JSON is not representing a valid document")

        /** Invalid URL for link with [rel]. */
        public class Url(public val rel: String) :
            Parsing("The JSON is not representing a valid document")
    }

    /**
     * Errors while reading or writing a LCP container (LCPL, EPUB, LCPDF, etc.)
     */
    public sealed class Container(
        message: String,
        cause: Error? = null,
    ) : LcpError(message, cause) {

        /** Can't access the container, it's format is wrong. */
        public object OpenFailed :
            Container("Can't open the license container")

        /** The file at given relative path is not found in the Container. */
        public class FileNotFound(public val url: Url) :
            Container("License not found in container")

        /** Can't read the file at given relative path in the Container. */
        public class ReadFailed(public val url: Url?) :
            Container("Can't read license from container")

        /** Can't write the file at given relative path in the Container. */
        public class WriteFailed(public val url: Url?) :
            Container("Can't write license in container")
    }

    /**
     * An error occurred while checking the integrity of the License, it can't be retrieved.
     */
    public sealed class LicenseIntegrity(
        message: String,
        cause: Error? = null,
    ) : LcpError(message, cause) {

        public object CertificateRevoked :
            LicenseIntegrity("Certificate has been revoked in the CRL")

        public object InvalidCertificateSignature :
            LicenseIntegrity("Certificate has not been signed by CA")

        public object InvalidLicenseSignatureDate :
            LicenseIntegrity("License has been issued by an expired certificate")

        public object InvalidLicenseSignature :
            LicenseIntegrity("License signature does not match")

        public object InvalidUserKeyCheck :
            LicenseIntegrity("User key check invalid")
    }

    public sealed class Decryption(
        message: String,
        cause: Error? = null,
    ) : LcpError(message, cause) {

        public object ContentKeyDecryptError :
            Decryption("Unable to decrypt encrypted content key from user key")

        public object ContentDecryptError : Decryption(
            "Unable to decrypt encrypted content from content key"
        )
    }

    public companion object {

        internal fun wrap(e: Exception): LcpError = when (e) {
            is LcpException -> e.error
            is NetworkException -> Network(e)
            is SocketTimeoutException -> Network(e)
            is CancellationException -> throw e
            else -> Unknown(e)
        }
    }
}

internal class LcpException(val error: LcpError) : Exception(error.message, ErrorException(error))
