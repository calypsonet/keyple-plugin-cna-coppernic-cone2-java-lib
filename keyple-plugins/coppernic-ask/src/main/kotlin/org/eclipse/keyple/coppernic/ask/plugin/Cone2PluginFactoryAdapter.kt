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

import android.content.Context
import org.eclipse.keyple.core.common.CommonApiProperties
import org.eclipse.keyple.core.plugin.PluginApiProperties
import org.eclipse.keyple.core.plugin.ReaderIOException
import org.eclipse.keyple.core.plugin.spi.PluginFactorySpi
import org.eclipse.keyple.core.plugin.spi.PluginSpi

internal class Cone2PluginFactoryAdapter : Cone2PluginFactory, PluginFactorySpi {

    @Throws(ReaderIOException::class)
    suspend fun init(context: Context): Cone2PluginFactoryAdapter {
        val plugin = ParagonReader.init(context)
        return plugin?.let {
            this
        }
        ?: throw ReaderIOException("Could not init Ask Library")
    }

    override fun getPluginName(): String = Cone2Plugin.PLUGIN_NAME

    override fun getPlugin(): PluginSpi = Cone2PluginAdapter()

    override fun getCommonApiVersion(): String = CommonApiProperties.VERSION

    override fun getPluginApiVersion(): String = PluginApiProperties.VERSION
}
