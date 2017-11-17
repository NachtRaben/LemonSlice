package com.nachtraben.lemonslice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by NachtDesk on 7/22/2016.
 */
public class ConfigurationUtils {
    private static Logger logger = LoggerFactory.getLogger(ConfigurationUtils.class);

    public static final Gson GSON_P;

    static {
        GsonBuilder gb = new GsonBuilder();
        gb.setPrettyPrinting();
        gb.disableHtmlEscaping();
        GSON_P = gb.create();
    }

    public static File getConfigFile(ClassLoader classLoader, String file, File dataDir) throws IOException {
        File config = new File(dataDir, file);
        dataDir.mkdirs();
        if (!config.exists()) {
            InputStream in = classLoader.getResourceAsStream(file);
            if (in != null) {
                logger.info("No configuration file present, Creating default configuration.");
                if (!config.createNewFile()) {
                    logger.error("Failed to create default configuration!");
                    throw new RuntimeException("Failed to create default configuration!");
                }
                FileOutputStream out = new FileOutputStream(config);
                try {
                    writeToFile(in, out, 1024);
                } catch (IOException e) {
                    logger.error("Failed to copy default configuration.");
                    e.printStackTrace();
                }
            } else {
                if (!config.createNewFile()) {
                    logger.error("Failed to create configuration file!");
                    throw new RuntimeException("Failed to create configuration file!");
                }
            }
        }
        return config;
    }


    public static <T extends JsonProperties> T loadProperties(String fileName, File dataDir, Class<? extends T> propertyMappings) {
        return loadProperties(ConfigurationUtils.class.getClassLoader(), fileName, dataDir, propertyMappings);
    }

    public static <T extends JsonProperties> T loadProperties(ClassLoader classLoader, String filename, File dataDir, Class<? extends T> propertyMappings) {
        try {
            File config = getConfigFile(classLoader, filename, dataDir);
            Constructor<? extends T> constructor = propertyMappings.getDeclaredConstructor();
            constructor.setAccessible(true);
            T properties = constructor.newInstance();
            FileReader in = new FileReader(config);
            JsonReader jr = new JsonReader(in);
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(jr);
            if (je.isJsonNull()) {
                System.out.println("Populating defaults!");
                saveData(filename, dataDir, properties);
            }
            properties.read(je);
            in.close();
            return properties;
        } catch (IOException e) {
            logger.error("Failed to read " + filename + ".", e);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            logger.error("Failed to read " + filename + ", constructor wasn't accessible.", e);
        }
        return null;
    }

    public static <T extends CustomJsonIO> T load(String filename, File dataDir, T data) {
        File config = new File(dataDir, filename);
        dataDir.mkdirs();
        if (!config.exists()) {
            try {
                config.createNewFile();
                saveData(filename, dataDir, data);
            } catch (IOException e) {
                logger.error("Failed create file " + filename + ".", e);
            }
        } else {
            FileReader in;
            try {
                in = new FileReader(config);
                JsonReader jr = new JsonReader(in);
                JsonParser jp = new JsonParser();
                JsonElement je = jp.parse(jr);
                data.read(je);
                in.close();
                return data;
            } catch (IOException e) {
                logger.error("Failed to read " + filename + ".", e);
            }
        }
        return data;
    }

    public static <T extends CustomJsonIO> void saveData(String filename, File dataDir, T data) {
        File config = new File(dataDir, filename);
        try {
            if (data != null) {
                if (!dataDir.exists())
                    dataDir.mkdirs();
                if (!config.exists())
                    config.createNewFile();

                FileWriter fw = new FileWriter(config);
                GSON_P.toJson(data.write(), fw);
                fw.close();
                File backupCopy = new File(dataDir, config + ".backup");
                if (!backupCopy.exists())
                    backupCopy.createNewFile();
                Files.copy(config.toPath(), backupCopy.toPath(), REPLACE_EXISTING);
            }
        } catch (IOException e) {
            logger.debug("Failed to write configuration " + filename + ".", e);
        }
    }


    private static void writeToFile(InputStream in, FileOutputStream out, int buffer) throws IOException {
        int read;
        byte[] bytes = new byte[buffer];
        while ((read = in.read(bytes)) != -1) {
            out.write(bytes, 0, read);
        }
        in.close();
        out.flush();
        out.close();
    }
}
