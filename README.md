# BLAB Client - Human Robotics

## Instructions

1. Create a UTF-8-encoded file `settings.ini` with the following contents,
   fill in the missing values and optionally edit the pre-filled values,
   according to the respective comments:

    ```ini
    ########## ROBIOS ##########
    
    # API key that allows access to Human Robotics services
    ROBIOS_API_KEY=...
    
    # Robot address ("robots.humanrobotics.ai" for avatars,
    #   IPs/hostnames for physical robots)
    ROBIOS_ROBOT_ADDRESS=...
    
    # Id of the robot or avatar
    ROBIOS_ROBOT_ID=...
    
    
    ########## BLAB ##########
    
    # URL to access BLAB HTTP(S) server
    #   (e.g. "http://localhost:8000/api/chat" for local development environments)
    BLAB_CHAT_SERVER_URL=...
    
    # URL to access BLAB WebSocket server
    #   (e.g. "ws://localhost:8000/ws/chat" for local development environments)
    BLAB_CHAT_WS_SERVER_URL=...
    
    # Comma-separated list of bots to include in every conversation
    #   (if required, the separator can be changed in the next parameter)
    BLAB_CHAT_BOTS=...
    
    # Separator used in the previous parameter
    #  (comma by default; it must not be a substring of any bot name)
    BLAB_CHAT_BOTS_SEP=,
    
    
    ########## CLIENT ##########
    
    # The first sentence that the bot says
    GREETING=Hi
    
    # How long should we wait for the user to say something? (in milliseconds)
    USER_MESSAGE_TIMEOUT=60000
    
    # How long should we wait for the bot to answer? (in milliseconds)
    BOT_MESSAGE_TIMEOUT=60000
    
    # When the robot says something, when should we ask it to start listening?
    #   The number of non-space characters in the robot's sentence is multiplied
    #   by this factor to obtain the delay in milliseconds.
    DELAY_PER_CHARACTER=65
    
    # For short sentences, sometimes the value computed using the previous
    #   parameter is not enough, hence a minimum delay (in milliseconds)
    #   can be defined
    MIN_DELAY=500
    
    ```

