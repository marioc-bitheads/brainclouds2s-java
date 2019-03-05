/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bitheads.braincloud.s2s;

import static com.bitheads.braincloud.s2s.Brainclouds2sSetupTest.m_serverUrl;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
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
public class Brainclouds2sClientTest {

    static protected String m_serverUrl = "";
    static protected String m_appId = "";
    static protected String m_serverSecret = "";
    static protected String m_serverName = "";

    public Brainclouds2sClientTest() {
    }

    private static Brainclouds2s _s2sClient;

    /// <summary>
    /// Routine loads up brainCloud configuration info from "tests/ids.txt" (hopefully)
    /// in a platform agnostic way.
    /// </summary>
    static private void LoadIds() {
        if (m_serverUrl.length() > 0) {
            return;
        }

        File idsFile = new File("ids.txt");
        try {
            System.out.println("Looking for ids.txt file in " + idsFile.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (idsFile.exists()) {
            System.out.println("Found ids.txt file");
        }

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

        LoadIds();
        _s2sClient = new Brainclouds2s();
        _s2sClient.init(m_appId, m_serverName, m_serverSecret);
        _s2sClient.setLogEnabled(true);

        if (!_s2sClient.isIsInitialized()) {
            fail("Initialization Failed");
        }
    }

    @AfterClass
    public static void tearDownClass() {
        _s2sClient.disconnect();
    }

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {
    }

    /**
     * Test of globalEntity service, of Brainclouds2s request.
     */
    @Test
    public void testGetList() {
        System.out.println("testGetList");
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
        json.put("data", params);

        _s2sClient.request(json, (Brainclouds2s context, JSONObject jsonData) -> {
            if (context != _s2sClient) {
                fail("wrong context returned");
            }
            if (jsonData.getInt("status") != 200) {
                fail("Error returned");
            }
            if (!jsonData.has("data")) {
                fail("Missing data in return");
            }
            if (!jsonData.getJSONObject("data").has("entityList")) {
                fail("Missing entityList in return");
            }
            JSONArray list = jsonData.getJSONObject("data").getJSONArray("entityList");

            System.err.println(jsonData.toString());
        });
    }

    /**
     * Test of globalApp service, of Brainclouds2s request.
     */
    @Test
    public void testReadProperties() {
        System.out.println("testReadProperties");
        JSONObject json = new JSONObject();
        json.put("service", "globalApp");
        json.put("operation", "READ_PROPERTIES");

        _s2sClient.request(json, (Brainclouds2s context, JSONObject jsonData) -> {
            if (context != _s2sClient) {
                fail("wrong context returned");
            }
            if (jsonData.getInt("status") != 200) {
                fail("Error returned");
            }

            if (jsonData.has("Status")) {
                JSONObject Sample = jsonData.getJSONObject("Sample");
                if (Sample.has("value")) {
                    if (!"DoNotChangeThisValue".equals(Sample.getString("value"))) {
                        fail("Unexpected READ_PROPERTIES response " + Sample.toString());
                    }
                }
            }
            System.err.println(jsonData.toString());
        });
    }

    /**
     * Test of globalApp service, of Brainclouds2s request.
     */
    @Test
    public void testReadPropertiesString() {
        System.out.println("testReadProperties");
        String json = "{\"service\":\"globalApp\",\"operation\":\"READ_PROPERTIES\"}";
        _s2sClient.request(json, (Brainclouds2s context, String jsonString) -> {
            if (context != _s2sClient) {
                fail("wrong context returned");
            }
            if (jsonString.length() == 0) {
                fail("Error returned");
            }
            JSONObject jsonData = new JSONObject(jsonString);
            if (jsonData.has("Status")) {
                JSONObject Sample = jsonData.getJSONObject("Sample");
                if (Sample.has("value")) {
                    if (!"DoNotChangeThisValue".equals(Sample.getString("value"))) {
                        fail("Unexpected READ_PROPERTIES response " + Sample.toString());
                    }
                }
            }
            System.err.println(jsonData.toString());
        });
    }

    /**
     * Test of script service, of Brainclouds2s request.
     */
    @Test
    public void testGetScheduleCloudScripts() {
        System.out.println("testGetScheduleCloudScripts");
        JSONObject json = new JSONObject();
        json.put("service", "script");
        json.put("operation", "GET_SCHEDULED_CLOUD_SCRIPTS");

        JSONObject params = new JSONObject();
        params.put("startDateUTC", 1508295320000l);
        json.put("data", params);

        _s2sClient.request(json, (Brainclouds2s context, JSONObject jsonData) -> {
            if (context != _s2sClient) {
                fail("wrong context returned");
            }
            if (jsonData.getInt("status") != 200) {
                fail("Error returned");
            }
            System.err.println(jsonData.toString());
        });
    }

    /**
     * Test of event service, of Brainclouds2s request.
     */
    @Test
    public void testSendEvent() {
        System.out.println("testSendEvent");
        JSONObject json = new JSONObject();
        json.put("service", "event");
        json.put("operation", "SEND");

        JSONObject eventData = new JSONObject();
        JSONObject data = new JSONObject();
        eventData.put("someMapAttribute", "somValue");
        data.put("toId", "player1");
        data.put("eventType", "type1");
        data.put("eventData", eventData);
        data.put("recordLocally", false);
        json.put("data", data);

        _s2sClient.request(json, (Brainclouds2s context, JSONObject jsonData) -> {
            if (context != _s2sClient) {
                fail("wrong context returned");
            }
            if (jsonData.getInt("status") != 200) {
                fail("Error returned");
            }
            System.err.println(jsonData.toString());
        });
    }

    /**
     * Test of disconnect, of Brainclouds2s request.
     */
    @Test
    public void testDisconnect() {
        System.out.println("testDisconnect");
        JSONObject json = new JSONObject();
        json.put("service", "globalApp");
        json.put("operation", "READ_PROPERTIES");

        _s2sClient.disconnect();

        _s2sClient.request(json, (Brainclouds2s context, JSONObject jsonData) -> {
            if (context != _s2sClient) {
                fail("wrong context returned");
            }
            if (200 != jsonData.getInt("status")) {
                fail("Error returned");
            }
            System.err.println(jsonData.toString());
        });
    }
}
