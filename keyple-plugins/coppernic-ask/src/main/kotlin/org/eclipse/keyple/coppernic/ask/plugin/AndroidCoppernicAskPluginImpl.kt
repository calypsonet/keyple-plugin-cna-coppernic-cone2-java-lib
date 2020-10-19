package org.eclipse.keyple.coppernic.ask.plugin

import org.eclipse.keyple.core.seproxy.SeReader
import org.eclipse.keyple.core.seproxy.plugin.AbstractPlugin
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Handle native Readers mapped for Keyple
 */
internal class AndroidCoppernicAskPluginImpl: AbstractPlugin(AndroidCoppernicAskPlugin.PLUGIN_NAME), AndroidCoppernicAskPlugin{
    private val parameters: MutableMap<String, String> = HashMap() // not in use in this plugin

    override fun setParameter(key: String, value: String) {
        Timber.w("Android C-One² Plugin does not support parameters, use reader instead")
    }

    override fun getParameters(): MutableMap<String, String> {
        Timber.w("Android C-One² Plugin does not support parameters, use reader instead")
        return parameters
    }

    override fun initNativeReaders(): ConcurrentMap<String, SeReader> {
        Timber.w("Init native readers")
        val seReaders = ConcurrentHashMap<String, SeReader>() 
        val sam1 = AndroidCoppernicAskContactReaderImpl(AndroidCoppernicAskContactReaderImpl.ContactInterface.ONE)
        seReaders[sam1.name] = sam1
        val sam2 = AndroidCoppernicAskContactReaderImpl(AndroidCoppernicAskContactReaderImpl.ContactInterface.TWO)
        seReaders[sam2.name] = sam2
        val nfc = AndroidCoppernicAskContactlessReaderImpl()
        seReaders[nfc.name] = nfc
        return seReaders
    }


}