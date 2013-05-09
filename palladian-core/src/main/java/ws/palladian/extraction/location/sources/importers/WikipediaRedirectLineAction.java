package ws.palladian.extraction.location.sources.importers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

public abstract class WikipediaRedirectLineAction extends LineAction {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikipediaRedirectLineAction.class);

    private int redirectCounter = 0;

    @Override
    public void performAction(String line, int lineNumber) {
        // if (lineNumber % 1000 == 0) {
        LOGGER.info("Read {} redirects, {} lines", redirectCounter, lineNumber);
        // }
        if (!line.startsWith("INSERT INTO `redirect` VALUES")) {
            return;
        }
        List<String> recordSplit = splitLine(line);
        for (String record : recordSplit) {
            String[] parse = parseRecord(record);
            assert parse.length == 5;
            int fromPageId = Integer.valueOf(parse[0]);
            int namespace = Integer.valueOf(parse[1]);
            String toTitle = parse[2].replace('_', ' ');
            readRedirect(fromPageId, namespace, toTitle);
            redirectCounter++;
        }
    }

    public abstract void readRedirect(int fromPageId, int namespace, String toTitle);

    /** I tried using RegEx. I failed. I wrote code nobody, including me will ever understand again. It worked. */
    static List<String> splitLine(String string) {
        int i = 0;
        while (string.charAt(i) != '(') {
            i++;
        }
        List<String> split = CollectionHelper.newArrayList();
        StringBuilder buffer = new StringBuilder();
        boolean inQuote = false; // are we currently inside a 'quote'
        boolean skipChars = false; // skip separating chars (i.e. "),(")
        boolean escapeChar = false; // is the next char to be escaped
        char currentChar;
        while (++i < string.length()) {
            currentChar = string.charAt(i);
            if (LOGGER.isTraceEnabled()) {
                String out = String.format("%d|%c|s=%b|q=%b|e=%b", i, currentChar, skipChars, inQuote, escapeChar);
                LOGGER.trace(out);
            }
            if (!escapeChar && currentChar == '\'') {
                inQuote ^= true;
            }
            if (!inQuote) {
                if (currentChar == ')') {
                    skipChars = true;
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("+ {}", buffer.toString());
                    }
                    split.add(buffer.toString());
                    buffer = new StringBuilder();
                } else if (currentChar == '(') {
                    skipChars = false;
                    continue;
                }
            }
            if (!skipChars) {
                buffer.append(currentChar);
            }
            escapeChar = !escapeChar && currentChar == '\\';
        }
        return split;
    }

    private static final Pattern PARSE_RECORD_PATTERN = Pattern
            .compile("'([^'\\\\]|\\\\'|\\\\\"|\\\\\\\\)*?'|\\d+(\\.\\d+(e-\\d+)?)?|NULL");

    static String[] parseRecord(String string) {
        Matcher matcher = PARSE_RECORD_PATTERN.matcher(string);
        List<String> data = CollectionHelper.newArrayList();
        while (matcher.find()) {
            String value = matcher.group();
            if (value.startsWith("'") && value.endsWith("'")) {
                value = value.substring(1, value.length() - 1);
            }
            value = value.replace("\\", "");
            data.add(value);
        }
        return data.toArray(new String[0]);
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        InputStream in = new GZIPInputStream(new FileInputStream("/Users/pk/Downloads/enwiki-20130403-redirect.sql.gz"));
        FileHelper.performActionOnEveryLine(in, new WikipediaRedirectLineAction() {
            @Override
            public void readRedirect(int fromPageId, int namespace, String title) {
                System.out.println("Redirect from " + fromPageId + " to title " + title + " in namespace " + namespace);
            }
        });
    }

}
