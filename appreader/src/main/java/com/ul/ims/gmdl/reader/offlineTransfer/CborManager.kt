/*
 * Copyright (C) 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.ul.ims.gmdl.reader.offlineTransfer

import android.content.Context
import android.nfc.Tag
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.MutableLiveData
import com.ul.ims.gmdl.cbordata.deviceEngagement.DeviceEngagement
import com.ul.ims.gmdl.cbordata.interpreter.CborDataInterpreter
import com.ul.ims.gmdl.cbordata.request.DataElements
import com.ul.ims.gmdl.cbordata.security.CoseKey
import com.ul.ims.gmdl.cbordata.security.mdlauthentication.Handover
import com.ul.ims.gmdl.issuerauthority.IIssuerAuthority
import com.ul.ims.gmdl.offlinetransfer.appLayer.IofflineTransfer
import com.ul.ims.gmdl.offlinetransfer.config.AppMode
import com.ul.ims.gmdl.offlinetransfer.config.BleServiceMode
import com.ul.ims.gmdl.offlinetransfer.executorLayer.verifier.VerifierExecutor
import com.ul.ims.gmdl.offlinetransfer.transportLayer.EventType
import com.ul.ims.gmdl.offlinetransfer.transportLayer.ITransportLayer
import com.ul.ims.gmdl.offlinetransfer.transportLayer.TransferChannels
import com.ul.ims.gmdl.offlinetransfer.transportLayer.TransportManager
import com.ul.ims.gmdl.offlinetransfer.utils.Resource
import com.ul.ims.gmdl.security.sessionencryption.verifier.VerifierSessionManager
import java.util.*

class CborManager(
    private val context: Context,
    private val actAs: AppMode,
    transportChannel: TransferChannels,
    bleServiceMode: BleServiceMode,
    bleUUID: UUID?,
    publicKey: ByteArray,
    wifiPassphrase: String?,
    nfcTag: Tag?,
    apduCommandLength: Int?
) : IofflineTransfer {

    // Livedata with the transfer status and received data
    override var data = MutableLiveData<Resource<Any>>()

    //TransportChannelManager obj retrieves a Transportation Channel such as BLE or NFC
    private var transportChannelManager: TransportChannelManager =
        TransportChannelManager(
            context,
            transportChannel,
            actAs,
            bleServiceMode,
            bleUUID,
            publicKey,
            wifiPassphrase,
            nfcTag,
            apduCommandLength
        )

    //TransportManager obj
    private var transportManager: TransportManager? = null

    //ITransportLayer obj
    private var transportLayer: ITransportLayer? = null

    // CBor Interpreter
    private val interpreter = CborDataInterpreter()

    // Request/Response executor instances
    private var readerExecutor: VerifierExecutor? = null

    init {
        transportManager = transportChannelManager.getTransportManager()
        transportManager?.setTransportProgressListener(this)

        transportLayer = transportChannelManager.getTransportLayer()
        updateLiveData(Resource.connecting())
    }

    companion object {
        val LOG_TAG = CborManager::class.java.simpleName
    }

    /**
     *
     * Transfer Data Between two devices
     *
     * **/
    override fun setupHolder(
        credentialName: String,
        deviceEngagement: ByteArray,
        isAuthRequired: Boolean,
        issuerAuthority: IIssuerAuthority,
        handover: Handover
    ) {
        // Only used for holder app
    }

    override fun setupVerifier(
        coseKey: CoseKey, requestItems: DataElements,
        deviceEngagement: DeviceEngagement,
        handover: Handover
    ) {
        if (AppMode.VERIFIER == actAs) {
            transportLayer?.let {
                readerExecutor = VerifierExecutor(
                    interpreter,
                    it,
                    data,
                    VerifierSessionManager(coseKey, deviceEngagement, handover),
                    requestItems,
                    context
                )
            }
        }
    }

    /**
     *
     * Handle Transportation events sent by transportManager
     *
     * **/
    override fun onEvent(event: EventType, description: String) {
        when (event) {
            // Connected to a device
            EventType.GATT_CONNECTED -> {
                updateLiveData(Resource.connecting())
            }
            // Not connected to a device after scan finishes
            EventType.NO_DEVICE_FOUND -> {
                updateLiveData(Resource.noDeviceFound(description))
            }
            // HolderExecutor start to transfer data
            EventType.TRANSFER_IN_PROGRESS -> {
                updateLiveData(Resource.transferring())
            }
            // HolderExecutor data transfer is complete
            EventType.TRANSFER_COMPLETE -> {
                updateLiveData(Resource.success())
            }
            // Error happened during transfer or to connect to a device
            EventType.ERROR -> {
                updateLiveData(Resource.error(description))
            }
            else -> Log.d(LOG_TAG, "onEvent: $event")
        }

        Log.d(LOG_TAG, "onTransportEvent event = $event desc = $description")
    }

    /**
     *
     * Update Livedata Status
     *
     * **/
    private fun updateLiveData(resource: Resource<Any>) {
        resource.let { res ->
            val handler = Handler(Looper.getMainLooper())
            val runnable = Runnable {
                data.value = res
            }
            handler.post(runnable)
        }
    }

    override fun askForUserConsent(requestItems: Map<String, Boolean>) {
        updateLiveData(Resource.askUserConsent(requestItems))
    }

    override suspend fun onUserConsent(userConsentMap: Map<String, Boolean>?) {
        // Used on holder app
    }

    override fun tearDown() {
        transportLayer?.closeConnection()
    }

    override fun getCryptoObject(): BiometricPrompt.CryptoObject? {
        // Used on holder app
        return null
    }
}

