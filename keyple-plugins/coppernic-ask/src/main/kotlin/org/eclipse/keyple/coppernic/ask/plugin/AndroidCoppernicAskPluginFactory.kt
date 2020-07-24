package org.eclipse.keyple.coppernic.ask.plugin

import android.content.ComponentCallbacks
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.keyple.core.seproxy.PluginFactory
import org.eclipse.keyple.core.seproxy.ReaderPlugin
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderIOException
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

object AndroidCoppernicAskPluginFactory: PluginFactory {

    @Throws(KeypleReaderIOException::class)
    fun init(context: Context, callback: (success: AndroidCoppernicAskPluginFactory)-> Unit){
        AskReader.init(context){
            when(it){
                true -> callback(this)
                else -> throw KeypleReaderIOException("Could not init Ask Library")
            }
        }
    }

    override fun getPluginName(): String {
        return AndroidCoppernicAskPlugin.PLUGIN_NAME
    }

    override fun getPlugin(): ReaderPlugin {
        return AndroidCoppernicAskPluginImpl
    }
}