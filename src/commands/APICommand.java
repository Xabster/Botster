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

 Copyright (C) 2010 Fredrik Holmar
 */

package commands;

import botster.AbstractPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The APICommand provides API searches.
 */
public class APICommand extends AbstractPlugin {
    private static final int FIND_ALL = 0;
    private static final int FIND_CLASSES = 1;
    private static final int FIND_METHODS = 2;
    private static final int FIND_FIELDS = 3;
    private static final int FIND_SOURCE = 4;
    public static final int URL_START_LENGTH = 13;
    private final Map<String, List<String>> classes = new HashMap<>();
    private final Map<String, List<String>> methods = new HashMap<>();
    private final Map<String, List<String>> fields = new HashMap<>();
    private final Map<String, List<String>> source = new HashMap<>();
    private final Collection<String> apis = new ArrayList<>();

    /**
     * Creates a new instance of APICommand and registers the "api", "class",
     * "method", "field" and "apis" response.
     */
    public APICommand() {
        initialize();
        addCommand("api");
        addCommand("class");
        addCommand("method");
        addCommand("field");
        addCommand("apis");
        addCommand("src");
    }

    /**
     * Adds a search term to the given Map.
     *
     * @param map  the Map to add the term to
     * @param term the term to add
     * @param url  the url for the given term
     */
    private static void addSearchTerm(Map<String, List<String>> map, String term, String url) {
        map.computeIfAbsent(term.toLowerCase(), (str) -> new ArrayList<>()).add(url);
    }

    /**
     * Searches the given map for the given search term
     *
     * @param map the Map to search in
     * @param key the search term
     * @return a List of Strings with the resulting URLs
     */
    private static Collection<String> get(Map<String, List<String>> map, String key) {
        return map.getOrDefault(key, Collections.emptyList());
    }

    /**
     * Returns the Damerau-Levenshtein distance between two strings.
     *
     * @param string1 the first string
     * @param string2 the string to compare to
     * @return the Damerau-Levenshtein distance between string1 and string2
     */
    private static int damLevDistance(CharSequence string1, CharSequence string2) {
        int length1 = string1.length();
        int length2 = string2.length();
        int[][] leastOperationsTable = new int[length1 + 1][length2 + 1];

        for (int i = 0; i <= length1; i++)
            leastOperationsTable[i][0] = i;

        for (int j = 0; j <= length2; j++)
            leastOperationsTable[0][j] = j;

        for (int i = 1; i <= length1; i++) {
            for (int j = 1; j <= length2; j++) {
                int diff;
                if (string1.charAt(i - 1) == string2.charAt(j - 1))
                    diff = 0;
                else
                    diff = 1;
                leastOperationsTable[i][j] = Math.min(Math.min(leastOperationsTable[i - 1][j] + 1, // insertion
                        leastOperationsTable[i][j - 1] + 1), // deletion
                        leastOperationsTable[i - 1][j - 1] + diff); // substitution

                if (i > 1 && j > 1 && string1.charAt(i - 1) == string2.charAt(j - 2) && string1.charAt(i - 2) == string2.charAt(j - 1)) {
                    leastOperationsTable[i][j] = Math.min(leastOperationsTable[i][j], leastOperationsTable[i - 2][j - 2] + diff); // transposition
                }
            }
        }
        return leastOperationsTable[length1][length2];
    }

    /**
     * Reads the api files
     */
    private void initialize() {
        try {
            loadApis();
        } catch (Exception e) {
            System.err.println("Something went wrong while loading the APIs:");
            e.printStackTrace();
        }
    }

