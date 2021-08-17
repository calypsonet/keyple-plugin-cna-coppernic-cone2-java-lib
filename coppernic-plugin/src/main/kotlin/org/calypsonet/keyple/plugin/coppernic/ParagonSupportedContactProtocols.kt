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

enum class ParagonSupportedContactProtocols {
    ISO_7816_3_T0,
    ISO_7816_3_T1,
    INNOVATRON_HIGH_SPEED_PROTOCOL;

    companion object {
        fun findEnumByKey(key: String): ParagonSupportedContactProtocols {
            for (value in values()) {
                if (value.name == key) {
                    return value
                }
            }
            throw IllegalStateException("ParagonSupportedContactProtocols '$key' is not defined")
        }
    }
}
