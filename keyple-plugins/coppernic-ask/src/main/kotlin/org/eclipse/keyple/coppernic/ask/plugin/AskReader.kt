package org.eclipse.keyple.coppernic.ask.plugin

import android.content.Context
import fr.coppernic.sdk.ask.Reader
import fr.coppernic.sdk.core.Defines
import fr.coppernic.sdk.utils.io.InstanceListener
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantLock

/**
 * Provides one instance of ASK reader to be shared between contact and contactless reader.
 */
internal object AskReader {

    lateinit var uniqueInstance : WeakReference<Reader>

    // Avoid timeout issue when a call to checkSePresence has been sent
    // within a transmitApdu command.
    val isTransmitting = ReentrantLock()

    /**
     * Init the reader, is call when instanciating this plugin's factory
     */
    @Throws(Exception::class)
    public fun init(context: Context){
        if(uniqueInstance?.get() == null){
            Reader.getInstance(context, object : InstanceListener<Reader>{
                override fun onCreated(reader: Reader) {
                    var result = reader.cscOpen(Defines.SerialDefines.ASK_READER_PORT, 115200, false)

                    if(result != fr.coppernic.sdk.ask.Defines.RCSC_Ok){
                        //TODO Log and throw error
                        throw java.lang.Exception();
                    }

                    // Initializes reader
                    val sb = StringBuilder()
                    result = reader.cscVersionCsc(sb)

                    if(result != fr.coppernic.sdk.ask.Defines.RCSC_Ok){
                        //TODO Log and throw error
                        throw java.lang.Exception();
                    }

                    uniqueInstance = WeakReference(reader)

                }
                override fun onDisposed(reader: Reader) {
                    TODO("Not yet implemented")
                }
            })
        }
    }

    /**
     * Get Reader instance
     */
    public fun getInstance(): Reader{
        return uniqueInstance.get()!!
    }

//    /**
//     * Reset the instance
//     * TODO: How to reuse the lib as init is only call once in factory?
//     */
//    public fun clearInstance(){
//        getInstance()?.let{
//            uniqueInstance?.get()?.destroy()
//            uniqueInstance = null;
//        }
//    }

    /**
     * Lock to synchronize reader exchanges
     */
    public fun acquireLock(){
        isTransmitting.lock()
    }

    /**
     * Release Lock
     */
    public fun releaseLock(){
        isTransmitting.unlock()
    }

}