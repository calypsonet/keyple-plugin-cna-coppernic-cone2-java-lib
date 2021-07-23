/********************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.calypsonet.keyple.plugin.coppernic

import org.eclipse.keyple.core.common.KeypleReaderExtension

interface Cone2ContactlessReader : KeypleReaderExtension {

    /**
     * Sets the timeout used in the waitForCardAbsent method
     */
    var checkForAbsenceTimeout: Int?

    /**
     * Sets the thread wait timeout
     */
    var threadWaitTimeout: Int?

    companion object {
        const val READER_NAME = "Cone2ContactlessReader"

        /**
         * Default value for CHECK_FOR_ABSENCE_TIMEOUT parameter
         */
        const val CHECK_FOR_ABSENCE_TIMEOUT_DEFAULT = 500

        /**
         * Default value for THREAD_WAIT_TIMEOUT parameter
         */
        const val THREAD_WAIT_TIMEOUT_DEFAULT = 2000
    }
}
