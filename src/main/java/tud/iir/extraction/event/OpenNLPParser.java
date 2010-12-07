package tud.iir.extraction.event;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.coref.DiscourseEntity;
import opennlp.tools.coref.LinkerMode;
import opennlp.tools.coref.mention.DefaultParse;
import opennlp.tools.coref.mention.Mention;
import opennlp.tools.lang.english.TreebankLinker;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.DataHolder;
import tud.iir.helper.StopWatch;

/**
 * OpenNLP Parser
 * 
 * @author Martin Wunderwald
 */
public class OpenNLPParser extends AbstractParser {

    /**
     * Logger for this class.
     */
    protected static final Logger LOGGER = Logger
            .getLogger(OpenNLPParser.class);

    private opennlp.tools.parser.Parse parse;

    private final String MODEL;

    public OpenNLPParser() {
        this.setName("OpenNLP Parser");
        PropertiesConfiguration config = null;

        try {
            config = new PropertiesConfiguration("config/models.conf");
        } catch (final ConfigurationException e) {
            LOGGER.error("could not get model path from config/models.conf, "
                    + e.getMessage());
        }

        if (config != null) {
            MODEL = config.getString("models.opennlp.en.parser");
        } else {
            MODEL = "";
        }
    }

    @Override
    public boolean loadModel(String configModelPath) {

        try {

            opennlp.tools.parser.Parser parser;

            if (DataHolder.getInstance().containsDataObject(configModelPath)) {
                parser = (opennlp.tools.parser.Parser) DataHolder.getInstance()
                        .getDataObject(configModelPath);

            } else {

                final StopWatch stopWatch = new StopWatch();
                stopWatch.start();

                final InputStream modelIn = new FileInputStream(configModelPath);
                final ParserModel model = new ParserModel(modelIn);
                parser = ParserFactory.create(model);
                DataHolder.getInstance().putDataObject(configModelPath, parser);

                stopWatch.stop();
                LOGGER.info("Reading " + this.getName() + " from file "
                        + configModelPath + " in "
                        + stopWatch.getElapsedTimeString());
            }

            setModel(parser);

            return true;
        } catch (final IOException e) {
            LOGGER.error(e);
            return false;
        }
    }

    @Override
    public boolean loadModel() {
        return this.loadModel(MODEL);
    }

    /**
     * Peforms a full parsing on a sentence of space-delimited tokens.
     * 
     * @param sentence
     *            the sentence
     * @return parse of the sentence or <code>null</code>, if the parser is not
     *         initialized or the sentence is empty
     */
    @Override
    public void parse(String sentence) {

        parse(sentence, 0);

    }

    /**
     * Persforms a full parse and selects the given index where 0 is the most
     * likely parse
     * 
     * @param sentence
     * @param index
     */
    public void parse(String sentence, int index) {

        parse = getFullParse(sentence)[index];

        final TagAnnotations tagAnnotations = new TagAnnotations();

        parse2Annotations(parse, tagAnnotations);

        setTagAnnotations(tagAnnotations);
    }

    /**
     * @return the full parse
     */
    public opennlp.tools.parser.Parse[] getFullParse(String sentence) {

        opennlp.tools.parser.Parse[] parse;

        if (((opennlp.tools.parser.Parser) getModel()) != null
                && sentence.length() > 0) {
            parse = ParserTool.parseLine(sentence,
                    ((opennlp.tools.parser.Parser) getModel()), 1);

        } else {
            parse = null;
        }
        return parse;
    }

    /**
     * @return the most likely parse
     */
    public opennlp.tools.parser.Parse getParse() {
        return parse;
    }

    /**
     * @param parse
     *            the parse to set
     */
    public void setParse(opennlp.tools.parser.Parse parse) {
        this.parse = parse;
    }

    /**
     * Identifies coreferences in an array of full parses of sentences.
     * 
     * @param parses
     *            array of full parses of sentences
     */
    public static void link(Parse[] parses) {
        int sentenceNumber = 0;
        final List<Mention> document = new ArrayList<Mention>();

        TreebankLinker linker;
        try {
            if (DataHolder.getInstance().containsDataObject(
                    "data/models/opennlp/coref/")) {
                linker = (TreebankLinker) DataHolder.getInstance()
                        .getDataObject("data/models/opennlp/coref/");

            } else {

                linker = new TreebankLinker("data/models/opennlp/coref/",
                        LinkerMode.TEST);
                DataHolder.getInstance().putDataObject(
                        "data/models/opennlp/coref/", linker);
            }
            final DiscourseEntity[] entities = linker.getEntities(document
                    .toArray(new Mention[document.size()]));

            CollectionHelper.print(entities);

            for (final Parse parse : parses) {
                final DefaultParse dp = new DefaultParse(parse, sentenceNumber);
                final Mention[] extents = linker.getMentionFinder()
                        .getMentions(dp);

                // construct new parses for mentions which do not have
                // constituents
                for (int i = 0; i < extents.length; i++) {
                    if (extents[i].getParse() == null) {
                        final opennlp.tools.parser.Parse snp = new Parse(parse
                                .getText(), extents[i].getSpan(), "NML", 1.0, i);
                        parse.insert(snp);
                        extents[i].setParse(new DefaultParse(snp,
                                sentenceNumber));
                    }
                }

                document.addAll(Arrays.asList(extents));
                sentenceNumber++;
            }

            if (document.size() > 0) {
                // Mention[] ms = document.toArray(new
                // Mention[document.size()]);
                // DiscourseEntity[] entities = linker.getEntities(ms);
                // TODO return results in an appropriate data structure
                CollectionHelper.print(document);
            }

        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
