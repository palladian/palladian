package tud.iir.classification.controlledtagging;

import org.tartarus.snowball.SnowballStemmer;

/**
 * A NOP stemmer.
 * 
 * @author Philipp Katz
 *
 */
public class NoStemmer extends SnowballStemmer {

    @Override
    public boolean stem() {
        return false;
    }    

}
