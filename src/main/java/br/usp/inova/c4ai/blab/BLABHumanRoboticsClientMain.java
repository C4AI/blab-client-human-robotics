package br.usp.inova.c4ai.blab;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Entry point for {@link BLABHumanRoboticsClient}.
 */
public final class BLABHumanRoboticsClientMain {

    /**
     * Loads configuration file.
     *
     * @param configFileName name of the UTF-8-encoded configuration file
     * @return a {@link Properties} instance with the parsed configuration
     */
    private static Properties loadConfig(String configFileName) {
        Properties properties = new Properties();
        try (InputStreamReader input = new InputStreamReader(new FileInputStream(configFileName), StandardCharsets.UTF_8)) {
            properties.load(input);
        } catch (FileNotFoundException e) {
            System.err.format("File “%s” does not exist.%n", configFileName);
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }

    /**
     * Starts the program.
     *
     * @param args command-line arguments (currently, the only argument is the settings file name, which is "settings.ini" by default)
     */
    public static void main(String[] args) {
        Properties config = loadConfig(args.length >= 1 ? args[0] : "settings.ini");
        BLABHumanRoboticsClient client = new BLABHumanRoboticsClient(config);
        client.start();
    }
}
