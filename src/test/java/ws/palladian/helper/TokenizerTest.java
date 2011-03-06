package ws.palladian.helper;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import ws.palladian.helper.nlp.Tokenizer;

/**
 * Test cases for the Tokenizer class.
 * 
 * @author David Urbansky
 */
public class TokenizerTest extends TestCase {

    public TokenizerTest(String name) {
        super(name);
    }

    @Test
    public void testCalculateCharNGrams() {
        assertEquals(16, Tokenizer.calculateCharNGrams("allthelilacsinohio", 3).size());
        assertEquals(3, Tokenizer.calculateCharNGrams("hiatt", 3).size());
        assertEquals(81, Tokenizer.calculateAllCharNGrams("allthelilacsinohio", 3, 8).size());
        assertEquals(1, Tokenizer.calculateCharNGrams("hiatt", 5).size());
        assertEquals(0, Tokenizer.calculateCharNGrams("hiatt", 6).size());
    }

    @Test
    public void testCalculateWordNgrams() {
        assertEquals(4, Tokenizer.calculateWordNGrams("all the lilacs in ohio", 2).size());
        assertEquals(3, Tokenizer.calculateWordNGrams("all the lilacs in ohio", 3).size());
        assertEquals(2, Tokenizer.calculateWordNGrams("all the lilacs in ohio", 4).size());
        assertEquals(1, Tokenizer.calculateWordNGrams("all the lilacs in ohio", 5).size());
        assertEquals(0, Tokenizer.calculateWordNGrams("all the lilacs in ohio", 6).size());
    }

    @Test
    public void testTokenize() {
        List<String> tokens = Tokenizer.tokenize("That poster costs $22.40. twenty-one.");
        // CollectionHelper.print(tokens);
        assertEquals(7, tokens.size());

        tokens = Tokenizer.tokenize("Mr. <MUSICIAN>John Hiatt</MUSICIAN> is awesome.");
        assertEquals(9, tokens.size());

        tokens = Tokenizer.tokenize("Mr. '<MUSICIAN>John Hiatt</MUSICIAN>' is awesome.");
        assertEquals(11, tokens.size());

        tokens = Tokenizer.tokenize("Mr. ^<MUSICIAN>John Hiatt</MUSICIAN>) is awesome!!!");
        // CollectionHelper.print(tokens);
        assertEquals(11, tokens.size());

        tokens = Tokenizer.tokenize("asp.net is very web 2.0. isn't it? web2.0, .net");
        // CollectionHelper.print(tokens);
        assertEquals(14, tokens.size());

        tokens = Tokenizer.tokenize("40,000 residents");
        assertEquals(2, tokens.size());

        tokens = Tokenizer
                .tokenize("The United States of America are often called the USA, the U.S.A., or simply the U.S. The U.N. has its headquarter in N.Y.C. on the east coast.");
        // CollectionHelper.print(tokens);
        assertEquals(30, tokens.size());

    }

    @Test
    public void testGetSentence() {

        assertEquals(Tokenizer.getPhraseToEndOfSentence("Although, many of them (30.2%) are good. As long as"),
        "Although, many of them (30.2%) are good.");
        assertEquals(Tokenizer.getPhraseFromBeginningOfSentence("...now. Although, many of them (30.2%) are good"),
        "Although, many of them (30.2%) are good");
        assertEquals(Tokenizer.getSentence(
                "...now. Although, have 234 ft.lbs. of torque ... many of them (30.2%) are good. As long as", 10),
        "Although, have 234 ft.lbs. of torque ... many of them (30.2%) are good.");
        assertEquals(Tokenizer.getSentence(
                "...now. Although, have 234 ft.lbs. of torque ... many of them (30.2%) are good. As long as", 40),
        "Although, have 234 ft.lbs. of torque ... many of them (30.2%) are good.");
        assertEquals(Tokenizer.getSentence("...now. Although, many of them (30.2%) are good. As long as", 10),
        "Although, many of them (30.2%) are good.");
        assertEquals(Tokenizer.getSentence("...now. Although, many of them (30.2%) are good. As long as", 40),
        "Although, many of them (30.2%) are good.");
        assertEquals(Tokenizer.getSentence("...now. Although, many of them (30.2%) are good.As long as", 40),
        "Although, many of them (30.2%) are good.");
        assertEquals(Tokenizer.getSentence("What is the largest city in usa, (30.2%) in population. - Yahoo! Answers,",
                12), "What is the largest city in usa, (30.2%) in population. - Yahoo!");
        assertEquals(Tokenizer.getSentence("What is the largest city in usa, (30.2%) in population? - Yahoo! Answers,",
                12), "What is the largest city in usa, (30.2%) in population?");
        assertEquals(Tokenizer.getSentence(
                "...now. Although, has 234,423,234 sq.miles area many of them (30.2%) are good. As long as", 10),
        "Although, has 234,423,234 sq.miles area many of them (30.2%) are good.");
    }

