package ws.palladian.retrieval.cooccurrence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.feature.StemmerAnnotator;
import ws.palladian.extraction.feature.TermCorpus;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.retrieval.cooccurrence.CooccurrenceMatrix;

public class PhraseProbabilityCalculator {

    private final CooccurrenceMatrix matrix;

    private final StemmerAnnotator stemmer;

    public PhraseProbabilityCalculator(File matrixFile, Language language) {
        Validate.notNull(matrixFile, "matrixFile must not be null");
        try {
            this.matrix = CooccurrenceMatrix.load(matrixFile);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        if (language != null) {
            stemmer = new StemmerAnnotator(language);
        } else {
            stemmer = null;
        }
    }

    /**
     * @param matrix The {@link CooccurrenceMatrix}.
     * @param language The language. Set to <code>null</code> if no stemming is to be used.
     */
    public PhraseProbabilityCalculator(CooccurrenceMatrix matrix, Language language) {
        Validate.notNull(matrix, "matrix must not be null");
        this.matrix = matrix;
        if (language != null) {
            stemmer = new StemmerAnnotator(language);
        } else {
            stemmer = null;
        }
    }

    public double getProbability(String phrase) {
        String[] tokens = phrase.split("\\s");
        String stem = stem(tokens[0].toLowerCase());
        double probability = matrix.getProbability(stem, true);
        double phraseProbability = Math.log10(probability);
        for (int i = 0; i <= tokens.length - 2; i++) {
            String first = stem(tokens[i].toLowerCase());
            String second = stem(tokens[i + 1].toLowerCase());
            double bigramProbability = matrix.getConditionalProbability(second, first, true);
            phraseProbability += Math.log10(bigramProbability);
        }
        // System.out.println(phrase + " : " + phraseProbability);
        return phraseProbability;
    }

    private String stem(String token) {
        if (stemmer == null) {
            return token;
        }
        return stemmer.stem(token);
    }

    /**
     * <p>
     * Convert both unigram/bigram {@link TermCorpus} to a {@link CooccurrenceMatrix}.
     * </p>
     * 
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void convert() throws FileNotFoundException, IOException {
        final CooccurrenceMatrix counts = new CooccurrenceMatrix();
        FileHelper.performActionOnEveryLine(new GZIPInputStream(new FileInputStream(
                "/Users/pk/Dropbox/Uni/Datasets/TermCorpora/bigrams25min.gz")), new LineAction() {
            @Override
            public void performAction(String text, int number) {
                String[] split = text.split("#");
                if (number <= 1 || split.length != 2) {
                    String[] tokens = split[0].split("\\s");
                    counts.add(tokens[0], tokens[1], Integer.parseInt(split[1]));
                }
            }
        });
        FileHelper.performActionOnEveryLine(new GZIPInputStream(new FileInputStream(
                "/Users/pk/Dropbox/Uni/Datasets/TermCorpora/wikipediaTermCorpusStemmed25min.gz")), new LineAction() {
            @Override
            public void performAction(String text, int number) {
                String[] split = text.split("#");
                if (number <= 1 || split.length != 2) {
                    counts.set(split[0], Integer.parseInt(split[1]));
                }
            }
        });
        OutputStream stream = new GZIPOutputStream(new FileOutputStream("matrix.gz"));
        counts.save(stream);
    }

    public static void main(String[] args) throws IOException {
        String s1 = "parts still contain an old village core";
        String s2 = "core still village contain parts old an";
        PhraseProbabilityCalculator probs = new PhraseProbabilityCalculator(new File("matrix.gz"), Language.ENGLISH);
        System.out.println(s1 + " : " + probs.getProbability(s1));
        System.out.println(s2 + " : " + probs.getProbability(s2));
    }

}
