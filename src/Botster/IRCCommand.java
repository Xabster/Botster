/*This file is part of Botski.

 Botski is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Botski is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Botski.  If not, see <http://www.gnu.org/licenses/>.

 Copyright (C) 2010 Olaf Keijsers
 */

package Botster;

import java.util.HashSet;
import java.util.Set;

/**
 * The IRCCommand class is a basis for building new responses. Extending classes
 * can register public or restricted commands. Only public commands will be
 * visible when the help command is issued.
 */
public abstract class IRCCommand {
    /**
     * The nickname of the person who made the last request.
     */
    private String nickName;

    /**
     * The login (name) of the person who made the last request.
     */
    private String login;

    /**
     * The hostname of the person who made the last request.
     */
    private String hostName;

    /**
     * The channel in which the last request was made.
     */
    private String channel;

    /**
     * A reference to the Botster containing the command.
     */
    private Botster bot;

    /**
     * A Set of public commands this instance registered.
     */
    private final Set<String> commands = new HashSet<>();

    /**
     * A set of restricted commands this instance registered.
     */
    private final Set<String> restrictedCommands = new HashSet<>();

    /**
     * Sets the nickName, login and hostName for this command.
     *
     * @param nickName the nick name of the person who made last the request
     * @param login    the login of the person who made last the request
     * @param hostName the host name of the person who made last the request
     */
    public void setNames(final String nickName, final String login, final String hostName, final String channel) {
        this.nickName = nickName;
        this.login = login;
        this.hostName = hostName;
        this.channel = channel;
    }

    /**
     * Sets the reference to the Botster this command belongs to.
     *
     * @param bot the Botster this command belongs to.
     */
    public void setBot(final Botster bot) {
        this.bot = bot;
    }

    /**
     * Generates a reply based on a command and parameters.
     *
     * @param command the command used to get to this IRCCommand
     * @param message the parameters, if any, that were used
     */
    public abstract String getReply(String command, String message);

    /**
     * Registers a public command for this instance.
     *
     * @param command the name of the command to register
     */
    protected void addCommand(final String command) {
        commands.add(command);
    }

    /**
     * Registers a restricted command for this instance.
     *
     * @param command the name of the command to register
     */
    protected void addRestrictedCommand(final String command) {
        restrictedCommands.add(command);
    }

    /**
     * Gets the public commands registered by this instance.
     *
     * @return a Set<String> with the public commands for this IRCCommand
     */
    public Set<String> getCommands() {
        return commands;
    }

    /**
     * Gets the restricted commands registered by this instance.
     *
     * @return a Set<String> with the restricted commands for this IRCCommand
     */
    public Set<String> getRestrictedCommands() {
        return restrictedCommands;
    }

    /**
     * Gets the Botster that this IRCCommand uses
     *
     * @return the Botster that this IRCCommand uses
     */
    protected Botster getBot() {
        return bot;
    }

    /**
     * Returns the nick name of the person making the last request
     *
     * @return the nick name of the person making the last request
     */
    protected String getNickName() {
        return nickName;
    }

    /**
     * Returns the login of the person making the last request
     *
     * @return the login of the person making the last request
     */
    protected String getLogin() {
        return login;
    }

    /**
     * Returns the host name of the person making the last request
     *
     * @return the host name of the person making the last request
     */
    protected String getHostName() {
        return hostName;
    }

    /**
     * Returns the channel in which the last request was made
     *
     * @return the channel in which the last request was made
     */
    protected String getChannel() {
        return channel;
    }

    /**
     * Queries the Botster whether the user making the request is an authorized
     * user, based on their nick name, login and host name.
     */
    protected boolean isAuthorizedUser() {
        return getBot().isAuthorizedUser(getHostName());
    }
}