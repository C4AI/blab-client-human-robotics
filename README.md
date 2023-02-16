# BLAB Client - Human Robotics

This program integrates [Human Robotics](https://www.humanrobotics.ai/) robots/avatars
with [BLAB Controller](https://github.com/C4AI/blab-controller).

## Instructions

### Prerequisites

1. Install Java Runtime Environment ≥ 17. If you want to compile from source, install Java Development Kit ≥ 17.

    - Make sure that `java` points to JRE/JDK 17 or later versions. The version is displayed in the
      output of `java -version`. If you have multiple versions installed, set the `JAVA_HOME`
      system variable accordingly.

2. Set up a [Robios](https://www.humanrobotics.ai/robios) instance using either
   a [physical robot](https://www.humanrobotics.ai/robios) or an avatar
   on [the Android app](https://play.google.com/store/apps/details?id=ai.humanrobotics.robot.head.maestro).

   You will need:
    - the API key;
    - the robot address (`robots.humanrobotics.ai` for avatars, IPs/hostnames for physical robots);
    - the robot ID.

   On the [website](https://robots.humanrobotics.ai/login), choose the robot/avatar and see the ID in the item _Robot
   ID_.

   On the [app](https://play.google.com/store/apps/details?id=ai.humanrobotics.robot.head.maestro), long-press
   _Avançado_ and see the address and ID in the items _IP_ and _Robot_, respectively.

3. Set up or use an existing installation of [BLAB Controller](https://github.com/C4AI/blab-controller).
   You will need:
    - the HTTP(S) server URL (e.g. `http://localhost:8000/api/chat` for local development environments);
    - the WebSocket server URL (e.g. `ws://localhost:8000/ws/chat` for local development environments);
    - the exact names of the bots to include in the conversation.

4. Create a UTF-8-encoded file `settings.ini` with the contents below,
   fill in the blanks and optionally edit the pre-filled values,
   according to the respective comments:

    ```ini
    ########## ROBIOS ##########
    
    # API key that allows access to Human Robotics services
    ROBIOS_API_KEY=
    
    # robot address
    ROBIOS_ROBOT_ADDRESS=
    
    # id of the robot or avatar
    ROBIOS_ROBOT_ID=
    
    # whether native dialogs should be disabled at startup
    ROBIOS_DISABLE_NATIVE_DIALOGS=true
    
    ########## BLAB ##########
    
    # the address to access BLAB HTTP(S) server
    BLAB_CHAT_SERVER_URL=
    
    # the address to access BLAB WebSocket server
    BLAB_CHAT_WS_SERVER_URL=
    
    # a comma-separated list of bots to include in every conversation
    #   (if required, the separator can be changed in the next parameter)
    BLAB_CHAT_BOTS=
    
    # the separator used in the previous parameter
    #  (comma by default; it must not be a substring of any bot name)
    BLAB_CHAT_BOTS_SEP=,
    
    
    ########## CLIENT ##########
    
    # the first sentence that the bot says
    GREETING=Hi
    
    # how long should we wait for the user to say something (in milliseconds)
    USER_MESSAGE_TIMEOUT=60000
    
    # how long should we wait for the BLAB bot to answer (in milliseconds)
    BOT_MESSAGE_TIMEOUT=60000
    
    # how many milliseconds per non-space character should we wait after asking the robot to say something
    DELAY_PER_CHARACTER=65
    
    # minimum delay (in milliseconds), in case the value computed using the previous parameter is too small for short sentences
    MIN_DELAY=500
    
    ```

### Running from a fat JAR

If you have a fat JAR (a.k.a. JAR with dependencies or uber JAR), you can execute the program by running the following
command, replacing the file names/paths with those of your files:

```shell
java -jar blab-client-human-robotics-1.0.0-jar-with-dependencies.jar settings.ini
```

**NOTE:** on some operating systems, it may be necessary to add an extra argument to the previous command line:

```shell
java --add-opens=java.base/java.lang=ALL-UNNAMED -jar blab-client-human-robotics-1.0.0-jar-with-dependencies.jar settings.ini
```

### Compiling from source and running

1. Install [Maven](https://maven.apache.org/) 3.6.3 or newer.

2. Obtain the [Human Robotics](https://www.humanrobotics.ai/) libraries. Create a `lib/` directory in the
   project root and store those JAR files in subdirectories according
   to [Maven2 Repository Layout](https://maven.apache.org/repository/layout.html):
    - `lib/io/humanrobotics/api/4.0.0/api-4.0.0.jar`
    - `lib/io/humanrobotics/api/4.0.0/api-4.0.0-javadoc.jar`
    - `lib/io/humanrobotics/communication/2.2.0/communication-2.2.0.jar`
    - `lib/io/humanrobotics/messaging/2.0.0/messaging-2.0.0.jar`

   **NOTE:** these files may not be publicly available.

3. Compile:
    ```shell
    mvn compile
    ``` 

4. Run the program:
    ```shell
    mvn exec:java -Dexec.args='"settings.ini"'
    ``` 
   Note: replace `settings.ini` with the name or path to your settings file.

5. _(Optional)_ Generate JAR files:
    ```shell
    mvn package
    ``` 
   Two JAR files will be generated in the `target` directory. The fat JAR (JAR with dependencies)
   contains all the dependencies, hence it can be copied to other computers, and it is enough to run the
   program.

   **IMPORTANT:** the Human Robotics libraries will also be embedded — make sure you are
   allowed to distribute those files.
