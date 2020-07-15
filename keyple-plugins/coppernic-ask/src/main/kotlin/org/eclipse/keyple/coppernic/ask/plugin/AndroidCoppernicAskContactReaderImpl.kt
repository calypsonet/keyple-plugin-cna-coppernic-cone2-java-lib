package org.eclipse.keyple.coppernic.ask.plugin

import fr.coppernic.sdk.ask.Defines
import fr.coppernic.sdk.ask.Defines.RCSC_Ok
import fr.coppernic.sdk.utils.core.CpcBytes
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderIOException
import org.eclipse.keyple.core.seproxy.plugin.local.AbstractLocalReader
import org.eclipse.keyple.core.seproxy.protocol.SeProtocol
import org.eclipse.keyple.core.seproxy.protocol.TransmissionMode
import org.eclipse.keyple.core.util.ByteArrayUtil
import timber.log.Timber


/**
 * Keyple SE Reader's Implementation for the Coppernic ASK Contact (SAM access) reader
 */
internal class AndroidCoppernicAskContactReaderImpl(val contactInterface: ContactInterface): AbstractLocalReader(AndroidCoppernicAskPlugin.PLUGIN_NAME, "${AndroidCoppernicAskContactReader.READER_NAME}_${contactInterface.slotId}"), AndroidCoppernicAskContactReader{


    public enum class ContactInterface(val slotId: Byte){
        ONE(1.toByte()), TWO(2.toByte())
    }


    private val reader = AskReader.getInstance()
    private val apduOut = ByteArray(260)
    private val apduOutLen = IntArray(1)
    var atr: ByteArray? = null

    override fun transmitApdu(apduIn: ByteArray): ByteArray {
        Timber.d("Data Length to be sent to tag : ${apduIn?.size}")
        Timber.d("Data In : ${ByteArrayUtil.toHex(apduIn)}")
        try {
            AskReader.acquireLock()
            val result = reader.cscIsoCommandSam(apduIn, apduIn.size, apduOut, apduOutLen)
            if (result != Defines.RCSC_Ok) {
                throw KeypleReaderIOException("cscIsoCommandSam failde with code: $result")
            } else {
                if (apduOutLen[0] >= 2) {
                    val apduAnswer = ByteArray(apduOutLen[0]);
                    System.arraycopy(apduOut, 0, apduAnswer, 0, apduAnswer.size);
                    Timber.d("Data Out : ${ByteArrayUtil.toHex(apduAnswer)}")
                    return apduAnswer
                } else {
                    throw KeypleReaderIOException("Empty Answer")
                }
            }
        } finally {
            AskReader.releaseLock()
        }
    }


    override fun getATR(): ByteArray? {
       return atr
    }

    override fun openPhysicalChannel() {
        try {
            AskReader.acquireLock()
            //val samSlot = getSetSamSlot()
            val result = reader.cscSelectSam(contactInterface.slotId, Defines.SAM_PROT_HSP_INNOVATRON)
            if(result != RCSC_Ok){
                throw KeypleReaderIOException("Could select SAM slot")
            }
            atr = ByteArray(256)
            val atrLength = IntArray(1)
            reader.cscResetSam(contactInterface.slotId, atr, atrLength)
            Timber.i("SAM ATR: %s", CpcBytes.byteArrayToString(atr, atrLength[0]))
        }finally {
            AskReader.releaseLock()
        }
    }

    override fun setParameter(key: String, value: String) {
        parameters[key] = value
    }

    override fun getParameters(): MutableMap<String, String> {
        return parameters
    }

    override fun protocolFlagMatches(protocolFlag: SeProtocol?): Boolean {
        return true
    }

    override fun isPhysicalChannelOpen(): Boolean {
        return atr != null
    }

    override fun checkSePresence(): Boolean {
        return try {
                AskReader.acquireLock()
                //val samSlot = getSetSamSlot()
                val result = reader.cscSelectSam(contactInterface.slotId, Defines.SAM_PROT_HSP_INNOVATRON)
                result != RCSC_Ok
            }finally {
                AskReader.releaseLock()
            }
    }

    override fun getTransmissionMode(): TransmissionMode {
        return TransmissionMode.CONTACTS
    }

    override fun closePhysicalChannel() {
        atr = null
    }



//    /**
//     * Returns sam slot set within parameters.
//     * Should be "1" or "2" else default value "1" is returned
//     */
//    private fun getSetSamSlot(): Byte{
//        val samSlot =  parameters[CONTACT_INTERFACE_ID] ?: ContactInterface.ONE.slotId
//        return try{
//            samSlot.toInt().toByte()
//        }catch (e: NumberFormatException){
//            ContactInterface.ONE.slotId.toInt().toByte()
//        }
//    }
}