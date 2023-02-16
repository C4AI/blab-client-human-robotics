package br.usp.inova.c4ai.blab.hr;

import io.humanrobotics.api.Robios;
import io.humanrobotics.api.RobiosApi;
import io.humanrobotics.api.RobiosConfig;
import io.humanrobotics.api.exception.RobiosException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Handles bidirectional communication with Robios robots and avatars.
 */
public class HumanRoboticsControl implements Closeable {

    /**
     * Class logger.
     */
    private static final Logger logger = LogManager.getLogger();

    /**
     * Pattern that matches whitespace characters
     **/
    private static final Pattern BLANK = Pattern.compile("\\s");

    /**
     * Address of the robot (Human Robotics server or local IP).
     */
    private final String robotAddress;

    /**
     * ID of the robot.
     */
    private final String robotId;

    /**
     * API key to access Human Robotics services.
     */
    private final String apiKey;

    /**
     * A {@link Robios} instance that represents a robot or an avatar.
     */
    private final Robios robios;

    /**
     * Function that is called whenever the user says something to the robot.
     */
    private final Consumer<String> callback;

    /**
     * How many milliseconds each non-space character lasts in the robot's voice, approximately.
     */
    private final long delayPerChar;

    /**
     * Minimum number of milliseconds to wait before listening to the user.
     */
    private final long minDelay;

    /**
     * Initializes an instance with the given arguments.
     *
     * @param robotAddress         address of the robot (Human Robotics server or local IP)
     * @param robotId              ID of the robot
     * @param apiKey               API key to access Human Robotics services
     * @param delayPerChar         how many milliseconds each non-space character takes in the robot's voice
     * @param minDelay             minimum number of milliseconds to wait before listening to the user.
     * @param callback             a function that is called whenever the user says something to the robot
     * @param disableNativeDialogs whether the native dialogs should be disabled at startup
     */
    public HumanRoboticsControl(String robotAddress, String robotId, String apiKey, long delayPerChar, long minDelay, Consumer<String> callback, boolean disableNativeDialogs) {
        this.robotAddress = robotAddress;
        this.robotId = robotId;
        this.apiKey = apiKey;
        try {
            this.robios = createRobios();
            robios.ext().setAllLogsEnabled(true);
        } catch (RobiosException e) {
            throw new RuntimeException(e);
        }
        this.callback = callback;
        this.delayPerChar = delayPerChar;
        this.minDelay = minDelay;
        if (disableNativeDialogs) {
            try {
                robios.useNativeDialogs(false);
            } catch (Exception e) {
                logger.warn("Could not disable native dialogs");
            }
        }
        robios.addVoiceRecognitionCallback(this::onUserTextReceived);
    }

    /**
     * Asks the robot/avatar to read a sentence aloud and then wait for user input.
     * This method does not block.
     * Since there seems to be no way to tell how long the robot's voice will take to pronounce the sentence,
     * an approximate delay is calculated as the product of {@code delayPerChar} and the number of non-blank
     * characters in the sentence, or {@code minDelay} in case this is longer.
     *
     * @param text the text to be said by the robot
     * @return whether the request was accepted
     */
    public boolean sayAndListen(String text) {
        try {
            long ms = Math.max(minDelay, BLANK.matcher(text).replaceAll("").length() * delayPerChar);
            logger.info("Waiting {}ms while sentence is spoken and then listening to user", ms);
            robios.say(text).delay(ms).listen();
            return true;
        } catch (Exception e) {
            logger.error("Error when listening or speaking", e);
            return false;
        }
    }

    /**
     * Ask the robot to wait for user input.
     * This method does not block.
     *
     * @return whether the request was accepted
     */
    public boolean listen() {
        try {
            robios.listen();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calls the callback function whenever a message is received from the user.
     *
     * @param text text sent by the user
     */
    private void onUserTextReceived(String text) {
        callback.accept(text);
    }

    /**
     * Creates a connection with Robios
     *
     * @return a {@link Robios} instance
     */
    private Robios createRobios() throws RobiosException {
        RobiosConfig config = new RobiosConfig();
        config.setRobotAddress(robotAddress);
        config.setRobotId(robotId);
        return RobiosApi.get(apiKey, config);
    }


    /**
     * Closes the connection.
     */
    public void close() {
        try {
            robios.close();
        } catch (Exception ignored) {
        }
    }
}
