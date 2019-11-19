package ws.palladian.retrieval.search.videos;

import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class YouTubeSearcherTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    /** This uses unofficial ways and is very likely to break */
    @Ignore
    @Test
    public void getCaptions() {
        YouTubeSearcher youTubeSearcher = new YouTubeSearcher("notneedednow");
        String captions = youTubeSearcher.getCaptions("BCrQD7rjEro", true);
        collector.checkThat(captions, Matchers.containsString("verschrauben das alles mit der"));

        captions = youTubeSearcher.getCaptions("BCrQD7rjEro", false);
        collector.checkThat(captions, Matchers.containsString("verschrauben das alles mit der"));
    }
}