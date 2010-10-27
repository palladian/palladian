package tud.iir.extraction.event;

import opennlp.tools.parser.Parse;

import org.apache.log4j.Logger;

import tud.iir.helper.StopWatch;

/**
 * This is the AbstractParser
 * 
 * @author Martin Wunderwald
 * 
 */
public abstract class AbstractParser {

    /**
     * Logger for this class
     */
    protected static final Logger LOGGER = Logger
            .getLogger(AbstractParser.class);

    protected final static String MODEL_PATH = "data/models/";

    protected final static String MODEL_PARSE_OPENNLP = MODEL_PATH
            + "opennlp/parser/";

    /**
     * Object holding the model
     */
    private Object model;

    /**
     * Name of the Parser
     */
    private String name;

    /**
     * the final Parse
     */
    private Object parse;

    /**
     * loads the model into the parser
     * 
     * @param configModelPath
     * @return
     */
    public abstract boolean loadModel(String configModelPath);

    /**
     * loads the default model into the parser
     * 
     * @return
     */
    public abstract boolean loadModel();

    /**
     * parses a given string and writes it into the parse object of this class
     * 
     * @param sentence
     */
    public abstract void parse(String sentence);

    public Object getModel() {
        return model;
    }

    public void setModel(Object model) {
        this.model = model;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getParse() {
        return parse;
    }

    public void setParse(Object parse) {
        this.parse = parse;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        OpenNLPParser op = new OpenNLPParser();

        op.loadModel();

        StopWatch sw = new StopWatch();
        sw.start();

        op.parse("Death toll rises after Indonesia tsunami.");
        Parse parse = (Parse) op.getParse();
        parse.show();

        sw.stop();
        LOGGER.info("time elapsed: " + sw.getElapsedTimeString());
    }

}