    @Test
    public void testGetSentences() {

        // this is the LingPipe example (last sentence ends with "!" to make it more difficult:
        // http://alias-i.com/lingpipe/demos/tutorial/sentences/read-me.html
        String inputText = "The induction of immediate-early (IE) response genes, such as egr-1, c-fos, and c-jun, occurs rapidly after the activation of T lymphocytes. The process of activation involves calcium mobilization, activation of protein kinase C (PKC), and phosphorylation of tyrosine kinases. p21(ras), a guanine nucleotide binding factor, mediates T-cell signal transduction through PKC-dependent and PKC-independent pathways. The involvement of p21(ras) in the regulation of calcium-dependent signals has been suggested through analysis of its role in the activation of NF-AT. We have investigated the inductions of the IE genes in response to calcium signals in Jurkat cells (in the presence of activated p21(ras)) and their correlated consequences!";
        List<String> sentences = Tokenizer.getSentences(inputText);

        // System.out.println(DigestUtils.md5Hex("text"));

        assertEquals(5, sentences.size());
        assertEquals(
                "The induction of immediate-early (IE) response genes, such as egr-1, c-fos, and c-jun, occurs rapidly after the activation of T lymphocytes.",
                sentences.get(0));
        assertEquals(
                "We have investigated the inductions of the IE genes in response to calcium signals in Jurkat cells (in the presence of activated p21(ras)) and their correlated consequences!",
                sentences.get(4));

        inputText = "This Paragraph is more difficult...or isn't it?hm, well (!), I don't know!!! I really don't.";
        sentences = Tokenizer.getSentences(inputText);
        // CollectionHelper.print(sentences);

        assertEquals(3, sentences.size());
        assertEquals("This Paragraph is more difficult...or isn't it?", sentences.get(0));
        assertEquals("hm, well (!), I don't know!!!", sentences.get(1));
        assertEquals("I really don't.", sentences.get(2));
        // CollectionHelper.print(sentences);

        inputText = "ActionScript 3.0 (or Flex 3.0.1) supports flash.stage.MovieClip(), cool he?";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(1, sentences.size());
        assertEquals("ActionScript 3.0 (or Flex 3.0.1) supports flash.stage.MovieClip(), cool he?", sentences.get(0));

        inputText = "Mr. X is sometimes called Mr. X Jr., too!";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(1, sentences.size());
        assertEquals("Mr. X is sometimes called Mr. X Jr., too!", sentences.get(0));

        // those patterns were causing an Exception which is fixed now : java.lang.StringIndexOutOfBoundsException
        // at tud.iir.helper.StringHelper.getSubstringBetween(StringHelper.java:984)
        inputText = "  Dont repeat yourself. Dont repeat yourself.";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(2, sentences.size());
        assertEquals("Dont repeat yourself.", sentences.get(0));
        assertEquals("Dont repeat yourself.", sentences.get(1));

        inputText = "Dies    ist  ein toller Test. Hallo Tag wird toll";
        sentences = Tokenizer.getSentences(inputText);
        assertEquals(2, sentences.size());
        assertEquals("Dies    ist  ein toller Test.", sentences.get(0));
        assertEquals("Hallo Tag wird toll", sentences.get(1));
        System.out.println(sentences);

    }
}