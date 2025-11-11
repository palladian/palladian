package ws.palladian.retrieval.ranking.services;

import org.junit.Ignore;
import org.junit.Test;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.ranking.RankingServiceException;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

@Ignore // FIXME @Philipp, please fix, started breaking 11.11.2025
public class WikipediaExternalLinksTest {
    @Test
    public void testMultipleLanguages() throws RankingServiceException {
        var langs = Arrays.asList(Language.ENGLISH, Language.GERMAN);
        var retriever = new WikipediaExternalLinks(langs);
        var urls = Arrays.asList(new String[]{ //
                "https://www.sueddeutsche.de/", //
                "https://www.sueddeutsche.de/wirtschaft/entwurf-fuer-ceta-verhandlungen-ueber-freihandelsabkommen-mit-kanada-abgeschlossen-1.2078844", //
        });
        var rankings = retriever.getRanking(urls);

        var ranking1 = rankings.get(urls.get(0));
        assertTrue(ranking1.getRankingById("wikipedia_exturl_domain_de").intValue() > 190);
        assertTrue(ranking1.getRankingById("wikipedia_exturl_domain_en").intValue() > 300);
        assertTrue(ranking1.getRankingById("wikipedia_exturl_page_de").intValue() > 190);
        assertTrue(ranking1.getRankingById("wikipedia_exturl_page_en").intValue() > 300);

        var ranking2 = rankings.get(urls.get(1));
        assertTrue(ranking2.getRankingById("wikipedia_exturl_domain_de").intValue() > 190);
        assertTrue(ranking2.getRankingById("wikipedia_exturl_domain_en").intValue() > 300);
        assertTrue(ranking2.getRankingById("wikipedia_exturl_page_de").intValue() >= 1);
        assertTrue(ranking2.getRankingById("wikipedia_exturl_page_en").intValue() >= 1);
    }

}
