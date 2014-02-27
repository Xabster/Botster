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

import sun.misc.Unsafe;

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

    Factoid(String[] names, String hostMask, String text) {

        this.names = new HashSet<>(Arrays.asList(names));
        this.hostMask = hostMask;
        this.text = text;
    }

    public void addName(String s) {
        this.names.add(s);
    }

    public void removeName(String s) {
        this.names.remove(s);
    }

    public Set<String> getNames() {
        return this.names;
    }

    public boolean hasName(String s) {
        return this.names.stream().anyMatch(x -> x.equalsIgnoreCase(s));
    }

    public String getHostMask() {
        return this.hostMask;
    }

    public String getText() {
        return this.text;
    }

    public void setHostMask(String hostMask) {
        this.hostMask = hostMask;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String toString() {
        return this.text;
    }
}