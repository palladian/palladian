package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import ws.palladian.extraction.TokenFrequencyMap;
import ws.palladian.extraction.content.PageContentExtractorException;
import ws.palladian.extraction.content.PalladianContentExtractor;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.RetrieverCallback;

public class InformativenessAssigner {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(InformativenessAssigner.class);

    private TokenFrequencyMap tokenFrequencies = new TokenFrequencyMap();
    private Map<String, Double> normalizedTokenFrequencies = new HashMap<String, Double>();

    private InformativenessAssigner() {
        // loadFrequencyMap();
    }

    static class SingletonHolder {
        static InformativenessAssigner instance = new InformativenessAssigner();
    }

    public static InformativenessAssigner getInstance() {
        return SingletonHolder.instance;
    }

    public void loadFrequencyMap() {
        tokenFrequencies = FileHelper.deserialize("data/temp/tokenFrequencyMap.gz");
        normalizeFrequencyMap();
    }

    public void saveFrequencyMap() {
        FileHelper.serialize(tokenFrequencies, "data/temp/tokenFrequencyMap.gz");
    }

    private void normalizeFrequencyMap() {
        // normalize frequency using the token with the highest frequency as upper cap = 1

        double highestValue = 0;
        // for (Double value : tokenFrequencies.values()) {
        // if (value > highestValue) {
        // highestValue = value;
        // }
        // }
        for (Entry<String, Double> value : tokenFrequencies.entrySet()) {
            if (value.getKey().length() > 1 && value.getValue() > highestValue) {
                highestValue = value.getValue();
            }
        }

        for (Entry<String, Double> entry : tokenFrequencies.entrySet()) {
            normalizedTokenFrequencies.put(entry.getKey().toString(), entry.getValue() / highestValue);
        }
    }

    public void initTokenFrequencyMap() {

        CountMap<String> tokenFrequencyMap = CountMap.create();

        for (int i = 0; i < 2; i++) {
            // get texts from web pages
            List<String> texts = getTexts();

            // calculate token frequencies
            int totalTokens = 0;
            for (String text : texts) {
                List<String> tokens = Tokenizer.tokenize(text);

                for (String token : tokens) {
                    tokenFrequencyMap.increment(token);
                }

                totalTokens += tokens.size();
            }

            for (String token : tokenFrequencyMap.uniqueItems()) {
                int count = tokenFrequencyMap.get(token);
                tokenFrequencies.put(token, (double) count / totalTokens);
            }

            LOGGER.debug("added another set of " + texts.size() + " texts, number of tokens now "
                    + tokenFrequencies.keySet().size());

            if ((i + 1) % 10 == 0) {
                LOGGER.debug("saving frequency map (i = " + i + "...");
                saveFrequencyMap();
            }

        }
        saveFrequencyMap();

        FileHelper.writeToFile("data/temp/tfmap.txt",
                CollectionHelper.getPrint(tokenFrequencyMap.getSortedMap().entrySet()));
    }

