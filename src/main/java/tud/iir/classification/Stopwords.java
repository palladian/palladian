package tud.iir.classification;

import java.util.HashSet;

import tud.iir.helper.FileHelper;
import tud.iir.helper.LineAction;

/**
 * List of stopwords. Use the constants {@link Stopwords#STOP_WORDS_EN} or {@link Stopwords#STOP_WORDS_DE} for
 * initialization with pre-defined stopword list.
 * 
 * TODO when using Toolkit JAR in another project, the stopwords have to be copied to this project now. Use
 * class.getResouce() to avoid this?
 * 
 * http://www.devx.com/tips/Tip/5697
 * 
 * @author Philipp Katz
 * 
 */
public class Stopwords extends HashSet<String> {
	
    private static final long serialVersionUID = 8764752921113362657L;
    
    public static final String STOP_WORDS_EN = "config/stopwords_en.txt";
    public static final String STOP_WORDS_DE = "config/stopwords_de.txt";
	
	public Stopwords() {
	    this(STOP_WORDS_EN);
	}
	
    public Stopwords(String filePath) {
        addFromFile(filePath);
    }

    /**
     * Add stopwords from file. One word each line, lines with # are treated as comments.
     * 
     * @param filePath
     */
    public void addFromFile(String filePath) {
        FileHelper.performActionOnEveryLine(filePath, new LineAction(new Object[] { this }) {

            @Override
            public void performAction(String line, int lineNumber) {

                String lineString = line.trim();

                // ingore comments and empty lines ...
                if (!lineString.startsWith("#") && !lineString.isEmpty()) {
                    Stopwords stopwords = (Stopwords) arguments[0];
                    stopwords.add(line);
                }

            }
        });
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
		return "#stopwords:" + this.size();
	}
	
	public static void main(String[] args) {
	    Stopwords stopwords = new Stopwords();
        System.out.println(stopwords.contains("The"));
        System.out.println(stopwords);
    }

}