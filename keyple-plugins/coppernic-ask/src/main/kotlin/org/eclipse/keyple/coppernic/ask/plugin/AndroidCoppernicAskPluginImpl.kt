package org.eclipse.keyple.coppernic.ask.plugin

import org.eclipse.keyple.core.seproxy.SeReader
import org.eclipse.keyple.core.seproxy.plugin.AbstractPlugin
import org.eclipse.keyple.core.seproxy.plugin.local.AbstractLocalReader
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Handle native Readers mapped for Keyple
 */
object AndroidCoppernicAskPluginImpl: AbstractPlugin(AndroidCoppernicAskPlugin.PLUGIN_NAME), AndroidCoppernicAskPlugin{
    override fun setParameter(key: String, value: String) {
        Timber.w("Android C-One² Plugin does not support parameters")
        parameters[key] = value
    }

    override fun getParameters(): MutableMap<String, String> {
        Timber.w("Android C-One² Plugin does not support parameters")
        return parameters
    }

    override fun initNativeReaders(): ConcurrentMap<String, SeReader> {
        val seReaders = ConcurrentHashMap<String, SeReader>()
        val sam1 = AndroidCoppernicAskContactReaderImpl(AndroidCoppernicAskContactReaderImpl.ContactInterface.ONE)
        seReaders[sam1.name] = sam1
        val sam2 = AndroidCoppernicAskContactReaderImpl(AndroidCoppernicAskContactReaderImpl.ContactInterface.TWO)
        seReaders[sam2.name] = sam2
        val nfc = AndroidCoppernicAskContactlessReaderImpl
        seReaders[nfc.name] = nfc
        return readers
    }


}