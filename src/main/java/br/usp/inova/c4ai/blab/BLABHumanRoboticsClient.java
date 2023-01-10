package br.usp.inova.c4ai.blab;

import br.usp.inova.c4ai.blab.blab.BLABClient;
import br.usp.inova.c4ai.blab.hr.HumanRoboticsControl;
import io.humanrobotics.api.exception.RobiosException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BLABHumanRoboticsClient {

    private final HumanRoboticsControl robotControl;
    private final BLABClient blabControl;

    List<String> botNames;

    Properties config;

    String greeting;

    long userMessageTimeout;

    long botMessageTimeout;

    BlockingQueue<String> userMessageQueue;


    BlockingQueue<String> botMessageQueue;

    public BLABHumanRoboticsClient(Properties config) throws RobiosException {
        this.config = config;

        botMessageQueue = new LinkedBlockingQueue<>();
        userMessageQueue = new LinkedBlockingQueue<>();

        this.robotControl = new HumanRoboticsControl(
                config.getProperty("ROBIOS_ROBOT_ADDRESS"),
                config.getProperty("ROBIOS_ROBOT_ID"),
                config.getProperty("ROBIOS_API_KEY"),
                Long.parseLong(config.getProperty("DELAY_PER_CHARACTER_MS")),
                this::userMessageReceived
        );
        this.botNames = Arrays.stream(config.getProperty("BLAB_CHAT_BOTS").split(config.getProperty("BLAB_CHAT_BOTS_SEP", ","))).toList();
        this.greeting = config.getProperty("GREETING", "Hello");
        this.userMessageTimeout = Long.parseLong(config.getProperty("USER_MESSAGE_TIMEOUT"));
        this.botMessageTimeout = Long.parseLong(config.getProperty("BOT_MESSAGE_TIMEOUT"));
        this.blabControl = new BLABClient(config.getProperty("BLAB_CHAT_SERVER_URL"), config.getProperty("BLAB_CHAT_WS_SERVER_URL"), this::botMessageReceived);
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

    private void userMessageReceived(String text) {
        if (!userMessageQueue.offer(text)) {
            System.err.format("User said “%s”, but the message could not be added to the queue", text);
        }
    }

    private void botMessageReceived(String text) {
        if (!botMessageQueue.offer(text)) {
            System.err.format("Bot said “%s”, but the message could not be added to the queue", text);
        }
    }

    private String waitForUserMessage() {
        try {
            return userMessageQueue.poll(userMessageTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String waitForBotMessage() {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        do {
            try {
                String m = botMessageQueue.poll(isFirst ? botMessageTimeout : 100, TimeUnit.MILLISECONDS);
                if (m != null) {
                    if (!isFirst)
                        sb.append('\n');
                    sb.append(m);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }
            isFirst = false;
        } while (!botMessageQueue.isEmpty());
        return sb.toString();
    }

    public void start() {
        blabControl.startConversation("", botNames, " ", conversationId -> {
            robotControl.sayAndListen(greeting);
            String userMessage;
            while ((userMessage = waitForUserMessage()) != null) {
                blabControl.sendMessage(userMessage);
                String botMessage;
                if ((botMessage = waitForBotMessage()) == null) {
                    robotControl.close();
                    break;
                }
                robotControl.sayAndListen(botMessage);
            }
        });
    }

}