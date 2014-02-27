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

import botster.AbstractPlugin;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * The SearchCommand class allows searching via search engines.
 */
public class SearchCommand extends AbstractPlugin {
    public static final String GOOGLE = "google";
    public static final String LMGTFY = "lmgtfy";
    public static final String WIKI = "wiki";
    public static final String WIKIPEDIA = "wikipedia";

    /**
     * Creates a new instance of SearchCommand and registers the available
     * search engines.
     */
    public SearchCommand() {
        addCommand(GOOGLE);
        addCommand(LMGTFY);
        addCommand(WIKI);
        addCommand(WIKIPEDIA);
    }

    @Override
    public String getReply(String command, String message) {
        String search;
        try {
            search = URLEncoder.encode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            search = message;
        }
        switch (command) {
            case GOOGLE:
                return "http://www.google.com/search?q=" + search;
            case LMGTFY:
                return "http://lmgtfy.com/?q=" + search;
            case WIKI:
            case WIKIPEDIA:
                return "http://en.wikipedia.org/wiki/Special:Search/" + search;
        }
        return null;
    }
}