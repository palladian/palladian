package ws.palladian.preprocessing.nlp.pos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.helper.nlp.Tokenizer;
import ws.palladian.preprocessing.nlp.TagAnnotation;
import ws.palladian.preprocessing.nlp.TagAnnotations;
import ws.palladian.retrieval.semantics.Word;
import ws.palladian.retrieval.semantics.WordDB;

public class WiktionaryPosTagger extends PosTagger {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WiktionaryPosTagger.class);

    /**
     * <p>We need a mapping from word type names in the Wiktionary DB to POS tags. We use the <a
     * href="http://en.wikipedia.org/wiki/Brown_Corpus#Part-of-speech_tags_used">Brown Corpus</a> tags for the
     * mapping.</p>
     */
    private Map<String, String> posTagMapping;

    public WiktionaryPosTagger() {
        setName("WiktionaryPosTagger");
        posTagMapping = new HashMap<String, String>();
        setMapping();
    }

    /**
     * Create the mapping.
     */
    private void setMapping() {
        posTagMapping.put("Article", "AT");
        posTagMapping.put("Noun", "NN");
        posTagMapping.put("Proper", "NNP");
        posTagMapping.put("Adjective", "JJ");
        posTagMapping.put("Verb", "VB");
        posTagMapping.put("Preposition", "IN");
    }

    @Override
    public PosTagger loadModel() {
        WordDB wordDB = new WordDB("data/temp/wordDatabaseEnglish/");
        setModel(wordDB);
        return this;
    }

    @Override
    public PosTagger loadModel(String modelFilePath) {
        WordDB wordDB = new WordDB(modelFilePath);
        setModel(wordDB);
        return this;
    }

    @Override
    public WordDB getModel() {
        return (WordDB) super.getModel();
    }

    @Override
    public PosTagger tag(String sentence) {
        List<String> tokens = Tokenizer.tokenize(sentence);

        // you can load the database into the memory for faster read access (requires lots of RAM)
        // wordDB.loadDbToMemory();

        TagAnnotations tagAnnotations = new TagAnnotations();

        int lastIndex = -1;

        for (String token : tokens) {

            TagAnnotation tagAnnotation = null;

            int index = sentence.indexOf(token, lastIndex);

            // single characters are left alone (they are their own type)
            if (token.length() == 1 && !token.equals("I") && !token.equalsIgnoreCase("a")) {

                tagAnnotation = new TagAnnotation(index, token, token);

            } else {

                String type = "?";

                // check whether word can be tagged with hard coded tags
                if (token.equalsIgnoreCase("a") || token.equalsIgnoreCase("the")) {
                    type = "AT";
                } else if (token.equals("be")) {
                    type = "BE";
                } else if (token.equals("were")) {
                    type = "BED";
                } else if (token.equals("was")) {
                    type = "BEDZ";
                } else if (token.equals("being")) {
                    type = "BEG";
                } else if (token.equals("am")) {
                    type = "BEM";
                } else if (token.equals("been")) {
                    type = "BEN";
                } else if (token.equals("are")) {
                    type = "BER";
                } else if (token.equals("is")) {
                    type = "BEZ";
                } else if (token.equals("and") || token.equals("or")) {
                    type = "CC";
                } else if (StringHelper.isNumericExpression(token)) {
                    type = "CD";
                } else if (token.equals("do")) {
                    type = "DO";
                } else if (token.equals("did")) {
                    type = "DOD";
                } else if (token.equals("does")) {
                    type = "DOZ";
                } else if (token.equals("this") || token.equals("that")) {
                    type = "DT";
                } else if (token.equals("some") || token.equals("any")) {
                    type = "DTI";
                } else if (token.equals("these") || token.equals("those")) {
                    type = "DTS";
                } else if (token.equals("either")) {
                    type = "DTX";
                } else if (token.equals("have")) {
                    type = "HV";
                } else if (token.equals("had")) {
                    type = "HVD";
                } else if (token.equals("having")) {
                    type = "HVG";
                } else if (token.equals("can") || token.equals("should") || token.equals("will")) {
                    type = "MD";
                } else if (token.equals("me") || token.equals("him") || token.equals("them")) {
                    type = "PPO";
                } else if (token.equals("he") || token.equals("she") || token.equals("it") || token.equals("one")) {
                    type = "PPS";
                } else if (token.equals("I") || token.equals("we") || token.equals("they") || token.equals("you")) {
                    type = "PPSS";
                } else {

                    // search a word in the database
                    Word word = getModel().getWord(token);

                    LOGGER.debug(word);

                    if (word != null) {
                        type = word.getType();
                    } else {

                        // if we did not find it, we try lowercase (since word could have been at the start of a
                        // sentence)
                        word = getModel().getWord(token.toLowerCase());
                        if (word != null) {
                            type = word.getType();
                        }
                    }

                    String mappedType = posTagMapping.get(type);
                    if (mappedType != null) {
                        type = mappedType;
                    }

                }

                tagAnnotation = new TagAnnotation(sentence.indexOf(token), type, token);
            }

            lastIndex = index + 1;

            tagAnnotations.add(tagAnnotation);
        }

        setTagAnnotations(tagAnnotations);

        return this;
    }

    @Override
    public PosTagger tag(String sentence, String modelFilePath) {
        loadModel(modelFilePath);
        tag(sentence);
        return this;
    }

    /**
     * Test the Wiktionary POS Tagger. You would need the model (either in data/temp/wordDatabaseEnglish or you have to
     * specify the path). Also you probably want to give more RAM using the -Xmx flag (-Xmx1800M) to run this code.
     * 
     * @param args
     */
    public static void main(String[] args) {

        String sentence = "The quick brown fox jumps over the lazy dog. I did this as fast as you and was as happy as 1000$ could make one.";
        // LingPipe: The/AT quick/JJ brown/JJ fox/NN jumps/NNS over/IN the/AT lazy/JJ dog/NN ./. I/PPSS did/DOD this/DT
        // as/QL fast/RB as/CS you/PPSS and/CC was/BEDZ as/QL happy/JJ as/CS 1000/CD $/NNS could/MD make/VB one/CD ./.

        // Wiktionary: The/AT quick/JJ brown/NN fox/NN jumps/NN over/JJ the/AT lazy/JJ dog/NN ./. I/PPSS did/DOD this/DT
        // as/NN fast/VB as/NN you/PPSS and/CC was/BEDZ as/NN happy/JJ as/NN 1000/CD $/$ could/VB make/VB one/PPS ./.

        PosTagger tagger = new WiktionaryPosTagger();
        // tagger = new LingPipePOSTagger();
        tagger.loadModel();
        LOGGER.info(tagger.tag(sentence).getTaggedString());
    }
}
