package ws.palladian.extraction.location.sources.importers;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;

public class WikipediaRedirectLineActionTest {

    @Test
    public void testSplit() {
        List<String> split = WikipediaRedirectLineAction
                .splitLine("(15301,0,'I_think,_therefore_I_am','',7,1,0,0.552158173687903,'20120916033327',44021815,29),"
                        + "(14257,0,'Haddocks\\\'_Eyes','',152,0,0,0.185402593748088,'20121001005353',510501780,7955),"
                        + "(323098,1,'J.P._\"The_Big_Bopper\"_Richardson','',0,0,0,0.186738492542,'20070519040300',131934524,0),"
                        + "(315211,0,'John_Baldwin','',0,0,0,8.5764565e-05,'20120719184930',503159646,1658),"
                        + "(592709,0,'List_of_places_with_\\\"Silicon\\\"_names','',0,0,0,0.342420995395,'20120918182249',511530628,5495)"
                        + "(586265,3,'Benjohnson','',0,0,0,0.142379215519,'20121001142141',85222394,3219),"
                        + "(586266,0,'C:\\\\','',0,1,0,0.859205707376,'20120905200902',408726926,37),"
                        + "(586267,3,'Mike_sa','',0,0,1,0.572177235769,'20060824080442',16417572,1289)");
        assertEquals(8, split.size());
        CollectionHelper.print(split);
    }

    @Test
    public void testParseRecord() {
        String record = "979,2,'Larry_Sanger/Larry\\\'s_Text/Anarchism_and_natural_law_theory','',726,0,0,0.252615454841978,'20090304125939',15899489,13773";
        String[] parse = WikipediaRedirectLineAction.parseRecord(record);
        // System.out.println(Arrays.toString(parse));
        assertEquals(11, parse.length);

        record = "592709,0,'List_of_places_with_\\\"Silicon\\\"_names','',0,0,0,0.342420995395,'20120918182249',511530628,5495";
        parse = WikipediaRedirectLineAction.parseRecord(record);
        // System.out.println(Arrays.toString(parse));
        assertEquals(11, parse.length);

        record = "11883817,1,'Ballade_in_F_minor_(Chopin)',NULL,NULL";
        parse = WikipediaRedirectLineAction.parseRecord(record);
        // System.out.println(Arrays.toString(parse));
        assertEquals(5, parse.length);
    }

}
