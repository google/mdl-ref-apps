package com.android.mdl.app.provisioning;

import android.util.Log;

import androidx.annotation.NonNull;

import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.UnicodeString;

abstract class BaseFlow {
    private static final String TAG = "BaseFlow";
    protected Listener listener;
    protected String sessionId;

    interface Listener {
        void onMessageSessionEnd(@NonNull String reason);

        void sendMessageRequestEnd(@NonNull String reason);

        void onError(@NonNull String error);
    }


    protected boolean hasValidSessionId(Map response) {
        if (sessionId != null) {
            String newSessionId = ((UnicodeString) response.get(new UnicodeString("eSessionId"))).getString();
            if (newSessionId != null && newSessionId.equals(sessionId)) {
                return true;
            }
            String message = "Response error eSessionId expected found '" + newSessionId + "'";
            Log.e(TAG, message);
            listener.onError(message);
            return false;
        }
        sessionId = ((UnicodeString) response.get(new UnicodeString("eSessionId"))).getString();
        if (sessionId == null || sessionId.isEmpty()) {
            String message = "Response error eSessionId expected found null or empty";
            Log.e(TAG, message);
            listener.onError(message);
            return false;
        }
        return true;
    }

    protected boolean hasValidMessageType(Map response, String expectedMessageType) {
        String messageType = ((UnicodeString) response.get(new UnicodeString("messageType"))).getString();
        if (messageType != null) {
            if (messageType.equals(expectedMessageType)) {
                return true;
            } else if (messageType.equals("EndSessionMessage")) {
                listener.onMessageSessionEnd("" + ((UnicodeString) response.get(new UnicodeString("reason"))).getString());
                return false;
            }
        }
        String message = "Response error MessageType '" + expectedMessageType + "' expected found '" + messageType + "'";
        Log.e(TAG, message);
        listener.onError(message);
        return false;
    }
}
