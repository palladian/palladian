package tud.iir.extraction.event;

import opennlp.tools.parser.AbstractBottomUpParser;
import tud.iir.helper.DataHolder;
import tud.iir.helper.StopWatch;

/**
 * OpenNLP Parser
 * 
 * @author Martin Wunderwald
 */
public class OpenNLPParser extends AbstractParser {

    private opennlp.tools.parser.Parse parse;

    public OpenNLPParser() {
        setName("OpenNLP Parser");
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

                final int beamSize = AbstractBottomUpParser.defaultBeamSize;
                final double advancePercentage = AbstractBottomUpParser.defaultAdvancePercentage;

                parser = null;// FIXME: TreebankParser.getParser(configModelPath, true, false,beamSize,
                              // advancePercentage);
                DataHolder.getInstance().putDataObject(configModelPath, parser);

                stopWatch.stop();
                LOGGER.info("Reading " + getName() + " from file "
                        + configModelPath + " in "
                        + stopWatch.getElapsedTimeString());
            }

            setModel(parser);

            return true;
        } catch (Exception e) {
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

        opennlp.tools.parser.Parse[] parse = null;

        if ((opennlp.tools.parser.Parser) getModel() != null
                && sentence.length() > 0) {
            // FIXME:parse = TreebankParser.parseLine(sentence,((opennlp.tools.parser.Parser) getModel()), 1);

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

}
