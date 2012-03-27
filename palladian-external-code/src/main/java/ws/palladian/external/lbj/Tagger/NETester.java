package ws.palladian.external.lbj.Tagger;


import java.util.Vector;

import lbj.NELabel;
import lbj.NETaggerLevel1;
import lbj.NETaggerLevel2;

import LBJ2.classify.Classifier;
import LBJ2.classify.TestDiscrete;
import LBJ2.learn.SparseNetworkLearner;
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


public class NETester
{

	public static void test(String testFilename,String fileFormat)
	{
		Vector<LinkedVector> testData=null;
		if(fileFormat.equals("-c")){
			Reuters2003Parser parser = new Reuters2003Parser(testFilename);
			testData=parser.readAndAnnotate();
		}
		else{
			if(fileFormat.equals("-r")){
				testData=BracketFileManager.readAndAnnotate(testFilename);
			}
			else{
				System.out.println("Fatal error: unrecognized file format: "+fileFormat);
				System.exit(0);
			}
		}

		NETaggerLevel1 taggerLevel1=new NETaggerLevel1();
		taggerLevel1=(NETaggerLevel1)Classifier.binaryRead(Parameters.pathToModelFile+".level1");
		NETaggerLevel2 taggerLevel2=new NETaggerLevel2();
		taggerLevel2=(NETaggerLevel2)Classifier.binaryRead(Parameters.pathToModelFile+".level2");
		printTestResults(testData,taggerLevel1,taggerLevel2);
	}

	public static TestDiscrete[] printTestResults(Vector<LinkedVector> data,SparseNetworkLearner taggerLevel1, SparseNetworkLearner taggerLevel2)
	{	 

		NELabel labeler = new NELabel();



		TestDiscrete resultsPhraseLevel1 = new TestDiscrete();
		resultsPhraseLevel1.addNull("O");
		TestDiscrete resultsTokenLevel1 = new TestDiscrete();
		resultsTokenLevel1.addNull("O");

		TestDiscrete resultsPhraseLevel2 = new TestDiscrete();
		resultsPhraseLevel2.addNull("O");
		TestDiscrete resultsTokenLevel2 = new TestDiscrete();
		resultsTokenLevel2.addNull("O");		

		annotateBothLevels(data,taggerLevel1,taggerLevel2);

		for (int k=0;k<data.size();k++)
		{
			LinkedVector vector=data.elementAt(k);
			int N = vector.size();	
			String[] predictionsLevel1 = new String[N],predictionsLevel2 = new String[N], labels = new String[N];

			for (int i = 0; i < N; ++i)
			{
				predictionsLevel1[i] =  ((NEWord)vector.get(i)).neTypeLevel1;	 
				predictionsLevel2[i] =  ((NEWord)vector.get(i)).neTypeLevel2;	 
				labels[i] = labeler.discreteValue(vector.get(i));
				String pLevel1=predictionsLevel1[i];
				String pLevel2=predictionsLevel2[i];
				if(pLevel1.indexOf('-')>-1) {
                    pLevel1=pLevel1.substring(2);
                }
				if(pLevel2.indexOf('-')>-1) {
                    pLevel2=pLevel2.substring(2);
                }
				String l=labels[i];
				if(l.indexOf('-')>-1) {
                    l=l.substring(2);
                }
				resultsTokenLevel1.reportPrediction(pLevel1, l);
				resultsTokenLevel2.reportPrediction(pLevel2, l);
			}


			//getting phrase level accuracy level1
			for (int i = 0; i < N; ++i)
			{
				String p = "O", l = "O";
				int pEnd = -1, lEnd = -1;

				if (predictionsLevel1[i].startsWith("B-")
						|| predictionsLevel1[i].startsWith("I-")
						&& (i == 0
								|| !predictionsLevel1[i - 1]
								                      .endsWith(predictionsLevel1[i].substring(2))))
				{
					p = predictionsLevel1[i].substring(2);
					pEnd = i;
					while (pEnd + 1 < N && predictionsLevel1[pEnd + 1].equals("I-" + p)) {
                        ++pEnd;
                    }
				}

				if (labels[i].startsWith("B-"))
				{
					l = labels[i].substring(2);
					lEnd = i;
					while (lEnd + 1 < N && labels[lEnd + 1].equals("I-" + l)) {
                        ++lEnd;
                    }
				}

				if (!p.equals("O") || !l.equals("O"))
				{
					if (pEnd == lEnd) {
                        resultsPhraseLevel1.reportPrediction(p, l);
                    } else
					{
						if (!p.equals("O")) {
                            resultsPhraseLevel1.reportPrediction(p, "O");
                        }
						if (!l.equals("O")) {
                            resultsPhraseLevel1.reportPrediction("O", l);
                        }
					}
				}
			}

			//getting phrase level accuracy level2
			for (int i = 0; i < N; ++i)
			{
				String p = "O", l = "O";
				int pEnd = -1, lEnd = -1;

				if (predictionsLevel2[i].startsWith("B-")
						|| predictionsLevel2[i].startsWith("I-")
						&& (i == 0
								|| !predictionsLevel2[i - 1]
								                      .endsWith(predictionsLevel2[i].substring(2))))
				{
					p = predictionsLevel2[i].substring(2);
					pEnd = i;
					while (pEnd + 1 < N && predictionsLevel2[pEnd + 1].equals("I-" + p)) {
                        ++pEnd;
                    }
				}

				if (labels[i].startsWith("B-"))
				{
					l = labels[i].substring(2);
					lEnd = i;
					while (lEnd + 1 < N && labels[lEnd + 1].equals("I-" + l)) {
                        ++lEnd;
                    }
				}

				if (!p.equals("O") || !l.equals("O"))
				{
					if (pEnd == lEnd) {
                        resultsPhraseLevel2.reportPrediction(p, l);
                    } else
					{
						if (!p.equals("O")) {
                            resultsPhraseLevel2.reportPrediction(p, "O");
                        }
						if (!l.equals("O")) {
                            resultsPhraseLevel2.reportPrediction("O", l);
                        }
					}
				}
			}
		}

		System.out.println("Phrase-level Acc Level1:");
		resultsPhraseLevel1.printPerformance(System.out);
		System.out.println("Token-level Acc Level1:");
		resultsTokenLevel1.printPerformance(System.out);

		System.out.println("Phrase-level Acc Level2:");
		resultsPhraseLevel2.printPerformance(System.out);
		System.out.println("Token-level Acc Level2:");
		resultsTokenLevel2.printPerformance(System.out);

		TestDiscrete[] res={resultsPhraseLevel1,resultsPhraseLevel2};
		return res;
	}  

