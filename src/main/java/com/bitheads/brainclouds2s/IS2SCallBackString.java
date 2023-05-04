package com.bitheads.brainclouds2s;

public interface IS2SCallBackString {
    /**
     * The serverCallback() method returns server data back to the layer
     * interfacing with the BrainCloud library.
     *
     * @param context - client instance making the request
     * @param jsonString - returned data from the server 
     */
    void s2sCallback(BrainCloudS2S context, String jsonString);
}
