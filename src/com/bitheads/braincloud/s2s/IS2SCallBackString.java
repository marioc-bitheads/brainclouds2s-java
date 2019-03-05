/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bitheads.braincloud.s2s;

/**
 *
 * @author marioc
 */
public interface IS2SCallBackString {
    /**
     * The serverCallback() method returns server data back to the layer
     * interfacing with the BrainCloud library.
     *
     * @param serviceName - name of the requested service
     * @param serviceOperation - requested operation
     * @param jsonString - returned data from the server 
     */
    void s2sCallback(Brainclouds2s context, String jsonString);
}
