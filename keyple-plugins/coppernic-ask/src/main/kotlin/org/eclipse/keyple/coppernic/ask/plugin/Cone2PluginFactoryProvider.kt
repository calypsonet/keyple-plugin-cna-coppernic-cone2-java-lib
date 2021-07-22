package org.eclipse.keyple.coppernic.ask.plugin

import android.content.Context

object Cone2PluginFactoryProvider {
     suspend fun getFactory(context: Context): Cone2PluginFactory{
         val pluginFactory = Cone2PluginFactoryAdapter()
         pluginFactory.init(context)

         return pluginFactory
     }

}