package tud.iir.extraction.event;

import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.extraction.Extractor;
import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.helper.StopWatch;

import com.aliasi.hmm.HmmDecoder;
import com.aliasi.tag.Tagging;

/**
 * Event Extractor
 * 
 * @author Martin Wunderwald
 */
public class EventExtractor extends Extractor { // NOPMD by Martin Wunderwald on
	// 8/6/10 4:20 PM

	/* the instance of this class */
	private static final EventExtractor INSTANCE = new EventExtractor();

	/** the logger for this class */
	private static final Logger LOGGER = Logger.getLogger(EventExtractor.class);

	
	/**
	 * @return EventExtractor
	 */
	public static EventExtractor getInstance() {
		return INSTANCE;
	}

	/**
	 * construtor of this class
	 */
	private EventExtractor() {
		// do not analyze any binary files
		addSuffixesToBlackList(Extractor.URL_BINARY_BLACKLIST);

	}

	public void startExtraction() {
		startExtraction(true);
	}

	public void startExtraction(boolean continueFromLastExtraction) {

		// reset stopped command
		setStopped(false);

	}

	@Override
	protected void saveExtractions(boolean saveExtractions) {
		// TODO Auto-generated method stub

	}

	/**
	 * extracts an event from given url
	 * 
	 * @param url
	 *            - url of a news article
	 * @return Event - The event
	 */
	public Event extractEventFromURL(String url) {
		try {

			
			PageContentExtractor pce = new PageContentExtractor();
			pce.setDocument(url);

			return new Event(pce.getResultTitle(), pce.getResultText(), url);

		} catch (PageContentExtractorException e) {

			e.printStackTrace();
			LOGGER.error("URL not found: " + url);
			return null;
		}
	}


	/**
	 * first best Chunking
	 * 
	 * @param tokenList
	 * @param decoder
	 */
	static void firstBest(List<String> tokenList, HmmDecoder decoder) {
		Tagging<String> tagging = decoder.tag(tokenList);
		LOGGER.trace("\nFIRST BEST");
		for (int i = 0; i < tagging.size(); ++i) {
			LOGGER.trace(tagging.token(i) + "_" + tagging.tag(i) + " ");
		}
		LOGGER.trace("");

	}

	@SuppressWarnings(value = { "unused" })
	private static void trySRL(){

		/*
		 * LexicalizedParser lp = new LexicalizedParser(
		 * "data/models/englishPCFG.ser.gz", new Options());
		 * lp.setOptionFlags(new String[] { "-maxLength", "80",
		 * "-retainTmpSubcategories" });
		 * 
		 * // String[] sent = lp.{ "This", "is", "an", "easy", "sentence", "."
		 * }; Tree parse = (Tree) lp.apply("I have got to go to bed.");
		 * parse.pennPrint();
		 * 
		 * 
		 * TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		 * GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
		 * GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		 * Collection tdl = gs.typedDependenciesCollapsed();
		 * System.out.println(tdl); System.out.println();
		 * 
		 * 
		 * TreePrint tp = new
		 * TreePrint("wordsAndTags,penn,typedDependenciesCollapsed");
		 * tp.printTree(parse);
		 */

		/*
		 * lp.parse("Hello my name ist Martin"); Tree tr =
		 * lp.getBestPCFGParse(); tr.toString();
		 */
		// CollectionHelper.print(Tokenizer.calculateWordNGrams("Hello how are you",2));

	}
	
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		System.setProperty("wordnet.database.dir",
				"/usr/local/WordNet-3.0/dict");

		EventExtractor eventExtractor = EventExtractor.getInstance();

		// Event e =
		// ee.extractEventFromURL("http://www.bbc.co.uk/news/world-asia-pacific-10707945");
		Event event = eventExtractor
				.extractEventFromURL("http://www.bbc.co.uk/news/world-middle-east-10851692?print=true");

		// System.out.println(e.getText());
		// Event e =
		// ee.extractEventFromURL("http://edition.cnn.com/2010/WORLD/asiapcf/07/21/china.tropical.cyclone/?hpt=T2#fbid=SCaNqNDe8oB");

		// sets textFeatures and entityFeatures

		StopWatch sw = new StopWatch();
		sw.start();
		EventFeatureExtractor.setFeatures(event);
		
		// EventFeatureExtractor.coreference(event);

	
		
		sw.stop();
		LOGGER.info("Time elapsed:" + sw.getElapsedTime());

		/*
		 * DatabaseReader reader = new FNDatabaseReader(new File(fnhome),
		 * false); reader.read(frameNet);
		 */

	}

}
