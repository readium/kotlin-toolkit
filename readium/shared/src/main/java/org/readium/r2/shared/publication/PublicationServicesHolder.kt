/*
 * Copyright 2023 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

@file:OptIn(InternalReadiumApi::class)

package org.readium.r2.shared.publication

import kotlin.reflect.KClass
import org.readium.r2.shared.InternalReadiumApi
import org.readium.r2.shared.extensions.tryOrLog
import org.readium.r2.shared.util.Closeable

/**
 * Holds [Publication.Service] instances for a [Publication].
 */
public interface PublicationServicesHolder : Closeable {
    /**
     * Returns the first publication service that is an instance of [serviceType].
     */
    public fun <T : Publication.Service> findService(serviceType: KClass<T>): T?

    /**
     * Returns all the publication services that are instances of [serviceType].
     */
    public fun <T : Publication.Service> findServices(serviceType: KClass<T>): List<T>
}

internal class ListPublicationServicesHolder(
    var services: List<Publication.Service> = emptyList(),
) : PublicationServicesHolder {
    override fun <T : Publication.Service> findService(serviceType: KClass<T>): T? =
        findServices(serviceType).firstOrNull()

    override fun <T : Publication.Service> findServices(serviceType: KClass<T>): List<T> =
        services.filterIsInstance(serviceType.java)

    override fun close() {
        for (service in services) {
            tryOrLog { service.close() }
        }
    }
}
