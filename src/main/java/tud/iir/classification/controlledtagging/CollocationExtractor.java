package tud.iir.classification.controlledtagging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;

import tud.iir.classification.Stopwords;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.Tokenizer;
import tud.iir.web.Crawler;

public class CollocationExtractor {

    // private Stopwords stopwords = new Stopwords(Stopwords.STOP_WORDS_EN);
    private Stopwords stopwords = new Stopwords(Stopwords.Predefined.EN);


    public void extractCollocations(String text) {
        extractCollocations(text, 5);
    }

    public void extractCollocations(String text, int maxLength) {

        List<String> tokens = Tokenizer.tokenize(text);
        Bag<String> tokensBag = new HashBag<String>(tokens);
        List<Gram> collocations = new ArrayList<Gram>();

        for (int i = maxLength; i >= 2; i--) {

            List<Gram> grams = makeGrams(tokens, i);
            Bag<Gram> gramsBag = new HashBag<Gram>(grams);

            for (Gram gram : gramsBag.uniqueSet()) {

                boolean accept = true;
                float score = Float.MAX_VALUE;

                StringBuilder debug = new StringBuilder();
                debug.append(gram).append(" : ").append(gramsBag.getCount(gram));
                debug.append("  {");

                for (String token : gram) {

                    accept = accept && token.matches("[A-Za-z0-9\\-\\.]{3,}") && !stopwords.contains(token);
                    int count = tokensBag.getCount(token);
                    score = Math.min(score, count);

                    debug.append(token).append(":").append(count).append(",");

                }

                debug.delete(debug.length() - 1, debug.length());
                debug.append("}");

                int gramCount = gramsBag.getCount(gram);
                score = gramCount / score;
                debug.append(" score:").append(score);

                for (Gram existingGram : collocations) {
                    if (existingGram.toString().contains(gram.toString()) && existingGram.getCount() == gramCount) {
                        accept = false;
                    }
                }

                if (accept && gramCount > 1) {
                    gram.setCount(gramCount);
                    gram.setScore(score);
                    collocations.add(gram);
                }

            }
        }

        Collections.sort(collocations, new GramComparator());
        for (Gram entry : collocations) {
            System.out.println(entry.toString() + " -> " + entry.getScore() + " " + entry.getCount());
        }
        System.out.println("----------------------");
        System.out.println(collocations.size());

    }

    private List<Gram> makeGrams(List<String> tokens, int n) {

        List<Gram> result = new ArrayList<Gram>();
        String[] tokensArray = tokens.toArray(new String[tokens.size()]);

        for (int i = 0; i <= tokensArray.length - n; i++) {

            Gram nGram = new Gram();

            for (int j = i; j < i + n; j++) {
                nGram.add(tokensArray[j]);
            }

            result.add(nGram);

        }
        return result;

    }

    public static void main(String[] args) {
        // String content = new PageContentExtractor().getResultText("http://en.wikipedia.org/wiki/Healthscope");
        // System.out.println(content);

        Crawler crawler = new Crawler();
        CollocationExtractor ce = new CollocationExtractor();
        // Document doc = crawler.getWebDocument("http://en.wikipedia.org/wiki/Apple_iPhone");
        // Document doc = crawler.getWebDocument("http://en.wikipedia.org/wiki/San_Francisco");
        // Document doc = crawler.getWebDocument("http://en.wikipedia.org/wiki/%22Manos%22_The_Hands_of_Fate");
        Document doc = crawler.getWebDocument("http://en.wikipedia.org/wiki/Cat");
        String content = HTMLHelper.htmlToString(doc);

        ce.extractCollocations(content, 10);

    }

}

class Gram extends ArrayList<String> {
    private static final long serialVersionUID = 1L;

    private int count;
    private float score;

    @Override
    public String toString() {
        return StringUtils.join(this, " ");
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Gram) {
            Gram gram = (Gram) obj;
            if (gram.toString().equals(obj.toString())) {
                return true;
            }
        }
        return false;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public float getScore() {
        return count * score;
    }
}

class GramComparator implements Comparator<Gram> {

    @Override
    public int compare(Gram g0, Gram g1) {
        return new Float(g0.getScore()).compareTo(g1.getScore());
        // return new Integer(g0.getCount()).compareTo(g1.getCount());
    }

}
