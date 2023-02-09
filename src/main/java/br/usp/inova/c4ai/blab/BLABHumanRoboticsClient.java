package br.usp.inova.c4ai.blab;

import br.usp.inova.c4ai.blab.blab.BLABClient;
import br.usp.inova.c4ai.blab.hr.HumanRoboticsControl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


/**
 * Client for BLAB controller that interacts with Robios robots and avatars.
 */
public class BLABHumanRoboticsClient {

    /**
     * Class logger.
     */
    private static final Logger logger = LogManager.getLogger();

    /**
     * Pattern that matches newline characters.
     */
    private static final Pattern NEWLINE = Pattern.compile("[\\r\\n]");

    /**
     * Instance of a class that handles bidirectional communication with Robios robots and avatars.
     */
    private final HumanRoboticsControl robotControl;

    /**
     * Instance of a class that handles bidirectional communication with BLAB Controller.
     */
    private final BLABClient blabControl;

    /**
     * List of bot names that are included in every conversation.
     */
    private final List<String> botNames;

    /**
     * Message that is sent to the user as soon as the conversation is started.
     */
    private final String greeting;

    /**
     * Time limit for the user to say something, in milliseconds.
     */
    private final long userMessageTimeout;

    /**
     * Time limit for the bot to say something, in milliseconds.
     */
    private final long botMessageTimeout;

    /**
     * Queue of messages sent by the user.
     */
    private final BlockingQueue<String> userMessageQueue;

    /**
     * Queue of messages sent by bots.
     */
    private final BlockingQueue<String> botMessageQueue;

    /**
     * Initializes an instance with a given configuration.
     *
     * @param config configuration (see *README.md* for details).
     */
    BLABHumanRoboticsClient(Properties config) {
        botMessageQueue = new LinkedBlockingQueue<>();
        userMessageQueue = new LinkedBlockingQueue<>();
        robotControl = new HumanRoboticsControl(
                config.getProperty("ROBIOS_ROBOT_ADDRESS"),
                config.getProperty("ROBIOS_ROBOT_ID"),
                config.getProperty("ROBIOS_API_KEY"),
                Long.parseLong(config.getProperty("DELAY_PER_CHARACTER", "0")),
                Long.parseLong(config.getProperty("MIN_DELAY", "0")),
                this::userMessageReceived
        );
        this.botNames = Arrays.stream(config.getProperty("BLAB_CHAT_BOTS").split(config.getProperty("BLAB_CHAT_BOTS_SEP", ","))).toList();
        this.greeting = config.getProperty("GREETING", "Hello");
        this.userMessageTimeout = Long.parseLong(config.getProperty("USER_MESSAGE_TIMEOUT"));
        this.botMessageTimeout = Long.parseLong(config.getProperty("BOT_MESSAGE_TIMEOUT"));
        this.blabControl = new BLABClient(config.getProperty("BLAB_CHAT_SERVER_URL"), config.getProperty("BLAB_CHAT_WS_SERVER_URL"), this::botMessageReceived);
    }

    /**
     * Enqueues a message sent by the user.
     * This method is called whenever a message is received from the user.
     *
     * @param text the text emitted (usually spoken) by the user
     */
    private void userMessageReceived(String text) {
        if (!userMessageQueue.offer(text)) {
            logger.error("User said \"{}\", but the message could not be added to the queue", text);
        }
    }

    /**
     * Enqueues a message sent by a bot.
     * This method is called whenever a message is received from a bot.
     *
     * @param text the text sent by a bot
     */
    private void botMessageReceived(String text) {
        if (!botMessageQueue.offer(text)) {
            logger.error("Bot said \"{}\", but the message could not be added to the queue", text);
        }
    }

    /**
     * Blocks until a message is received from the user in the message queue.
     *
     * @return the message sent by the user if it exists, or {@code null} otherwise.
     */
    private String waitForUserMessage() {
        logger.info("Waiting at most {}ms for a message from the user...", userMessageTimeout);
        String message;
        try {
            message = userMessageQueue.poll(userMessageTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.error("Queue retrieval was interrupted", e);
            message = null;
        }
        if (message != null) {
            logger.info("User said: \"{}\"", message);
        } else {
            logger.warn("Could not listen to user");
        }
        return message;
    }

    /**
     * Block until a message is received from a bot in the message queue.
     *
     * @return the message sent by the bot if it exists, or {@code null} otherwise.
     */
    private String waitForBotMessage() {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        logger.info("Waiting at most {}ms for a message from the bots...", botMessageTimeout);
        do {
            try {
                String message = botMessageQueue.poll(isFirst ? botMessageTimeout : 100, TimeUnit.MILLISECONDS);
                if (null != message) {
                    if (!isFirst)
                        sb.append(System.lineSeparator());
                    sb.append(message);
                }
            } catch (InterruptedException e) {
                logger.error("Queue retrieval was interrupted.", e);
                return null;
            }
            isFirst = false;
        } while (!botMessageQueue.isEmpty());
        String message = sb.toString();
        if (message.isBlank())
            logger.warn("Bot did not reply");
        else
            logger.info("Bot said: \"{}\"", NEWLINE.matcher(message).replaceAll(" "));
        return message;
    }

    /**
     * Start a conversation.
     */
    public void start() {
        blabControl.startConversation("", botNames, " ", conversationId -> {
            if (!robotControl.sayAndListen(greeting))
                logger.error("Failed to say greeting \"{}\" or to listen", greeting);
            String userMessage;
            while ((userMessage = waitForUserMessage()) != null) {
                blabControl.sendMessage(userMessage);
                String botMessage;
                if (null == (botMessage = waitForBotMessage())) {
                    robotControl.close();
                    break;
                }

                if (!robotControl.sayAndListen(botMessage))
                    logger.error("Failed to say \"{}\" or to listen", botMessage);
            }
        });
    }

}