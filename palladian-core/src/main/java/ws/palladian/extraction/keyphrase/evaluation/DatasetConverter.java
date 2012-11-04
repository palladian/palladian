package ws.palladian.extraction.keyphrase.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

/**
 * <p>This class converts various datasets to the Palladian internal dataset format.</p>
 * 
 * @author Philipp Katz
 * 
 */
public class DatasetConverter {
    
    /** The new line character used when writing files. */
    static final char NEWLINE = '\n';
    
    /** The character for separating file name and individual key phrases. */
    static final char SEPARATOR = '#';

    public static void createCiteULike(File taggerDirectory, File indexOutput) throws IOException {
        Factory<CountMap<String>> factory = new Factory<CountMap<String>>() {
            @Override
            public CountMap<String> create() {
                return CountMap.create();
            }
        };
        Map<String, CountMap<String>> filenameKeyphrases = LazyMap.create(new TreeMap<String, CountMap<String>>(), factory);

        File[] tagFiles = FileHelper.getFiles(taggerDirectory.getAbsolutePath(), "tags", true, false);
        // Collection<File> tagFiles = FileUtils.listFiles(taggerDirectory, new String[] {"tags"}, true);
        for (File tagFile : tagFiles) {
            List<String> tags = FileHelper.readFileToArray(tagFile);
            String filename = tagFile.getName().replace(".tags", ".txt");
            CountMap<String> documentTags = filenameKeyphrases.get(filename);
            for (String tag : tags) {
                if (tag.length() > 0) {
                    // some .tag files in the dataset contain junk,
                    // which we filter here
                    if (tag.contains("  ")) {
                        tag = tag.substring(tag.indexOf("  ") + 2, tag.length());
                    }
                    tag = tag.trim();
                    documentTags.add(tag);
                }
            }
        }

        // write index file
        StringBuilder builder = new StringBuilder();

        for (Entry<String, CountMap<String>> entry : filenameKeyphrases.entrySet()) {
            builder.append(entry.getKey()).append(SEPARATOR);
            builder.append(StringUtils.join(entry.getValue().uniqueItems(), SEPARATOR));
            builder.append(NEWLINE);
        }
        FileHelper.writeToFile(indexOutput.getAbsolutePath(), builder);
    }
    
    public static void createSemEval2010(File keyphraseFileInput, File indexFileOutput) throws IOException {
        final Map<String,Set<String>> filenameKeyphrases = new HashMap<String,Set<String>>();
        FileHelper.performActionOnEveryLine(keyphraseFileInput.getAbsolutePath(), new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                Set<String> keyphrases = new HashSet<String>();
                String[] split = line.split(" : ");
                    String[] split2 = split[1].split(",");
                    for (String s : split2) {
                        keyphrases.addAll(Arrays.asList(s.split("\\+")));
                    }
                String filename = split[0].concat(".txt.final");
                filenameKeyphrases.put(filename,keyphrases);
            }
        });
        StringBuilder stringBuilder = new StringBuilder();
        for (Entry<String, Set<String>> entry : filenameKeyphrases.entrySet()) {
            stringBuilder.append(entry.getKey()).append(SEPARATOR);
            stringBuilder.append(StringUtils.join(entry.getValue(), SEPARATOR));
            stringBuilder.append(NEWLINE);
        }
        FileHelper.writeToFile(indexFileOutput.getAbsolutePath(), stringBuilder);
    }

    public static void createFAO(String pathToRawFiles, String resultFile) {

        File[] files = FileHelper.getFiles(pathToRawFiles, ".key");
        StringBuilder sb = new StringBuilder();

        for (File file : files) {

            String keyFile = file.getName();
            List<String> keywords = FileHelper.readFileToArray(pathToRawFiles + "/" + keyFile);

            sb.append(keyFile.replace(".key", ".txt"));
            sb.append(SEPARATOR);
            sb.append(StringUtils.join(keywords, SEPARATOR));
            sb.append(NEWLINE);

        }

        FileHelper.writeToFile(resultFile, sb);

    }

    public static void createDeliciousT140(final File pathToTaginfoXml, final File indexFileOutput) {
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            DefaultHandler handler = new DeliciousT140Handler(indexFileOutput, 50, 0.05f);
            parser.parse(pathToTaginfoXml, handler);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        } catch (SAXException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static void main(String[] args) throws Exception {
        
        createDeliciousT140(new File("/Users/pk/Desktop/delicioust140/taginfo.xml"), new File("/Users/pk/Desktop/delicioust140index.txt"));

        //createCiteULike(
        //       new File("/Users/pk/Desktop/citeulike180/taggers"),
        //        new File("/Users/pk/Desktop/citeulike180index.txt"));
        
        // createSemEval2010(new File("/Users/pk/Desktop/SemEval2010/train/train.combined.final"), new File("/Users/pk/Desktop/semEvalTrainCombinedIndex.txt"));

        // createFAO("/Users/pk/temp/fao780", "/Users/pk/temp/fao780.txt");
        // createDeliciousT140("/home/pk/DATASETS/delicioust140", "/home/pk/temp/deliciousT140");
        // createDeliciousT140("/Users/pk/Studium/Diplomarbeit/delicioust140", "/Users/pk/temp/deliciousT140");
    }

}
