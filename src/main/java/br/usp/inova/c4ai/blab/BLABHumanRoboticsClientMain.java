package br.usp.inova.c4ai.blab;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
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
     * Class logger.
     */
    private static final Logger logger = LogManager.getLogger();

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
            logger.fatal("File “{}” does not exist.", configFileName);
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.fatal("Could not read or parse the file “{}”.", configFileName);
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
        Configurator.setLevel(LogManager.getRootLogger(), Level.INFO);
        File file = new File(args.length >= 1 ? args[0] : "settings.ini");
        logger.info("Reading settings from “{}”.", file.getAbsolutePath());
        Properties config = loadConfig(file.getAbsolutePath());
        logger.debug(config);
        BLABHumanRoboticsClient client = new BLABHumanRoboticsClient(config);
        logger.info("Starting conversation...");
        client.start();
    }
}
