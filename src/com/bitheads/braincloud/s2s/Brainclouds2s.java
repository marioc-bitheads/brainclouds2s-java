/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bitheads.braincloud.s2s;

import com.bitheads.braincloud.client.ReasonCodes;
import com.bitheads.braincloud.client.StatusCodes;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author marioc
 */
public class Brainclouds2s implements Runnable {

    private static final long NO_PACKET_EXPECTED = -1;
    private static final String DEFAULT_S2S_URL = "https://sharedprod.braincloudservers.com/s2sdispatcher";
    private String _serverUrl;
    private String _appId;
    private String _serverSecret;
    private String _serverName;
    private String _sessionId = null;
    private long _packetId;
    private boolean _isInitialized = false;

    private ScheduledFuture _heartbeatTimer = null;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Object _lock = new Object();

    private boolean _loggingEnabled = false;
    private long _heartbeatSeconds = 1800;  // Default to 30 mins

    /**
     * Initialize brainclouds2s context
     *
     * @param appId Application ID
     * @param serverName Server name
     * @param serverSecret Server secret key
     */
    public void init(String appId, String serverName, String serverSecret) {
        init( appId, serverName, serverSecret,DEFAULT_S2S_URL);
    }

    /**
     * Initialize brainclouds2s context
     *
     * @param appId Application ID
     * @param serverName Server name
     * @param serverSecret Server secret key
     * @param serverUrl The server URL to send the request to. Defaults to the
     * default brainCloud portal
     */
    public void init(String appId, String serverName, String serverSecret, String serverUrl) {
        _packetId = 0;
        _serverUrl = serverUrl;
        _appId = appId;
        _serverSecret = serverSecret;
        _serverName = serverName;
        _sessionId = null;
        _isInitialized = true;
    }

    /**
     * Send an S2S request.
     *
     * @param json S2S operation to be sent as JSON object
     * @param callback Callback function
     */
    public void request(JSONObject json, IS2SCallback callback) {

        if (!isAuthenticated()) {
            authenticate((Brainclouds2s context, JSONObject response) -> {
                try {
                    if (response != null && response.getInt("status") == 200) {
                        JSONObject data = response.getJSONObject("data");
                        _sessionId = data.getString("sessionId");
                        if (data.has("heartbeatSeconds")) {
                            _heartbeatSeconds = data.getLong("heartbeatSeconds");                            
                        }
                        sendRequest(createPacket(json), callback);
                    } else {
                        if (response != null) LogString("Could not authenticate :" + response.toString());
                        if (callback != null) callback.s2sCallback(context, response);
                    }
                } catch (JSONException e) {
                    LogString("Could not authenticate " + e);
                    if (callback != null) {
                        callback.s2sCallback(context, generateError(StatusCodes.BAD_REQUEST, ReasonCodes.MESSAGE_CONTENT_INVALID_JSON, e.getMessage()));
                    }
                }
            });
        } else {
            sendRequest(createPacket(json), callback);
        }
    }

    /**
     * Send an S2S request.
     *
     * @param json S2S operation to be sent as JSON formatted string.
     * @param callback Callback function
     */
    public void request(String json,  IS2SCallBackString callback) {
        JSONObject jsonObject = new JSONObject(json);
        
        request(jsonObject, ((context, jsonData) -> {
            callback.s2sCallback(context, jsonData.toString());
        }));
    }
    

