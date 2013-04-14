package Botster;

import org.jibble.pircbot.PircBot;

class Sender {
    private final PircBot pircBot;
    private static final long SERVER_TIME_CONSTANT = 10000;
    private long timeVariable;

    Sender(final PircBot bot) {
        timeVariable = System.currentTimeMillis();
        this.pircBot = bot;
    }

    public boolean canHandle(final String command) {
        final int milisecondsNeeded = (2 + (command.length() / 128)) * 1000;

        if (timeVariable < System.currentTimeMillis()) {
            timeVariable = System.currentTimeMillis() + milisecondsNeeded;
            return true;
        } else if ((timeVariable + milisecondsNeeded) < (System.currentTimeMillis() + SERVER_TIME_CONSTANT)) {
            timeVariable += milisecondsNeeded;
            return true;
        }
        return false;
    }

    public void execute(final String command) {
        pircBot.sendRawLine(command);
    }
}