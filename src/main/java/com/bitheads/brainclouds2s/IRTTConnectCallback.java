package com.bitheads.brainclouds2s;

public interface IRTTConnectCallback {
	void rttConnectSuccess();
    void rttConnectFailure(String errorMessage);
}