	public static void clearPredictions(Vector<LinkedVector> data){
		for (int k=0;k<data.size();k++){
			for(int i=0;i<data.elementAt(k).size();i++){
				((NEWord)data.elementAt(k).get(i)).neTypeLevel1=null;
				((NEWord)data.elementAt(k).get(i)).neTypeLevel2=null;
			}
		}		
	}

	/*
	 * 
	 *  use taggerLevel2=null if you want to use only one level of inference
	 * 
	 */
	public static void annotateBothLevels(Vector<LinkedVector> data,SparseNetworkLearner taggerLevel1,SparseNetworkLearner taggerLevel2) {
		clearPredictions(data);
		NETaggerLevel1.isTraining=false;
		NETaggerLevel2.isTraining=false;
		for (int k=0;k<data.size();k++){
			for (int i = 0; i < data.elementAt(k).size() ; ++i)
			{
				NEWord w=(NEWord)data.elementAt(k).get(i);
				w.neTypeLevel1 =  taggerLevel1.discreteValue(w);	 
			}
		}			    

		if(taggerLevel2!=null&&(Parameters.featuresToUse.containsKey("PatternFeatures")||Parameters.featuresToUse.containsKey("PredictionsLevel1"))){
			//annotate with patterns
			if(Parameters.featuresToUse.containsKey("PatternFeatures")) {
                PatternExtractor.annotate(data, false, false);
            }
			if(Parameters.featuresToUse.containsKey("PredictionsLevel1")){
				GlobalFeatures.aggregateLevel1Predictions(data);
				GlobalFeatures.aggregateEntityLevelPredictions(data);
			}
			for (int k=0;k<data.size();k++) {
                for (int i = 0; i < data.elementAt(k).size(); ++i){
					((NEWord)data.elementAt(k).get(i)).neTypeLevel2=taggerLevel2.discreteValue(data.elementAt(k).get(i));
				}
            }
		}
		else
		{
			for (int k=0;k<data.size();k++) {
                for (int i = 0; i < data.elementAt(k).size(); i++){
					NEWord w=(NEWord)data.elementAt(k).get(i);
					w.neTypeLevel2=w.neTypeLevel1;
				}
            }
		}

		if(Parameters.taggingScheme.equalsIgnoreCase(Parameters.BILOU)){
			Bio2Bilou.bilou2BioPredictionsLevel1(data);
			Bio2Bilou.Bilou2BioPredictionsLevel2(data);
		}
	}
}

