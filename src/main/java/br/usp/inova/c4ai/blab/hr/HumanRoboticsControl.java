package br.usp.inova.c4ai.blab.hr;

import io.humanrobotics.api.Robios;
import io.humanrobotics.api.RobiosApi;
import io.humanrobotics.api.RobiosConfig;
import io.humanrobotics.api.exception.RobiosException;

import java.io.Closeable;
import java.util.function.Consumer;

import static java.lang.Math.max;


public class HumanRoboticsControl implements Closeable {

    private final String robotAddress;
    private final String robotId;
    private final String apiKey;

    private final Robios robios;

    private final Consumer<String> callback;

    private final long delayPerChar;

    private final long minDelay;


    public HumanRoboticsControl(String robotAddress, String robotId, String apiKey, long delayPerChar, long minDelay, Consumer<String> callback) throws RobiosException {
        this.robotAddress = robotAddress;
        this.robotId = robotId;
        this.apiKey = apiKey;
        this.robios = createRobios();
        this.callback = callback;
        this.delayPerChar = delayPerChar;
        this.minDelay = minDelay;
        robios.addVoiceRecognitionCallback(this::onUserTextReceived);
    }

    public boolean sayAndListen(String text) {
        try {
            long ms = max(minDelay, text.replaceAll("\\s", "").length() * delayPerChar);
            System.out.format("Waiting %dms while sentence is spoken and then listening to user%n", ms);
            robios.say(text).delay(ms).listen();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean listen() {
        try {
            robios.listen();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void onUserTextReceived(String text) {
        callback.accept(text);
    }

    private Robios createRobios() throws RobiosException {
        RobiosConfig config = new RobiosConfig();
        config.setRobotAddress(robotAddress);
        config.setRobotId(robotId);
        return RobiosApi.get(apiKey, config);
    }


    public void close() {
        try {
            robios.close();
        } catch (Exception ignored) {
        }
    }
}
