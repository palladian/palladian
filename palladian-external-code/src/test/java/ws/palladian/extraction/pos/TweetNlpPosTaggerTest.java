package ws.palladian.extraction.pos;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ws.palladian.core.Annotation;

/**
 * <p>
 * Test for ark-tweet-nlp POS tagger. The test cases which are applied here were converted from the provided sample
 * data. As the models are required for tagging, which take up a considerable amount of disk space, this test is
 * {@link Ignore}ed for now.
 * </p>
 * 
 * @author Philipp Katz
 */
@Ignore
@RunWith(Parameterized.class)
public class TweetNlpPosTaggerTest {

    private final String tweetText;
    private final String[] expectedTags;

    public TweetNlpPosTaggerTest(String tweetText, String[] expectedTags) {
        this.tweetText = tweetText;
        this.expectedTags = expectedTags;
    }

    @Parameters
    public static Collection<Object[]> testData() {
        String tweet1 = "I predict I won't win a single game I bet on. Got Cliff Lee today, so if he loses its on me RT @e_one: Texas (cont) http://tl.gd/6meogh";
        String[] tags1 = {"O", "V", "O", "V", "V", "D", "A", "N", "O", "V", "P", ",", "V", "^", "^", "N", ",", "R",
                "P", "O", "V", "L", "P", "O", "~", "@", "~", "^", ",", "~", ",", "U"};

        String tweet2 = "RT @DjBlack_Pearl: wat muhfuckaz wearin 4 the lingerie party?????";
        String[] tags2 = {"~", "@", "~", "O", "N", "V", "P", "D", "N", "N", ","};

        String tweet3 = "Wednesday 27th october 2010. 》have a nice day :)";
        String[] tags3 = {"^", "$", "^", "$", ",", "V", "D", "A", "N", "E"};

        String tweet4 = "RT @ddlovato: @joejonas oh, hey THANKS jerk!";
        String[] tags4 = {"~", "@", "~", "@", "!", ",", "!", "N", "N", ","};

        String tweet5 = "@thecamion I like monkeys, but I still hate COSTCO parking lots..";
        String[] tags5 = {"@", "O", "V", "N", ",", "&", "O", "R", "V", "^", "N", "N", ","};

        String tweet6 = "@DDaimaru I may have to get minecraft after watching videos of it";
        String[] tags6 = {"@", "O", "V", "V", "P", "V", "^", "P", "V", "N", "P", "O"};

        String tweet7 = "RT @eye_ee_duh_Esq: LMBO! This man filed an EMERGENCY Motion for Continuance on account of the Rangers game tonight! « Wow lmao";
        String[] tags7 = {"~", "@", "~", "!", ",", "D", "N", "V", "D", "N", "N", "P", "N", "P", "N", "P", "D", "^",
                "N", "N", ",", "~", "!", "!"};

        String tweet8 = "RT @musicdenver: Lady Gaga - Bad Romance http://dld.bz/n6Xv";
        String[] tags8 = {"~", "@", "~", "^", "^", ",", "A", "N", "U"};

        String tweet9 = "RT @cheriexamor: When you have a good thing, hold it, squeeze it, never let it go.";
        String[] tags9 = {"~", "@", "~", "R", "O", "V", "D", "A", "N", ",", "V", "O", ",", "V", "O", ",", "R", "V",
                "O", "V", ","};

        String tweet10 = "Texas Rangers are in the World Series!  Go Rangers!!!!!!!!! http://fb.me/D2LsXBJx";
        String[] tags10 = {"^", "^", "V", "P", "D", "^", "^", ",", "V", "^", ",", "U"};

        Object[][] data = new Object[][] { {tweet1, tags1}, {tweet2, tags2}, {tweet3, tags3}, {tweet4, tags4},
                {tweet5, tags5}, {tweet6, tags6}, {tweet7, tags7}, {tweet8, tags8}, {tweet9, tags9}, {tweet10, tags10}};
        return Arrays.asList(data);
    }

    @Test
    public void testTweetNlpPosTagger() {
        TweetNlpPosTagger posTagger = new TweetNlpPosTagger();
        List<Annotation> tags = posTagger.getAnnotations(tweetText);
        assertEquals(expectedTags.length, tags.size());
        for (int i = 0; i < expectedTags.length; i++) {
            assertEquals(expectedTags[i], tags.get(i).getTag());
        }
    }

}