    /**
     * Searches for the given query in the API. Currently just responds with a
     * Google "I'm feeling lucky" string ;)
     */
    @Override
    public String getReply(String command, String message) {
        String ret;
        if (command.equals("apis"))
            return "All loaded APIs: " + apis.stream().collect(Collectors.joining(", "));

        if (message.isEmpty())
            return getNickName() + ", please provide something to search for.";

        SearchResults results;
        switch (command) {
            case "class":
                results = search(message, FIND_CLASSES);
                break;
            case "method":
                results = search(message, FIND_METHODS);
                break;
            case "field":
                results = search(message, FIND_FIELDS);
                break;
            case "src":
                results = search(message, FIND_SOURCE);
                break;
            default:
                results = search(message, FIND_ALL);
                break;
        }
        if (results.urls.size() == 0)
            return "No matches found.";

        String[] urlStrings = new String[Math.min(3, results.urls.size())];
        for (int i = 0; i < Math.min(3, results.urls.size()); i++)
            urlStrings[i] = results.urls.get(i);

        ret = Arrays.stream(urlStrings).collect(Collectors.joining(" | "));
        if (results.urls.size() > 3)
            ret += " (" + results.urls.size() + " total)";

        if (results.type == SearchResults.BEST_MATCH) {
            if (urlStrings.length > 1)
                ret = "Best matches: " + ret;
            else
                ret = "Best match: " + ret;
        }

        return ret;
    }

    /**
     * Loads the API files specified in apidata/api.xml
     */
    private void loadApis() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse("apidata/api.xml");

