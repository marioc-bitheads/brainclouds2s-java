package com.bitheads.brainclouds2s;

import org.json.JSONObject;

public interface IS2SCallback {
    /**
     * The serverCallback() method returns server data back to the layer
     * interfacing with the BrainCloud library.
     *
     * @param context - client instance making the request
     * @param jsonData - returned data from the server
     */
    void s2sCallback(BrainCloudS2S context, JSONObject jsonData);

}

