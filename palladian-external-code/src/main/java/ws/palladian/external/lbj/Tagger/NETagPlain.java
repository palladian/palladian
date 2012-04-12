package ws.palladian.external.lbj.Tagger;

import java.util.Iterator;
import java.util.Vector;

import lbj.FeaturesLevel1;
import lbj.FeaturesLevel2;
import lbj.NETaggerLevel1;
import lbj.NETaggerLevel2;
import ws.palladian.external.lbj.IO.OutFile;
import LBJ2.classify.Classifier;
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


public class NETagPlain
{
  
    public static void tagFile(String inputFile,String outputFile,boolean debug)
    {
    	System.out.println("Tagging file: "+inputFile);
    	Vector<LinkedVector> data=BracketFileManager.parsePlainText(inputFile);
        NETaggerLevel1 tagger1 = new NETaggerLevel1();
        System.out.println("Reading model file : "+ Parameters.pathToModelFile+".level1");
        tagger1=(NETaggerLevel1)Classifier.binaryRead(Parameters.pathToModelFile+".level1");
        NETaggerLevel2 tagger2 = new NETaggerLevel2();
        System.out.println("Reading model file : "+ Parameters.pathToModelFile+".level2");
        tagger2=(NETaggerLevel2)Classifier.binaryRead(Parameters.pathToModelFile+".level2");

        NETester.annotateBothLevels(data,tagger1,tagger2);
        
        if(debug){
        	System.out.println("---------------    Active features: --------------");
        	for(Iterator<String> iter=Parameters.featuresToUse.keySet().iterator();iter.hasNext();System.out.println(iter.next())) {
                ;
            }
        	System.out.println("-------- Sentence splitting details (each sentence is a new line)---------");
        	for(int i=0;i<data.size();i++){
        		System.out.println("\t");
        		for(int j=0;j<data.elementAt(i).size();j++) {
                    System.out.print(((NEWord)data.elementAt(i).get(j)).form+" ");
                }
        		System.out.println("");
        	}
        	System.out.println("\n\n------------  Level1 features report  ----------------\n\n");
			FeaturesLevel1 feats1=new FeaturesLevel1();
        	for(int i=0;i<data.size();i++){
        		for(int j=0;j<data.elementAt(i).size();j++) {
                    System.out.println("\t"+((NEWord)data.elementAt(i).get(j)).form+" "+feats1.classify(data.elementAt(i).get(j)));
                }
        	}
    		if(tagger2!=null&&(Parameters.featuresToUse.containsKey("PatternFeatures")||Parameters.featuresToUse.containsKey("PredictionsLevel1"))){
            	System.out.println("\n\n---------  Level2 features report  -----------\n\n");
    			FeaturesLevel2 feats2=new FeaturesLevel2();
            	for(int i=0;i<data.size();i++){
            		for(int j=0;j<data.elementAt(i).size();j++) {
                        System.out.println("\t"+((NEWord)data.elementAt(i).get(j)).form+" "+feats2.classify(data.elementAt(i).get(j)));
                    }
            	}
    		}
        }
        
        OutFile out=new OutFile(outputFile);
        for(int i=0;i<data.size();i++){
            LinkedVector vector = data.elementAt(i);
            StringBuffer res=new StringBuffer();
            boolean open=false;
            String[] predictions=new String[vector.size()];
            String[] words=new String[vector.size()];
            for(int j=0;j<vector.size();j++){
            	predictions[j] =   bilou2bio(((NEWord)vector.get(j)).neTypeLevel2);
            	words[j]=((NEWord)vector.get(j)).form;
            }
            for(int j=0;j<vector.size();j++)
            { 
            	if (predictions[j].startsWith("B-")
            			|| 
            			j>0&&predictions[j].startsWith("I-") && !predictions[j-1].endsWith(predictions[j].substring(2))){
                    res.append(" [" + predictions[j].substring(2) + " ");
            		open=true;
            	}

                if (words[j].length() > 0
                        && (Character.isLetterOrDigit(words[j].charAt(0)) || isBracket(words[j].charAt(0)))) {
                    res.append(" ");
                }

                // res.append(words[j]+ " ");
                res.append(words[j]);
                // if (Character.isLetterOrDigit(words[j].charAt(0)) && !open) {
                // res.append(" ");
                // }

                if (open) {
            		boolean close=false;
            		if(j==vector.size()-1){
            			close=true;
            		}
            		else
            		{
            			if(predictions[j+1].startsWith("B-")) {
                            close=true;
                        }
            			if(predictions[j+1].equals("O")) {
                            close=true;
                        }
            			if(predictions[j+1].indexOf('-')>-1&&!predictions[j].endsWith(predictions[j+1].substring(2))) {
                            close=true;
                        }
            		}
            		if(close){
                        res.append(" ]");
            			open=false;
            		}
            	}
            }
            out.print(res.toString());
        }
        out.close();    	
    }
    
    private static boolean isBracket(char character) {
        final char[] BRACKETS = { '(', ')', '{', '}', '[', ']' };
        for (char element : BRACKETS) {
            if (element == character) {
                return true;
            }

        }
        return false;
    }

    public static String bilou2bio(String prediction){
    	if(Parameters.taggingScheme.equalsIgnoreCase(Parameters.BILOU)){
    		if(prediction.startsWith("U-")) {
                prediction="B-"+prediction.substring(2);
            }
    		if(prediction.startsWith("L-")) {
                prediction="I-"+prediction.substring(2);
            }
    	}
    	return prediction;
    }
}

