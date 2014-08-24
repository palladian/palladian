package ws.palladian.retrieval.wiki;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.retrieval.wiki.MediaWikiFormattingParser.ParserCallback;

public class MediaWikiFormattingParserTest {

    private static final class TestingCallback implements ParserCallback {
        private final StringBuilder builder = new StringBuilder();
        private boolean italic;
        private boolean bold;

        @Override
        public void italic() {
            italic ^= true;
            builder.append(italic ? "<i>" : "</i>");
        }

        @Override
        public void character(char ch) {
            builder.append(ch);
        }

        @Override
        public void boldItalic() {
            italic ^= true;
            bold ^= true;
            if (!italic)
                builder.append("</i>");
            if (!bold)
                builder.append("</b>");
            if (bold)
                builder.append("<b>");
            if (italic)
                builder.append("<i>");
        }

        @Override
        public void bold() {
            bold ^= true;
            builder.append(bold ? "<b>" : "</b>");
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }

    @Test
    public void testWikiFormatParser() {
        assertEquals("hello ' blah", parse("hello ' blah"));

        assertEquals("hello <i> blah", parse("hello '' blah"));

        assertEquals("hello <b> blah", parse("hello ''' blah"));

        assertEquals("hello l'<i>amour</i> l<b>ouest</b> blah", parse("hello l'''amour'' l'''ouest''' blah"));
        assertEquals("hello mon'<i>amour</i> blah", parse("hello mon'''amour'' blah"));
        assertEquals("hello '<i>amour</i> <b>blah </b>blah", parse("hello '''amour'' '''blah '''blah"));

        assertEquals("hello '<b>amour</b> now <i>italics unbalanced, but that's ok",
                parse("hello ''''amour''' now ''italics unbalanced, but that's ok"));
        assertEquals("hello '<b>amour</b> now, <b>bold unbalanced, but that's ok",
                parse("hello ''''amour''' now, '''bold unbalanced, but that's ok"));
        assertEquals("hello ''<i>amour<b> now </i></b>bold and italics unbalanced, so invoke this special case",
                parse("hello ''''amour''' now '''''bold and italics unbalanced, so invoke this special case"));

        assertEquals("hello <b><i> blah", parse("hello ''''' blah"));

        assertEquals("hello '''''<b><i> blah", parse("hello '''''''''' blah"));
        assertEquals("hello <b>bold '''''</b><i> blah", parse("hello '''bold '''''''''' blah"));
    }

    private static String parse(String text) {
        TestingCallback callback = new TestingCallback();
        MediaWikiFormattingParser.parse(text, callback);
        return callback.toString();
    }

}
