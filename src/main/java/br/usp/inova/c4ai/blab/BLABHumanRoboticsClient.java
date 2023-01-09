package br.usp.inova.c4ai.blab;

import io.humanrobotics.api.Robios;
import io.humanrobotics.api.RobiosApi;
import io.humanrobotics.api.RobiosConfig;
import io.humanrobotics.api.exception.RobiosException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BLABHumanRoboticsClient {

    Properties config;
    Robios robios;

    BlockingQueue<String> messageQueue;

    public BLABHumanRoboticsClient(Properties config) throws RobiosException {
        this.config = config;
        this.robios = createRobios(config.getProperty("ROBIOS_ROBOT_ADDRESS"), config.getProperty("ROBIOS_ROBOT_ID"), config.getProperty("ROBIOS_API_KEY"));
        messageQueue = new LinkedBlockingQueue<>();
        robios.addVoiceRecognitionCallback(this::onUserTextReceived);

    }

    private static Robios createRobios(String robotAddress, String robotId, String apiKey) throws RobiosException {
        RobiosConfig config = new RobiosConfig();
        config.setRobotAddress(robotAddress);
        config.setRobotId(robotId);
        return RobiosApi.get(apiKey, config);
    }

    private static Properties loadConfig(String configFileName) {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFileName)) {
            properties.load(fis);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }

    public static void main(String[] args) throws Exception {
        Properties config = loadConfig("settings.ini");
        BLABHumanRoboticsClient client = new BLABHumanRoboticsClient(config);
        client.start();
    }


    public void start() throws Exception {
        robios.ask("Olá!");
        for (int i = 0; i < 5; i++) {
            robios.listen();
            String userMessage = messageQueue.take();
            robios.ask("Você disse: " + userMessage);
        }
        robios.close();
    }

    private void onUserTextReceived(String text) {
        messageQueue.add(text);
    }
}