package ws.palladian.external.lbj.Tagger;

import java.util.Hashtable;
import java.util.Vector;

import lbj.NETaggerLevel1;
import lbj.NETaggerLevel2;

import ws.palladian.external.lbj.StringStatisticsUtils.OccurrenceCounter;
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

public class GlobalFeatures {

	
	public static void displayLevel1AggregationData(Vector<LinkedVector> data){
		for(int i=0;i<data.size();i++) {
            for(int j=0;j<data.elementAt(i).size();j++){
				NEWord w=(NEWord)data.elementAt(i).get(j);
				System.out.println("Word: "+w.form);
				System.out.println("\t entity: "+w.entity);
				System.out.println("\t enity type: "+w.entityType);
				System.out.println("\t token in entity supertype: ");
				String[] arr=w.mostFrequentLevel1TokenInEntityType.getTokens();
				for(int k=0;k<arr.length;k++) {
                    System.out.println("\t\t"+arr[k]+":"+ w.mostFrequentLevel1TokenInEntityType.getCount(arr[k])/w.mostFrequentLevel1TokenInEntityType.totalTokens);
                }
				System.out.println("\t supertype: ");
				arr=w.mostFrequentLevel1SuperEntityType.getTokens();
				for(int k=0;k<arr.length;k++) {
                    System.out.println("\t\t"+arr[k]+":"+ w.mostFrequentLevel1SuperEntityType.getCount(arr[k])/w.mostFrequentLevel1SuperEntityType.totalTokens);
                }
				System.out.println("\t exact entity type: ");
				arr=w.mostFrequentLevel1ExactEntityType.getTokens();
				for(int k=0;k<arr.length;k++) {
                    System.out.println("\t\t"+arr[k]+":"+ w.mostFrequentLevel1ExactEntityType.getCount(arr[k])/w.mostFrequentLevel1ExactEntityType.totalTokens);
                }
				System.out.println("\t token maj: ");
				arr=w.mostFrequentLevel1Prediction.getTokens();
				for(int k=0;k<arr.length;k++) {
                    System.out.println("\t\t"+arr[k]+":"+ w.mostFrequentLevel1Prediction.getCount(arr[k])/w.mostFrequentLevel1Prediction.totalTokens);
                }
				System.out.println("\t token maj type: ");
				arr=w.mostFrequentLevel1PredictionType.getTokens();
				for(int k=0;k<arr.length;k++) {
                    System.out.println("\t\t"+arr[k]+":"+ w.mostFrequentLevel1PredictionType.getCount(arr[k])/w.mostFrequentLevel1PredictionType.totalTokens);
                }
				System.out.println("\t token maj not-O: ");
				arr=w.mostFrequentLevel1NotOutsidePrediction.getTokens();
				for(int k=0;k<arr.length;k++) {
                    System.out.println("\t\t"+arr[k]+":"+ w.mostFrequentLevel1NotOutsidePrediction.getCount(arr[k])/w.mostFrequentLevel1NotOutsidePrediction.totalTokens);
                }
				System.out.println("\t token maj type not-O: ");
				arr=w.mostFrequentLevel1NotOutsidePredictionType.getTokens();
				for(int k=0;k<arr.length;k++) {
                    System.out.println("\t\t"+arr[k]+":"+ w.mostFrequentLevel1NotOutsidePredictionType.getCount(arr[k])/w.mostFrequentLevel1NotOutsidePredictionType.totalTokens);
                }
			}
        }
	}

	//
	// assumes that the data has been tagged with level 1
	// 
	//
	public static void aggregateLevel1Predictions(Vector<LinkedVector> data){
		for(int i=0;i<data.size();i++) {
            for(int j=0;j<data.elementAt(i).size();j++) {
                aggregateTokenLevelLevel1Predictions((NEWord)data.elementAt(i).get(j));
            }
        }
	}

