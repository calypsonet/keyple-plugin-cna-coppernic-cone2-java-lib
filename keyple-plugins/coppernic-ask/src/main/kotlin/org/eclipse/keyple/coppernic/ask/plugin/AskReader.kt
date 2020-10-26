package org.eclipse.keyple.coppernic.ask.plugin

import android.content.Context
import fr.coppernic.sdk.ask.Reader
import fr.coppernic.sdk.core.Defines
import fr.coppernic.sdk.utils.io.InstanceListener
import org.eclipse.keyple.coppernic.ask.plugin.utils.suspendCoroutineWithTimeout
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderIOException
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Provides one instance of ASK reader to be shared between contact and contactless reader.
 */
internal object AskReader {

    private const val INIT_TIMEOUT: Long = 10000

    lateinit var uniqueInstance: WeakReference<Reader?>
    private val isInitied = AtomicBoolean(false)

    // Avoid timeout issue when a call to checkSePresence has been sent
    // within a transmitApdu command.
    val isTransmitting = ReentrantLock()

    /**
     * Init the reader, is call when instanciating this plugin's factory
     */
    @Throws(Exception::class)
    public suspend fun init(context: Context): Reader? {
        if (!isInitied.get()) {
            Timber.d("Start Init")
            //val result = ConePeripheral.RFID_ASK_UCM108_GPIO.descriptor.power(context, true).blockingGet()
            //Timber.d("Powered on $result")

            val reader: Reader? = suspendCoroutineWithTimeout(INIT_TIMEOUT) { continuation ->
                Reader.getInstance(context, object : InstanceListener<Reader> {
                    override fun onCreated(reader: Reader) {
                        Timber.d("onCreated")
                        var result =
                            reader.cscOpen(Defines.SerialDefines.ASK_READER_PORT, 115200, false)

                        if (result != fr.coppernic.sdk.ask.Defines.RCSC_Ok) {
                            Timber.e("Error while cscOpen: $result")
                            continuation.resumeWithException(KeypleReaderIOException("Error while cscOpen: $result"))
                            return
                        }

                        // Initializes reader
                        val sb = StringBuilder()
                        result = reader.cscVersionCsc(sb)

                        if (result != fr.coppernic.sdk.ask.Defines.RCSC_Ok) {
                            Timber.w("Error while cscVersionCsc: $result")
                            //TODO: Check with Coppernic why this methods fails
//                            continuation.resumeWithException(KeypleReaderIOException("Error while cscVersionCsc: $result"))
//                            return
                        }

                        uniqueInstance = WeakReference(reader)
                        isInitied.set(true)
                        Timber.d("End Init")
                        continuation.resume(reader)
                    }

                    override fun onDisposed(reader: Reader) {
                        Timber.d("onDisposed")
                        isInitied.set(false)
                        continuation.resume(null)
                    }
                })
            }

            return reader
        } else {
            return null
        }
    }

    /**
     * Get Reader instance
     */
    @Throws(KeypleReaderException::class)
    public fun getInstance(): Reader {
        Timber.d("Get Instance")
        if (!isInitied.get()) {
            throw KeypleReaderIOException("Ask Reader not inited")
        }
        return uniqueInstance?.get()!!
    }

    /**
     * Reset the instance
     * TODO: How to reuse the lib as init is only call once in factory?
     */
    fun clearInstance() {
        Timber.d("Clear Ask Reader instance")
        getInstance().let {
            uniqueInstance.get()?.destroy()
            uniqueInstance.clear()
            uniqueInstance = WeakReference<Reader?>(null)
            isInitied.set(false)
        }
    }

    /**
     * Lock to synchronize reader exchanges
     */
    public fun acquireLock() {
        isTransmitting.lock()
    }

    /**
     * Release Lock
     */
    public fun releaseLock() {
        isTransmitting.unlock()
    }

}