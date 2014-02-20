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

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Stream;

public class Botster extends PircBot {
    public final static String PREFIX = "-";

    private final Map<String, String> lastLines = new HashMap<>();
    private final Map<String, String> channels;

    private final List<String> privilegedUsers;

    private final Map<String, Long> lastMessage = new HashMap<>(); // user -> timestamp of last msg
    private final Map<String, Long> ignoredUsers = new HashMap<>(); // user -> timestamp of the ignore expiration

    private final String autoSendTarget;
    private final String autoSendMessage;
    private final CommandSender cmdSender;

    private Map<String, IRCCommand> commands;
    private Set<String> publicCommands;
    private Set<String> restrictedCommands;

    private Botster(final Map<String, String> channels, final List<String> privilegedUsers, final String autoSendTarget, final String autoSendMessage) {
        this.channels = channels;
        this.privilegedUsers = privilegedUsers;
        this.autoSendTarget = autoSendTarget;
        this.autoSendMessage = autoSendMessage;
        this.cmdSender = new CommandSender(this);
        cmdSender.start();
        reloadCommands();
    }

    public static void main(final String[] args) throws Exception {
        loadServerConfig();
    }

    /**
     * Reads the server config from the given filename and creates bots.
     */
    private static void loadServerConfig() {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document document = builder.parse("servers.xml");

            final Node root = document.getFirstChild();
            final NodeList children = root.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                final Node child = children.item(i);
                if (!child.getNodeName().equals("server"))
                    continue;

                final NamedNodeMap attributes = child.getAttributes();
                //final String serverName = attributes.getNamedItem("name").getNodeValue();
                final String serverHost = attributes.getNamedItem("host").getNodeValue();
                int serverPort;
                try {
                    serverPort = Integer.parseInt(attributes.getNamedItem("port").getNodeValue());
                } catch (NumberFormatException e) {
                    serverPort = 6667;
                }
                final String botName = attributes.getNamedItem("botName").getNodeValue();
                String autoSendTarget = null;
                String autoSendMessage = null;
                final Map<String, String> channels = new HashMap<>();
                final List<String> privilegedUsers = new ArrayList<>();

                final NodeList serverChildren = child.getChildNodes();
                for (int j = 0; j < serverChildren.getLength(); j++) {
                    final Node serverChild = serverChildren.item(j);
                    if (serverChild.getNodeName().equals("autosend")) {
                        final NamedNodeMap autoSendAttributes = serverChild.getAttributes();
                        autoSendTarget = autoSendAttributes.getNamedItem("target").getNodeValue();
                        autoSendMessage = autoSendAttributes.getNamedItem("message").getNodeValue();
                    } else if (serverChild.getNodeName().equals("channels")) {
                        handleChannel(channels, serverChild);
                    } else if (serverChild.getNodeName().equals(("privilegedUsers"))) {
                        handlePrivilegedUser(privilegedUsers, serverChild);
                    }
                }

                startBot(serverHost, serverPort, botName, autoSendTarget, autoSendMessage, channels, privilegedUsers);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleChannel(final Map<String, String> channels, final Node serverChild) {
        final NodeList channelChildren = serverChild.getChildNodes();

        for (int k = 0; k < channelChildren.getLength(); k++) {
            final Node channelChild = channelChildren.item(k);
            if (!channelChild.getNodeName().equals("channel"))
                continue;

            final NamedNodeMap channelAttributes = channelChild.getAttributes();
            final String channelName = channelAttributes.getNamedItem("name").getNodeValue();
            final String channelPass = channelAttributes.getNamedItem("password").getNodeValue();
            channels.put(channelName, channelPass);
        }
    }

    private static void handlePrivilegedUser(final List<String> privilegedUsers, final Node serverChild) {
        final NodeList userChildren = serverChild.getChildNodes();
        for (int k = 0; k < userChildren.getLength(); k++) {
            final Node userChild = userChildren.item(k);

            if (!userChild.getNodeName().equals("user"))
                continue;

            final NamedNodeMap userAttributes = userChild.getAttributes();
            final String userHost = userAttributes.getNamedItem("host").getNodeValue();
            privilegedUsers.add(userHost);
        }
    }

    private static void startBot(final String serverHost, final int serverPort, final String botName, final String autoSendTarget, final String autoSendMessage, final Map<String, String> channels, final List<String> privilegedUsers) throws IOException, IrcException {
        final Botster bot = new Botster(channels, privilegedUsers, autoSendTarget, autoSendMessage);
        bot.setName(botName);
        bot.setLogin(botName);
        bot.setFinger(botName);
        bot.setAutoNickChange(true);
        bot.setVerbose(true);
        bot.setEncoding("UTF-8");
        bot.connect(serverHost, serverPort);
    }

    public void reloadCommands() {
        this.commands = new HashMap<>();
        this.publicCommands = new HashSet<>();
        this.restrictedCommands = new HashSet<>();
        loadCommand("Admin");
        loadCommand("API");
        loadCommand("Exec");
        loadCommand("Search");
        loadCommand("Factoid");
        loadCommand("Translate");
        loadCommand("Help");
        System.out.println("Loaded public commands: " + publicCommands);
        System.out.println("Loaded restricted commands: " + restrictedCommands);
    }

    public Set<String> loadCommand(final String name) {
        final Set<String> ret = new HashSet<>();
        try {
            final URL u = new File("build/classes").toURI().toURL();
            final URLClassLoader ucl = new URLClassLoader(new URL[]{u});
            final Class<?> cl = ucl.loadClass("commands." + name + "Command");
            final IRCCommand com = (IRCCommand) cl.newInstance();
            for (final String s : com.getCommands()) {
                if (!commands.containsKey(s)) {
                    this.commands.put(s, com);
                    this.publicCommands.add(s);
                    ret.add(s);
                }
            }
            for (final String s : com.getRestrictedCommands()) {
                if (!commands.containsKey(s)) {
                    this.commands.put(s, com);
                    this.restrictedCommands.add(s);
                    ret.add(name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public Set<String> getPublicCommands() {
        return publicCommands;
    }

    public Set<String> getRestrictedCommands() {
        return restrictedCommands;
    }

    @Override
    public void onMessage(final String channel, final String nickName, final String login, final String hostName, final String message) {
        final String userMask = login + "@" + hostName;

        if (userIsIgnored(userMask))
            return;

        if (ignoreIfRepeated(message, userMask))
            return;

        lastMessage.put(userMask, System.currentTimeMillis());
        lastLines.put(userMask, message);

        if (message.startsWith(PREFIX) || message.startsWith("!")) {
            final String[] splitMessage = message.split("\\s+", 2);
            final String commandName = splitMessage[0].substring(1);

            if (commands.containsKey(commandName)) {
                final IRCCommand command = commands.get(commandName);
                command.setNames(nickName, login, hostName, channel);
                command.setBot(this);

                final String reply = (splitMessage.length > 1) ? command.getReply(commandName, splitMessage[1]) : command.getReply(commandName, "");

                if (reply != null) {
                    final Scanner scan = new Scanner(reply);
                    int count = 0;
                    String line;
                    while (scan.hasNext() && count <= 3 && ((line = scan.nextLine()) != null) && !line.isEmpty()) {
                        count++;
                        cmdSender.sendMessage(channel, line);
                    }
                }
            }
        }
    }

    private boolean ignoreIfRepeated(final String message, final String userMask) {
        if (!isAuthorizedUser(userMask.substring(userMask.indexOf('@') + 1))) {
            if (lastLines.containsKey(userMask)) {
                if (message.trim().equalsIgnoreCase(lastLines.get(userMask).trim())) {
                    long time = 0;
                    if (lastMessage.containsKey(userMask))
                        time = lastMessage.get(userMask);

                    if (System.currentTimeMillis() - time < 3000) {
                        ignoredUsers.put(userMask, System.currentTimeMillis() + 60000);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected boolean userIsIgnored(final String userMask) {
        if (ignoredUsers.containsKey(userMask)) {
            if (System.currentTimeMillis() > ignoredUsers.get(userMask))
                ignoredUsers.remove(userMask); // remove it if the time is up
            else
                return true;
        }
        return false;
    }

    @Override
    public void onPrivateMessage(final String nickName, final String login, final String hostName, final String message) {
        onMessage(nickName, nickName, login, hostName, message);
    }

    public boolean isAuthorizedUser(final String hostName) {
        return privilegedUsers.contains(hostName);
    }

    @Override
    public void onDisconnect() {
        while (!isConnected()) {
            try {
                Thread.sleep(10000);
                reconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onConnect() {
        auth();
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Botster.this.joinChannels();
            }
        }, 2, 10000);
    }

    protected void joinChannels() {
        for (final Map.Entry<String, String> channel : channels.entrySet()) {
            if (isNotOnChannel(channel.getKey())) {
                if (channel.getValue().isEmpty())
                    cmdSender.joinChannel(channel.getKey());
                else
                    cmdSender.joinChannel(channel.getKey(), channel.getValue());
            }
        }
    }

    protected void auth() {
        if (!this.autoSendTarget.isEmpty() && !this.autoSendMessage.isEmpty()) {
            cmdSender.sendMessage(this.autoSendTarget, this.autoSendMessage);
            if (autoSendTarget.equalsIgnoreCase("Q@CServe.quakenet.org"))
                cmdSender.setMode(getNick(), "+x");
        }
    }

    public String getLastLine(final String name) {
        return lastLines.get(name);
    }

    public CommandSender getCommandSender() {
        return cmdSender;
    }

    private boolean isNotOnChannel(final String channel) {
        return !Stream.of(getChannels()).anyMatch(c -> c.equalsIgnoreCase(channel));
    }
}