package ws.palladian.extraction.token;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;

/**
 * <p>
 * Test cases for the Tokenizer class.
 * </p>
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public class TokenizerTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testComputeStartingWordNGrams() {
        // CollectionHelper.print(Tokenizer.computeSplits("my broccoli rabe “spaghetti,” tomato & chicken", 1, 8,500));

        collector.checkThat(Tokenizer.computeStartingWordNGrams("This is a test.", 1, 3), hasItem("This"));
        collector.checkThat(Tokenizer.computeStartingWordNGrams("This is a test.", 1, 3), hasItem("This is"));
        collector.checkThat(Tokenizer.computeStartingWordNGrams("This is a test.", 1, 3), hasItem("This is a"));
        collector.checkThat(Tokenizer.computeStartingWordNGrams("This is a test.", 1, 3).size(), is(3));

        collector.checkThat(Tokenizer.computeStartingWordNGrams("my broccoli rabe “spaghetti,” tomato & chicken", 1, 3).size(), is(3));
    }

    @Test
    public void testComputeSplits() {
        // CollectionHelper.print(Tokenizer.computeSplits("This is a test.", 1, 8,500));
        // CollectionHelper.print(Tokenizer.computeSplits("my broccoli rabe “spaghetti,” tomato & chicken", 1, 8,500));
        // CollectionHelper.print(Tokenizer.computeSplits("This is a 3,5 test with,another comma.", 1, 8,500));

        collector.checkThat(Tokenizer.computeSplits("my broccoli rabe “spaghetti,” tomato & chicken", 1, 8, 1000).size(), is(64));
        collector.checkThat(Tokenizer.computeSplits("This is a 3,5 test with,another comma.", 1, 4, 1000).size(), is(56));
        Set<List<String>> lists = Tokenizer.computeSplits("American Flatbread Handmade Thin & Crispy Pizza Vegan Harvest", 1, 7, 2000);
        collector.checkThat(lists, hasItem(Arrays.asList("American Flatbread", "Handmade Thin & Crispy", "Pizza", "Vegan", "Harvest")));
    }

    public void testCalculateCharEdgeNGrams() {
        collector.checkThat(Tokenizer.calculateCharEdgeNGrams("allthelilacsinohio", 3, false), hasItems("all", "hio"));
        collector.checkThat(Tokenizer.calculateCharEdgeNGrams("allthelilacsinohio", 3, false).size(), is(2));

        collector.checkThat(Tokenizer.calculateAllCharEdgeNGrams("allthelilacsinohio", 1, 4), hasItems("all", "ohio", "io", "al"));
        collector.checkThat(Tokenizer.calculateAllCharEdgeNGrams("allthelilacsinohio", 1, 4).size(), is(8));
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
        assertEquals(1, Tokenizer.calculateAllWordNGrams("a++", 1, 10).size());
        assertEquals(4, Tokenizer.calculateWordNGrams("Mr. A. Anderson.", 1).size());
        assertEquals(4, Tokenizer.calculateWordNGrams("all the lilacs in ohio", 2).size());
        assertEquals(3, Tokenizer.calculateWordNGrams("all the lilacs in ohio", 3).size());
        assertEquals(2, Tokenizer.calculateWordNGrams("all the lilacs in ohio", 4).size());
        assertEquals(1, Tokenizer.calculateWordNGrams("all the lilacs in ohio", 5).size());
        assertEquals(0, Tokenizer.calculateWordNGrams("all the lilacs in ohio", 6).size());
        assertEquals(4, Tokenizer.calculateWordNGrams("all the lilacs\n\n\nin   ohio", 2).size());
    }

    @Test
    public void testTokenize() {
        List<String> tokens = Tokenizer.tokenize("That poster costs $22.40. twenty-one.");
        // CollectionHelper.print(tokens);
        assertEquals(7, tokens.size());

        tokens = Tokenizer.tokenize("Mr. <MUSICIAN>John Hiatt</MUSICIAN> is awesome.");
        assertEquals(8, tokens.size());

        tokens = Tokenizer.tokenize("Mr. '<MUSICIAN>John Hiatt</MUSICIAN>' is awesome.");
        assertEquals(10, tokens.size());

        tokens = Tokenizer.tokenize("Mr. ^<MUSICIAN>John Hiatt</MUSICIAN>) is awesome!!!");
        // CollectionHelper.print(tokens);
        assertEquals(10, tokens.size());

        tokens = Tokenizer.tokenize("asp.net is very web 2.0. isn't it? web2.0, .net");
        // CollectionHelper.print(tokens);
        assertEquals(14, tokens.size());

        tokens = Tokenizer.tokenize("40,000 residents");
        assertEquals(2, tokens.size());

        tokens = Tokenizer.tokenize(
                "The United States of America are often called the USA, the U.S.A., or simply the U.S. The U.N. has its headquarter in N.Y.C. on the east coast.");
        // CollectionHelper.print(tokens);
        assertEquals(30, tokens.size());

        // tokens = Tokenizer.tokenize("Text with some link: http://www.example.com.");
        // TODO would be nice to keep URLs.

        // gives StackOverflowError caused by RegEx
        // Tokenizer
        // .tokenize("abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.abc.");

    }

    @Test
    public void testGetSentence() {
        assertEquals(Tokenizer.getPhraseToEndOfSentence("Although, many of them (30.2%) are good. As long as"), "Although, many of them (30.2%) are good.");
        assertEquals(Tokenizer.getPhraseFromBeginningOfSentence("...now. Although, many of them (30.2%) are good"), "Although, many of them (30.2%) are good");
        // assertEquals(Tokenizer.getSentence(
        // "...now. Although, have 234 ft.lbs. of torque ... many of them (30.2%) are good. As long as", 10),
        // "Although, have 234 ft.lbs. of torque ... many of them (30.2%) are good.");
        // assertEquals(Tokenizer.getSentence(
        // "...now. Although, have 234 ft.lbs. of torque ... many of them (30.2%) are good. As long as", 40),
        // "Although, have 234 ft.lbs. of torque ... many of them (30.2%) are good.");
        assertEquals(Tokenizer.getSentence("...now. Although, many of them (30.2%) are good. As long as", 10), "Although, many of them (30.2%) are good.");
        assertEquals(Tokenizer.getSentence("...now. Although, many of them (30.2%) are good. As long as", 40), "Although, many of them (30.2%) are good.");
        // assertEquals(Tokenizer.getSentence("...now. Although, many of them (30.2%) are good.As long as", 40),
        // "Although, many of them (30.2%) are good.");
        assertEquals(Tokenizer.getSentence("What is the largest city in usa, (30.2%) in population. Yahoo! Answers,", 12),
                "What is the largest city in usa, (30.2%) in population.");
        assertEquals(Tokenizer.getSentence("What is the largest city in usa, (30.2%) in population? - Yahoo! Answers,", 12),
                "What is the largest city in usa, (30.2%) in population?");
        assertEquals(Tokenizer.getSentence("...now. Although, has 234,423,234 sq.miles area many of them (30.2%) are good. As long as", 10),
                "Although, has 234,423,234 sq.miles area many of them (30.2%) are good.");
    }
}
