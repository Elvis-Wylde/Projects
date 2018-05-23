package com.wylde.model;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class Configuration {

    private static ResourceBundle bundle = getResourceBundle();
    private static byte[] fileContent;

    public static String get(String key, String defaultValue) {
        try {
            return bundle.getString(key);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    public static int getInteger(String key, int defaultValue) {
        try {
            String value = bundle.getString(key);
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    public static byte[] getTerrainMeta() {
        if (fileContent != null) {
            return fileContent;
        }
        String separator = System.getProperty("file.separator");
        String curPath = System.getProperty("java.class.path");
        String proFilePath = curPath.substring(0, curPath.lastIndexOf(separator)) + separator + "config" + separator +"terrain.json";
        // RunTime
        // String proFilePath = System.getProperty("user.dir") + separator + "TileService" + separator + "config" + separator +"terrain.json";
        // Debug
        // String proFilePath = System.getProperty("user.dir") + separator + "config" + separator +"mongo.properties";
        File file = new File(proFilePath);
        fileContent = new byte[(int)file.length()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(fileContent);
            in.close();
            return fileContent;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static ResourceBundle getResourceBundle() {
        ResourceBundle rb;
        BufferedInputStream inputStream;
        String separator = System.getProperty("file.separator");
        String curPath = System.getProperty("java.class.path");
        String proFilePath = curPath.substring(0, curPath.lastIndexOf(separator)) + separator + "config" + separator +"mongo.properties";
        // RunTime
        // String proFilePath = System.getProperty("user.dir") + separator + "TileService" + separator + "config" + separator +"mongo.properties";
        // Debug
        // String proFilePath = System.getProperty("user.dir") + separator + "config" + separator +"mongo.properties";
        try {
            inputStream = new BufferedInputStream(new FileInputStream(proFilePath));
            rb = new PropertyResourceBundle(inputStream);
            inputStream.close();
            return rb;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
