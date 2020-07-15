package org.eclipse.keyple.coppernic.ask.plugin

import android.content.Context
import org.eclipse.keyple.core.seproxy.PluginFactory
import org.eclipse.keyple.core.seproxy.ReaderPlugin

class AndroidCoppernicAskPluginFactory(val context: Context): PluginFactory {

    init{
        AskReader.init(context)
    }

    override fun getPluginName(): String {
        return AndroidCoppernicAskPlugin.PLUGIN_NAME
    }

    override fun getPlugin(): ReaderPlugin {
        return AndroidCoppernicAskPluginImpl
    }
}