    private void startHeartbeat() {
        stopHeartbeat();
        _heartbeatTimer = scheduler.schedule(this, _heartbeatSeconds, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (_heartbeatTimer != null) {
            _heartbeatTimer.cancel(true);
        }
    }

    /**
     * Current state of the logger.
     * @return 
     */
    public boolean getLogEnabled() {
        return _loggingEnabled;
    }

    public boolean isIsInitialized() {
        return _isInitialized;
    }

    /**
     * Enable logging of communication with server.
     * @param isEnabled 
     */
    public void setLogEnabled(boolean isEnabled) {
        _loggingEnabled = isEnabled;
    }

    private boolean isAuthenticated() {
        return (_sessionId != null);
    }

    private JSONObject createPacket(JSONObject json) {
        JSONObject allMessages;
        // If already a wrapped in a packet then just update it.
        if (json.has("sessionId") && json.has("packetId")) {
            allMessages = json;
        } else {
            JSONArray messages = new JSONArray();
            messages.put(json);

            allMessages = new JSONObject();
            allMessages.put("messages", messages);
        }
        allMessages.put("sessionId", _sessionId);
        allMessages.put("packetId", _packetId);
        return allMessages;
    }

    private JSONObject extractResponseAt(int index, JSONObject json) {

        JSONObject response = null;
        try {
            JSONArray messageResponses = json.getJSONArray("messageResponses");
            if (!messageResponses.isEmpty()) {
                response = messageResponses.getJSONObject(0);
            }
        } catch (Exception e) {
            LogString("Error decoding response " + e);
        }
        return response;
    }

    private JSONObject generateError(int statusCode, int reasonCode, String statusMessage) {
        JSONObject jsonError = new JSONObject();
        try {
            jsonError.put("status", statusCode);
            jsonError.put("reason_code", reasonCode);
            jsonError.put("severity", "ERROR");
            jsonError.put("status_message", statusMessage);
        } catch (JSONException je) {
            LogString("Error encoding error " + je.getMessage());
        }
        return jsonError;
    }

    private void authenticate(IS2SCallback callback) {

        JSONObject authenticateData = new JSONObject();
        authenticateData.put("appId", _appId);
        authenticateData.put("serverName", _serverName);
        authenticateData.put("serverSecret", _serverSecret);

        JSONObject authenticateMsg = new JSONObject();
        authenticateMsg.put("service", "authenticationV2");
        authenticateMsg.put("operation", "AUTHENTICATE");
        authenticateMsg.put("data", authenticateData);

        JSONArray messages = new JSONArray();
        messages.put(authenticateMsg);

        JSONObject allMessages = new JSONObject();
        allMessages.put("messages", messages);
        _packetId = 0;
        allMessages.put("packetId", _packetId);

        sendRequest(allMessages, callback);

    }

    private boolean sendRequest(JSONObject jsonRequest, IS2SCallback callback) {
        synchronized (_lock) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(_serverUrl).openConnection();
                connection.setDoOutput(true);

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");

                String body = jsonRequest.toString() + "\r\n\r\n";

                connection.setRequestProperty("charset", "utf-8");
                byte[] postData = body.getBytes("UTF-8");
                connection.setRequestProperty("Content-Length", Integer.toString(postData.length));

                // to avoid taking the json parsing hit even when logging is disabled
                if (_loggingEnabled) {
                    try {
                        JSONObject jlog = new JSONObject(body);
                        LogString("OUTGOING: " + jlog.toString(2) + ", t: " + new Date().toString());
                    } catch (JSONException e) {
                        // should never happen
                        LogString("OUTGOING: Malform JSON " + e.getMessage() + ", t: " + new Date().toString());
                    }
                }

                _packetId++;
                stopHeartbeat(); // Will restart below

                connection.connect();
                try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                    wr.write(postData);
                }

                // Get server response
                String responseBody = readResponseBody(connection);

                // to avoid taking the json parsing hit even when logging is disabled
                if (_loggingEnabled) {
                    try {
                        JSONObject jlog = new JSONObject(responseBody);
                        LogString("INCOMING (" + connection.getResponseCode() + "): " + jlog.toString(2) + ", t: " + new Date().toString());
                    } catch (JSONException e) {
                        // in case we get a non-json response from the server
                        LogString("INCOMING (" + connection.getResponseCode() + "): " + responseBody + ", t: " + new Date().toString());
                    }
                }

                // non-200 status, retry
                if ((connection.getResponseCode() != HttpURLConnection.HTTP_OK && connection.getResponseCode() != HttpURLConnection.HTTP_FORBIDDEN) || responseBody.length() == 0) {
                    if (callback != null) {
                        callback.s2sCallback(this, generateError(connection.getResponseCode(), ReasonCodes.CLIENT_NETWORK_ERROR_TIMEOUT, "Network error"));
                    }
                    return true;
                }

                JSONObject jsonResponse;
                jsonResponse = new JSONObject(responseBody);

                // check for expired session
                if (connection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                    if (jsonResponse.getInt("reason_code") == ReasonCodes.SERVER_SESSION_EXPIRED) {
                        _sessionId = null;
                        request(jsonRequest, callback); //re-issue the request to auto login.
                        return true;
                    }
                } else if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    jsonResponse = extractResponseAt(0, jsonResponse);
                }

