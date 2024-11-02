package io.github.crisenpuer.tpksp.util;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;

import java.io.IOException;

public class Cmd {

    private final String msg;
    private final String sender;
    private final String prefix;
    private final String streamerName;

    /**
      * Utility class for checking commands
      *
      * @param event The main ChannelMessageEvent
      * @param prefix Command prefix used in comparasions
      * @param streamerName User that have bot admin privilleges
      * @param chatClient Chat client that can be created from TwitchClient
      */
    public Cmd(ChannelMessageEvent event, String prefix, String streamerName) {
        this.msg = event.getMessage();
        this.sender = event.getUser().getName(); // Correctly retrieve the sender's name
        this.prefix = prefix;
        this.streamerName = streamerName;
    }

    /**
     * Checks if command is equal to given command.
     *
     * @param command the command to be compared to
     */
    public boolean is(String command) {
        String expectedCommand = prefix + command.trim();
    
        // Split the message to get command and arguments
        String[] parts = this.msg.split(" ", 2); // Split into command and the rest
        
        if (parts.length < 1 || !parts[0].equalsIgnoreCase(expectedCommand)) {
            return false; // Command doesn't match
        }
        return true;
    }

    /**
     * Checks if command is equal to given command and if it's a moderator only command.
     *
     * @param command the command to be compared to
     * @param isProtected if the command is streamer only
     * @throws IOException if there is an issue checking the user's moderator status
     */
    public boolean is(String command, boolean isProtected) {
        if (is(command)) {
            if (isProtected) {
                if (streamerName.equalsIgnoreCase(this.sender)) {
                    return true;
                }
                return false;
            }
            return true;
        }
        return false;
    }
}
