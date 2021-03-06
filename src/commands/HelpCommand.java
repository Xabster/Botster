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

 Copyright (C) 2010 Olaf Keijsers, Fredrik Holmar
 */

package commands;

import botster.AbstractPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The HelpCommand provides one response, by listing the available (public)
 * commands people can give.
 */
public class HelpCommand extends AbstractPlugin {

    public static final int RESULTS_PER_PAGE = 30;

    /**
     * Creates a new instance of HelpCommand and registers the "help" response.
     */
    public HelpCommand() {
        addCommand("help");
        addRestrictedCommand("help2");
    }

    @Override
    public String getReply(String command, String message) {
        String ret = "";
        int page;

        try {
            page = Integer.parseInt(message);
        } catch (Exception e) {
            page = 1;
        }

        if (command.equals("help"))
            ret = getPage(page, RESULTS_PER_PAGE, getBot().getPublicCommands());
        else if (command.equals("help2") && isAuthorizedUser())
            ret = getPage(page, RESULTS_PER_PAGE, getBot().getRestrictedCommands());

        return ret;
    }

    String getPage(int page, int perpage, Collection<String> commands) {

        List<String> list = new ArrayList<>(commands);
        Collections.sort(list);

        int totalPages = (list.size() - 1) / perpage + 1;

        if (page < 1)
            page = 1;

        if (page > totalPages)
            page = totalPages;

        int startIndex = (page - 1) * perpage;

        int endIndex = Math.min(page * perpage, list.size());

        List<String> sublist = list.subList(startIndex, endIndex);

        StringBuilder sb = new StringBuilder();
        sb.append("Available commands");

        if (totalPages > 1) {
            sb.append(", page ");
            sb.append(page);
            sb.append("/");
            sb.append(totalPages);
        }

        sb.append(": ");
        for (String s : sublist)
            sb.append(s).append(", ");

        sb.setLength(sb.length() - 2);

        return sb.toString();
    }
}