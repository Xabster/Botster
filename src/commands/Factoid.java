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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Saves information about a factoid
 */
class Factoid {
    private final Set<String> names;
    private String hostMask;
    private String text;

    public Factoid(final String[] names, final String hostMask, final String text) {
        this.names = new HashSet<>(Arrays.asList(names));
        setHostMask(hostMask);
        setText(text);
    }

    public void addName(final String s) {
        this.names.add(s);
    }

    public void removeName(final String s) {
        this.names.remove(s);
    }

    public Set<String> getNames() {
        return this.names;
    }

    public boolean hasName(final String s) {
        return this.names.stream().anyMatch(x -> x.equalsIgnoreCase(s));
    }

    public String getHostMask() {
        return this.hostMask;
    }

    public String getText() {
        return this.text;
    }

    public void setHostMask(final String hostMask) {
        this.hostMask = hostMask;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public String toString() {
        return this.text;
    }
}