	private static void aggregateTokenLevelLevel1Predictions(NEWord word){
		OccurrenceCounter mostFrequentLevel1Prediction=new OccurrenceCounter();
		OccurrenceCounter mostFrequentLevel1PredictionType=new OccurrenceCounter();
		OccurrenceCounter mostFrequentLevel1NotOutsidePrediction=new OccurrenceCounter();
		OccurrenceCounter mostFrequentLevel1NotOutsidePredictionType=new OccurrenceCounter();
		if(!Character.isUpperCase(word.form.charAt(0))) {
            return;
        }
		if(Parameters.featuresToUse.containsKey("PredictionsLevel1"))
		{  
			int i=0;
			NEWord w = word, last = word.nextIgnoreSentenceBoundary;

			for (i = 0; i < 1000 && last != null; ++i) {
                last = last.nextIgnoreSentenceBoundary;
            }
			for (i = 0; i > -1000 && w.previousIgnoreSentenceBoundary != null; --i) {
                w = w.previousIgnoreSentenceBoundary;
            }

			do{
				if(w.form.equalsIgnoreCase(word.form)&&
						Character.isUpperCase(w.form.charAt(0))&&w!=word)
				{
					String prediction="O";
					if(NETaggerLevel2.isTraining){
						prediction=w.neLabel;
						if(Parameters.level1AggregationRandomGenerator.useNoise()) {
                            prediction=Parameters.level1AggregationRandomGenerator.randomLabel();
                        }
					}
					else{
						prediction=w.neTypeLevel1;
					}
					mostFrequentLevel1Prediction.addToken(prediction);
					if(!prediction.equals("O")) {
                        mostFrequentLevel1NotOutsidePrediction.addToken(prediction);
                    }
					if(prediction.indexOf('-')>-1){
						prediction=prediction.substring(2,prediction.length());
						mostFrequentLevel1NotOutsidePredictionType.addToken(prediction);
					}
					mostFrequentLevel1PredictionType.addToken(prediction);
				}
				w = w.nextIgnoreSentenceBoundary; 
			}while(w != last);	   		
		}
		word.mostFrequentLevel1Prediction=mostFrequentLevel1Prediction;
		word.mostFrequentLevel1NotOutsidePrediction=mostFrequentLevel1NotOutsidePrediction;
		word.mostFrequentLevel1PredictionType=mostFrequentLevel1PredictionType;
		word.mostFrequentLevel1NotOutsidePredictionType=mostFrequentLevel1NotOutsidePredictionType;
	}

	public static void aggregateEntityLevelPredictions(Vector<LinkedVector> data){
		annotatePredictionLevel1Entities(data);
		for(int i=0;i<data.size();i++) {
            for(int j=0;j<data.elementAt(i).size();j++){
				setMajorityExactEntityFeatures((NEWord)data.elementAt(i).get(j));
				setMajoritySuperEntityFeatures((NEWord)data.elementAt(i).get(j));
				setMajorityTokenInEntityFeatures((NEWord)data.elementAt(i).get(j));
			}
        }				
	}

	public static void setMajoritySuperEntityFeatures(NEWord word){
		if(Parameters.featuresToUse.containsKey("PredictionsLevel1")&&word.entity!=null)
		{  
			OccurrenceCounter majority=new OccurrenceCounter();
			int i=0;
			NEWord w = word, last = word.nextIgnoreSentenceBoundary;

			for (i = 0; i < 1000 && last != null; ++i) {
                last = last.nextIgnoreSentenceBoundary;
            }
			for (i = 0; i > -1000 && w.previousIgnoreSentenceBoundary != null; --i) {
                w = w.previousIgnoreSentenceBoundary;
            }

			do{
				if(w!=word&&w!=null&&w!=last&&w.entity!=null&&w.entity.indexOf(word.entity)>-1&&!w.entity.equals(word.entity))
				{
					majority.addToken(w.entityType);
					String entity=w.entity;
					while(w!=null&&w.entity!=null&&w.entity.equals(entity)&&w!=last) {
                        w=w.nextIgnoreSentenceBoundary;
                    }
				}
				else{
					if(w!=last) {
                        w = w.nextIgnoreSentenceBoundary;
                    }
				}
			}while(w != last);

			word.mostFrequentLevel1SuperEntityType=majority;
		}		
	}


	public static void setMajorityTokenInEntityFeatures(NEWord word){
		if(Parameters.featuresToUse.containsKey("PredictionsLevel1")&&Character.isUpperCase(word.form.charAt(0)))
		{  
			OccurrenceCounter majority=new OccurrenceCounter();
			int i=0;
			NEWord w = word, last = word.nextIgnoreSentenceBoundary;

			for (i = 0; i < 1000 && last != null; ++i) {
                last = last.nextIgnoreSentenceBoundary;
            }
			for (i = 0; i > -1000 && w.previousIgnoreSentenceBoundary != null; --i) {
                w = w.previousIgnoreSentenceBoundary;
            }

			Hashtable<NEWord, Boolean> takenWords=new Hashtable<NEWord, Boolean>();
			takenWords.put(word, true);
			NEWord temp=word.nextIgnoreSentenceBoundary;
			while(temp!=null&&word.entity!=null&&temp.entity!=null&&word.entity.equals(temp.entity))
			{
				takenWords.put(temp, true);
				temp=temp.nextIgnoreSentenceBoundary;
			}
			temp=word.previousIgnoreSentenceBoundary;
			while(temp!=null&&word.entity!=null&&temp.entity!=null&&word.entity.equals(temp.entity))
			{
				takenWords.put(temp, true);
				temp=temp.previousIgnoreSentenceBoundary;
			}


			do{
				if(!takenWords.containsKey(w)&&w!=null&&w!=last&&w.entity!=null&&
						w.entity.indexOf(word.form.toLowerCase()+" ")>-1&&
						!w.entity.equals(word.form.toLowerCase()+" "))
				{
					majority.addToken(w.entityType);
					String entity=w.entity;
					while(w!=null&&w.entity!=null&&w.entity.equals(entity)&&w!=last) {
                        w=w.nextIgnoreSentenceBoundary;
                    }
				}
				else{
					if(w!=last) {
                        w = w.nextIgnoreSentenceBoundary;
                    }
				}
			}while(w != last);

			word.mostFrequentLevel1TokenInEntityType=majority;
		}		
	}


