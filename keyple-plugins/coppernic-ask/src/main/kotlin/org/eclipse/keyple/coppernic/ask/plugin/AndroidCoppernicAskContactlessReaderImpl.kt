package org.eclipse.keyple.coppernic.ask.plugin

import fr.coppernic.sdk.ask.Defines
import fr.coppernic.sdk.ask.RfidTag
import fr.coppernic.sdk.ask.sCARD_SearchExt
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderIOException
import org.eclipse.keyple.core.seproxy.plugin.reader.AbstractObservableLocalReader
import org.eclipse.keyple.core.seproxy.plugin.reader.AbstractObservableState
import org.eclipse.keyple.core.seproxy.plugin.reader.CardAbsentPingMonitoringJob
import org.eclipse.keyple.core.seproxy.plugin.reader.ObservableReaderStateService
import org.eclipse.keyple.core.seproxy.plugin.reader.SmartInsertionMonitoringJob
import org.eclipse.keyple.core.seproxy.plugin.reader.SmartInsertionReader
import org.eclipse.keyple.core.seproxy.plugin.reader.WaitForSeInsertion
import org.eclipse.keyple.core.seproxy.plugin.reader.WaitForSeProcessing
import org.eclipse.keyple.core.seproxy.plugin.reader.WaitForSeRemoval
import org.eclipse.keyple.core.seproxy.plugin.reader.WaitForStartDetect
import org.eclipse.keyple.core.seproxy.protocol.SeProtocol
import org.eclipse.keyple.core.seproxy.protocol.TransmissionMode
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Implementation of {@link org.eclipse.keyple.core.seproxy.SeReader} to communicate with NFC Tag
 * using ASK Coppernic library
 */
object AndroidCoppernicAskContactlessReaderImpl: AbstractObservableLocalReader(AndroidCoppernicAskPlugin.PLUGIN_NAME,
    AndroidCoppernicAskContactlessReader.READER_NAME), AndroidCoppernicAskContactlessReader,
    SmartInsertionReader{

    private val parameters: MutableMap<String, String> = ConcurrentHashMap()
    private val reader = AskReader.getInstance()

    // RFID tag information returned when card is detected
    private var rfidTag: RfidTag? = null

    // Indicates whether or not physical channel is opened.
    // Physical channel is irrelevant for ASK reader
    private val isPhysicalChannelOpened = AtomicBoolean(false)

    // This boolean indicates that a card has been discovered
    private val isCardDiscovered = AtomicBoolean(false)
    private val isWaitingForCard = AtomicBoolean(false)
    private val executorService = Executors.newSingleThreadExecutor()

    init {
        Timber.d("init")
        // We set parameters to default values
        parameters[AndroidCoppernicAskContactlessReader.CHECK_FOR_ABSENCE_TIMEOUT_KEY] = AndroidCoppernicAskContactlessReader.CHECK_FOR_ABSENCE_TIMEOUT_DEFAULT;
        parameters[AndroidCoppernicAskContactlessReader.THREAD_WAIT_TIMEOUT_KEY] = AndroidCoppernicAskContactlessReader.THREAD_WAIT_TIMEOUT_DEFAULT;
        // Gets ASK reader instance
        this.stateService = initStateService()
    }

    /**
     * Initialization of the state machine system for Observable reader
     * We have to set All 4 states of monitoring states
     */
    override fun initStateService(): ObservableReaderStateService {
        Timber.d("initStateService")
        val states = HashMap<AbstractObservableState.MonitoringState, AbstractObservableState>(4)

        states[AbstractObservableState.MonitoringState.WAIT_FOR_START_DETECTION] = WaitForStartDetect(this)
        states[AbstractObservableState.MonitoringState.WAIT_FOR_SE_INSERTION] = WaitForSeInsertion(this, SmartInsertionMonitoringJob(this), executorService)
        states[AbstractObservableState.MonitoringState.WAIT_FOR_SE_PROCESSING] = WaitForSeProcessing(this)
        states[AbstractObservableState.MonitoringState.WAIT_FOR_SE_REMOVAL] = WaitForSeRemoval(this, CardAbsentPingMonitoringJob(this), executorService)

        return ObservableReaderStateService(this, states, AbstractObservableState.MonitoringState.WAIT_FOR_START_DETECTION);
    }

    override fun protocolFlagMatches(protocolFlag: SeProtocol?): Boolean {
       return true
    }

    override fun checkSePresence(): Boolean {
        return isCardDiscovered.get()
    }


    override fun setParameter(key: String, value: String) {
        parameters[key]=value
    }

    override fun getATR(): ByteArray? {
        return rfidTag?.atr
    }

    override fun getParameters(): MutableMap<String, String> {
        return parameters
    }

    override fun openPhysicalChannel() {
        Timber.d("openPhysicalChannel")
        isPhysicalChannelOpened.set(true)
    }

    override fun closePhysicalChannel() {
        Timber.d("closePhysicalChannel")
        isPhysicalChannelOpened.set(false)
        AskReader.clearInstance()
    }

    override fun isPhysicalChannelOpen(): Boolean {
        Timber.d("isPhysicalChannelOpen: ${isPhysicalChannelOpened.get()}")
        return isPhysicalChannelOpened.get()
    }

    override fun getTransmissionMode(): TransmissionMode {
        return TransmissionMode.CONTACTLESS
    }


    /**
     * For SmartInsertionReader
     */
    override fun waitForCardPresent(): Boolean {
        Timber.d("waitForCardPresent")
        var ret = false
        isWaitingForCard.set(true)
        AskReader.acquireLock()
        // Entering a loop with successive hunts for card
        while (isWaitingForCard.get()) {
            val rfidTag = enterHuntPhase()
            if (rfidTag.communicationMode != RfidTag.CommunicationMode.Unknown) {
                this.rfidTag = rfidTag
                isCardDiscovered.set(true)
                isWaitingForCard.set(false)
                // This allows synchronisation with PLugin when powering off the reader
                ret = true
            }
        }
        // This allows synchronisation with Plugin when powering off the reader
        AskReader.releaseLock()
        return ret
    }

    override fun stopWaitForCard() {
        Timber.d("stopWaitForCard")
        isWaitingForCard.set(false)
    }
    /***/

    private fun enterHuntPhase(): RfidTag{
        Timber.d("enterHuntPhase")
        try{
            AskReader.acquireLock()
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

            return if (ret == Defines.RCSC_Timeout || com[0] == 0x6F.toByte() || com[0] == 0x00.toByte()) {
                    Timber.w("Timeout type result")
                    isCardDiscovered.set(false)
                    RfidTag(0x6F.toByte(), ByteArray(0))
                } else {
                    val correctSizedAtr = ByteArray(lpcbAtr[0])
                    System.arraycopy(atr, 0, correctSizedAtr, 0, correctSizedAtr.size)
                    Timber.d("RFID Tag built ${com[0]} $correctSizedAtr")
                    RfidTag(com[0], correctSizedAtr)
                }
        }finally {
            AskReader.releaseLock()
        }
    }

    override fun transmitApdu(apduIn: ByteArray): ByteArray {
        Timber.d("enterHuntPhase")
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