    private List<String> getTexts() {
        StopWatch sw = new StopWatch();

        final List<String> texts = new ArrayList<String>();

        // WebSearcher<WebResult> sr = new BingSearcher();
        // List<String> urls = sr.searchUrls("and with many in of", 20);

        List<String> urls = new ArrayList<String>();
        for (int i = 0; i < 2; i++) {
            urls.add("http://en.wikipedia.org/wiki/Special:Random?a=" + Math.random());
            urls.add("http://random.yahoo.com/bin/ryl?a=" + Math.random());
            urls.add("http://www.randomwebsite.com/cgi-bin/random.pl?a=" + Math.random());
        }

        RetrieverCallback<Document> callback = new RetrieverCallback<Document>() {

            @Override
            public void onFinishRetrieval(Document document) {
                PalladianContentExtractor pse = new PalladianContentExtractor();
                try {
                    pse.setDocument(document);
                } catch (PageContentExtractorException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                texts.add(pse.getSentencesString());
            }
        };

        DocumentRetriever ud = new DocumentRetriever();
        ud.getWebDocuments(urls, callback);
        // Set<Document> documents = ud.start();

        // for (Document document : documents) {
        // pse.setDocument(document);
        // texts.add(pse.getSentencesString());
        // }

        LOGGER.info("got " + texts.size() + " texts in " + sw.getElapsedTimeString());

        return texts;
    }

    public String tagText(String text) {

        List<String> tokens = Tokenizer.tokenize(text);

        // count the occurrences of the tokens
        CountMap<String> cm = CountMap.create();
        for (String token : tokens) {
            cm.increment(token);
        }

        // normalize frequency using the token with the highest frequency as upper cap = 1
        int highestFrequency = 1;
        for (String item : cm.uniqueItems()) {
            int frequency = cm.get(item);
            if (frequency > highestFrequency) {
                highestFrequency = frequency;
            }
        }

        Map<String, Double> informativenessMap = new HashMap<String, Double>();
        for (String item : cm.uniqueItems()) {
            int count = cm.get(item);
            informativenessMap.put(item, (double) count / highestFrequency);
        }

        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            // double informativeness = Math.round(informativenessMap.get(token));

            double informativeness = getInformativeness(token);

            double hue = Math.round(255 * informativeness);
            sb.append("<token style=\"color:hsl(").append(hue).append(",100%, 35%)\">");
            sb.append(token);
            sb.append("</token>");
            sb.append(" ");
        }

        return sb.toString();
    }

    public double getInformativeness(String token) {
        double informativeness = 1;

        Double frequencyScore = normalizedTokenFrequencies.get(token);
        if (frequencyScore != null) {
            // informativeness = 1 - Math.log(10000 * frequencyScore + 1) / Math.log(10000);
            informativeness = frequencyScore;
        }

        return informativeness;
    }

    public String removeWordsWithLowInformativeness(String text, double informativenessLimit) {
        String filteredString = "";
        String[] tokens = text.split("\\s");

        for (String string : tokens) {
            double informativeness = getInformativeness(string);
            if (informativeness > informativenessLimit) {
                filteredString += string + " ";
            }
        }

        return filteredString.trim();
    }

