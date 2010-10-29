package tud.iir.extraction.event;

import java.io.IOException;

import opennlp.tools.lang.english.TreebankParser;
import opennlp.tools.parser.AbstractBottomUpParser;
import tud.iir.helper.StopWatch;

/**
 * OpenNLP Parser
 * 
 * @author Martin Wunderwald
 */
public class OpenNLPParser extends AbstractParser {

    public OpenNLPParser() {
        this.setName("OpenNLP Parser");
    }

    @Override
    public boolean loadModel(String configModelPath) {

        try {
            final StopWatch sw = new StopWatch();
            sw.start();

            final int beamSize = AbstractBottomUpParser.defaultBeamSize;
            final double advancePercentage = AbstractBottomUpParser.defaultAdvancePercentage;

            setModel(TreebankParser.getParser(configModelPath, true, false,
                    beamSize, advancePercentage));

            sw.stop();
            LOGGER.info("loaded model in " + sw.getElapsedTimeString());

            return true;
        } catch (final IOException e) {
            LOGGER.error(e);
            return false;
        }
    }

    @Override
    public boolean loadModel() {
        return this.loadModel(MODEL_PARSE_ONLP);
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

        final opennlp.tools.parser.Parse parse = ((((opennlp.tools.parser.Parser) getModel()) != null && sentence
                .length() > 0)
        // only get first parse (that is most likely to be correct)
        ? TreebankParser.parseLine(sentence,
                ((opennlp.tools.parser.Parser) getModel()), 1)[0]
                : null);

        final TagAnnotations tagAnnotations = new TagAnnotations();

        parse2Annotations(parse, tagAnnotations);

        setTagAnnotations(tagAnnotations);

    }

}
