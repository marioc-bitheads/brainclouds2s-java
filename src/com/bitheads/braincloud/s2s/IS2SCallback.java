package com.bitheads.braincloud.s2s;

import org.json.JSONObject;

public interface IS2SCallback {
    /**
     * The serverCallback() method returns server data back to the layer
     * interfacing with the BrainCloud library.
     *
     * @param serviceName - name of the requested service
     * @param serviceOperation - requested operation
     * @param jsonData - returned data from the server
     */
    void s2sCallback(Brainclouds2s context, JSONObject jsonData);

}

