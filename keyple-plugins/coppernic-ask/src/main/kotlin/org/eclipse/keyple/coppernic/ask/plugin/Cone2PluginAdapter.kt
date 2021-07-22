/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.coppernic.ask.plugin

import org.eclipse.keyple.core.plugin.spi.ObservablePluginSpi
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Handle native Readers mapped for Keyple
 */
internal class Cone2PluginAdapter : Cone2Plugin, ObservablePluginSpi {


    companion object {
        private const val MONITORING_CYCLE_DURATION_MS = 1000
    }

    private lateinit var seReaders: ConcurrentHashMap<String, ReaderSpi>

    override fun searchAvailableReaders(): MutableSet<ReaderSpi> {

        Timber.w("searchAvailableReaders")
        seReaders = ConcurrentHashMap<String, ReaderSpi>()

        val sam1 = Cone2ContactReaderAdapter(Cone2ContactReaderAdapter.ContactInterface.ONE)
        seReaders[sam1.name] = sam1
        val sam2 = Cone2ContactReaderAdapter(Cone2ContactReaderAdapter.ContactInterface.TWO)
        seReaders[sam2.name] = sam2
        val nfc = Cone2ContactlessReaderAdapter()
        seReaders[nfc.name] = nfc

        return seReaders.map {
            it.value
        }.toMutableSet()
    }

    override fun searchAvailableReaderNames(): MutableSet<String> {
        return seReaders.map {
            it.key
        }.toMutableSet()
    }

    override fun searchReader(readerName: String?): ReaderSpi? {
        return if (seReaders.containsKey(readerName)) {
            seReaders[readerName]!!
        }
        else{
            null
        }
    }

    override fun getMonitoringCycleDuration(): Int {
        return MONITORING_CYCLE_DURATION_MS
    }

    override fun getName(): String = Cone2Plugin.PLUGIN_NAME

    override fun onUnregister() {
        //Do nothing
    }
}
