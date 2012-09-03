package ws.palladian.external.lbj.Tagger;

import java.util.Vector;

import lbj.NETaggerLevel1;

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


/*
 * 
 * will annotate the plain text using the specified config file and model, then
 * it will write it to the specified output file (use log file) finally, it'll use
 * my brackets file reader to load the tagged data, and compare to the tagging
 * results. The idea is that if there is a bug in saving annotated data or loading
 * annotated data, we'll see the inconsistency with the original tagging results. 
 * 
 */
public class TestAnnotationSanity {
	public static void main(String[]args){
		try{
		  	Parameters.readConfigAndLoadExternalData(args[args.length-1]);
		    if(Parameters.featuresToUse.containsKey("GazetteersFeatures")) {
                Gazzetteers.init("Data/KnownLists");
            }
			
		    NETagPlain.tagFile(args[0], args[1],false);
			
			
	    	Vector<LinkedVector> rawData=BracketFileManager.parsePlainText(args[0]);
	    	NETaggerLevel1 tagger=(NETaggerLevel1)Classifier.binaryRead(Parameters.pathToModelFile);
	        NETaggerLevel1.isTraining=false;
			Vector<String> tags=new Vector<String>();
	    	Vector<String> words=new Vector<String>();
			for(int i=0;i<rawData.size();i++){
				for(int j=0;j<rawData.elementAt(i).size();j++){
					NEWord w=(NEWord)rawData.elementAt(i).get(j);
					words.addElement(w.form);
					String tag=NETagPlain.bilou2bio(tagger.discreteValue(w));
					if(tags.size()>0&&tag.indexOf("I-")>-1&&!tags.elementAt(tags.size()-1).endsWith(tag.substring(2))) {
                        tag="B-"+tag.substring(2);
                    }
					tags.addElement(tag);
				}
			}
	    	Vector<String> readTags=new Vector<String>();
	    	Vector<String> readWords=new Vector<String>();
			BracketFileManager.parseBracketedFile(args[1], readTags, readWords);
			for(int i=0;i<Math.min(readWords.size(), words.size());i++){
				System.out.println("Expected: ("+words.elementAt(i)+","+tags.elementAt(i)+")\t Read: ("+readWords.elementAt(i)+","+readTags.elementAt(i)+")");
				boolean exit=false;
				if(!readWords.elementAt(i).equals(words.elementAt(i))){
					System.out.println("Error- mismatching words!!!!");
					exit=true;
				}
				if(!readTags.elementAt(i).equals(tags.elementAt(i))){
					System.out.println("Error- mismatching tags!!!!");
					exit=true;
				}
				if(exit){					
					System.out.println("Next few tokens:");
					i++;
					int end=i+5;
					for(;i<Math.min(readWords.size(), words.size())&&i<end;i++) {
                        System.out.println("Expected: ("+words.elementAt(i)+","+tags.elementAt(i)+")\t Read: ("+readWords.elementAt(i)+","+readTags.elementAt(i)+")");
                    }
					System.exit(0);
				}
			}
			if(readWords.size()!= words.size()){
				System.out.println("Error!- the number of read words does not mach the number of expected words!");
				System.exit(0);
			}
			if(readTags.size()!= tags.size()){
				System.out.println("Error!- the number of read tags does not mach the number of expected tags!");
				System.exit(0);
			}
		}
		catch(Exception e){
			System.out.println("Exception caught: ");
			e.printStackTrace();
			System.out.println("Maybe wrong usage?");
			System.out.println("Usage: java TestAnnotationSanity <inputFileRaw> <outputFileTagged> <configFile>");
		}
	}
}
