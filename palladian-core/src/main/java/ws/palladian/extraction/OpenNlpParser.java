package ws.palladian.extraction;

import java.io.File;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.Cache;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Annotation;

/**
 * OpenNLP Parser
 * 
 * @author Martin Wunderwald
 */
public class OpenNlpParser extends AbstractParser {

    /** Logger for this class. */
    protected static final Logger LOGGER = LoggerFactory.getLogger(OpenNlpParser.class);

    private final String corefPath;
    
    private transient opennlp.tools.parser.Parse openNLPParse;

    /** The model path. **/
    private final transient String model;
    
    public OpenNlpParser(File modelFile, File corefModelDirectory) {
        super();
        if (!corefModelDirectory.isDirectory()) {
            throw new IllegalArgumentException("\"" + corefModelDirectory.getAbsolutePath() + "\" is not a directory.");
        }
        setName("OpenNLP Parser");
        model = modelFile.getAbsolutePath();
        corefPath = corefModelDirectory.getAbsolutePath();
    }

    /**
     * Identifies coreferences in an array of full parses of sentences.
     * 
     * @param parses
     *            array of full parses of sentences
     */
    public void link(Parse[] parses) {
        int sentenceNumber = 0;
        List<Mention> document = new ArrayList<Mention>();

        TreebankLinker linker;
        try {
            if (Cache.getInstance().containsDataObject(corefPath)) {
                linker = (TreebankLinker) Cache.getInstance().getDataObject(corefPath);

            } else {

                linker = new TreebankLinker(corefPath, LinkerMode.TEST);
                Cache.getInstance().putDataObject(corefPath, linker);
            }
            DiscourseEntity[] entities = linker.getEntities(document.toArray(new Mention[document.size()]));

            CollectionHelper.print(entities);

            for (Parse parse : parses) {
                DefaultParse defaultParser = new DefaultParse(parse, sentenceNumber);
                Mention[] extents = linker.getMentionFinder().getMentions(defaultParser);

                // construct new parses for mentions which do not have
                // constituents
                for (int i = 0; i < extents.length; i++) {
                    if (extents[i].getParse() == null) {
                        opennlp.tools.parser.Parse snp = new Parse(parse.getText(), extents[i].getSpan(), "NML",
                                1.0, i);
                        parse.insert(snp);
                        extents[i].setParse(new DefaultParse(snp, sentenceNumber));
                    }
                }

                document.addAll(Arrays.asList(extents));
                sentenceNumber++;
            }

            if (!document.isEmpty()) {
                // Mention[] ms = document.toArray(new
                // Mention[document.size()]);
                // DiscourseEntity[] entities = linker.getEntities(ms);
                // TODO return results in an appropriate data structure
                LOGGER.info(document.toString());
            }

        } catch (IOException e) {
            LOGGER.error("IOException while loading the linker model from {}", corefPath, e);
        }
    }



    /**
     * Returns the full parse for a sentence as openNLP parse.
     * 
     * @param sentence
     * @return full parse
     */
    public opennlp.tools.parser.Parse[] getFullParse(String sentence) {

        opennlp.tools.parser.Parse[] parse = null;

        if ((opennlp.tools.parser.Parser) getModel() != null && sentence.length() > 0) {
            parse = ParserTool.parseLine(sentence, ((opennlp.tools.parser.Parser) getModel()), 1);

        }

        return parse;
    }

    /**
     * @return the most likely parse
     */
    public opennlp.tools.parser.Parse getParse() {
        return openNLPParse;
    }

    @Override
    public OpenNlpParser loadDefaultModel() {
        return loadModel(model);
    }

    @Override
    public OpenNlpParser loadModel(String configModelPath) {

        try {

            opennlp.tools.parser.Parser parser;

            if (Cache.getInstance().containsDataObject(configModelPath)) {
                parser = (opennlp.tools.parser.Parser) Cache.getInstance().getDataObject(configModelPath);

            } else {

                StopWatch stopWatch = new StopWatch();

                InputStream modelIn = new FileInputStream(configModelPath);
                ParserModel model = new ParserModel(modelIn);
                parser = ParserFactory.create(model);
                Cache.getInstance().putDataObject(configModelPath, parser);

                stopWatch.stop();
                LOGGER.info("Reading " + getName() + " from file " + configModelPath + " in "
                        + stopWatch.getElapsedTimeString());
            }

            setModel(parser);

        } catch (IOException e) {
            LOGGER.error("IOException while loading the parser model from {}", configModelPath, e);
        }
        return this;
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
    public final OpenNlpParser parse(String sentence) {
        return parse(sentence, 0);
    }

    /**
     * Persforms a full parse and selects the given index where 0 is the most
     * likely parse
     * 
     * @param sentence
     * @param index
     */
    public final OpenNlpParser parse(String sentence, int index) {

        openNLPParse = getFullParse(sentence)[index];

        List<Annotation> tagAnnotations = CollectionHelper.newArrayList();

        parse2Annotations(openNLPParse, tagAnnotations);

        setTagAnnotations(tagAnnotations);

        return this;
    }

    /**
     * @param parse
     *            the parse to set
     */
    public void setParse(opennlp.tools.parser.Parse parse) {
        this.openNLPParse = parse;
    }
}