        Node root = document.getFirstChild();
        NodeList children = root.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals("api")) {

                NamedNodeMap attributes = child.getAttributes();
                String name = attributes.getNamedItem("name").getNodeValue();
                String baseurl = attributes.getNamedItem("baseurl").getNodeValue();
                String urlprefix = attributes.getNamedItem("urlprefix").getNodeValue();

                apis.add(name);

                NodeList files = child.getChildNodes();

                for (int j = 0; j < files.getLength(); j++) {
                    Node file = files.item(j);

                    if (!file.getNodeName().equals("file"))
                        continue;

                    String fileName = file.getAttributes().getNamedItem("name").getNodeValue();

                    parse(fileName, baseurl, urlprefix);
                }
            } else if (child.getNodeName().equals("src")) {
                NamedNodeMap attributes = child.getAttributes();
                String name = attributes.getNamedItem("name").getNodeValue();
                String baseurl = attributes.getNamedItem("baseurl").getNodeValue();
                String urlsuffix = attributes.getNamedItem("urlsuffix").getNodeValue();

                apis.add(name);

                NodeList files = child.getChildNodes();

                for (int j = 0; j < files.getLength(); j++) {
                    Node file = files.item(j);

                    if (!file.getNodeName().equals("file"))
                        continue;

                    String fileName = file.getAttributes().getNamedItem("name").getNodeValue();

                    parseSrc(fileName, baseurl, urlsuffix);
                }
            }
        }
    }

    private void parseSrc(String file, String baseurl, String urlsuffix) throws Exception {
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("apidata", file)), "ISO-8859-1"))) {
            String line;
            while ((line = br.readLine()) != null) {
                int packEnd = line.lastIndexOf('/');
                String pack = line.substring(0, packEnd).replace('/', '.');
                String className = line.substring(packEnd + 1);

                // remove .java
                className = className.substring(0, className.length() - 5);

                String fullUrl = baseurl + line + urlsuffix;

                addSearchTerm(source, className, fullUrl);
                addSearchTerm(source, pack + "." + className, fullUrl);
            }
        }
    }

    /**
     * Parses a given text file and adds all of the classes, methods and fields
     * to the relevant Maps.
     *
     * @param file the name of the file to parse
     */
    private void parse(String file, String baseurl, String urlprefix) throws Exception {
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("apidata", file)), "ISO-8859-1"))) {
            String line;
            while ((line = br.readLine()) != null) {
                int foundIndex = line.indexOf("<dt>");
                while (foundIndex != -1) {
                    handleEntry(line.substring(foundIndex), baseurl, urlprefix);
                    foundIndex = line.indexOf("<dt>", foundIndex + 1);
                }
            }
        }
    }

    /**
     * Handles a single entry from the text files that we're parsing
     *
     * @param entry the entry to handle
     */
    private void handleEntry(String entry, String baseurl, CharSequence urlprefix) {
        int urlStart = URL_START_LENGTH + urlprefix.length();
        int endIndex = entry.indexOf('\"', urlStart);
        String url = entry.substring(urlStart, endIndex);
        int slashIndex = url.lastIndexOf("/");

        String pack = url.substring(0, slashIndex);
        pack = pack.replace('/', '.');
        String name = url.substring(slashIndex + 1);

        String fullUrl = baseurl + url;
        fullUrl = fullUrl.replace("(", "%28");
        fullUrl = fullUrl.replace(")", "%29");
        fullUrl = fullUrl.replace(" ", "%20");

        int hashPos = name.indexOf('#');

        if (hashPos == -1) {
            String className = name.substring(0, name.length() - 5);
            addSearchTerm(classes, className, fullUrl);
            addSearchTerm(classes, pack + "." + className, fullUrl);
        } else {
            String className = name.substring(0, hashPos - 5);
            String member = name.substring(hashPos + 1);

            if (member.endsWith("-")) {
                int parenPos = member.indexOf('-');

                String methodName = member.substring(0, parenPos);

                if (methodName.equals(className))
                    methodName = "<init>";

                if (!methodName.equals("<init>"))
                    addSearchTerm(methods, methodName, fullUrl);

                addSearchTerm(methods, className + "." + methodName, fullUrl);
                addSearchTerm(methods, pack + "." + className + "." + methodName, fullUrl);
            } else {
                addSearchTerm(fields, member, fullUrl);
                addSearchTerm(fields, className + "." + member, fullUrl);
                addSearchTerm(fields, pack + "." + className + "." + member, fullUrl);
            }
        }
    }

    /**
     * Searches all maps for the given term, or only one if limit != FIND_ALL.
     * Will find the best match if no 100% match is found.
     *
     * @param term  the term to search for
     * @param limit the type of search to limit to
     * @return a SearchResults with the resulting matches
     */
    SearchResults search(String term, int limit) {
        term = term.toLowerCase().replace(" ", "");

        List<String> results = new ArrayList<>();

        if (limit == FIND_ALL || limit == FIND_CLASSES)
            results.addAll(get(classes, term));
        if (limit == FIND_ALL || limit == FIND_METHODS)
            results.addAll(get(methods, term));
        if (limit == FIND_ALL || limit == FIND_FIELDS)
            results.addAll(get(fields, term));
        if (limit == FIND_SOURCE)
            results.addAll(get(source, term));

        if (results.isEmpty())
            return bestMatch(term, limit);

        return new SearchResults(SearchResults.EXACT_MATCH, Collections.unmodifiableList(results));
    }

    /**
     * Finds the best match for a search term
     *
     * @param term  the term to search for
     * @param limit the type of search to limit to
     * @return a SearchResults the most likely matches
     */
    private SearchResults bestMatch(String term, int limit) {
        Collection<Map.Entry<String, List<String>>> lookIn = new ArrayList<>();

        if (limit == FIND_ALL || limit == FIND_CLASSES)
            lookIn.addAll(classes.entrySet());
        if (limit == FIND_ALL || limit == FIND_METHODS)
            lookIn.addAll(methods.entrySet());
        if (limit == FIND_ALL || limit == FIND_FIELDS)
            lookIn.addAll(fields.entrySet());
        if (limit == FIND_SOURCE)
            lookIn.addAll(source.entrySet());

        List<String> results = new ArrayList<>();
        int bestDist = Integer.MAX_VALUE;

        for (Map.Entry<String, List<String>> entry : lookIn) {
            int distance = damLevDistance(term, entry.getKey());

            if (distance < bestDist) {
                bestDist = distance;
                results.clear();
                results.addAll(entry.getValue());
            } else if (distance == bestDist)
                results.addAll(entry.getValue());
        }

        return new SearchResults(SearchResults.BEST_MATCH, Collections.unmodifiableList(results));
    }

    public static class SearchResults {
        public static final int EXACT_MATCH = 0, BEST_MATCH = 1;
        public final int type;
        public final List<String> urls;

        public SearchResults(int type, List<String> urls) {
            this.type = type;
            this.urls = urls;
        }
    }
}