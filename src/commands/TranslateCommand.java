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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * The TranslateCommand class allows for translation of foreign languages to
 * English.
 */
public class TranslateCommand extends IRCCommand {
    /**
     * Creates a new instance of the TranslateCommand class and registers its
     * commands.
     */
    public TranslateCommand() {
        addCommand("translate");
    }

    /**
     * Translates the last line the given user said, or if that user has not
     * said any lines, it will translate the message directly.
     *
     * @param message the user whose line should be translated, or a message to
     *                translate
     */
    @Override
    public String getReply(final String command, final String message) {
        String language = "en";
        if (getChannel().equals("#java.de")) {
            language = "de";
        }
        String ret = "http://translate.google.com/#auto%7c" + language + "%7c";
        String translate = getBot().getLastLine(getLogin() + "@" + getHostName());
        if (translate == null) {
            translate = message;
        }
        try {
            translate = URLEncoder.encode(translate, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        ret += translate;
        return ret;
    }
}