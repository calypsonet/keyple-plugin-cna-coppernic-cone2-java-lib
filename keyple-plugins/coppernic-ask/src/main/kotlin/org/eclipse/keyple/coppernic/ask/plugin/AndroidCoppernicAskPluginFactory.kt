package org.eclipse.keyple.coppernic.ask.plugin

import android.content.Context
import org.eclipse.keyple.core.service.Plugin
import org.eclipse.keyple.core.service.PluginFactory
import org.eclipse.keyple.core.service.exception.KeypleReaderIOException

object AndroidCoppernicAskPluginFactory : PluginFactory {

    @Throws(KeypleReaderIOException::class)
    suspend fun init(context: Context): AndroidCoppernicAskPluginFactory {
        val plugin = AskReader.init(context)
        return plugin?.let {
            this
        } ?:
        throw KeypleReaderIOException("Could not init Ask Library")
    }

    override fun getPluginName(): String {
        return AndroidCoppernicAskPlugin.PLUGIN_NAME
    }

    override fun getPlugin(): Plugin {
        return AndroidCoppernicAskPluginImpl()
    }
}