package br.usp.inova.c4ai.blab;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class BLABHumanRoboticsClientMain {
    public static Properties loadConfig(String configFileName) {
        Properties properties = new Properties();
        try (InputStreamReader input = new InputStreamReader(new FileInputStream(configFileName), StandardCharsets.UTF_8)) {
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }

    public static void main(String[] args) {
        Properties config = loadConfig(args.length >= 1 ? args[0] : "settings.ini");
        BLABHumanRoboticsClient client = new BLABHumanRoboticsClient(config);
        client.start();
    }
}
