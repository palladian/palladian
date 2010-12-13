package tud.iir.extraction.keyphrase;

import java.util.Set;

public abstract class AbstractKeyphraseExtractor {
    
    public abstract Set<Keyphrase> extract(String inputText);

}