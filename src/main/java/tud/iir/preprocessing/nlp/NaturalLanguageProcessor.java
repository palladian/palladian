package tud.iir.preprocessing.nlp;

import tud.iir.extraction.entity.ner.Annotations;
import tud.iir.extraction.entity.ner.NamedEntityRecognizer;

/**
 * The NaturalLanguageProcessor holds functionality from the field of natural
 * language processing, such as a <code>NamedEntityRecognizer</code>,
 * <code>POSTagger</code> and <code>Parser</code>.
 *
 * @author Martin Wunderwald
 */
public abstract class NaturalLanguageProcessor {

    /** The Named Entity Recognizer. **/
    protected NamedEntityRecognizer ner;

    /** The POS-Tagger used in this class. **/
    protected AbstractPOSTagger posTagger;

    /** The PhraseChunker. **/
    protected AbstractPhraseChunker phraseChunker;

    /** The Parser. **/
    protected AbstractParser parser;

    /** The SentenceDetector. **/
    protected AbstractSentenceDetector sentenceDetector;

    /**
     * prepares the processor by loading its parts.
     */
    protected abstract void init();

    /**
     * @return the ner
     */
    public final NamedEntityRecognizer getNer() {
        return ner;
    }

    /**
     * @param ner
     *            the ner to set
     */
    public final void setNer(final NamedEntityRecognizer ner) {
        this.ner = ner;
    }

    /**
     * @return the posTagger
     */
    public final AbstractPOSTagger getPosTagger() {
        return posTagger;
    }

    /**
     * @param posTagger
     *            the posTagger to set
     */
    public final void setPosTagger(final AbstractPOSTagger posTagger) {
        this.posTagger = posTagger;
    }

    /**
     * @return the phraseChunker
     */
    public final AbstractPhraseChunker getPhraseChunker() {
        return phraseChunker;
    }

    /**
     * @param phraseChunker
     *            the phraseChunker to set
     */
    public final void setPhraseChunker(final AbstractPhraseChunker phraseChunker) {
        this.phraseChunker = phraseChunker;
    }

    /**
     * @return the parser
     */
    public final AbstractParser getParser() {
        return parser;
    }

    /**
     * @param parser
     *            the parser to set
     */
    public final void setParser(final AbstractParser parser) {
        this.parser = parser;
    }

    /**
     * @return the sentenceDetector
     */
    public final AbstractSentenceDetector getSentenceDetector() {
        return sentenceDetector;
    }

    /**
     * @param sentenceDetector
     *            the sentenceDetector to set
     */
    public final void setSentenceDetector(
            final AbstractSentenceDetector sentenceDetector) {
        this.sentenceDetector = sentenceDetector;
    }

    /**
     * returns POS-Tags of a string.
     *
     * @param sentence
     * @return the tag annotations
     */
    public final TagAnnotations getPOSTags(final String sentence) {
        return posTagger.loadDefaultModel().tag(sentence).getTagAnnotations();
    }

    /**
     * performs phrase chunking on a sentence.
     *
     * @param sentence
     *            - The sentence
     * @return The part of speach tags.
     */
    public final TagAnnotations getPhraseChunks(final String sentence) {
        return phraseChunker.loadDefaultModel().chunk(sentence)
                .getTagAnnotations();
    }

    /**
     * returns a Parse on a sentence.
     *
     * @param sentence
     * @return the parse
     */
    public final TagAnnotations getParse(final String sentence) {
        return parser.loadDefaultModel().parse(sentence).getTagAnnotations();
    }

    /**
     * Split a provided string into sentences and return a set of sentence
     * chunks.
     *
     * @param sentence
     * @return the sentences
     */
    public final String[] getSentences(final String text) {
        // sentenceDetector.loadDefaultModel();
        return sentenceDetector.loadDefaultModel().detect(text).getSentences();
    }

    /**
     * @param text
     * @return
     */
    public Annotations getNamedEntityAnnotations(final String text) {
        return ner.getAnnotations(text);
    }

}
