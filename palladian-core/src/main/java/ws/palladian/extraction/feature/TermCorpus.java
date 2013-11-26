package ws.palladian.extraction.feature;

public interface TermCorpus {

    /**
     * <p>
     * Get the number of documents containing the specified term.
     * </p>
     * 
     * @param term The term for which to retrieve the number of containing documents.
     * @return The number of documents containing the specified term.
     */
    int getCount(String term);

    /**
     * <p>
     * Get the inverse document frequency for the specified term. To avoid division by zero, the number of documents
     * containing the specified term is incremented by one.
     * </p>
     * 
     * @param term The term for which to retrieve the inverse document frequency.
     * @param smoothing Use add-one smoothing to avoid division by zero.
     * @return The inverse document frequency for the specified term.
     */
    double getIdf(String term, boolean smoothing);

    /**
     * <p>
     * Get the probability for the given term.
     * </p>
     * 
     * @param term The term.
     * @return The probability.
     */
    double getProbability(String term);

    /**
     * <p>
     * Get the number of documents in this corpus.
     * </p>
     * 
     * @return The number of documents in this corpus.
     */
    int getNumDocs();

    /**
     * <p>
     * Get the number of total terms in this corpus, i.e. also count duplicates.
     * </p>
     * 
     * @return The total number of terms in this corpus.
     */
    int getNumTerms();

    /**
     * <p>
     * Get the number of unique terms in this corpus, i.e. count the same terms only once.
     * </p>
     * 
     * @return The number of unique terms in this corpus.
     */
    int getNumUniqueTerms();

}
