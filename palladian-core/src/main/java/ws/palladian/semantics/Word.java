package ws.palladian.semantics;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * This class represents a single word which is held in the {@link WordDB}.
 * </p>
 * 
 * @author David Urbansky
 */
public class Word {

    /** The database id. */
    private final int id;

    /** The actual word. */
    private final String word;

    /** The word's plural if it's a noun. */
    private String plural = "";

    /** The type of the word, e.g. "noun" or "adjective". */
    private String type = "";

    /** The language of the word. */
    private String language = "";

    /** A set of synonyms for this word. */
    private Set<Word> synonyms = new LinkedHashSet<Word>();

    /** A set of hypernyms for this word. */
    private Set<Word> hypernyms = new LinkedHashSet<Word>();

    /** A set of hyponyms for this word. */
    private Set<Word> hyponyms = new LinkedHashSet<Word>();

    public Word(int id, String word, String plural, String type, String language) {
        this.id = id;
        this.word = word;
        this.plural = plural;
        this.type = type;
        this.language = language;
    }

    public int getId() {
        return id;
    }

    public String getWord() {
        return word;
    }

    public void setPlural(String plural) {
        this.plural = plural;
    }

    public String getPlural() {
        return plural;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Set<Word> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(Set<Word> synonyms) {
        this.synonyms = synonyms;
    }

    public Set<Word> getHypernyms() {
        return hypernyms;
    }

    public void setHypernyms(Set<Word> hypernyms) {
        this.hypernyms = hypernyms;
    }

    public Set<Word> getHyponyms() {
        return hyponyms;
    }

    public void setHyponyms(Set<Word> hyponyms) {
        this.hyponyms = hyponyms;
    }

    /**
     * <p>
     * Generate the path from this word to the root by following the hypernyms.
     * </p>
     * <p>
     * For example, the path to "banana" could be [fruit,food,thing]
     * </p>
     * 
     * @return A sorted list of words where each entry is the hypernym of the preceding entry.
     */
    public List<String> getPath(WordDB wordDb) {
        return getPath(wordDb, 0, new HashSet<String>());
    }

    private LinkedList<String> getPath(WordDB wordDb, int depth, Set<String> seenWords) {

        LinkedList<String> path = new LinkedList<String>();

        if (depth > 3) {
            return path;
        }

        Set<Word> hypernyms = getHypernyms();

        for (Word hypernym : hypernyms) {
            if (hypernym.getWord().equalsIgnoreCase(getWord()) || !seenWords.add(hypernym.getWord())) {
                continue;
            }
            wordDb.aggregateInformation(hypernym);
            LinkedList<String> currentPath = hypernym.getPath(wordDb, depth + 1, seenWords);
            if (currentPath.size() > path.size()) {
                path = currentPath;
            }
        }

        if (!path.contains(getWord())) {
            path.addFirst(getWord());
        }

        return path;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + (word == null ? 0 : word.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Word other = (Word) obj;
        if (id != other.id) {
            return false;
        }
        if (word == null) {
            if (other.word != null) {
                return false;
            }
        } else if (!word.equals(other.word)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Word [id=");
        builder.append(id);
        builder.append(", word=");
        builder.append(word);
        builder.append(", plural=");
        builder.append(plural);
        builder.append(", type=");
        builder.append(type);
        builder.append(", language=");
        builder.append(language);
        builder.append(", synonyms=");

        int i = 0;
        for (Word synonym : synonyms) {
            if (i++ >= 1) {
                builder.append(",");
            }
            builder.append(synonym.getWord());
        }

        builder.append(", hypernyms=");

        i = 0;
        for (Word hypernym : hypernyms) {
            if (i++ >= 1) {
                builder.append(",");
            }
            builder.append(hypernym.getWord());
        }

        builder.append("]");
        return builder.toString();
    }

}