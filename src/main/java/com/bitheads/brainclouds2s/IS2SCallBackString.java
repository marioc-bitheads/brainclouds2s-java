package com.bitheads.brainclouds2s;

public interface IS2SCallBackString {
    /**
     * The serverCallback() method returns server data back to the layer
     * interfacing with the BrainCloud library.
     *
     * @param serviceName - name of the requested service
     * @param serviceOperation - requested operation
     * @param jsonString - returned data from the server 
     */
    void s2sCallback(BrainCloudS2S context, String jsonString);
}