	public static void setMajorityExactEntityFeatures(NEWord word){
		if(Parameters.featuresToUse.containsKey("PredictionsLevel1")&&word.entity!=null)
		{  
			OccurrenceCounter majority=new OccurrenceCounter();
			int i=0;
			NEWord w = word, last = word.nextIgnoreSentenceBoundary;

			for (i = 0; i < 1000 && last != null; ++i) {
                last = last.nextIgnoreSentenceBoundary;
            }
			for (i = 0; i > -1000 && w.previousIgnoreSentenceBoundary != null; --i) {
                w = w.previousIgnoreSentenceBoundary;
            }

			Hashtable<NEWord, Boolean> takenWords=new Hashtable<NEWord, Boolean>();
			takenWords.put(word, true);
			NEWord temp=word.nextIgnoreSentenceBoundary;
			while(temp!=null&&word.entity!=null&&temp.entity!=null&&word.entity.equals(temp.entity))
			{
				takenWords.put(temp, true);
				temp=temp.nextIgnoreSentenceBoundary;
			}
			temp=word.previousIgnoreSentenceBoundary;
			while(temp!=null&&word.entity!=null&&temp.entity!=null&&word.entity.equals(temp.entity))
			{
				takenWords.put(temp, true);
				temp=temp.previousIgnoreSentenceBoundary;
			}
			do{
				if(!takenWords.containsKey(w)&&w!=null&&w!=last&&w.entity!=null&&
						w.entity.equals(word.entity)&&
						!w.entity.equalsIgnoreCase((w.form+" ").toLowerCase()))
				{
					String entity=w.entity;
					majority.addToken(w.entityType);
					while(w!=null&&w.entity!=null&&w.entity.equals(entity)&&w!=last) {
                        w=w.nextIgnoreSentenceBoundary;
                    }
				}
				else{
					if(w!=last) {
                        w = w.nextIgnoreSentenceBoundary;
                    }
				}
			}while(w != last);

			word.mostFrequentLevel1ExactEntityType=majority;
		}		
	}

	public static void annotatePredictionLevel1Entities(Vector<LinkedVector> data){
		NEWord w=(NEWord)data.elementAt(0).get(0);
		while(w!=null){
			String label=w.neTypeLevel1;
			if(NETaggerLevel2.isTraining) {
                label=w.neLabel;
            }
			if(label.startsWith("B-")||label.startsWith("U-")){
				String expression=w.form+" ";
				String type=label.substring(2);
				NEWord temp=w.nextIgnoreSentenceBoundary;
				if(NETaggerLevel2.isTraining){
					while(temp!=null&&temp.neLabel.endsWith(type)&&
							!temp.neLabel.startsWith("B-")&&
							!temp.neLabel.startsWith("U-")){
						expression+=temp.form+" ";
						temp=temp.nextIgnoreSentenceBoundary;
					}
				}
				else{
					while(temp!=null&&temp.neTypeLevel1.endsWith(type)&&
							!temp.neTypeLevel1.startsWith("B-")&&
							!temp.neTypeLevel1.startsWith("U-")){
						expression+=temp.form+" ";
						temp=temp.nextIgnoreSentenceBoundary;
					}					
				}
				if(Parameters.level1AggregationRandomGenerator.useNoise()) {
                    type=Parameters.level1AggregationRandomGenerator.randomType();
                }
				while(w!=temp){
					w.entity=expression.toLowerCase();
					w.entityType=type;
					w=w.nextIgnoreSentenceBoundary;
				}
			} else {
                w=w.nextIgnoreSentenceBoundary;
            }
		}
	}

