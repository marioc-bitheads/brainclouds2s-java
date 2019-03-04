/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bitheads.braincloud.s2s;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author marioc
 */
public class Brainclouds2sSetupTest {

    static protected String m_serverUrl = "";
    static protected String m_appId = "";
    static protected String m_serverSecret = "";
    static protected String m_serverName = "";

    public Brainclouds2sSetupTest() {
    }

    class Callback implements IS2SCallback {

        private int _expectStatus = 200;      
        Brainclouds2s _instance = null;
        public Callback(int expectStatus,Brainclouds2s instance) {
            _expectStatus = expectStatus;
            _instance = instance;
        }
        
        @Override
        public void s2sCallback(Brainclouds2s context, JSONObject jsonData) {         
            if (jsonData != null && jsonData.getInt("status") == _expectStatus ) {
                System.out.println("Received " + jsonData.toString());
            } else if (jsonData == null) {
                System.out.println("Request failed null respose");
                fail("Request failed null respose");      
            } else {
                System.out.println("Request failed unexpected status, wanted  " + _expectStatus + " received :" + jsonData.toString());
                fail("Request failed unexpected status, wanted  " + _expectStatus + " received :" + jsonData.toString());                
            }
            _instance.disconnect();
        }

    }

    /// <summary>
    /// Routine loads up brainCloud configuration info from "tests/ids.txt" (hopefully)
    /// in a platform agnostic way.
    /// </summary>
    private void LoadIds() {
        if (m_serverUrl.length() > 0) return;

        File idsFile = new File("ids.txt");
        try {
            System.out.println("Looking for ids.txt file in " + idsFile.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (idsFile.exists()) System.out.println("Found ids.txt file");

        List<String> lines = new ArrayList<>();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(idsFile));
            String text;
            while ((text = reader.readLine()) != null) {
                lines.add(text);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (String line : lines) {
            String[] split = line.split("=");
            switch (split[0]) {
                case "serverUrl":
                    m_serverUrl = split[1];
                    break;
                case "appId":
                    m_appId = split[1];
                    break;
                case "serverName":
                    m_serverName = split[1];
                    break;
                case "serverSecret":
                    m_serverSecret = split[1];
                    break;
            }
        }
}
   
    @BeforeClass
    public static void setUpClass() {
        
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        LoadIds();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of initialize method, of class Brainclouds2s.
     */
    @Test
    public void testInitialize_3args() {
        System.out.println("testInitialize_3args");
        String appId = m_appId;
        String serverName = m_serverName;
        String serverSecret = m_serverSecret;
        Brainclouds2s instance = new Brainclouds2s();
        instance.init(appId, serverName, serverSecret);

        if (!instance.isIsInitialized()) {
            fail("Initialization Failed");
        }

    }

    /**
     * Test of initialize method, of class Brainclouds2s.
     */
    @Test
    public void testInitialize_4args() {
        System.out.println("testInitialize_4args");
        String appId = m_appId;
        String serverName = m_serverName;
        String serverSecret = m_serverSecret;
        String serverUrl = "https://sharedprod.braincloudservers.com/s2sdispatcher";
        Brainclouds2s instance = new Brainclouds2s();
        instance.init(appId, serverName, serverSecret, serverUrl);
        if (!instance.isIsInitialized()) {
            fail("Initialization with url Failed.");
        }        
    }

    /**
     * Test of getLoggingEnabled method, of class Brainclouds2s.
     */
    @Test
    public void testGetLoggingEnabled() {
        System.out.println("testGetLoggingEnabled");
        Brainclouds2s instance = new Brainclouds2s();
        boolean expResult = false;
        boolean result = instance.getLogEnabled();
        assertEquals(expResult, result);
        if (result) {
            fail("Logging enable at initialization.");
        }
    }

    /**
     * Test of enableLogging method, of class Brainclouds2s.
     */
    @Test
    public void testEnableLogging() {
        System.out.println("testEnableLogging");
        boolean isEnabled = true;
        Brainclouds2s instance = new Brainclouds2s();
        instance.setLogEnabled(true);
        if (!instance.getLogEnabled()) {
            fail("Could not enable logging.");
        }
        instance.setLogEnabled(false);
        if (instance.getLogEnabled()) {
            fail("Could not disable logging.");
        }
    }

    /**
     * Test of request method, of class Brainclouds2s.
     */
    @Test
    public void testRequest() {
        System.out.println("testRequest");
        String appId = m_appId;
        String serverName = m_serverName;
        String serverSecret = m_serverSecret;
        Brainclouds2s instance = new Brainclouds2s();
        instance.init(appId, serverName, serverSecret);

        JSONObject json = new JSONObject();
        json.put("service", "globalEntity");
        json.put("operation", "GET_LIST");

        JSONObject orderBy = new JSONObject();
        JSONObject where = new JSONObject();
        JSONObject params = new JSONObject();
        where.put("entityType", "address");
        params.put("where", where);
        params.put("orderBy", orderBy);
        params.put("maxReturn", 5);
        json.put("data",params);
        IS2SCallback callback = new Callback(200,instance);
        instance.request(json, callback);
    }
    /**
     * Test of request method while logging, of class Brainclouds2s.
     */
    @Test
    public void testRequestWithLog() {
        System.out.println("testRequestWithLog");
        String appId = m_appId;
        String serverName = m_serverName;
        String serverSecret = m_serverSecret;
        Brainclouds2s instance = new Brainclouds2s();
        instance.init(appId, serverName, serverSecret);
        instance.setLogEnabled(true);
        JSONObject json = new JSONObject();
        json.put("service", "globalEntity");
        json.put("operation", "GET_LIST");

        JSONObject orderBy = new JSONObject();
        JSONObject where = new JSONObject();
        JSONObject params = new JSONObject();
        where.put("entityType", "address");
        params.put("where", where);
        params.put("orderBy", orderBy);
        params.put("maxReturn", 5);
        json.put("data",params);
        IS2SCallback callback = new Callback(200,instance);
        instance.request(json, callback);
    }
    
    /**
     * Test of request method with invalid secret, of class Brainclouds2s.
     */
    @Test
    public void testRequestInvalidSecret() {
        System.out.println("testRequestInvalidSecret");
        String appId = m_appId;
        String serverName = m_serverName;
        String serverSecret = "aaaaaaaa-bbbb-111111111-222222222222";
        Brainclouds2s instance = new Brainclouds2s();
        instance.init(appId, serverName, serverSecret);
//        instance.setLogEnabled(true);
        JSONObject json = new JSONObject();
        json.put("service", "globalEntity");
        json.put("operation", "GET_LIST");

        JSONObject orderBy = new JSONObject();
        JSONObject where = new JSONObject();
        JSONObject params = new JSONObject();
        where.put("entityType", "address");
        params.put("where", where);
        params.put("orderBy", orderBy);
        params.put("maxReturn", 5);
        json.put("data",params);
        IS2SCallback callback = new Callback(400,instance);
        instance.request(json, callback);
    }
    /**
     * Test of request method with invalid secret, of class Brainclouds2s.
     */
    @Test
    public void testRequestInvalidAppId() {
        System.out.println("testRequestInvalidSappId");
        String appId = "";
        String serverName = m_serverName;
        String serverSecret = "aaaaaaaa-bbbb-111111111-222222222222";
        Brainclouds2s instance = new Brainclouds2s();
        instance.init(appId, serverName, serverSecret);
        JSONObject json = new JSONObject();
        json.put("service", "globalEntity");
        json.put("operation", "GET_LIST");

        JSONObject orderBy = new JSONObject();
        JSONObject where = new JSONObject();
        JSONObject params = new JSONObject();
        where.put("entityType", "address");
        params.put("where", where);
        params.put("orderBy", orderBy);
        params.put("maxReturn", 5);
        json.put("data",params);
        IS2SCallback callback = new Callback(403,instance);
        instance.request(json, callback);
    }
    /**
     * Test of runCallbacks method, of class Brainclouds2s.
     */
    @Test
    public void testExpiredSession() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        System.out.println("testExpiredSession");
        String appId = m_appId;
        String serverName = m_serverName;
        String serverSecret = m_serverSecret;
        String expiredSessionId = "m336o7k9ekd7l71kqhtb1ce8ul";
        
        Field sessionId = Brainclouds2s.class.getDeclaredField("_sessionId");
        sessionId.setAccessible(true);
        Brainclouds2s instance = new Brainclouds2s();
        instance.init(appId, serverName, serverSecret);
        instance.setLogEnabled(true);
        sessionId.set(instance,expiredSessionId);
        JSONObject json = new JSONObject();
        json.put("service", "heartbeat");
        json.put("operation", "HEARTBEAT");

        IS2SCallback callback = new Callback(200,instance);
        System.out.println("testExpiredSession: initial sessionId:"+expiredSessionId);
        instance.request(json, callback);

    }
}
