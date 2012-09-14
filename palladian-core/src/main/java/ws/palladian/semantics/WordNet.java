package ws.palladian.semantics;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.helper.nlp.StringHelper;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class WordNet {

	
    /**
     * Return noun synonyms for the given word by looking it up in the WordNet database.
     * 
     * @param word The word.
     * @param number The number.
     * @return An array of synonyms.
     */
    public static String[] getSynonyms(String word, int number) {
        return WordNet.getSynonyms(word, number, false);
    }
    
    public static String[] getSynonyms(String word, int number, boolean includeBaseWord) {
    	return WordNet.getSynonyms(word, number, includeBaseWord, true);
    }

    public static String[] getSynonyms(String word, int number, boolean includeBaseWord, boolean justNouns) {

        ArrayList<String> synonyms = new ArrayList<String>();
        if (includeBaseWord)
            synonyms.add(word.toLowerCase());

        WordNetDatabase database = WordNetDatabase.getFileInstance();

        Synset[] nouns;
        if (justNouns) {
        	nouns = database.getSynsets(word, SynsetType.NOUN);
        } else {
        	nouns = database.getSynsets(word);
        }

        int wordCounter = 0;
        for (int i = 0; i < nouns.length; ++i) {
            String[] wordNetSynonyms = nouns[i].getWordForms();
            for (int j = 0; j < wordNetSynonyms.length; ++j) {
                // do not add words that are equal to the word given and do not add duplicates
                if (synonyms.contains(nouns[i].getWordForms()[j]))
                    continue;
//                System.out.println(nouns[i].getWordForms()[j]);
                synonyms.add(nouns[i].getWordForms()[j]);
                ++wordCounter;
                if (wordCounter >= number)
                    break;
            }
            if (wordCounter >= number)
                break;
        }

        String str[] = new String[synonyms.size()];
        synonyms.toArray(str);
        return str;
    }
    
    /**
     * 
     * @param word
     * @param depth
     * @param maxWords
     * @return
     * 
     * @author Dmitry Myagkikh
     */
    public static String[] getHypernyms(String word, int depth, int maxWords) {
        ArrayList<String> hypernyms = new ArrayList<String>();
        if (depth > 0) {
	        WordNetDatabase database = WordNetDatabase.getFileInstance();
	        Synset[] synsets = database.getSynsets(word, SynsetType.NOUN);
	        for (int i = 0; i < synsets.length; ++i) {
	        	NounSynset nounSynset = (NounSynset)(synsets[i]); 
	        	NounSynset[] hypernymSynsets = nounSynset.getHypernyms();
	        	for(NounSynset hypernymSynset: hypernymSynsets) {
	        		for(String hypernym: hypernymSynset.getWordForms()) {
	        			if (!hypernyms.contains(hypernym)) {
	        				if (hypernyms.size() <= maxWords) {
	        					hypernyms.add(hypernym);
	        				}
	        			}
	        			for(String hypernymHypernym: WordNet.getHypernyms(hypernym, depth - 1, maxWords - hypernyms.size())) {
	        				if (!hypernyms.contains(hypernymHypernym)) {
	        					if (hypernyms.size() <= maxWords) {
	        						hypernyms.add(hypernymHypernym);
	        					}
	        				}
	            		}
	        		}
	        	}
	        }
        }
        String str[] = new String[hypernyms.size()];
        hypernyms.toArray(str);
        return str;
    }
    
    /**
     * 
     * @param word
     * @param depth
     * @param maxWords
     * @return
     * 
     * @author Dmitry Myagkikh
     */
    public static String[] getHyponyms(String word, int depth, int maxWords) {
        ArrayList<String> hyponyms = new ArrayList<String>();
        if (depth > 0) {
	        WordNetDatabase database = WordNetDatabase.getFileInstance();
	        Synset[] synsets = database.getSynsets(word, SynsetType.NOUN);
	
	        for (int i = 0; i < synsets.length; ++i) {
	        	NounSynset nounSynset = (NounSynset)(synsets[i]); 
	        	NounSynset[] hyponymSynsets = nounSynset.getHyponyms();
	        	for(NounSynset hyponymSynset: hyponymSynsets) {
	        		for(String hyponym: hyponymSynset.getWordForms()) {
	        			if (!hyponyms.contains(hyponym)) {
	        				if (hyponyms.size() <= maxWords) {
	        					hyponyms.add(hyponym);
	        				}
	        			}
	        			for(String hypernymHypernym: WordNet.getHyponyms(hyponym, depth - 1, maxWords - hyponyms.size())) {
	        				if (!hyponyms.contains(hypernymHypernym)) {
	        					if (hyponyms.size() <= maxWords) {
	        						hyponyms.add(hypernymHypernym);
	        					}
	        				}
	            		}
	        		}
	        	}
	        }
        }
        String str[] = new String[hyponyms.size()];
        hyponyms.toArray(str);
        return str;
    }
    
    // TODO works, but I dont use it any more. keep? remove?
    /**
     * <p>
     * Try to transform a gerund back to its infinitive form. The following code is very "ad hoc" and depends on the
     * WordNet database. We simply try out different infinitive possibilities and check their occurence and counts in
     * WordNet. We assume that the occurence with the highest count in WordNet is the correct infinitive form of the
     * supplied gerund. Basically, there are three possibilities when transforming an infinitive to gerund:
     * </p>
     * 
     * <ol>
     * <li>think > thinking: Most simple variant by just appending the -ing suffix.</li>
     * <li>hit > hit<u>t</u>ing: The ending consonant is doubled, then -ing is appended.</li>
     * <li>take > tak<s>e</s><u>ing</u>: The -e is removed before appending -ing.</li>
     * </ol>
     * 
     * <p>
     * The problem when doing a revese-transformation is, that we cannot know from the gerund form itself which of the
     * above rules was applied (e. g. "thinking" vs. "taking"), so have to try out all three back-transformations.
     * </p>
     * 
     * @see <a href="http://web2.uvcs.uvic.ca/elc/studyzone/410/grammar/gerund.htm">Forming Gerunds</a>
     * @see <a href="http://wordnet.princeton.edu">Wordnet</a>
     * 
     * @param gerund the gerund to transform.
     * @return infinitive form of the gerund, or the supplied word, if no transformation can be applied.
     * 
     * @author Philipp Katz
     */
    public static String gerundToInfinitive(String gerund) {

        String infinitive = gerund;

        if (gerund.endsWith("ing") && gerund.length() > 4) {

            List<String> candidates = new ArrayList<String>(3);

            // simply remove -ing suffix
            String stem = gerund.substring(0, gerund.length() - 3).toLowerCase();

            candidates.add(stem);
            candidates.add(stem.concat("e"));

            // check if stemmed form ends on a doubled consonant, like "hitting"
            // if so, add "hit" to candidates
            char firstLast = stem.charAt(stem.length() - 1);
            char secondLast = stem.charAt(stem.length() - 2);
            boolean removeConsonant = true;
            removeConsonant = removeConsonant && !StringHelper.isVowel(firstLast);
            removeConsonant = removeConsonant && firstLast == secondLast;
            if (removeConsonant) {
                candidates.add(stem.substring(0, stem.length() - 1));
            }

            // now we check all candidates (two or three) for their plausibilities
            WordNetDatabase database = WordNetDatabase.getFileInstance();

            int maxCount = 0;
            for (String candidate : candidates) {
                Synset[] synsets = database.getSynsets(candidate, SynsetType.VERB);
                int currentCount = 0;
                for (Synset synset : synsets) {
                    for (String word : synset.getWordForms()) {
                        if (word.equals(candidate)) {
                            currentCount += 1 + synset.getTagCount(word);
                        }
                    }
                }
                if (currentCount > maxCount) {
                    infinitive = candidate;
                    maxCount = currentCount;
                }
            }
        }
        // System.out.println(gerund + " -> " + infinitive);
        return infinitive;

    }


    /**
     * @param args
     */
    public static void main(String[] args) {
        WordNet.getSynonyms("Country", 3, true);

        String[] synonyms = WordNet.getSynonyms("mobile phone", 3, true);
        System.out.println(synonyms.length + " " + synonyms[0] + " " + synonyms[1] + " " + synonyms[2] + " " + synonyms[3]);
    }
}