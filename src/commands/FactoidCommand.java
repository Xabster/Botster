package commands;

import Botster.Botster;
import Botster.IRCCommand;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
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
    private static final String FACTOID_COMMAND = "factoid";
    private static final String FACTOIDS_TXT = "factoids.txt";

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
        addCommand(FACTOID_COMMAND);
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
        try (final Scanner scan = new Scanner(new File(FACTOIDS_TXT))) {
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

    @Override
    public String getReply(final String command, String message) {
        switch (command) {
            case FACTOID_COMMAND: {
                return handleFactoidCommand(message);
            }
            case ADD_FACTOID: {
                return handleAddFactoid(message);
            }
            case REMOVE_FACTOID: {
                return handleRemoveFactoid(message);
            }
            case UPDATE_FACTOID: {
                return handleUpdateFactoid(message);
            }
            case FACTOID_HOST: {
                handleFactoidHost(message);
            }
            case ALIAS_FACTOID: {
                return handleAliasFactoid(message);
            }
            case UNALIAS_FACTOID: {
                return handleUnaliasFactoid(message);
            }
            case ALIASES_FACTOID: {
                return handleAliasesFactoid(message);
            }
            default: {
                return handleFactoid(command, message);
            }
        }
    }

    /**
     * Updates factoids.txt
     */
    private void saveFactoids() {
        try (final PrintWriter pw = new PrintWriter(new File("factoids.txt"))) {
            for (final Factoid f : factoids) {
                pw.println(f.getNames().stream().collect(Collectors.joining(" ")));
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

    private String handleFactoid(String command, String message) {
        final Factoid f = findFactoid(command);
        if (f == null)
            return null;
        return message.isEmpty() ? f.toString() : message + ": " + f.toString();
    }

    private String handleAliasesFactoid(String message) {
        if (message.startsWith("-") && message.length() > 1)
            message = message.substring(1);

        final Factoid aliasesFactoid = findFactoid(message);
        if (aliasesFactoid == null)
            return String.format("Factoid '%s' was not found.", message);

        return String.format("All known aliases for %s%s: %s", Botster.PREFIX, message, aliasesFactoid.getNames().stream().collect(Collectors.joining(", ")));
    }

    private String handleRemoveFactoid(String message) {
        if (!getChannel().startsWith("#"))
            return getNickName() + ", this command only works in channels.";

        final Factoid f = findFactoid(message);
        if (f == null)
            return getNickName() + ", that factoid does not exist.";

        if (f.getNames().size() != 1)
            return String.format("For safety reasons, you are not allowed to remove factoids that have aliases. Please remove the aliases first if you wish to remove this factoid. All known aliases: %s", f.getNames().stream().collect(Collectors.joining(", ")));

        factoids.remove(f);
        saveFactoids();
        return String.format("The %s%s factoid has been removed.", Botster.PREFIX, message);
    }

    private String handleUnaliasFactoid(String message) {
        final Factoid unaliasFactoid = findFactoid(message);
        if (unaliasFactoid == null)
            return String.format("Factoid '%s' was not found.", message);

        final Set<String> factoidNames = unaliasFactoid.getNames();
        if (factoidNames.size() <= 1)
            return String.format("You cannot remove the last known alias for a factoid. If you really want to remove this factoid, use %s%s instead.", Botster.PREFIX, REMOVE_FACTOID);

        unaliasFactoid.removeName(message);
        saveFactoids();
        return "Alias removed. All commands for this factoid: " + factoidNames.stream().collect(Collectors.joining(", "));
    }

    private String handleAliasFactoid(String message) {
        final String[] factoidNames = message.split("\\s+");

        if (factoidNames.length != 2)
            return "Please supply a current factoid and a new alias.";

        final Factoid currentFactoid = findFactoid(factoidNames[0]);
        final Factoid newFactoidAlias = findFactoid(factoidNames[1]);

        if (currentFactoid == null)
            return String.format("Factoid '%s' was not found.", factoidNames[0]);

        if (newFactoidAlias != null)
            return String.format("A factoid called '%s' already exists.", factoidNames[1]);

        if (getBot().getPublicCommands().contains(factoidNames[1]) || getBot().getRestrictedCommands().contains(factoidNames[1]))
            return String.format("A command called '%s' already exists.", factoidNames[1]);

        if (currentFactoid.hasName(factoidNames[1]))
            return String.format("The factoid '%s' already has the alias '%s'.", factoidNames[0], factoidNames[1]);

        currentFactoid.addName(factoidNames[1]);
        saveFactoids();
        return "Alias added. All commands for this factoid: " + currentFactoid.getNames().stream().collect(Collectors.joining(", "));
    }

    private String handleUpdateFactoid(String message) {
        if (!getChannel().startsWith("#"))
            return getNickName() + ", this command only works in channels.";

        if (!message.contains(" "))
            return getNickName() + ", you did not supply a factoid text.";

        final String factoidName = message.substring(0, message.indexOf(" "));
        final String factoidText = message.substring(message.indexOf(" ") + 1);
        final Factoid updateFactoid = findFactoid(factoidName);

        if (updateFactoid == null)
            return getNickName() + ", a factoid with that name does not exist; use " + Botster.PREFIX + ADD_FACTOID + " to create this factoid.";

        updateFactoid.setHostMask(String.format("%s!%s@%s", getNickName(), getLogin(), getHostName()));
        updateFactoid.setText(factoidText);
        saveFactoids();
        return String.format("%s%s is now: %s", Botster.PREFIX, factoidName, factoidText);
    }

    private void handleFactoidHost(String message) {
        if (isAuthorizedUser()) {
            final Factoid f = findFactoid(message);
            if (f != null)
                getBot().getCommandSender().sendMessage(getNickName(), String.format("The factoid %s%s was last updated by: %s", Botster.PREFIX, message, f.getHostMask()));
            else
                getBot().getCommandSender().sendMessage(getNickName(), "That factoid does not exist.");
        }
    }

    private String handleAddFactoid(String message) {
        if (!getChannel().startsWith("#"))
            return getNickName() + ", this command only works in channels.";

        if (!message.contains(" "))
            return getNickName() + ", you did not supply a factoid text.";

        final String factoidName = message.substring(0, message.indexOf(" "));
        final String factoidText = message.substring(message.indexOf(" ") + 1);

        if (getBot().getPublicCommands().contains(factoidName) || getBot().getRestrictedCommands().contains(factoidName))
            return getNickName() + ", there is already a command with that name.";

        if (findFactoid(factoidName) != null)
            return getNickName() + ", a factoid with that name already exists; use " + Botster.PREFIX + UPDATE_FACTOID + " to update this factoid.";

        final String factoidHostMask = String.format("%s!%s@%s", getNickName(), getLogin(), getHostName());
        final Factoid newFactoid = new Factoid(new String[]{factoidName}, factoidHostMask, factoidText);
        factoids.add(newFactoid);
        addCommand(factoidName);
        saveFactoids();
        getBot().reloadCommands();
        return String.format("%s%s is now: %s", Botster.PREFIX, factoidName, factoidText);
    }

    private String handleFactoidCommand(String message) {
        Factoid replyFactoid = findFactoid(message);
        if (replyFactoid != null)
            return message + " is: " + replyFactoid;
        else
            return getNickName() + ", that factoid was not found.";
    }
}