package br.usp.inova.c4ai.blab;

import br.usp.inova.c4ai.blab.blab.BLABClient;
import br.usp.inova.c4ai.blab.hr.HumanRoboticsControl;
import io.humanrobotics.api.exception.RobiosException;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

class BLABHumanRoboticsClient {

    private static final Pattern NEWLINE = Pattern.compile("[\\r\\n]");
    private final HumanRoboticsControl robotControl;
    private final BLABClient blabControl;

    private final List<String> botNames;

    private final String greeting;

    private final long userMessageTimeout;

    private final long botMessageTimeout;

    private final BlockingQueue<String> userMessageQueue;


    private final BlockingQueue<String> botMessageQueue;

    BLABHumanRoboticsClient(Properties config) {

        botMessageQueue = new LinkedBlockingQueue<>();
        userMessageQueue = new LinkedBlockingQueue<>();

        try {
            robotControl = new HumanRoboticsControl(
                    config.getProperty("ROBIOS_ROBOT_ADDRESS"),
                    config.getProperty("ROBIOS_ROBOT_ID"),
                    config.getProperty("ROBIOS_API_KEY"),
                    Long.parseLong(config.getProperty("DELAY_PER_CHARACTER", "0")),
                    Long.parseLong(config.getProperty("MIN_DELAY", "0")),
                    this::userMessageReceived
            );
        } catch (RobiosException e) {
            throw new RuntimeException(e);
        }
        this.botNames = Arrays.stream(config.getProperty("BLAB_CHAT_BOTS").split(config.getProperty("BLAB_CHAT_BOTS_SEP", ","))).toList();
        this.greeting = config.getProperty("GREETING", "Hello");
        this.userMessageTimeout = Long.parseLong(config.getProperty("USER_MESSAGE_TIMEOUT"));
        this.botMessageTimeout = Long.parseLong(config.getProperty("BOT_MESSAGE_TIMEOUT"));
        this.blabControl = new BLABClient(config.getProperty("BLAB_CHAT_SERVER_URL"), config.getProperty("BLAB_CHAT_WS_SERVER_URL"), this::botMessageReceived);
    }

    private void userMessageReceived(String text) {
        if (!userMessageQueue.offer(text)) {
            System.err.format("User said “%s”, but the message could not be added to the queue%n", text);
        }
    }

    private void botMessageReceived(String text) {
        if (!botMessageQueue.offer(text)) {
            System.err.format("Bot said “%s”, but the message could not be added to the queue%n", text);
        }
    }

    private String waitForUserMessage() {
        System.out.format("Waiting at most %dms for a message from the user...%n", userMessageTimeout);
        String message;
        try {
            message = userMessageQueue.poll(userMessageTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            message = null;
        }
        if (message != null) {
            System.out.format("User said: %s%n", message);
        } else {
            System.out.println("Could not listen to user");
        }
        return message;
    }

    private String waitForBotMessage() {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        System.out.format("Waiting at most %dms for a message from the bots...%n", botMessageTimeout);
        do {
            try {
                String message = botMessageQueue.poll(isFirst ? botMessageTimeout : 100, TimeUnit.MILLISECONDS);
                if (null != message) {
                    if (!isFirst)
                        sb.append(System.lineSeparator());
                    sb.append(message);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }
            isFirst = false;
        } while (!botMessageQueue.isEmpty());
        String message = sb.toString();
        if (message.isBlank())
            System.out.println("Bot did not reply");
        else
            System.out.format("Bot said: %s%n", NEWLINE.matcher(message).replaceAll(" "));
        return message;
    }

    public void start() {
        blabControl.startConversation("", botNames, " ", conversationId -> {
            if (!robotControl.sayAndListen(greeting))
                System.err.format("Failed to say greeting or listen: “%s”%n", greeting);
            String userMessage;
            while ((userMessage = waitForUserMessage()) != null) {
                blabControl.sendMessage(userMessage);
                String botMessage;
                if (null == (botMessage = waitForBotMessage())) {
                    robotControl.close();
                    break;
                }

                if (!robotControl.sayAndListen(botMessage))
                    System.err.format("Failed to say or listen: “%s”%n", botMessage);
            }
        });
    }

}