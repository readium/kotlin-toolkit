/*
 * Module: r2-lcp-kotlin
 * Developers: Aferdita Muriqi
 *
 * Copyright (c) 2019. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.lcp.license

import org.joda.time.DateTime
import org.readium.lcp.sdk.DRMContext
import org.readium.lcp.sdk.Lcp
import org.readium.r2.lcp.license.model.LicenseDocument
import org.readium.r2.lcp.license.model.StatusDocument
import org.readium.r2.lcp.license.model.components.Link
import org.readium.r2.lcp.public.LCPAuthenticating
import org.readium.r2.lcp.public.LCPError
import org.readium.r2.lcp.public.StatusError
import org.readium.r2.lcp.service.CRLService
import org.readium.r2.lcp.service.DeviceService
import org.readium.r2.lcp.service.NetworkService
import org.readium.r2.lcp.service.PassphrasesService
import timber.log.Timber

sealed class Either<A, B> {
    class Left<A, B>(val left: A) : Either<A, B>()
    class Right<A, B>(val right: B) : Either<A, B>()
}

private val supportedProfiles = listOf("http://readium.org/lcp/basic-profile", "http://readium.org/lcp/profile-1.0")

typealias Context = Either<DRMContext, StatusError>

typealias Observer = (ValidatedDocuments?, Exception?) -> Unit

private var observers: MutableList<Pair<Observer, ObserverPolicy>> = mutableListOf()

enum class ObserverPolicy {
    once,
    always
}

data class ValidatedDocuments constructor(val license: LicenseDocument, private val context: Context, val status: StatusDocument? = null) {
    fun getContext(): DRMContext {
        when (context) {
            is Either.Left -> return context.left
            is Either.Right -> throw context.right
        }
    }
}

sealed class State {
    object start : State()
    data class validateLicense(val data: ByteArray, val status: StatusDocument?) : State()
    data class fetchStatus(val license: LicenseDocument) : State()
    data class validateStatus(val license: LicenseDocument, val data: ByteArray) : State()
    data class fetchLicense(val license: LicenseDocument, val status: StatusDocument) : State()
    data class checkLicenseStatus(val license: LicenseDocument, val status: StatusDocument?) : State()
    data class requestPassphrase(val license: LicenseDocument, val status: StatusDocument?) : State()
    data class validateIntegrity(val license: LicenseDocument, val status: StatusDocument?, val passphrase: String) : State()
    data class registerDevice(val documents: ValidatedDocuments, val link: Link) : State()
    data class valid(val documents: ValidatedDocuments) : State()
    data class failure(val error: Exception) : State()
}


sealed class Event {
    data class retrievedLicenseData(val data: ByteArray) : Event()
    data class validatedLicense(val license: LicenseDocument) : Event()
    data class retrievedStatusData(val data: ByteArray) : Event()
    data class validatedStatus(val status: StatusDocument) : Event()
    data class checkedLicenseStatus(val error: StatusError?) : Event()
    data class retrievedPassphrase(val passphrase: String) : Event()
    data class validatedIntegrity(val context: DRMContext) : Event()
    data class registeredDevice(val statusData: ByteArray?) : Event()
    data class failed(val error: Exception) : Event()
    object cancelled : Event()
}

class LicenseValidation(var authentication: LCPAuthenticating?,
                        val crl: CRLService,
                        val device: DeviceService,
                        val network: NetworkService,
                        val passphrases: PassphrasesService,
                        val context: android.content.Context,
                        val onLicenseValidated: (LicenseDocument) -> Unit) {


    var state: State = State.start
        set(newValue) {
            field = newValue
            handle(state)
        }

    sealed class Document {
        data class license(val data: ByteArray) : Document()
        data class status(val data: ByteArray) : Document()
    }

    fun validate(document: Document, completion: Observer) {
        val event: Event
        when (document) {
            is Document.license -> event = Event.retrievedLicenseData(document.data)
            is Document.status -> event = Event.retrievedStatusData(document.data)
        }
        Timber.d("validate $event ")
        observe(event, completion)
    }

    val isProduction: Boolean = {
        val prodLicenseInput = context.assets.open("prod-license.lcpl")
        val prodLicense = LicenseDocument(data = prodLicenseInput.readBytes())
        val passphrase = "7B7602FEF5DEDA10F768818FFACBC60B173DB223B7E66D8B2221EBE2C635EFAD"
        try {
            Lcp().findOneValidPassphrase(prodLicense.json.toString(), listOf(passphrase).toTypedArray()) == passphrase
        } catch (e: Exception) {
            false
        }
    }()

    val stateMachine = StateMachine.create<State, Event> {
        initialState(State.start)
        state<State.start> {
            on<Event.retrievedLicenseData> {
                Timber.d("State.validateLicense(it.data, null)")
                transitionTo(State.validateLicense(it.data, null))
            }
        }
        state<State.validateLicense> {
            on<Event.validatedLicense> {
                status?.let { status ->
                    Timber.d("State.checkLicenseStatus(it.license, status)")
                    transitionTo(State.checkLicenseStatus(it.license, status))
                } ?: run {
                    Timber.d("State.fetchStatus(it.license)")
                    transitionTo(State.fetchStatus(it.license))
                }
            }
            on<Event.failed> {
                Timber.d("State.failure(it.error)")
                transitionTo(State.failure(it.error))
            }
        }
        state<State.fetchStatus> {
            on<Event.retrievedStatusData> {
                Timber.d("State.validateStatus(license, it.data)")
                transitionTo(State.validateStatus(license, it.data))
            }
            on<Event.failed> {
                Timber.d("State.checkLicenseStatus(license, null)")
                transitionTo(State.checkLicenseStatus(license, null))
            }
        }
        state<State.validateStatus> {
            on<Event.validatedStatus> {
                if (license.updated < it.status.licenseUpdated) {
                    Timber.d("State.fetchLicense(license, it.status)")
                    transitionTo(State.fetchLicense(license, it.status))
                } else {
                    Timber.d("State.checkLicenseStatus(license, it.status)")
                    transitionTo(State.checkLicenseStatus(license, it.status))
                }
            }
            on<Event.failed> {
                Timber.d("State.checkLicenseStatus(license, null)")
                transitionTo(State.checkLicenseStatus(license, null))
            }
        }
        state<State.fetchLicense> {
            on<Event.retrievedLicenseData> {
                Timber.d("State.validateLicense(it.data, status)")
                transitionTo(State.validateLicense(it.data, status))
            }
            on<Event.failed> {
                Timber.d("State.checkLicenseStatus(license, status)")
                transitionTo(State.checkLicenseStatus(license, status))
            }
        }
        state<State.checkLicenseStatus> {
            on<Event.checkedLicenseStatus> {
                it.error?.let{ error ->
                    Timber.d("State.valid(ValidatedDocuments(license, Either.Right(error), status))")
                    transitionTo(State.valid(ValidatedDocuments(license, Either.Right(error), status)))
                }?: run  {
                    Timber.d("State.requestPassphrase(license, status)")
                    transitionTo(State.requestPassphrase(license, status))
                }
            }
        }
        state<State.requestPassphrase> {
            on<Event.retrievedPassphrase> {
                Timber.d("State.validateIntegrity(license, status, it.passphrase)")
                transitionTo(State.validateIntegrity(license, status, it.passphrase))
            }
            on<Event.failed> {
                Timber.d("State.failure(it.error)")
                transitionTo(State.failure(it.error))
            }
            on<Event.cancelled> {
                Timber.d("State.start)")
                transitionTo(State.start)
            }
        }
        state<State.validateIntegrity> {
            on<Event.validatedIntegrity> {
                val documents = ValidatedDocuments(license, Either.Left(it.context), status)
                val link = status?.link(StatusDocument.Rel.register)
                link?.let {
                    Timber.d("State.registerDevice(documents, link)")
                    transitionTo(State.registerDevice(documents, link))
                } ?: run {
                    Timber.d("State.valid(documents)")
                    transitionTo(State.valid(documents))
                }
            }
            on<Event.failed> {
                Timber.d("State.failure(it.error)")
                transitionTo(State.failure(it.error))
            }
        }
        state<State.registerDevice> {
            on<Event.registeredDevice> {
                it.statusData?.let { statusData->
                    Timber.d("State.validateStatus(documents.license, statusData)")
                    transitionTo(State.validateStatus(documents.license, statusData))
                } ?: run {
                    Timber.d("State.valid(documents)")
                    transitionTo(State.valid(documents))
                }
            }
            on<Event.failed> {
                Timber.d("State.valid(documents)")
                transitionTo(State.valid(documents))
            }
        }
        state<State.valid> {
            on<Event.retrievedStatusData> {
                Timber.d("State.validateStatus(documents.license, it.data)")
                transitionTo(State.validateStatus(documents.license, it.data))
            }
        }
        state<State.failure> {
            onEnter {
                Timber.d("throw error")
//                throw error
            }
        }
        onTransition { transition ->
            val validTransition = transition as? StateMachine.Transition.Valid
            validTransition?.let {
                state = it.toState
            }
        }
    }

    private fun raise(event: Event) {
        stateMachine.transition(event)
    }

    private fun handle(state: State) {
        try {
            when (state) {
                is State.start -> notifyObservers(documents = null, error = null)
                is State.validateLicense -> validateLicense(state.data)
                is State.fetchStatus -> fetchStatus(state.license)
                is State.validateStatus -> validateStatus(state.data)
                is State.fetchLicense -> fetchLicense(state.status)
                is State.checkLicenseStatus -> checkLicenseStatus(state.license, state.status)
                is State.requestPassphrase -> requestPassphrase(state.license)
                is State.validateIntegrity -> validateIntegrity(state.license, state.passphrase)
                is State.registerDevice -> registerDevice(state.documents.license, state.link)
                is State.valid -> notifyObservers(state.documents, null)
                is State.failure -> notifyObservers(null, state.error)
            }
        } catch (error: Exception) {
            Timber.e(error)
            raise(Event.failed(error))
        }
    }

    private fun observe(event: Event, observer: Observer) {
        raise(event)
        Companion.observe(this, ObserverPolicy.once, observer)
    }

    private fun notifyObservers(documents: ValidatedDocuments?, error: Exception?) {
        for (observer in observers) {
            Timber.d("observers $observers")
            observer.first(documents, error)
        }
        Timber.d("observers $observers")
        observers = (observers.filter { it.second != ObserverPolicy.once }).toMutableList()
        Timber.d("observers $observers")
    }

    private fun validateLicense(data: ByteArray) {
        val license = LicenseDocument(data = data)
        if (!isProduction && license.encryption.profile != "http://readium.org/lcp/basic-profile") {
            throw LCPError.licenseProfileNotSupported
        }
        onLicenseValidated(license)
        raise(Event.validatedLicense(license))
    }

    private fun fetchStatus(license: LicenseDocument) {
        val url = license.url(LicenseDocument.Rel.status).toString()
        network.fetch(url) { status, data ->
            if (status != 200) {
                throw LCPError.network(null)
            }
            raise(Event.retrievedStatusData(data!!))
        }
    }

    private fun validateStatus(data: ByteArray) {
        val status = StatusDocument(data = data)
        raise(Event.validatedStatus(status))
    }

    private fun fetchLicense(status: StatusDocument) {
        val url = status.url(StatusDocument.Rel.license).toString()
        network.fetch(url) { statusCode, data ->
            if (statusCode != 200) {
                throw LCPError.network(null)
            }
            raise(Event.retrievedLicenseData(data!!))
        }
    }

    private fun checkLicenseStatus(license: LicenseDocument, status: StatusDocument?) {
        var error: StatusError? = null
        val now = DateTime()
        val start = license.rights.start ?: now
        val end = license.rights.end ?: now
        if (start > now || now > end) {
            if (status != null) {
                val date = status.statusUpdated
                when (status.status) {
                    StatusDocument.Status.ready, StatusDocument.Status.active, StatusDocument.Status.expired -> error = StatusError.expired(start = start, end = end)
                    StatusDocument.Status.returned -> error = StatusError.returned(date)
                    StatusDocument.Status.revoked -> {
                        val devicesCount = status.events(org.readium.r2.lcp.license.model.components.lsd.Event.EventType.register).size
                        error = StatusError.revoked(date, devicesCount = devicesCount)
                    }
                    StatusDocument.Status.cancelled -> error = StatusError.cancelled(date)
                }
            } else {
                error = StatusError.expired(start = start, end = end)
            }
        }
        raise(Event.checkedLicenseStatus(error))
    }

    private fun requestPassphrase(license: LicenseDocument) {
        passphrases.request(license, authentication) { passphrase ->
            passphrase?.let {
                raise(Event.retrievedPassphrase(passphrase))
            } ?: run {
                raise(Event.cancelled)
            }
        }
    }

    private fun validateIntegrity(license: LicenseDocument, passphrase: String) {
        val profile = license.encryption.profile
        if (!supportedProfiles.contains(profile)) {
            throw LCPError.licenseProfileNotSupported
        }
        crl.retrieve { crl ->
            val context = Lcp().createContext(license.json.toString(), passphrase, crl)
            raise(Event.validatedIntegrity(context))
        }
    }

    private fun registerDevice(license: LicenseDocument, link: Link) {
        device.registerLicense(license, link) { data ->
            raise(Event.registeredDevice(data))
        }
    }

    companion object {
        fun observe(licenseValidation: LicenseValidation, policy: ObserverPolicy = ObserverPolicy.always, observer: Observer) {
            var notified = true
            when (licenseValidation.stateMachine.state) {
                is State.valid -> observer((licenseValidation.stateMachine.state as State.valid).documents, null)
                is State.failure -> observer(null, (licenseValidation.stateMachine.state as State.failure).error)
                else -> notified = false
            }
            if (notified && policy != ObserverPolicy.always) {
                return
            }
            observers.add(Pair(observer, policy))
        }
    }

}
