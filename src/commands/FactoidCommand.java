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

import Botster.Botster;
import Botster.IRCCommand;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The FactoidCommand provides a number of responses to answer questions asked
 * often. Factoids are read from factoids.txt, where each factoid consists of
 * two lines, the name of the factoid and the response.
 */
public class FactoidCommand extends IRCCommand {
    private static final String UPDATE_FACTOID = "updatefactoid";
    private static final String ADD_FACTOID = "addfactoid";
    private static final String REMOVE_FACTOID = "removefactoid";
    private static final String FACTOID_HOST = "factoidhost";
    private static final String ALIAS_FACTOID = "alias";
    private static final String UNALIAS_FACTOID = "unalias";
    private static final String ALIASES_FACTOID = "aliases";
    /**
     * The factoids available for this FactoidCommand. Keys are factoid names,
     * values are the actual responses.
     */
    private static Set<Factoid> factoids;

    /**
     * Creates a new instance of FactoidCommand. Registering of the factoids is
     * delegated to loadFactoids().
     */
    public FactoidCommand() {
        loadFactoids();
        addCommand("factoid");
        addCommand(UPDATE_FACTOID);
        addCommand(ADD_FACTOID);
        addCommand(REMOVE_FACTOID);
        addCommand(ALIAS_FACTOID);
        addCommand(UNALIAS_FACTOID);
        addCommand(ALIASES_FACTOID);
        addRestrictedCommand(FACTOID_HOST);
    }

