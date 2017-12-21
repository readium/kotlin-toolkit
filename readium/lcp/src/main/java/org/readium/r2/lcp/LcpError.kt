package org.readium.r2.lcp

enum class LcpErrorCase {
    unknown,
    invalidPath,
    invalidLcpl,
    statusLinkNotFound,
    licenseNotFound,
    licenseLinkNotFound,
    publicationLinkNotFound,
    hintLinkNotFound,
    registerLinkNotFound,
    LinkNotFound,
    renewLinkNotFound,
    noStatusDocument,
    licenseDocumentData,
    publicationData,
    registrationFailure,
    Failure,
    alreadyed,
    alreadyExpired,
    renewFailure,
    renewPeriod,
    deviceId,
    unexpectedServerError,
    invalidHintData,
    archive,
    fileNotInArchive,
    noPassphraseFound,
    emptyPassphrase,
    invalidJson,
    invalidContext,
    crlFetching,
    missingLicenseStatus,
    licenseStatusCancelled,
    licenseStatused,
    licenseStatusRevoked,
    licenseStatusExpired,
    invalidRights,
    invalidPassphrase
}

class LcpError {

    fun errorDescription(lcpErrorCase: LcpErrorCase) = when(lcpErrorCase){
        LcpErrorCase.unknown -> "Unknown error"
        LcpErrorCase.invalidPath -> "The provided license file path is incorrect."
        LcpErrorCase.invalidLcpl -> "The provided license isn't a correctly formatted LCPL file. "
        LcpErrorCase.licenseNotFound -> "No license found in base for the given identifier."
        LcpErrorCase.statusLinkNotFound -> "The status link is missing from the license document."
        LcpErrorCase.licenseLinkNotFound -> "The license link is missing from the status document."
        LcpErrorCase.publicationLinkNotFound -> "The publication link is missing from the license document."
        LcpErrorCase.hintLinkNotFound -> "The hint link is missing from the license document."
        LcpErrorCase.registerLinkNotFound -> "The register link is missing from the status document."
        LcpErrorCase.LinkNotFound -> "The  link is missing from the status document."
        LcpErrorCase.renewLinkNotFound -> "The renew link is missing from the status document."
        LcpErrorCase.noStatusDocument -> "Updating the license failed, there is no status document."
        LcpErrorCase.licenseDocumentData -> "Updating license failed, the fetche data is invalid."
        LcpErrorCase.publicationData -> "The publication data is invalid."
        LcpErrorCase.missingLicenseStatus -> "The license status couldn't be defined."
        LcpErrorCase.licenseStatused -> "This license has been ed."
        LcpErrorCase.licenseStatusRevoked -> "This license has been revoked by its provider."
        LcpErrorCase.licenseStatusCancelled -> "You have cancelled this license."
        LcpErrorCase.licenseStatusExpired -> "The license status is expired, if your provider allow it, you may be able to renew it."
        LcpErrorCase.invalidRights -> "The rights of this license aren't valid."
        LcpErrorCase.registrationFailure -> "The device could not be registered properly."
        LcpErrorCase.Failure -> "Your publication could not be ed properly."
        LcpErrorCase.alreadyed -> "Your publication has already been ed before."
        LcpErrorCase.alreadyExpired -> "Your publication has already expired."
        LcpErrorCase.renewFailure -> "Your publication could not be renewed properly."
        LcpErrorCase.deviceId -> "Couldn't retrieve/generate a proper deviceId."
        LcpErrorCase.unexpectedServerError -> "An unexpected error has occured."
        LcpErrorCase.invalidHintData -> "The data ed by the server for the hint is not valid."
        LcpErrorCase.archive -> "Coudn't instantiate the archive object."
        LcpErrorCase.fileNotInArchive -> "The file you requested couldn't be found in the archive."
        LcpErrorCase.noPassphraseFound -> "Couldn't find a valide passphrase in the database, please provide a passphrase."
        LcpErrorCase.emptyPassphrase -> "The passphrase provided is empty."
        LcpErrorCase.invalidJson -> "The JSON license is not valid."
        LcpErrorCase.invalidContext -> "The context provided is invalid."
        LcpErrorCase.crlFetching -> "Error while fetching the certificate revocation list."
        LcpErrorCase.invalidPassphrase -> "The passphrase entered is not valid."
        LcpErrorCase.renewPeriod -> "Incorrect renewal period, your publication could not be renewed."
    }

}