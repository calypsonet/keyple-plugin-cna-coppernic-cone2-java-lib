package org.eclipse.keyple.coppernic.ask.plugin

import fr.coppernic.sdk.ask.Defines
import fr.coppernic.sdk.ask.Defines.RCSC_Ok
import fr.coppernic.sdk.utils.core.CpcBytes
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderIOException
import org.eclipse.keyple.core.seproxy.plugin.reader.AbstractLocalReader
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols
import org.eclipse.keyple.core.seproxy.protocol.SeProtocol
import org.eclipse.keyple.core.seproxy.protocol.TransmissionMode
import org.eclipse.keyple.core.util.ByteArrayUtil
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap


/**
 * Keyple SE Reader's Implementation for the Coppernic ASK Contact (SAM access) reader
 */
internal class AndroidCoppernicAskContactReaderImpl(val contactInterface: ContactInterface): AbstractLocalReader(AndroidCoppernicAskPlugin.PLUGIN_NAME, "${AndroidCoppernicAskContactReader.READER_NAME}_${contactInterface.slotId}"), AndroidCoppernicAskContactReader{

    private val parameters: MutableMap<String, String> = ConcurrentHashMap()

    //private val isPhysicalChannelOpened= AtomicBoolean(false)

    public enum class ContactInterface(val slotId: Byte){
        ONE(1.toByte()), TWO(2.toByte())
    }


    private val reader = AskReader.getInstance()
    private val apduOut = ByteArray(260)
    private val apduOutLen = IntArray(1)
    var atr: ByteArray? = null

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
            val atr = ByteArray(256)
            val atrLength = IntArray(1)
            val ret = reader.cscResetSam(contactInterface.slotId, atr, atrLength)
            if(ret == RCSC_Ok){
                this.atr = ByteArray(atrLength[0])
                System.arraycopy(atr, 0, this.atr, 0, atrLength[0]);
                Timber.i("SAM ATR: ${CpcBytes.byteArrayToString(atr, atrLength[0])}")
            }else{
                this.atr = null
            }

            //isPhysicalChannelOpened.set(true);
        }finally {
            AskReader.releaseLock()
        }
    }

    override fun setParameter(key: String, value: String) {
        if (key == AndroidCoppernicAskContactReader.FLAG_READER_RESET_STATE) {
            closeLogicalAndPhysicalChannels()
        }
        parameters[key] = value
    }

    override fun getParameters(): MutableMap<String, String> {
        return parameters
    }

    override fun protocolFlagMatches(protocolFlag: SeProtocol?): Boolean {
        /*
         Based on C-One2 HF ASK technical specifications
          -> "RFID HF ASK Module: UCM108
                ...
                Supports up to 2 SAMs: ISO 7816 A,B,C, T=0, T=1, with PPS up to 1.2 Mb/s
                ..."
         */
        return protocolFlag == SeCommonProtocols.PROTOCOL_ISO7816_3
    }

    override fun isPhysicalChannelOpen(): Boolean {
        return atr != null
    }

    override fun checkSePresence(): Boolean {
        return try {
                AskReader.acquireLock()
                //val samSlot = getSetSamSlot()
                val result = reader.cscSelectSam(contactInterface.slotId, Defines.SAM_PROT_HSP_INNOVATRON)
                result == RCSC_Ok
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

    override fun transmitApdu(apduIn: ByteArray): ByteArray {
        Timber.d("Data Length to be sent to tag : ${apduIn?.size}")
        Timber.d("Data In : ${ByteArrayUtil.toHex(apduIn)}")
        try {
            AskReader.acquireLock()
            Timber.d("KEYPLE-APDU-SAM - Data Length to be sent to tag : ${apduIn.size}")
            Timber.d("KEYPLE-APDU-SAM - Data In : ${ByteArrayUtil.toHex(apduIn)}")
            val result = reader.cscIsoCommandSam(apduIn, apduIn.size, apduOut, apduOutLen)
            Timber.d("KEYPLE-APDU-SAM - Data Out : ${ByteArrayUtil.toHex(apduOut)}")

            if (result != RCSC_Ok) {
                Timber.d("KEYPLE-APDU-SAM - throw KeypleReaderIOException")
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
}