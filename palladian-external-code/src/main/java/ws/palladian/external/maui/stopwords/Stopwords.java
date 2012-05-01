package ws.palladian.external.maui.stopwords;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Class that can test whether a given string is a stop word.
 * Lowercases all words before the test.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 2.0
 */
public /*abstract*/ class Stopwords implements Serializable {
  
	private static final long serialVersionUID = 1L;
	
	// String filePath;
	
    /** The set containing the stopwords. */
    private Set<String> stopwords = new HashSet<String>();

	
	public Stopwords(String filePath) {
		// this.filePath = filePath;
	    loadFromFile(filePath);
	}
	
    private void loadFromFile(String filePath) {
        InputStream is = Stopwords.class.getResourceAsStream(filePath);
        if (is == null) {
            //System.err.println(filePath + " not found.");
            //return;
            throw new IllegalStateException(filePath + " not found.");
        }
        
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        
        try {
            String buffer = br.readLine();
            while (buffer != null) {
                if (!buffer.isEmpty()) {
                    stopwords.add(buffer.toString());
                }
                buffer = br.readLine();
            }
            br.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }        
    }
	
  /** 
   * Returns true if the given string is a stop word.
   */
  // public abstract boolean isStopword(String str);
    public boolean isStopword(String str) {
        return stopwords.contains(str.toLowerCase());
    }
    
    @Override
    public String toString() {
        return stopwords.toString();
    }
    
    public static void main(String[] args) {
        Stopwords stopwords = new Stopwords("stopwords_de.txt");
        System.out.println(stopwords);
    }
}


