package org.eclipse.keyple.coppernic.ask.plugin

import android.content.Context
import fr.coppernic.sdk.ask.Reader
import fr.coppernic.sdk.core.Defines
import fr.coppernic.sdk.power.impl.cone.ConePeripheral
import fr.coppernic.sdk.utils.core.CpcResult
import fr.coppernic.sdk.utils.io.InstanceListener
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderIOException
import timber.log.Timber
import java.lang.IllegalStateException
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.suspendCoroutine

/**
 * Provides one instance of ASK reader to be shared between contact and contactless reader.
 */
internal object AskReader {

    lateinit var uniqueInstance : WeakReference<Reader>
    private val isInitied = AtomicBoolean(false)

    // Avoid timeout issue when a call to checkSePresence has been sent
    // within a transmitApdu command.
    val isTransmitting = ReentrantLock()

    /**
     * Init the reader, is call when instanciating this plugin's factory
     */
    @Throws(Exception::class)
    public fun init(context: Context, callback:(success: Boolean) -> Unit) {
        if(!isInitied.get()){
            Timber.d("Start Init")
            //val result = ConePeripheral.RFID_ASK_UCM108_GPIO.descriptor.power(context, true).blockingGet()
            //Timber.d("Powered on $result")

            //TODO Co routine pour attendre le resultat de l'init
            Reader.getInstance(context, object : InstanceListener<Reader>{
                override fun onCreated(reader: Reader) {
                    Timber.d("onCreated")
                    var result = reader.cscOpen(Defines.SerialDefines.ASK_READER_PORT, 115200, false)

                    if(result != fr.coppernic.sdk.ask.Defines.RCSC_Ok){
                        throw KeypleReaderIOException("Error while cscOpen: $result");
                    }

                    // Initializes reader
                    val sb = StringBuilder()
                    result = reader.cscVersionCsc(sb)

                    if(result != fr.coppernic.sdk.ask.Defines.RCSC_Ok){
                        throw KeypleReaderIOException("Error while cscVersionCsc: $result");
                    }

                    uniqueInstance = WeakReference(reader)
                    isInitied.set(true)
                    Timber.d("End Init")
                    callback(true)
                }
                override fun onDisposed(reader: Reader) {
                    Timber.d("onDisposed")
                    isInitied.set(false)
                    callback(false)
                }
            })
        }else{
            callback(true)
        }
    }

    /**
     * Get Reader instance
     */
    @Throws(KeypleReaderException::class)
    public fun getInstance(): Reader{
        Timber.d("Get Instance")
        if(!isInitied.get()){
            throw KeypleReaderIOException("Ask Reader not inited")
        }
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