                // Start heartbeat 
                startHeartbeat();

                if (callback != null) {
                    callback.s2sCallback(this, jsonResponse);
                }

            } catch (java.net.SocketTimeoutException e) {
                LogString("TIMEOUT t: " + new Date().toString());
                if (callback != null) {
                    callback.s2sCallback(this, generateError(StatusCodes.CLIENT_NETWORK_ERROR, ReasonCodes.CLIENT_NETWORK_ERROR_TIMEOUT, "Network error"));
                }
                return true;
            } catch (JSONException e) {
                LogString("JSON ERROR " + e.getMessage() + " t: " + new Date().toString());
                if (callback != null) {
                    callback.s2sCallback(this, generateError(StatusCodes.CLIENT_NETWORK_ERROR, ReasonCodes.INVALID_REQUEST, e.getMessage()));
                }
                return true;
            } catch (IOException e) {
                try {
                    int status_code = (connection != null) ? connection.getResponseCode() : StatusCodes.CLIENT_NETWORK_ERROR;
                    if (callback != null) {
                        callback.s2sCallback(this, generateError(status_code, ReasonCodes.INVALID_REQUEST, e.getMessage()));
                    }
                } catch (IOException e2) {
                    if (callback != null) {
                        callback.s2sCallback(this, generateError(StatusCodes.CLIENT_NETWORK_ERROR, ReasonCodes.INVALID_REQUEST, e2.getMessage()));
                    }
                }
            }
            return true;
        }
    }

    private String readResponseBody(HttpURLConnection connection) {
        String responseBody = "";
        try {
            // Get server response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            responseBody = builder.toString();
        } catch (IOException e) {
            // In case of IOException we need to read body from ErrorSream
            try {
                if (connection.getContentLengthLong() > 0) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    String line;
                    StringBuilder builder = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    responseBody = builder.toString();
                }
            } catch (IOException e2) {
                responseBody = "{\"status\":500,\"severity\":\"ERROR\" ,\"reason_code\":0,\"status_message\":\""+e2.getMessage()+"\"}";
            }
        } catch (Exception e) {
            responseBody = "{\"status\":500,\"severity\":\"ERROR\" ,\"reason_code\":0,\"status_message\":\""+e.getMessage()+"\"}";
            
        }
        return responseBody;
    }

    private void LogString(String s) {
        if (_loggingEnabled) {
            // for now use System.out as unit tests do not support android.util.log class
            System.out.println("#BCC " + s);
        }
    }

    @Override
    public void run() {
        if (_sessionId != null) {
            JSONObject heartbeatMsg = new JSONObject();
            heartbeatMsg.put("service", "heartbeat");
            heartbeatMsg.put("operation", "HEARTBEAT");
            request(heartbeatMsg, null);
        }
    }

    /**
     * Terminate current session from server.
     * (New Session will automatically be created on next request)
     */
    public void disconnect() {
        stopHeartbeat();
        _sessionId = null;
    }

}
