package ws.palladian.external.lbj.Tagger;

import java.util.Hashtable;





/**
  * This project was started by Nicholas Rizzolo (rizzolo@uiuc.edu) . 
  * Most of design, development, modeling and
  * coding was done by Lev Ratinov (ratinov2@uiuc.edu).
  * For modeling details and citations, please refer
  * to the paper: 
  * External Knowledge and Non-local Features in Named Entity Recognition
  * by Lev Ratinov and Dan Roth 
  * submitted/to appear/published at NAACL 09.
  * 
 **/

public class ParametersForLbjCode {
	
	public static final String BIO="BIO";
	public static final String BILOU="BILOU";
	public static final String LbjTokenizationScheme="LbjTokenizationScheme";//use Nick's word splitter.
	public static final String DualTokenizationScheme="DualTokenizationScheme";//use spaces as token delimiters, but also use Nick's word splitter to get word parts.

	public static String pathToModelFile=null;
	public static String tokenizationScheme=null;// should be either LbjTokenizationScheme or DualTokenizationScheme
	public static String taggingScheme=null;// should be either BIO or BILOU
	
    public static RandomLabelGenerator patternLabelRandomGenerator = new RandomLabelGenerator();
    public static RandomLabelGenerator level1AggregationRandomGenerator=new RandomLabelGenerator();
	public static RandomLabelGenerator prevPredictionsLevel1RandomGenerator=new RandomLabelGenerator();
	public static RandomLabelGenerator prevPredictionsLevel2RandomGenerator=new RandomLabelGenerator();
	
	
	public static int trainingRounds=0;
	public static Hashtable<String,Boolean> featuresToUse=null;
	public static boolean forceNewSentenceOnLineBreaks=false;
}
