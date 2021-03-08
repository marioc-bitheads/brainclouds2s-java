package com.bitheads.brainclouds2s;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bitheads.brainclouds2s.BrainCloudS2S;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class BrainCloudS2STest {

    static protected String m_serverUrl = "";
    static protected String m_appId = "";
    static protected String m_serverSecret = "";
    static protected String m_serverName = "";

    public BrainCloudS2STest() {
    }

    private static BrainCloudS2S _s2sClient;

    /// <summary>
    /// Routine loads up brainCloud configuration info from "${PROJECT_ROOT}/ids.txt"
    /// (hopefully)
    /// in a platform agnostic way.
    /// </summary>
    static private void loadIds() {
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
                case "s2sUrl":
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
        loadIds();
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {

        System.out.println("setUp()");

        _s2sClient = new BrainCloudS2S();
        _s2sClient.init(m_appId, m_serverName, m_serverSecret, m_serverUrl);
        _s2sClient.setLogEnabled(true);

        if (!_s2sClient.isIsInitialized()) {
            fail("Initialization Failed");
            _s2sClient = null;
        }
    }

    @After
    public void tearDown() {

        System.out.println("tearDown()");

        if (_s2sClient != null) {
            _s2sClient.disconnect();
            _s2sClient = null;
        }
    }

    /**
     * Test of globalEntity service, of Brainclouds2s request.
     */
    @Test
    public void testGetList() {

        System.out.println("testGetList()");

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

        _s2sClient.request(json, (BrainCloudS2S context, JSONObject jsonData) -> {
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

            System.out.println(jsonData.toString());
        });
    }
}
