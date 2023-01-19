package ws.palladian.semantics.synonyms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.ProgressMonitor;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>
 * Create a simple synonym dictionary that is fast and in-memory. This creator can parse the Open Office dictionary file
 * format and can therefore create {@link SnyonymDictionary} files in every language supported by OpenOffice (derived
 * from WordNet). The input for an English thesaurus can be downloaded here:
 * http://lingucomponent.openoffice.org/MyThes-1.zip (input file is the .dat file in that zip package).
 * </p>
 *
 * @author David Urbansky
 * @see <a href="http://stackoverflow.com/questions/4175335/where-can-i-download-a-free-synonyms-database">Stack
 * Overflow: Where can I download a free synonyms database?</a>
 * @see <a href="http://lingucomponent.openoffice.org/MyThes-1.zip">Thesaurus file download</a>
 * @see <a href="https://www.openthesaurus.de/about/download">Open Thesaurus (German)</a>
 */
public class SynonymDictionaryCreator {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SynonymDictionaryCreator.class);

    public void createDictionaryOpenOfficeFormat(File inputFile, File dictionaryFile) throws IOException {
        StopWatch stopWatch = new StopWatch();

        List<String> lines = FileHelper.readFileToArray(inputFile.getPath());

        Set<String> allowedWordTypes = new HashSet<>();
        allowedWordTypes.add("(noun)");
        //        allowedWordTypes.add("(verb)");
        //        allowedWordTypes.add("(adj)");
        //        allowedWordTypes.add("(adv)");

        String currentWord = "";
        SynonymDictionary dictionary = new SynonymDictionary();
        ProgressMonitor monitor = new ProgressMonitor(lines.size());
        for (String line : lines) {
            if (!line.startsWith("(")) {
                currentWord = line.replaceAll("\\|.*", "");
            } else {
                String[] synonyms = line.split("\\|");
                for (String synonym : synonyms) {
                    if (!synonym.startsWith("(")) {
                        dictionary.addSynonym(currentWord.trim().intern(), synonym.trim().intern());
                    } else {
                        if (!allowedWordTypes.contains(synonym)) {
                            break;
                        }
                    }

                }
            }
            monitor.incrementAndPrintProgress();
        }

        LOGGER.info("saving dictionary to " + dictionaryFile.getName());
        FileHelper.serialize(dictionary, dictionaryFile.getPath());

        LOGGER.info("creating the dictionary took " + stopWatch.getElapsedTimeString());
    }

    public void createDictionaryOpenThesaurus(File inputFile, File dictionaryFile) throws IOException {
        StopWatch stopWatch = new StopWatch();

        List<String> lines = FileHelper.readFileToArray(inputFile.getPath());

        boolean nounsOnly = true;

        String currentWord = "";
        SynonymDictionary dictionary = new SynonymDictionary();
        ProgressMonitor monitor = new ProgressMonitor(lines.size());
        for (String line : lines) {
            monitor.incrementAndPrintProgress();

            // remove parentheses
            line = Pattern.compile("\\(.*?\\)").matcher(line).replaceAll("");
            line = StringHelper.trim(line);

            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }

            if (!Character.isUpperCase(line.charAt(0)) && nounsOnly) {
                continue;
            }

            line = line.toLowerCase();

            String[] parts = line.split(";");
            for (String part1 : parts) {
                for (String part2 : parts) {

                    if (part1.equals(part2)) {
                        continue;
                    }

                    dictionary.addSynonym(part1.trim().intern(), part2.trim().intern());

                }

            }

        }

        LOGGER.info("saving dictionary to " + dictionaryFile.getName());
        FileHelper.serialize(dictionary, dictionaryFile.getPath());

        LOGGER.info("creating the dictionary took " + stopWatch.getElapsedTimeString());
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        SynonymDictionaryCreator sfr = new SynonymDictionaryCreator();
        //        sfr.createDictionaryOpenOfficeFormat(new File("dict.dat"), new File("dictionary.gz"));
        //        sfr.createDictionaryOpenOfficeFormat(new File("E:\\Projects\\Programming\\Java\\Palladian\\palladian-core\\th_en_US_new.dat"), new File("E:\\Projects\\Programming\\Java\\Palladian\\palladian-core\\dictionary-nouns-en.gz"));
        sfr.createDictionaryOpenThesaurus(new File("E:\\Projects\\Programming\\Java\\Palladian\\palladian-core\\openthesaurus.txt"),
                new File("E:\\Projects\\Programming\\Java\\Palladian\\palladian-core\\dictionary-nouns-de.gz"));

        SynonymDictionary dictionary = (SynonymDictionary) FileHelper.deserialize("E:\\Projects\\Programming\\Java\\Palladian\\palladian-core\\dictionary-nouns-de.gz");
        CollectionHelper.print(dictionary.get("presskopf"));
        CollectionHelper.print(dictionary.get("schwartenmagen"));
        //        SynonymDictionary dictionary = (SynonymDictionary)FileHelper.deserialize("E:\\Projects\\Programming\\Java\\Palladian\\palladian-core\\dictionary-nouns-de.gz");
        //        CollectionHelper.print(dictionary.get("spaghetti"));
    }

}
