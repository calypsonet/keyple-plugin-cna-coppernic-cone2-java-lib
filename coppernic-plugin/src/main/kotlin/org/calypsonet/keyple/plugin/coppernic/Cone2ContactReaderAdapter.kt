/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.keyple.plugin.coppernic

import fr.coppernic.sdk.ask.Defines.RCSC_Ok
import fr.coppernic.sdk.ask.Defines.SAM_PROT_HSP_INNOVATRON
import fr.coppernic.sdk.ask.Defines.SAM_PROT_ISO_7816_T0
import fr.coppernic.sdk.ask.Defines.SAM_PROT_ISO_7816_T1
import fr.coppernic.sdk.utils.core.CpcBytes
import org.eclipse.keyple.core.plugin.ReaderIOException
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi
import org.eclipse.keyple.core.util.ByteArrayUtil
import timber.log.Timber

/**
 * Keyple SE Reader's Implementation for the Coppernic ASK Contact (SAM access) reader
 */
@Suppress("INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_WARNING")
internal class Cone2ContactReaderAdapter(val contactInterface: ContactInterface) :
    ReaderSpi,
    Cone2ContactReader {

    /**
     * Represent the physical SAM slots available inside the Cone-2 device
     */
    enum class ContactInterface(val slotId: Byte) {
        ONE(1.toByte()), TWO(2.toByte())
    }

    // Currently activated SAM protocol
    private var currentSamProtocol: Byte? = null

    // Currently activated SAM protocol name
    private var currentSamProtocolName: String? = null

    // Instance of the Paragon NFC reader
    private val reader = ParagonReader.getInstance()

    private val apduOut = ByteArray(260)
    private val apduOutLen = IntArray(1)
    var atr: ByteArray? = null

    /**
     * @see ReaderSpi.getPowerOnData
     */
    override fun getPowerOnData(): String {
        return ByteArrayUtil.toHex(atr)
    }

    /**
     * @see ReaderSpi.openPhysicalChannel
     */
    override fun openPhysicalChannel() {
        try {
            if (currentSamProtocol == null) {
                throw IllegalStateException("Sam protocol is not defined")
            }

            ParagonReader.acquireLock()
            // val samSlot = getSetSamSlot()
            Timber.d("Select SAM slot ${contactInterface.slotId} with protocol $currentSamProtocolName")
            val result =
                reader.cscSelectSam(contactInterface.slotId, currentSamProtocol!!)
            if (result != RCSC_Ok) {
                throw ReaderIOException("Could select SAM slot")
            }
            val atr = ByteArray(256)
            val atrLength = IntArray(1)
            val ret = reader.cscResetSam(contactInterface.slotId, atr, atrLength)
            if (ret == RCSC_Ok) {
                this.atr = ByteArray(atrLength[0])
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                System.arraycopy(atr, 0, this.atr, 0, atrLength[0])
                Timber.i("SAM ATR: ${CpcBytes.byteArrayToString(atr, atrLength[0])}")
            } else {
                this.atr = null
            }

            // isPhysicalChannelOpened.set(true);
        } finally {
            ParagonReader.releaseLock()
        }
    }

    /**
     * @see ReaderSpi.isContactless
     */
    override fun isContactless(): Boolean {
        return false
    }

    /**
     * @see ReaderSpi.activateProtocol
     */
    override fun activateProtocol(readerProtocol: String?) {
        when (readerProtocol) {
            ParagonSupportedContactProtocols.INNOVATRON_HIGH_SPEED_PROTOCOL.name -> {
                currentSamProtocol = SAM_PROT_HSP_INNOVATRON
            }
            ParagonSupportedContactProtocols.ISO_7816_3_T0.name -> {
                currentSamProtocol = SAM_PROT_ISO_7816_T0
            }
            ParagonSupportedContactProtocols.ISO_7816_3_T1.name -> {
                currentSamProtocol = SAM_PROT_ISO_7816_T1
            }
        }

        currentSamProtocolName = readerProtocol
        Timber.d("$name: Activate protocol $readerProtocol (= $currentSamProtocol).")
    }

    /**
     * @see ReaderSpi.deactivateProtocol
     */
    override fun deactivateProtocol(readerProtocol: String?) {
        if (currentSamProtocolName.isNullOrEmpty()) {
            throw IllegalStateException("No protocol currently activated")
        }

        if (!readerProtocol.equals(currentSamProtocolName)) {
            throw IllegalStateException("$readerProtocol is not currently activated. Current SAM protocol is : $currentSamProtocolName")
        } else {
            currentSamProtocol = null
            currentSamProtocolName = null
            Timber.d("$name: Deactivate protocol $readerProtocol.")
        }
    }

    override fun isProtocolSupported(readerProtocol: String?): Boolean {
        return try {
            ParagonSupportedContactProtocols.findEnumByKey(
                readerProtocol!!
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * @see ReaderSpi.isCurrentProtocol
     */
    override fun isCurrentProtocol(readerProtocolName: String?): Boolean {
        /*
         Based on C-One2 HF ASK technical specifications
          -> "RFID HF ASK Module: UCM108
                ...
                Supports up to 2 SAMs: ISO 7816 A,B,C, T=0, T=1, with PPS up to 1.2 Mb/s
                ..."
         */
        return readerProtocolName == null || readerProtocolName == currentSamProtocolName
    }

    /**
     * @see ReaderSpi.isPhysicalChannelOpen
     */
    override fun isPhysicalChannelOpen(): Boolean {
        return atr != null
    }

    /**
     * @see ReaderSpi.checkCardPresence
     */
    override fun checkCardPresence(): Boolean {
        return try {
            ParagonReader.acquireLock()
            // val samSlot = getSetSamSlot()
            currentSamProtocol?.let {
                val result =
                    reader.cscSelectSam(contactInterface.slotId, it)
                result == RCSC_Ok
            } ?: false
        } finally {
            ParagonReader.releaseLock()
        }
    }

    /**
     * @see ReaderSpi.closePhysicalChannel
     */
    override fun closePhysicalChannel() {
        atr = null
    }

    /**
     * @see ReaderSpi.transmitApdu
     */
    override fun transmitApdu(apduIn: ByteArray): ByteArray {
        Timber.d("Data Length to be sent to tag : ${apduIn.size}")
        Timber.d("Data In : ${ByteArrayUtil.toHex(apduIn)}")
        try {
            ParagonReader.acquireLock()
            Timber.d("KEYPLE-APDU-SAM - Data Length to be sent to tag : ${apduIn.size}")
            Timber.d("KEYPLE-APDU-SAM - Data In : ${ByteArrayUtil.toHex(apduIn)}")
            val result = reader.cscIsoCommandSam(apduIn, apduIn.size, apduOut, apduOutLen)
            Timber.d("KEYPLE-APDU-SAM - Data Out : ${ByteArrayUtil.toHex(apduOut)}")

            if (result != RCSC_Ok) {
                Timber.d("KEYPLE-APDU-SAM - throw KeypleReaderIOException")
                throw ReaderIOException("cscIsoCommandSam failde with code: $result")
            } else {
                if (apduOutLen[0] >= 2) {
                    val apduAnswer = ByteArray(apduOutLen[0])
                    System.arraycopy(apduOut, 0, apduAnswer, 0, apduAnswer.size)
                    Timber.d("Data Out : ${ByteArrayUtil.toHex(apduAnswer)}")
                    return apduAnswer
                } else {
                    throw ReaderIOException("Empty Answer")
                }
            }
        } finally {
            ParagonReader.releaseLock()
        }
    }

    /**
     * @see ReaderSpi.getName
     */
    override fun getName(): String {
        return if (contactInterface == ContactInterface.ONE) {
            Cone2ContactReader.SAM_READER_1_NAME
        } else {
            Cone2ContactReader.SAM_READER_2_NAME
        }
    }

    override fun onUnregister() {
        // Do nothing
    }
}
