package tud.iir.extraction.event;

import opennlp.tools.parser.Parse;

import org.apache.log4j.Logger;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.StopWatch;

/**
 * This is the AbstractParser.
 * 
 * @author Martin Wunderwald
 */
public abstract class AbstractParser {

    /**
     * Logger for this class.
     */
    protected static final Logger LOGGER = Logger
            .getLogger(AbstractParser.class);

    /**
     * Object holding the model.
     */
    private Object model;

    /**
     * Name of the Parser.
     */
    private String name;

    /**
     * Tagged Annotaions.
     */
    private TagAnnotations tagAnnotations;

    /**
     * loads the model into the parser.
     * 
     * @param configModelPath
     * @return Boolean
     */
    public abstract boolean loadModel(String configModelPath);

    /**
     * loads the default model into the parser.
     * 
     * @return success
     */
    public abstract boolean loadModel();

    /**
     * Parses a given string and writes it into the parse object of this class.
     * 
     * @param Sentence
     */
    public abstract void parse(String sentence);

    /**
     * @return the model
     */
    public final Object getModel() {
        return model;
    }

    /**
     * @param model
     *            the model to set
     */
    public final void setModel(Object model) {
        this.model = model;
    }

    /**
     * @return the name
     */
    public final String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * @return the tagAnnotations
     */
    public final TagAnnotations getTagAnnotations() {
        return tagAnnotations;
    }

    /**
     * @param tagAnnotations
     *            the tagAnnotations to set
     */
    public final void setTagAnnotations(TagAnnotations tagAnnotations) {
        this.tagAnnotations = tagAnnotations;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        final OpenNLPParser onlpp = new OpenNLPParser();

        onlpp.loadModel();

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        onlpp
                .parse("Government considers relocations after Indonesian tsunami");

        CollectionHelper.print(onlpp.getTagAnnotations());

        stopWatch.stop();
        LOGGER.info("time elapsed: " + stopWatch.getElapsedTimeString());
    }

    /**
     * Converts a parse tree into Annotations.
     * 
     * @param parse
     * @param tagAnnotations
     */
    public void parse2Annotations(Parse parse, TagAnnotations tagAnnotations) {
        if (parse.getChildCount() > 0) {
            for (int i = 0; i < parse.getChildCount(); i++) {
                final Parse child = parse.getChildren()[i];
                if (!child.getType().equals("TK")) {
                    tagAnnotations.add(new TagAnnotation(0, child.getType(),
                            child.getText().substring(
                                    child.getSpan().getStart(),
                                    child.getSpan().getEnd())));
                    parse2Annotations(child, tagAnnotations);
                }
            }
        }

    }

    /**
     * Prints out the parse tree.
     * 
     * @param parse
     */
    public void printParse(Parse parse) {
        if (parse.getChildCount() > 0) {
            for (int i = 0; i < parse.getChildCount(); i++) {
                final Parse child = parse.getChildren()[i];
                if (!child.getType().equals("TK")) {
                    LOGGER.info(child.getText().subSequence(
                            child.getSpan().getStart(),
                            child.getSpan().getEnd())
                            + ","
                            + child.getType()
                            + "("
                            + child.getTagSequenceProb());
                    printParse(child);
                }
            }
        }
    }

}
