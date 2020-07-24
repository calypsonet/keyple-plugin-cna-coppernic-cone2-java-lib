package org.eclipse.keyple.coppernic.ask.plugin

interface AndroidCoppernicAskContactlessReader {
    companion object{
        const val READER_NAME = "AndroidCoppernicAskContactlessReader"

        /**
         * This parameter sets the timeout used in the waitForCardAbsent method
         */
        const val  CHECK_FOR_ABSENCE_TIMEOUT_KEY = "CHECK_FOR_ABSENCE_TIMEOUT_KEY"

        /**
         * Default value for CHECK_FOR_ABSENCE_TIMEOUT parameter
         */
        const val  CHECK_FOR_ABSENCE_TIMEOUT_DEFAULT = "500"

        /**
         * This parameter sets the thread wait timeout
         */
        const val  THREAD_WAIT_TIMEOUT_KEY = "THREAD_WAIT_TIMEOUT_KEY"

        /**
         * Default value for THREAD_WAIT_TIMEOUT parameter
         */
        const val  THREAD_WAIT_TIMEOUT_DEFAULT = "2000"
    }
}