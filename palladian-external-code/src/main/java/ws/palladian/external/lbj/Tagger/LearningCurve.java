package ws.palladian.external.lbj.Tagger;

import java.util.Vector;

import lbj.NETaggerLevel1;
import lbj.NETaggerLevel2;

import LBJ2.classify.TestDiscrete;
import LBJ2.parse.LinkedVector;


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

public class LearningCurve
{


    public static void getLearningCurve(String trainFilename, String testFilename)
	{	  
		NETaggerLevel1 tagger1 = new NETaggerLevel1();
		tagger1.forget();
		NETaggerLevel2 tagger2 = new NETaggerLevel2();
		tagger2.forget();
		double bestF1Level1 = -1;
		int bestRoundLevel1 = 0;
		double bestF1Level2 = -1;
		int bestRoundLevel2 = 0;

		Vector<LinkedVector> trainData=null;
		Vector<LinkedVector> testData=null;

        SimpleColumnParser parser = new SimpleColumnParser(trainFilename);
        trainData = parser.readAndAnnotate();
        parser = new SimpleColumnParser(testFilename);
        testData = parser.readAndAnnotate();
		
		for (int i = 0; i < Parameters.trainingRounds; ++i)
		{
			System.out.println("Learning round "+i);
			NETester.clearPredictions(trainData);
			NETaggerLevel1.isTraining=true;
			NETaggerLevel2.isTraining=true;

            if (Parameters.taggingScheme.equalsIgnoreCase(Parameters.BILOU)) {
                Bio2Bilou.Bio2BilouLabels(trainData);
            }
				
			if (Parameters.featuresToUse.containsKey("PatternFeatures")) {
                PatternExtractor.annotate(trainData, true, false);
            }
			if(Parameters.featuresToUse.containsKey("PredictionsLevel1")){
				GlobalFeatures.aggregateLevel1Predictions(trainData);
				GlobalFeatures.aggregateEntityLevelPredictions(trainData);
				//GlobalFeatures.displayLevel1AggregationData(trainData);
			}
            for (int k = 0; k < trainData.size(); k++) {
                for (int j = 0; j < trainData.elementAt(k).size(); j++) {
					tagger1.learn(trainData.elementAt(k).get(j));
                    if (Parameters.featuresToUse.containsKey("PatternFeatures")
                            || Parameters.featuresToUse.containsKey("PredictionsLevel1")) {
                        tagger2.learn(trainData.elementAt(k).get(j));
                    }
				}
            }
			//after we're done training, go back to BIO. This will not cause
			//problems when testing because all the "pattern extraction" and
			//"prediction aggregation" will use the predicted tags and not the
			//gold labels.
            if (Parameters.taggingScheme.equalsIgnoreCase(Parameters.BILOU)) {
                Bio2Bilou.Bilou2BioLabels(trainData);
            }

			
			System.out.println("Testing round "+i);
			TestDiscrete[] results = NETester.printTestResults(testData,tagger1,tagger2);
			double f1Level1 = results[0].getOverallStats()[2];
			System.out.println("Level1: "+(i + 1) + "\t" + f1Level1);
			double f1Level2 = results[1].getOverallStats()[2];
			System.out.println("Level2: "+(i + 1) + "\t" + f1Level2);

			if (f1Level1 > bestF1Level1)
			{
				bestF1Level1 = f1Level1;
				bestRoundLevel1 = i + 1;
				NETaggerLevel1.getInstance().binaryWrite(Parameters.pathToModelFile+".level1");
			}
			if (f1Level2 > bestF1Level2)
			{
				bestF1Level2 = f1Level2;
				bestRoundLevel2 = i + 1;
				NETaggerLevel2.getInstance().binaryWrite(Parameters.pathToModelFile+".level2");
			}

            if ((i + 1) % 5 == 0) {
                System.err.println(i + 1 + " rounds.  Best so far: Level1(" + bestRoundLevel1 + ")=" + bestF1Level1
                        + " Level2(" + bestRoundLevel2 + ") " + bestF1Level2);
            }
		}
	}
}