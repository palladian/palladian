package ws.palladian.classification.page;

import java.io.InputStream;
import java.util.HashSet;

import ws.palladian.extraction.feature.StopTokenRemover;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

/**
 * List of stopwords. Use the enumeration {@link Predefined} for initialization
 * with predefined stopword lists. You can also use your own stopword list via
 * constructor {@link #Stopwords(String)} or {@link #addFromFile(String)}. The
 * file must be newline separated, each line containing one stopword. Lines
 * prefixed with # are treated as comments and are therefore ignored. The list
 * is case insensitive.
 * 
 * @author Philipp Katz
 * @deprecated This class has been replaced by {@link StopTokenRemover}.
 */
@Deprecated
public class Stopwords extends HashSet<String> {

    private static final long serialVersionUID = 8764752921113362657L;

    /**
     * Available predefined stopword lists. Those are included with the toolkit
     * as resource files.
     */
    public static enum Predefined {

        // you can add your own stopword lists here ...
        EN("/stopwords_en.txt"), DE("/stopwords_de.txt");

        private String file;

        private Predefined(String file) {
            this.file = file;
        }

        private String getFile() {
            return file;
        }
    }

    /** line action for reading the stopword lists. */
    private LineAction readLineAction = new LineAction() {

        @Override
        public void performAction(String line, int lineNumber) {
            String lineString = line.trim();

            // ignore comments and empty lines ...
            if (!lineString.startsWith("#") && !lineString.isEmpty()) {
                add(line);
            }
        }
    };

    public Stopwords() {
        this(Predefined.EN);
    }

    public Stopwords(Predefined stopwordList) {
        addFromResourceFile(stopwordList.getFile());
    }

    public Stopwords(String filePath) {
        addFromFile(filePath);
    }

    /**
     * Add stopwords from file. One word each line, lines with # are treated as
     * comments.
     * 
     * @param filePath
     */
    public final void addFromFile(String filePath) {
        FileHelper.performActionOnEveryLine(filePath, readLineAction);
    }

    private void addFromResourceFile(String filePath) {
        InputStream stream = this.getClass().getResourceAsStream(filePath);
        FileHelper.performActionOnEveryLine(stream, readLineAction);
    }

    public boolean isStopword(String string) {
        return contains(string);
    }

    @Override
    public boolean add(String e) {
        return super.add(e.toLowerCase());
    }

    @Override
    public boolean contains(Object o) {
        boolean result = false;
        if (o instanceof String) {
            String word = (String) o;
            result = super.contains(word.toLowerCase());
        }
        return result;
    }

    @Override
    public String toString() {
        return CollectionHelper.getPrint(this);
    }

    public static void main(String[] args) {

        Stopwords stopwords = new Stopwords(Stopwords.Predefined.EN);
        System.out.println(stopwords.contains("and"));
        System.out.println(stopwords.contains("edurdo"));

        // Stopwords stopwords = new Stopwords();
        // System.out.println(stopwords.contains("The"));
        // System.out.println(stopwords);
    }

}