    /**
     * Loads the factoids from factoids.txt, where each factoid consists of two
     * lines, the name of the factoid and the response.
     */
    private void loadFactoids() {
        try (final Scanner scan = new Scanner(new File("factoids.txt"))) {
            factoids = new HashSet<>();
            while (scan.hasNext()) {
                final String[] factoidNames = scan.nextLine().split(" ");
                final String factoidHostMask = scan.nextLine();
                final String factoidText = scan.nextLine();
                final Factoid f = new Factoid(factoidNames, factoidHostMask, factoidText);

                for (final String factoidName : factoidNames)
                    addCommand(factoidName);
                factoids.add(f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates factoids.txt and protectedfactoids.txt
     */
    private void saveFactoids() {
        try (final PrintWriter pw = new PrintWriter(new File("factoids.txt"))) {
            for (final Factoid f : factoids) {
                pw.println(f.getNames().stream().map((s) -> s).collect(Collectors.joining(" ")));
                pw.println(f.getHostMask());
                pw.println(f.getText());
            }
            getBot().reloadCommands();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Factoid findFactoid(final String name) {
        for (final Factoid f : factoids)
            if (f.hasName(name))
                return f;
        return null;
    }

    @Override
    public String getReply(final String command, String message) {
        String reply = null;
        final Factoid replyFactoid;
        switch (command) {
            case "factoid": {
                replyFactoid = findFactoid(message);
                if (replyFactoid != null) {
                    reply = message + " is: " + replyFactoid;
                } else {
                    reply = getNickName() + ", that factoid was not found.";
                }
                break;
            }
            case ADD_FACTOID: {
                if (getChannel().startsWith("#")) {
                    if (!message.contains(" ")) {
                        reply = getNickName() + ", you did not supply a factoid text.";
                    } else {
                        final String factoidName = message.substring(0, message.indexOf(" "));
                        final String factoidText = message.substring(message.indexOf(" ") + 1);
                        if (!getBot().getPublicCommands().contains(factoidName) && !getBot().getRestrictedCommands().contains(factoidName)) {
                            if (findFactoid(factoidName) != null) {
                                reply = getNickName() + ", a factoid with that name already exists; use " + Botster.PREFIX + UPDATE_FACTOID + " to update this factoid.";
                            } else {
                                final String factoidHostMask = String.format("%s!%s@%s", getNickName(), getLogin(), getHostName());
                                final Factoid newFactoid = new Factoid(new String[]{factoidName}, factoidHostMask, factoidText);
                                factoids.add(newFactoid);
                                addCommand(factoidName);
                                saveFactoids();
                                getBot().reloadCommands();
                                reply = String.format("%s%s is now: %s", Botster.PREFIX, factoidName, factoidText);
                            }
                        } else {
                            reply = getNickName() + ", there is already a command with that name.";
                        }
                    }
                } else {
                    reply = getNickName() + ", this command only works in channels.";
                }
                break;
            }
            case REMOVE_FACTOID: {
                if (getChannel().startsWith("#")) {
                    final Factoid f = findFactoid(message);
                    if (f != null) {
                        if (f.getNames().size() == 1) {
                            factoids.remove(f);
                            saveFactoids();
                            reply = String.format("The %s%s factoid has been removed.", Botster.PREFIX, message);
                        } else {
                            final Set<String> var = f.getNames();
                            reply = String.format("For safety reasons, you are not allowed to remove factoids that have aliases. Please remove the aliases first if you wish to remove this factoid. All known aliases: %s", Botster.implodeStrings(var.toArray(new String[var.size()]), ", "));
                        }
                    } else {
                        reply = getNickName() + ", that factoid does not exist.";
                    }
                } else {
                    reply = getNickName() + ", this command only works in channels.";
                }
                break;
            }
            case UPDATE_FACTOID: {
                if (getChannel().startsWith("#")) {
                    if (!message.contains(" ")) {
                        reply = getNickName() + ", you did not supply a factoid text.";
                    } else {
                        final String factoidName = message.substring(0, message.indexOf(" "));
                        final String factoidText = message.substring(message.indexOf(" ") + 1);
                        final Factoid updateFactoid = findFactoid(factoidName);
                        if (updateFactoid != null) {
                            updateFactoid.setHostMask(String.format("%s!%s@%s", getNickName(), getLogin(), getHostName()));
                            updateFactoid.setText(factoidText);
                            saveFactoids();
                            reply = String.format("%s%s is now: %s", Botster.PREFIX, factoidName, factoidText);
                        } else {
                            reply = getNickName() + ", a factoid with that name does not exist; use " + Botster.PREFIX + ADD_FACTOID + " to create this factoid.";
                        }
                    }
                } else {
                    reply = getNickName() + ", this command only works in channels.";
                }
                break;
            }
            case FACTOID_HOST: {
                if (isAuthorizedUser()) {
                    final Factoid f = findFactoid(message);
                    if (f != null) {
                        getBot().getCommandSender().sendMessage(getNickName(), String.format("The factoid %s%s was last updated by: %s", Botster.PREFIX, message, f.getHostMask()));
                    } else {
                        getBot().getCommandSender().sendMessage(getNickName(), "That factoid does not exist.");
                    }
                }
                break;
            }
            case ALIAS_FACTOID: {
                final String[] factoidNames = message.split("\\s+");
                if (factoidNames.length != 2) {
                    reply = "Please supply a current factoid and a new alias.";
                } else {
                    final Factoid aliasFactoid = findFactoid(factoidNames[0]);
                    final Factoid aliasFactoid2 = findFactoid(factoidNames[1]);
                    if (aliasFactoid != null) {
                        if (aliasFactoid2 == null) {
                            if (!getBot().getPublicCommands().contains(factoidNames[1]) && !getBot().getRestrictedCommands().contains(factoidNames[1])) {
                                if (!aliasFactoid.hasName(factoidNames[1])) {
                                    aliasFactoid.addName(factoidNames[1]);
                                    saveFactoids();
                                    final Set<String> var = aliasFactoid.getNames();
                                    reply = "Alias added. All commands for this factoid: " + Botster.implodeStrings(var.toArray(new String[var.size()]), ", ");
                                } else {
                                    reply = String.format("The factoid '%s' already has the alias '%s'.", factoidNames[0], factoidNames[1]);
                                }
                            } else {
                                reply = String.format("A command called '%s' already exists.", factoidNames[1]);
                            }
                        } else {
                            reply = String.format("A factoid called '%s' already exists.", factoidNames[1]);
                        }
                    } else {
                        reply = String.format("Factoid '%s' was not found.", factoidNames[0]);
                    }
                }
                break;
            }
            case UNALIAS_FACTOID: {
                final Factoid unaliasFactoid = findFactoid(message);
                if (unaliasFactoid != null) {
                    final Set<String> factoidNames = unaliasFactoid.getNames();
                    if (factoidNames.size() > 1) {
                        unaliasFactoid.removeName(message);
                        saveFactoids();
                        final Set<String> var = unaliasFactoid.getNames();
                        reply = "Alias removed. All commands for this factoid: " + Botster.implodeStrings(var.toArray(new String[var.size()]), ", ");
                    } else {
                        reply = String.format("You cannot remove the last known alias for a factoid. If you really want to remove this factoid, use %s%s instead.", Botster.PREFIX, REMOVE_FACTOID);
                    }
                } else {
                    reply = String.format("Factoid '%s' was not found.", message);
                }
                break;
            }
            case ALIASES_FACTOID: {
                if (message.startsWith("-") && message.length() > 1) {
                    message = message.substring(1);
                }
                final Factoid aliasesFactoid = findFactoid(message);
                if (aliasesFactoid != null) {
                    final Set<String> var = aliasesFactoid.getNames();
                    reply = String.format("All known aliases for %s%s: %s", Botster.PREFIX, message, Botster.implodeStrings(var.toArray(new String[var.size()]), ", "));
                } else {
                    reply = String.format("Factoid '%s' was not found.", message);
                }
                break;
            }
            default: {
                final Factoid f = findFactoid(command);
                if (f != null) {
                    if (message.isEmpty()) {
                        if (f.toString().startsWith("!")) {
                            reply = f.toString().substring(1);
                        } else {
                            reply = f.toString();
                        }
                    } else {
                        reply = message + ", ";
                        if (f.toString().startsWith("!")) {
                            reply += f.toString().substring(1);
                        } else {
                            reply += Character.toLowerCase(f.toString().charAt(0)) + f.toString().substring(1);
                        }
                    }
                }
                break;
            }
        }
        return reply;
    }
}