    public void saveAsHTML(String text, String path) {
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
        sb.append("<html>");
        sb.append("<head>");
        sb.append("<title>Informativeness Tagged Text</title>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append(text);
        sb.append("</body>");
        sb.append("</html>");

        FileHelper.writeToFile(path, sb);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        StopWatch sw = new StopWatch();

        Logger.getRootLogger().setLevel(Level.INFO);
        InformativenessAssigner ia = new InformativenessAssigner();
        ia.initTokenFrequencyMap();

        System.exit(0);

        // ia.initTokenFrequencyMap();
        ia.loadFrequencyMap();

        String text = "Superman is a fictional character, a comic book superhero appearing in publications by DC Comics, widely considered to be an American cultural icon. Created by American writer Jerry Siegel and Canadian-born American artist Joe Shuster in 1932 while both were living in Cleveland, Ohio, and sold to Detective Comics, Inc. (later DC Comics) in 1938, the character first appeared in Action Comics #1 (June 1938) and subsequently appeared in various radio serials, television programs, films, newspaper strips, and video games. With the success of his adventures, Superman helped to create the superhero genre and establish its primacy within the American comic book. The character's appearance is distinctive and iconic: a blue, red and yellow costume, complete with cape, with a stylized 'S' shield on his chest. This shield is now typically used across media to symbolize the character.<br><br><br>";
        text += "Dom Cobb (Leonardo DiCaprio) and his partner Arthur (Joseph Gordon-Levitt) perform an illegal corporate espionage by entering the subconscious minds of their targets, using two-level \"dream within a dream\" strategies to \"extract\" valuable information. Each of the \"extractors\" carries a \"totem\", a personalized small object whose behavior is unpredictable to anyone except its owner, to determine if they are within another person's dream. Cobb's totem is a spinning top which perpetually spins in the dream state. Cobb struggles with memories of his dead wife, Mal (Marion Cotillard), who manifests within his dreams and tries to sabotage his efforts. Cobb is approached by the wealthy Mr. Saito (Ken Watanabe), Cobb's last extraction target, asking them to perform the act of \"inception\", planting an idea within the person's subconscious mind. Saito wishes to break up the vast energy empire of his competitor, the ailing Maurice Fischer (Pete Postlethwaite), by suggesting this idea to his son Robert Fischer (Cillian Murphy) who will inherit the empire when his father dies. Should Cobb succeed, Saito promises to use his influence to clear Cobb of the murder charges for his wife's death, allowing Cobb to re-enter the United States and reunite with his children. Cobb assembles his team: Eames (Tom Hardy), an identity forger; Yusuf (Dileep Rao), a chemist who concocts the powerful sedative needed to stabilize the layers of the shared dream; and Ariadne (Ellen Page), a young student architect tasked with designing the labyrinth of the dream landscapes. Saito insists on joining the team as an observer and to assure the job is completed. While planning the inception, Ariadne learns of the guilt Cobb struggles with from Mal's suicide and his separation from his children when he fled the country as a fugitive. The job is set into motion when Maurice Fischer dies and his son accompanies his father's body from Sydney to Los Angeles. During the flight, Cobb sedates Fischer, and the team bring him into a three-level shared dream. At each stage, the member of the team who is \"creating\" the dream remains while the other team members fall asleep within the dream to travel further down into Fischer's subconscious. The dreamers will then ride a synchronized system of \"kicks\" (a car diving off a bridge, a falling elevator, and a collapsing building) back up the levels to wake up to reality. In the first level, Yusuf's dream of a rainy city, the team successfully abducts Fischer, but the team is attacked by Fischer's militarized subconscious projections, which have been trained to hunt and kill extractors. Saito is mortally wounded during the shoot-out, but due to the strength of Yusuf's sedative, dying in the dream will send them into limbo, a deep subconscious level where they may lose their grip on reality and be trapped indefinitely. Eames takes the appearance of Fischer's godfather Peter Browning (Tom Berenger) to suggest that he reconsider his opinion of his father's will. Yusuf remains on the first level driving a van through the streets, while the remaining characters enter Arthur's dream, taking place in a corporate hotel. Cobb turns Fischer against Browning and persuades him to join the team as Arthur runs point, and they descend to the third dream level, a snowy mountain fortress dreamed by Eames, which Fischer is told represents Browning's subconscious. Yusuf's evasive driving on the first level manifests as distorted gravity effects on the second and an avalanche on the third. Saito succumbs to his wounds, and Cobb's projection of Mal sabotages the plan by shooting Fischer dead.[11] Cobb and Ariadne elect to enter limbo to find Fischer and Saito. There, Cobb confronts his projection of Mal, who tries to convince him to stay with her and his kids in limbo. Cobb refuses and confesses that he was responsible for Mal's suicide: to help her escape from limbo during a shared dream experience, he inspired in her the idea that her world wasn't real. Once she had returned to reality, she became convinced that she was still dreaming and needed to die in order to wake up. Through his confession, Cobb attains catharsis and chooses to remain in limbo to search for Saito; Eames defibrillates Fischer to bring him back up to the third-level mountain fortress, where he enters a safe room and discovers and accepts the idea to split up his father's business empire. Leaving Cobb behind, the team members escape by riding the kicks back up the levels of the dream. Cobb eventually finds an elderly Saito who has been waiting in limbo for decades in dream time (just a few hours in real time), and the two help each other to remember their arrangement. The team awakens on the flight; Saito arranges for Cobb to get through U.S. customs, and he goes home to reunite with his children. Cobb uses his spinning top to test reality but is distracted by his children before he sees the result.";

        String taggedText = ia.tagText(text);
        ia.saveAsHTML(taggedText, "data/temp/taggedInformativeness.html");

        LOGGER.info("process took " + sw.getElapsedTimeString());
    }

}
