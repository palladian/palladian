package ws.palladian.external.lbj.Tagger;

import java.util.Random;



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

public class RandomLabelGenerator {
	public static final String[] labelTypes={"O","PER","ORG","LOC","MISC"};
	public static final String[] labelNamesBIO={"O","B-PER","I-PER","B-LOC","I-LOC","B-ORG","I-ORG","B-MISC","I-MISC"};
	public static final String[] labelNamesBILOU={"O","B-PER","I-PER","L-PER","U-PER","B-LOC","L-LOC","U-LOC","I-LOC","B-ORG","I-ORG","L-ORG","U-ORG","B-MISC","I-MISC","L-MISC","U-MISC"};

	public static final double noiseLevel=0.1;//this is the noise that we put into label aggregation feature for previous predictions and for level2; set this value to 0 to eliminate any noise 

	public static final int randomizationSeed=7;
	private Random rand=new Random(randomizationSeed);
	
	
	public  boolean useNoise(){
		return rand.nextDouble()<noiseLevel;
	}
	
	public  String randomLabel(){
		if(ParametersForLbjCode.taggingScheme.equalsIgnoreCase(ParametersForLbjCode.BILOU))
			return getRandomBilouLabel();
		return getRandomBioLabel();
	}

	public  String randomType(){
		int pos=(int)(rand.nextDouble()*labelTypes.length);
		if(pos>=labelTypes.length)
			pos=labelTypes.length-1;
		return labelTypes[pos];
	}

	
	private  String getRandomBioLabel(){
		int pos=(int)(rand.nextDouble()*labelNamesBIO.length);
		if(pos>=labelNamesBIO.length)
			pos=labelNamesBIO.length-1;
		return labelNamesBIO[pos];
	}
	
	
	private  String getRandomBilouLabel(){
		int pos=(int)(rand.nextDouble()*labelNamesBILOU.length);
		if(pos>=labelNamesBILOU.length)
			pos=labelNamesBILOU.length-1;
		return labelNamesBILOU[pos];
	}


}
