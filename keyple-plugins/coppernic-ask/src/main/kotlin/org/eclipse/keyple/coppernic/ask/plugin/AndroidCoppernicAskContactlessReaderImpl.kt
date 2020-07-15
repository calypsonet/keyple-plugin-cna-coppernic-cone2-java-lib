package org.eclipse.keyple.coppernic.ask.plugin

import fr.coppernic.sdk.ask.Defines
import fr.coppernic.sdk.ask.RfidTag
import fr.coppernic.sdk.ask.sCARD_SearchExt
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderIOException
import org.eclipse.keyple.core.seproxy.plugin.local.*
import org.eclipse.keyple.core.seproxy.plugin.local.monitoring.SmartInsertionMonitoringJob
import org.eclipse.keyple.core.seproxy.plugin.local.monitoring.SmartRemovalMonitoringJob
import org.eclipse.keyple.core.seproxy.plugin.local.state.WaitForSeInsertion
import org.eclipse.keyple.core.seproxy.plugin.local.state.WaitForSeProcessing
import org.eclipse.keyple.core.seproxy.plugin.local.state.WaitForSeRemoval
import org.eclipse.keyple.core.seproxy.plugin.local.state.WaitForStartDetect
import org.eclipse.keyple.core.seproxy.protocol.SeProtocol
import org.eclipse.keyple.core.seproxy.protocol.TransmissionMode
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean


internal object AndroidCoppernicAskContactlessReaderImpl: AbstractObservableLocalReader(AndroidCoppernicAskPlugin.PLUGIN_NAME,
    AndroidCoppernicAskContactlessReader.READER_NAME), AndroidCoppernicAskContactlessReader,
    SmartInsertionReader, SmartRemovalReader {

    private val reader = AskReader.getInstance()

    // RFID tag information returned when card is detected
    private var rfidTag: RfidTag? = null

    // Indicates whether or not physical channel is opened.
    // Physical channel is irrelevant for ASK reader
    private val isPhysicalChannelOpened = AtomicBoolean(false)

    /**
     * Initialization of the state machine system for Observable reader
     * We have to set All 4 states of monitoring states
     */
    override fun initStateService(): ObservableReaderStateService {

        val states = HashMap<AbstractObservableState.MonitoringState, AbstractObservableState>(4)

        states[AbstractObservableState.MonitoringState.WAIT_FOR_START_DETECTION] = WaitForStartDetect(this)
        states[AbstractObservableState.MonitoringState.WAIT_FOR_SE_INSERTION] = WaitForSeInsertion(this, SmartInsertionMonitoringJob(this), null)
        states[AbstractObservableState.MonitoringState.WAIT_FOR_SE_PROCESSING] = WaitForSeProcessing(this)
        //states[AbstractObservableState.MonitoringState.WAIT_FOR_SE_REMOVAL] = WaitForSeRemoval(this, SmartRemovalMonitoringJob(this), null) //FIXME
        states[AbstractObservableState.MonitoringState.WAIT_FOR_SE_REMOVAL] = WaitForSeRemoval(this)

        return ObservableReaderStateService(this, states, AbstractObservableState.MonitoringState.WAIT_FOR_START_DETECTION);
    }

    override fun protocolFlagMatches(protocolFlag: SeProtocol?): Boolean {
       return true
    }

    override fun checkSePresence(): Boolean {
        TODO("Not yet implemented")
    }


    override fun setParameter(key: String, value: String) {
        parameters[key] = value
    }

    override fun getATR(): ByteArray? {
        return rfidTag?.atr
    }

    override fun getParameters(): MutableMap<String, String> {
        return parameters
    }

    override fun openPhysicalChannel() {
        isPhysicalChannelOpened.set(true)
    }

    override fun closePhysicalChannel() {
        isPhysicalChannelOpened.set(false)
    }

    override fun isPhysicalChannelOpen(): Boolean {
        return isPhysicalChannelOpen
    }

    override fun getTransmissionMode(): TransmissionMode {
        return TransmissionMode.CONTACTLESS
    }

    /**
     * For SmartInsertionReader
     */
    override fun waitForCardPresent(): Boolean {
        Timber.d("waitForCardPresent")
        try{
            AskReader.acquireLock()
            // 1 - Sets the enter hunt phase parameters to no select application
            // 1 - Sets the enter hunt phase parameters to no select application
            reader.cscEnterHuntPhaseParameters(
                0x01.toByte(),
                0x01.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x00.toByte(),
                byteArrayOf(),
                0x00.toShort(),
                0x00.toByte()
            )
            val search = sCARD_SearchExt()
            search.OTH = 0
            search.CONT = 0
            search.INNO = 1
            search.ISOA = 1
            search.ISOB = 1
            search.MIFARE = 0
            search.MONO = 0
            search.MV4k = 0
            search.MV5k = 0
            search.TICK = 0
            val mask: Int =
                Defines.SEARCH_MASK_INNO or Defines.SEARCH_MASK_ISOA or Defines.SEARCH_MASK_ISOB

            val com = ByteArray(1)
            val lpcbAtr = IntArray(1)
            val atr = ByteArray(64)

            val ret: Int = reader.cscSearchCardExt(
                search,
                mask,
                0x00.toByte(),
                0x00.toByte(), //No timeout specified
                com,
                lpcbAtr,
                atr
            )

            val rfidTag: RfidTag
            rfidTag =
                if (ret == Defines.RCSC_Timeout || com[0] == 0x6F.toByte() || com[0] == 0x00.toByte()) {
                    Timber.w("Timeout type result")
                    RfidTag(0x6F.toByte(), ByteArray(0))
                } else {
                    val correctSizedAtr = ByteArray(lpcbAtr[0])
                    System.arraycopy(atr, 0, correctSizedAtr, 0, correctSizedAtr.size)
                    Timber.d("RFID Tag built ${com[0]} $correctSizedAtr")
                    RfidTag(com[0], correctSizedAtr)
                }

            return if(rfidTag.communicationMode != RfidTag.CommunicationMode.Unknown){
                Timber.d("RFID Tag communication mode ${rfidTag.communicationMode.name}")
                this.rfidTag = rfidTag;
                true;
            }else {
                Timber.w("RFID Tag communication mode unknown")
                false;
            }
        }finally {
            AskReader.releaseLock()
        }
    }

    override fun stopWaitForCard() {
        reader.stopDiscovery()
    }
    /***/

    /**
     * For SmartRemovalReader
     */
    override fun waitForCardAbsentNative(): Boolean {
        TODO("Not yet implemented")
    }

    override fun stopWaitForCardRemoval() {
        TODO("Not yet implemented")
    }
    /***/

    override fun transmitApdu(apduIn: ByteArray): ByteArray {
        try{
            AskReader.acquireLock()
            val dataReceived = ByteArray(256)
            val dataReceivedLength = IntArray(1)

            reader.cscISOCommand(apduIn, apduIn.size, dataReceived, dataReceivedLength)

            val length = dataReceivedLength[0]

            if(length < 2){
                throw KeypleReaderIOException("Incorrect APDU Answer")
            }else{
                val apduAnswer = ByteArray(length-1)
                System.arraycopy(dataReceived, 1, apduAnswer, 0, apduAnswer.size);
                return apduAnswer
            }

        }finally {
            AskReader.releaseLock()
        }
    }
}