	/*
	 * Make sure to call this function as a last possible function:
	 * this function already assumes that the data was annotated with dictionaries etc.
	 */
	public static void annotate(NEWord word){
		if(Parameters.featuresToUse.containsKey("aggregateContext")||Parameters.featuresToUse.containsKey("aggregateGazetteerMatches"))
		{  
			int i=0;
			NEWord w = word, last = word.nextIgnoreSentenceBoundary;

			Hashtable<NEWord, Boolean> takenWords=new Hashtable<NEWord, Boolean>();
			takenWords.put(word, true);
			NEWord temp=word.nextIgnoreSentenceBoundary;
			int k=0;
			while(temp!=null&&k<3)
			    {
				takenWords.put(temp, true);
				temp=temp.nextIgnoreSentenceBoundary;
				k++;
			    }
			temp=word.previousIgnoreSentenceBoundary;
			k=0;
			while(temp!=null&&k<3)
			    {
				takenWords.put(temp, true);
				temp=temp.previousIgnoreSentenceBoundary;
				k++;
			    }



			for (i = 0; i < 200 && last != null; ++i) {
                last = last.nextIgnoreSentenceBoundary;
            }
			for (i = 0; i > -200 && w.previousIgnoreSentenceBoundary != null; --i) {
                w = w.previousIgnoreSentenceBoundary;
            }

			do{
				if(w.form.equalsIgnoreCase(word.form)&&Character.isUpperCase(word.form.charAt(0))&&
						Character.isLowerCase(w.form.charAt(0))) {
                    updateFeatureCounts(word,"appearsDownCased");
                }
				if(w.form.equalsIgnoreCase(word.form)&&
						Character.isUpperCase(w.form.charAt(0))&&
						Character.isUpperCase(word.form.charAt(0))&&
						word!=w)
				{
				    if(!takenWords.containsKey(w)&&Parameters.featuresToUse.containsKey("aggregateGazetteerMatches")){
					//System.out.println(w.matchedMultiTokenGazEntries.size());
					for(int j=0;j<w.matchedMultiTokenGazEntries.size();j++) {
                        updateFeatureCounts(word,w.matchedMultiTokenGazEntryTypes.elementAt(j));
                    }
                                        for(int j=0;j<w.matchedMultiTokenGazEntryTypesIgnoreCase.size();j++) {
                                            updateFeatureCounts(word,w.matchedMultiTokenGazEntryTypesIgnoreCase.elementAt(j));
                                        }
				    }
				    if(Parameters.featuresToUse.containsKey("aggregateContext"))
					{
						if(w.previous==null) {
                            updateFeatureCounts(word,"appearancesUpperStartSentence");
                        }
						if(w.previous!=null) {
                            if(((NEWord)w.previous).form.endsWith(".")) {
                                updateFeatureCounts(word,"appearancesUpperStartSentence");
                            }
                        }
						if(w.previous!=null&&!((NEWord)w.previous).form.endsWith(".")) {
                            updateFeatureCounts(word,"appearancesUpperMiddleSentence");
                        }

						NEWord wtemp = w, lastTemp = w.nextIgnoreSentenceBoundary;
						int j=0;
						for (j = 0; j < 2 && lastTemp != null; ++j) {
                            lastTemp = lastTemp.nextIgnoreSentenceBoundary;
                        }
						for (j = 0; j > -2 && wtemp.previousIgnoreSentenceBoundary != null; --j) {
                            wtemp = wtemp.previousIgnoreSentenceBoundary;
                        }
						do{
							updateFeatureCounts(word,"context:"+j+":"+wtemp.form);		 				

							String[] brownPaths=BrownClusters.getPrefixes(wtemp.form);
							//for(int k=0;k<brownPaths.length;k++)
							//updateFeatureCounts(word,"contextPath:"+j+":"+brownPaths[k]);
							if(brownPaths.length>0) {
                                updateFeatureCounts(word,"contextPath:"+j+":"+brownPaths[0]);
                            }
							wtemp = wtemp.nextIgnoreSentenceBoundary; 
							j++;
						}while(wtemp != lastTemp);
					}
				}
				w = w.nextIgnoreSentenceBoundary; 
			}while(w != last);
		}

	}
	private static void updateFeatureCounts(NEWord w,String feature){
		if(w.nonLocalFeatures.containsKey(feature)){
			int i=w.nonLocalFeatures.get(feature)+1;
			w.nonLocalFeatures.remove(feature);
			w.nonLocalFeatures.put(feature, i);
		} else {
            w.nonLocalFeatures.put(feature, 1);
        }
	}

	public static void main(String[] args){
		System.out.println("Reading config");
		Parameters.readConfigAndLoadExternalData("Config/withLookaheadDualTokenizingBILOU.config");
		System.out.println("Reading data");
		Vector<LinkedVector> data=BracketFileManager.readAndAnnotate("Data/temp.txt");
		System.out.println("extracting non-local features");
		NETaggerLevel1.isTraining=true;
		NETaggerLevel2.isTraining=true;
		if(Parameters.featuresToUse.containsKey("PredictionsLevel1")){
			GlobalFeatures.aggregateLevel1Predictions(data);
			GlobalFeatures.aggregateEntityLevelPredictions(data);
			GlobalFeatures.displayLevel1AggregationData(data);
		}
	}
}
