package Botster;

import org.jibble.pircbot.PircBot;

import java.util.concurrent.LinkedBlockingDeque;

public class CommandSender extends Thread {
    public static final int THREAD_YIELD = 50;
    private final LinkedBlockingDeque<String> commands;
    private final Sender sender;
    private boolean askedToStop;

    CommandSender(final PircBot bot) {
        commands = new LinkedBlockingDeque<>();
        sender = new Sender(bot);
    }

    public void sendRawCommand(final String command) {
        commands.add(sanitizeMessage(command));
    }

    public void sendNotice(final String target, final String message) {
        commands.add("NOTICE " + target + " :" + sanitizeMessage(message));
    }

    public void sendMessage(final String target, final String message) {
        commands.add("PRIVMSG " + target + " :" + sanitizeMessage(message));
    }

    public void setMode(final String target, final String mode) {
        commands.add("MODE " + target + " " + sanitizeMessage(mode));
    }

    public void joinChannel(final String channel) {
        commands.add("JOIN " + sanitizeMessage(channel));
    }

    public void joinChannel(final String channel, final String key) {
        commands.add("JOIN " + channel + " " + sanitizeMessage(key));
    }

    public void partChannel(final String channel) {
        commands.add("PART " + sanitizeMessage(channel));
    }

    public void changeNick(final String nick) {
        commands.add("NICK :" + sanitizeMessage(nick));
    }

    public void quitServer(final String quitMessage) {
        commands.add("QUIT :" + sanitizeMessage(quitMessage));
    }

    @Override
    public void run() {
        while (!askedToStop) {
            try {
                final String command = commands.takeFirst();

                if (sender.canHandle(command)) {
                    sender.execute(command);
                } else {
                    commands.putFirst(command);
                    sleep(THREAD_YIELD);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // If the user appends \r\n to a message it will add 2 characters which in very rare circumstances
    // will slow down sending messages due to a longer message with useless \r\n at the end, since it'll
    // be appended later
    private String sanitizeMessage(final String message) {
        return message.replace("\r", "").replace("\n", "");
    }

    public void askToStop() {
        askedToStop = true;
    }
}