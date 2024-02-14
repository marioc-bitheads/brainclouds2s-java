package com.bitheads.brainclouds2s;

import static org.junit.Assert.fail;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class RTTTest extends TestFixtureBase implements IRTTConnectCallback{
	private boolean testComplete = false;
	
	private int successCount = 0;
	
	private IS2SCallback onPostChatMessageCallback = (BrainCloudS2S context, JSONObject jsonData) -> {
		successCount++;
	};
	
	private IS2SCallback onChannelConnectCallback = (BrainCloudS2S context, JSONObject jsonData) -> {
		successCount++;
		
		JSONObject postChatMessageJSON = new JSONObject();
		JSONObject data = new JSONObject();
		JSONObject content = new JSONObject();
		JSONObject custom = new JSONObject();
		JSONObject from = new JSONObject();
		content.put("text", "Hello Java");
		content.put("custom", custom);
		from.put("name", "Java Tester");
		from.put("pic", "http://www.simpsons.test/homer.jpg");
		data.put("channelId", _s2sClient.getAppId() + ":sy:mysyschannel");
		data.put("content", content);
		data.put("recordInHistory", false);
		data.put("from", from);
		postChatMessageJSON.put("service", "chat");
		postChatMessageJSON.put("operation", "SYS_POST_CHAT_MESSAGE");
		postChatMessageJSON.put("data", data);
		
		_s2sClient.request(postChatMessageJSON, onPostChatMessageCallback);
	};
	
	@Test
	public void sendChatMessage() {
		_s2sClient.enableRTT(this);	
		
		while(!testComplete) {
			_s2sClient.runCallbacks();
		}
	}
	
	public void onRTTEnabled() {
		System.out.println("RTT enabled!");
		
		JSONObject channelConnectJSON = new JSONObject();
		JSONObject data = new JSONObject();
		data.put("channelId", _s2sClient.getAppId() + ":sy:mysyschannel");
		data.put("maxReturn", 10);
		channelConnectJSON.put("service", "chat");
		channelConnectJSON.put("operation", "SYS_CHANNEL_CONNECT");
		channelConnectJSON.put("data", data);
		
		_s2sClient.registerRTTRawCallback(new IRTTCallback() {

			@Override
			public void rttCallback(JSONObject eventJSON) {				
				successCount++;
				
				// Verify chat message
				try {
					JSONObject data = eventJSON.getJSONObject("data");
					JSONObject from = data.getJSONObject("from");
					String name = from.getString("name");
					
					if(name.equals("Java Tester")) {
						successCount++;
					}
				} catch(JSONException e) {
					e.printStackTrace();
					fail("Failed to parse RTTCallback JSON");
				}
				
				_s2sClient.deregisterRTTRawCallback();
				
				_s2sClient.disableRTT();
				
				testComplete = true;
				
				if(!_s2sClient.getRTTEnabled()) {
					successCount++;
				}
				else {
					fail("RTT still enabled");
				}
				
				if(successCount != 6) {
					System.out.println("successCount != 6 (successCount: " + successCount);
					fail("successCount != 6 (successCount: " + successCount);
				}
			}
			
		});
		
		_s2sClient.request(channelConnectJSON, onChannelConnectCallback);
	}

	@Override
	public void rttConnectSuccess() {
		System.out.println("Connected successfully");
		successCount++;
		onRTTEnabled();
	}

	@Override
	public void rttConnectFailure(String errorMessage) {
		testComplete = true;
		fail("Failed to connect to RTT: " + errorMessage);
	}
}
