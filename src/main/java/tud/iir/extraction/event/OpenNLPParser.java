package tud.iir.extraction.event;

import java.io.IOException;

import opennlp.tools.lang.english.TreebankParser;
import tud.iir.helper.StopWatch;

/**
 * OpenNLP Parser
 * 
 * @author Martin Wunderwald
 * 
 */
public class OpenNLPParser extends AbstractParser {

    private opennlp.tools.parser.Parser parser;

    public OpenNLPParser() {
        this.setName("OpenNLP Parser");
    }

    @Override
    public boolean loadModel(String configModelPath) {

        try {
            StopWatch sw = new StopWatch();
            sw.start();

            int beamSize = opennlp.tools.parser.chunking.Parser.defaultBeamSize;
            double advancePercentage = opennlp.tools.parser.chunking.Parser.defaultAdvancePercentage;

            parser = TreebankParser.getParser(configModelPath, true, false,
                    beamSize, advancePercentage);

            sw.stop();
            LOGGER.info("loaded model in " + sw.getElapsedTimeString());

            return true;
        } catch (IOException e) {
            LOGGER.error(e);
            return false;
        }
    }

    @Override
    public boolean loadModel() {
        return this.loadModel(MODEL_PARSE_OPENNLP);
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

        this.setParse((opennlp.tools.parser.Parse) ((parser != null && sentence
                .length() > 0)
        // only get first parse (that is most likely to be correct)
        ? TreebankParser.parseLine(sentence, parser, 1)[0]
                : null));

    }

}
