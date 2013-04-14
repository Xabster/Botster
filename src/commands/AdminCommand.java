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

package commands;

import Botster.IRCCommand;

import java.util.Set;

/**
 * The AdminCommand is a plugin that provides administration commands to the bot
 * such as having it quit, join/part channels, change nick name and reload
 * plugins.
 */
public class AdminCommand extends IRCCommand {
    /**
     * Creates a new AdminCommand and registers its options as restricted, so
     * they will not show up when someone requests the public commands.
     */
    public AdminCommand() {
        addRestrictedCommand("quit");
        addRestrictedCommand("reload");
        addRestrictedCommand("join");
        addRestrictedCommand("part");
        addRestrictedCommand("nick");
        addRestrictedCommand("load");
    }

    @Override
    public String getReply(final String command, final String message) {
        String ret = null;
        if (isAuthorizedUser()) {
            if (command.equals("quit")) {
                getBot().getCommandSender().quitServer("Leaving!");
                System.exit(0);
            }
            if (command.equals("reload")) {
                getBot().reloadCommands();
                ret = "Reloaded commands.";
            }
            if (command.equals("join")) {
                getBot().getCommandSender().joinChannel(message);
            }
            if (command.equals("part")) {
                getBot().getCommandSender().partChannel(message);
            }
            if (command.equals("nick")) {
                getBot().getCommandSender().changeNick(message);
            }
            if (command.equals("load")) {
                final Set<String> newCommands = getBot().loadCommand(message);
                if (newCommands == null || newCommands.size() == 0) {
                    ret = "Something went wrong; either you specified an unknown plugin or the plugin was already loaded.";
                } else {
                    ret = "Loaded the " + message + " plugin with the following commands: " + newCommands;
                }
            }
        }
        return ret;
    }
}