package com.bitheads.brainclouds2s;

import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RTTComms {
	public enum RTTConnectionStatus {
		REQUESTING_CONNECTION_INFO,
		CONNECTING,
		CONNECTED,
		DISCONNECTING,
		DISCONNECTED
	}

	public enum RTTCallbackType {
		CONNECT_SUCCESS,
		CONNECT_FAILURE,
		EVENT
	}

	private RTTConnectionStatus rttConnectionStatus = RTTConnectionStatus.DISCONNECTED;

	private ArrayList<RTTCallback> callbackEventQueue = new ArrayList<RTTCallback>();
	private IRTTConnectCallback connectCallback = null;
	private IRTTCallback rttRawCallback = null;

	private BrainCloudS2S s2sClient;

	private JSONObject endpoint;
	private JSONObject auth;

	private WSClient webSocketClient;
	private Socket socket = null;

	private boolean disconnectedWithReason = false;
	private JSONObject disconnectMessage;

	private String connectionId;

	private int heartbeatSeconds = 30;
	private long lastHeartbeatTime = 0;

	public String getConnectionId() {
		return connectionId;
	}

	public RTTConnectionStatus getConnectionStatus() {
		return rttConnectionStatus;
	}

	/***
	 * Sends a request to establish an RTT system connection.
	 * 
	 * @param callback method to be invoked when the request is processed
	 */
	public void requestClientConnection(IS2SCallback callback) {
		JSONObject json = new JSONObject();
		json.put("service", "rttRegistration");
		json.put("operation", "REQUEST_SYSTEM_CONNECTION");

		s2sClient.request(json, callback);
	}

	/***
	 * Creates a WebSocket connection
	 */
	public void connect() {
		if (endpoint == null) {
			connectCallback.rttConnectFailure("No websocket endpoint available");
		} 
		else {
			boolean sslEnabled = endpoint.getBoolean("ssl");
			String scheme = sslEnabled ? "wss://" : "ws://";
			StringBuilder uri = new StringBuilder(scheme).append(endpoint.getString("host")).append(":")
					.append(endpoint.getInt("port"));

			if (auth != null) {
				char separator = '?';

				Iterator<String> it = auth.keys();
				while (it.hasNext()) {
					String key = (String) it.next();

					uri.append(separator).append(key).append('=').append(auth.getString(key));
					if (separator == '?')
						separator = '&';
				}
			}

			if (s2sClient.getLogEnabled()) {
				System.out.println("RTT WS: Connecting " + uri);
			}

			try {
				webSocketClient = new WSClient(uri.toString());

				if (sslEnabled) {
					setupSSL();
				}

				webSocketClient.connect();
			} catch (Exception e) {
				e.printStackTrace();
				failedToConnect();
				return;
			}
		}
	}

	/***
	 * Attempts to establish an RTT connection to the brainCloud servers.
	 * 
	 * @param s2sClient       reference to class containing session context/data
	 * @param connectCallback method to be invoked when a connection is or is not
	 *                        established
	 */
	public void enableRTT(BrainCloudS2S s2sClient, IRTTConnectCallback connectCallback) {
		this.s2sClient = s2sClient;
		this.connectCallback = connectCallback;

		switch (rttConnectionStatus) {
			case CONNECTED: {
				System.out.println("enableRTT: Already connected");
				break;
			}
			case DISCONNECTED: {
				rttConnectionStatus = RTTConnectionStatus.REQUESTING_CONNECTION_INFO;
	
				IS2SCallback onClientConnectionRequested = (BrainCloudS2S context, JSONObject jsonData) -> {
					try {
						JSONObject data = jsonData.getJSONObject("data");
						JSONArray endpoints = data.getJSONArray("endpoints");
	
						// get endpoint for WebSocket connection
						for (int i = 0; i < endpoints.length(); i++) {
							JSONObject tempEndpoint = endpoints.getJSONObject(i);
							String protocol = tempEndpoint.getString("protocol");
							if (protocol.equals("ws")) {
								endpoint = tempEndpoint;
							}
						}
	
						if (endpoint == null) {
							rttConnectionStatus = RTTConnectionStatus.DISCONNECTED;
							connectCallback.rttConnectFailure("No WebSocket endpoint available");
						} 
						else {
							auth = data.getJSONObject("auth");
	
							// Establish WebSocket connection
							connect();
						}
					} catch (JSONException e) {
						e.printStackTrace();
						rttConnectionStatus = RTTConnectionStatus.DISCONNECTED;
						connectCallback.rttConnectFailure("Failed to establish connection");
					}
				};
	
				requestClientConnection(onClientConnectionRequested);
				break;
			}
			case REQUESTING_CONNECTION_INFO:
			case CONNECTING: {
				System.out.println("enableRTT: Already in the process of connecting");
				break;
			}
			case DISCONNECTING: {
				System.out.println("enableRTT: Is currently disconnecting"); // This shouldn't happen
				break;
			}
		}
	}

	/***
	 * Disables the RTT connection.
	 */
	public void disableRTT() {
		switch (rttConnectionStatus) {
			case REQUESTING_CONNECTION_INFO:
			case CONNECTING:
			case CONNECTED: {
				if (webSocketClient != null) {
					rttConnectionStatus = RTTConnectionStatus.DISCONNECTING;
					webSocketClient.close();
					webSocketClient = null;
				}
				rttConnectionStatus = RTTConnectionStatus.DISCONNECTED;
				break;
			}
			case DISCONNECTED:
			case DISCONNECTING:
				break;
		}
	}

	/***
	 * Registers a method to be invoked when an RTT event of any service is
	 * received.
	 * 
	 * @param callback method to be invoked when an RTT event is received
	 */
	public void registerRTTRawCallback(IRTTCallback callback) {
		rttRawCallback = callback;
	}

	/***
	 * Deregisters the RTT event callback.
	 */
	public void deregisterRTTRawCallback() {
		rttRawCallback = null;
	}

	/***
	 * Returns whether or not there is an active RTT connection.
	 * 
	 * @return True if there is an active RTT connection
	 */
	public boolean rttIsEnabled() {
		return rttConnectionStatus == RTTConnectionStatus.CONNECTED;
	}

	/***
	 * Call RTT callback methods.
	 */
	public void runCallbacks() {
		synchronized (callbackEventQueue) {
			while (!callbackEventQueue.isEmpty()) {
				RTTCallback rttCallback = callbackEventQueue.remove(0);
				switch (rttCallback.rttCallbackType) {
					case CONNECT_SUCCESS: {
						connectCallback.rttConnectSuccess();
						break;
					}
					case CONNECT_FAILURE: {
						connectCallback.rttConnectFailure(rttCallback.rttCallbackMessage);
						break;
					}
					case EVENT: {
						rttRawCallback.rttCallback(rttCallback.rttCallbackJSON);
						break;
					}
				}
			}
		}

		// Start heartbeat
		if (rttIsEnabled()) {
			if (System.currentTimeMillis() - lastHeartbeatTime >= heartbeatSeconds * 1000) {
				lastHeartbeatTime = System.currentTimeMillis();
				try {
					JSONObject json = new JSONObject();
					json.put("operation", "HEARTBEAT");
					json.put("service", "rtt");
					sendWS(json);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class RTTCallback {
		public RTTCallbackType rttCallbackType;
		public String rttCallbackMessage;
		public JSONObject rttCallbackJSON;

		RTTCallback(RTTCallbackType type) {
			rttCallbackType = type;
		}

		RTTCallback(RTTCallbackType type, String message) {
			rttCallbackType = type;
			rttCallbackMessage = message;
		}

		RTTCallback(RTTCallbackType type, JSONObject json) {
			rttCallbackType = type;
			rttCallbackJSON = json;
		}
	}

	private class WSClient extends WebSocketClient {
		public WSClient(String ip) throws Exception {
			super(new URI(ip));
		}

		@Override
		public void onMessage(String message) {
			try {
				onRecv(message);
			} catch (Exception e) {
				e.printStackTrace();
				disconnect();
				return;
			}
		}

		@Override
		public void onMessage(ByteBuffer bytes) {
			String message = new String(bytes.array());
			try {
				onRecv(message);
			} catch (Exception e) {
				e.printStackTrace();
				disconnect();
				return;
			}
		}

		@Override
		public void onOpen(ServerHandshake handshake) {
			if (s2sClient.getLogEnabled()) {
				System.out.println("RTT WS Connected");
			}

			try {
				onWSConnected();
			} catch (Exception e) {
				e.printStackTrace();
				failedToConnect();
				return;
			}
		}

		@Override
		public void onClose(int code, String reason, boolean remote) {
			if (s2sClient.getLogEnabled()) {
				System.out.println("RTT WS onClose: " + reason + ", code: " + Integer.toString(code) + ", remote: "
						+ Boolean.toString(remote));
			}
			switch (rttConnectionStatus) {
				case REQUESTING_CONNECTION_INFO:
				case CONNECTING:
				case CONNECTED: {
					synchronized (callbackEventQueue) {
						disconnect();
						callbackEventQueue
								.add(new RTTCallback(RTTCallbackType.CONNECT_FAILURE, "webSocket onClose: " + reason));
					}
					break;
				}
				case DISCONNECTING:
				case DISCONNECTED:
					break;
			}
		}

		@Override
		public void onError(Exception e) {
			e.printStackTrace();
			synchronized (callbackEventQueue) {
				disconnect();
				callbackEventQueue.add(new RTTCallback(RTTCallbackType.CONNECT_FAILURE, "webSocket onError"));
			}
		}
	}

	/***
	 * Creates a connection request JSONObject
	 * @param protocol For now, this will only be WebSocket
	 * @return connection request JSONObject
	 * @throws Exception
	 */
	private JSONObject buildConnectionRequest(String protocol) throws Exception {
		JSONObject json = new JSONObject();
		json.put("operation", "CONNECT");
		json.put("service", "rtt");

		JSONObject system = new JSONObject();
		system.put("protocol", protocol);
		system.put("platform", "JAVA");

		JSONObject jsonData = new JSONObject();
		jsonData.put("appId", s2sClient.getAppId());
		jsonData.put("profileId", "s");
		jsonData.put("sessionId", s2sClient.getSessionId());
		jsonData.put("auth", auth);
		jsonData.put("system", system);
		json.put("data", jsonData);

		return json;
	}

	/***
	 * Sens a message through the WebSocket connection
	 * @param jsonData message to be sent
	 * @return TRUE if the message is sent
	 */
	private boolean sendWS(JSONObject jsonData) {
		try {
			String message = jsonData.toString();

			if (s2sClient.getLogEnabled()) {
				System.out.println("RTT SEND: " + message);
			}

			webSocketClient.send(message);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/***
	 * Builds a connection request and sends it through the WebSocket connection
	 * @throws Exception
	 */
	private void onWSConnected() throws Exception {
		sendWS(buildConnectionRequest("ws"));
	}

	/***
	 * Closes the WebSocket connection
	 */
	private void disconnect() {
		rttConnectionStatus = RTTConnectionStatus.DISCONNECTING;
		try {
			if (socket != null) {
				synchronized (socket) {
					if (socket != null) {
						socket.close();
						socket = null;
					}
				}
			}
			if (webSocketClient != null) {
				webSocketClient = null;
			}
			rttConnectionStatus = RTTConnectionStatus.DISCONNECTED;

		} catch (Exception e) {
		}

		if (s2sClient.getLogEnabled()) {
			if (disconnectedWithReason == true) {
				System.out.println("RTT Disconnect:" + disconnectMessage.toString());
			}
		}
	}

	/***
	 * Creates secure socket factory for the WebSocket connection.
	 * @throws Exception
	 */
	private void setupSSL() throws Exception {

		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}

			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}
		} };

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("TLS");
		sc.init(null, trustAllCerts, new SecureRandom());
		SSLSocketFactory factory = sc.getSocketFactory();

		webSocketClient.setSocket(factory.createSocket());
	}

	/***
	 * Triggers the connect failure callback.
	 */
	private void failedToConnect() {
		synchronized (callbackEventQueue) {
			String host = "";
			int port = 0;
			try {
				if (endpoint != null) {
					host = endpoint.getString("host");
					port = endpoint.getInt("port");
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			callbackEventQueue.add(new RTTCallback(RTTCallbackType.CONNECT_FAILURE,
					"Failed to connect to RTT Event server: " + host + ":" + Integer.toString(port)));
		}
	}

	/***
	 * Handles connect/disconnect messages and triggers appropriate callback methods.
	 * @param json JSONObject containing the message
	 * @throws JSONException
	 */
	private void processRTTMessage(JSONObject json) throws JSONException {
		String operation = json.getString("operation");
		switch (operation) {
			case "CONNECT": {
				rttConnectionStatus = RTTConnectionStatus.CONNECTED;
				heartbeatSeconds = json.getJSONObject("data").getInt("heartbeatSeconds");
				lastHeartbeatTime = System.currentTimeMillis();
				connectionId = json.getJSONObject("data").getString("cxId");
				if (json.getString("service").equals("rtt")) {
					synchronized (callbackEventQueue) {
						callbackEventQueue.add(new RTTCallback(RTTCallbackType.CONNECT_SUCCESS));
					}
				} 
				else {
					synchronized (callbackEventQueue) {
						disconnect();
						callbackEventQueue.add(new RTTCallback(RTTCallbackType.CONNECT_FAILURE));
					}
				}
				break;
			}
			case "DISCONNECT": {
				disconnectedWithReason = true;
				disconnectMessage.put("severity", "ERROR");
				disconnectMessage.put("reason", json.getJSONObject("data").getString("reason"));
				disconnectMessage.put("reasonCode", json.getJSONObject("data").getString("reasonCode"));
			}
		}
	}

	/***
	 * Handles messages received through the WebSocket connection.
	 * @param message String object containing received message
	 * @throws Exception
	 */
	private void onRecv(String message) throws Exception {
		if (s2sClient.getLogEnabled()) {
			System.out.println("RTT RECV: " + message);
		}

		try {
			JSONObject jsonData = new JSONObject(message);
			String service = jsonData.getString("service");

			switch (service) {
				case "evs":
				case "rtt":
					processRTTMessage(jsonData);
					break;
				default: {
					synchronized (callbackEventQueue) {
						callbackEventQueue.add(new RTTCallback(RTTCallbackType.EVENT, jsonData));
					}
					break;
				}
			}
		} catch (Exception e) {
			synchronized (callbackEventQueue) {
				disconnect();
				callbackEventQueue.add(new RTTCallback(RTTCallbackType.CONNECT_FAILURE, "Bad message: " + message));
			}
			throw (e);
		}
	}
}
