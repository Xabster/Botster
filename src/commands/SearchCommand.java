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

import botster.IRCCommand;

import java.net.URLEncoder;

/**
 * The SearchCommand class allows searching via search engines.
 */
public class SearchCommand extends IRCCommand {
    /**
     * Creates a new instance of SearchCommand and registers the available
     * search engines.
     */
    public SearchCommand() {
        addCommand("google");
        addCommand("lmgtfy");
        addCommand("wiki");
        addCommand("wikipedia");
    }

    @Override
    public String getReply(String command, String message) {
        String ret = null;
        String search;
        try {
            search = URLEncoder.encode(message, "UTF-8");
        } catch (Exception e) {
            search = message;
        }
        switch (command) {
            case "google":
                ret = "http://www.google.com/search?q=" + search;
                break;
            case "lmgtfy":
                ret = "http://lmgtfy.com/?q=" + search;
                break;
            case "wiki":
            case "wikipedia":
                ret = "http://en.wikipedia.org/wiki/Special:Search/" + search;
                break;
        }
        return ret;
    }
}