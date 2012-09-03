package ws.palladian.external.lbj.Tagger;

import java.util.StringTokenizer;

import lbj.NETaggerLevel1;
import lbj.NETaggerLevel2;

import ws.palladian.external.lbj.IO.Keyboard;
import LBJ2.classify.Classifier;


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

public class NerTagger {
	public static void main(String[] args){
		try{
			if(!args[0].equalsIgnoreCase("-trainSaveShapeClassifier")){
				Parameters.readConfigAndLoadExternalData(args[args.length-1]);
				Parameters.forceNewSentenceOnLineBreaks=Boolean.parseBoolean(args[args.length-2]);
			}

			if(args[0].equalsIgnoreCase("-annotate")) {
                NETagPlain.tagFile(args[1], args[2],false);
            }
			if(args[0].equalsIgnoreCase("-annotateAndDebug")) {
                NETagPlain.tagFile(args[1], args[2],true);
            }
			if(args[0].equalsIgnoreCase("-demo")){
				System.out.println("loading the tagger");
		        NETaggerLevel1 tagger1 = new NETaggerLevel1();
		        tagger1=(NETaggerLevel1)Classifier.binaryRead(Parameters.pathToModelFile+".level1");
		        NETaggerLevel2 tagger2 = new NETaggerLevel2();
		        tagger2=(NETaggerLevel2)Classifier.binaryRead(Parameters.pathToModelFile+".level2");
	        	System.out.println("Done- loading the tagger");
				String input="";
				while(true){
					input=Keyboard.readLine();
					if(input.startsWith(" **")){
						Parameters.forceNewSentenceOnLineBreaks=false;
						input=input.substring(3);
					}
					if(input.startsWith(" *true*")){
						Parameters.forceNewSentenceOnLineBreaks=true;
						input=input.substring(" *true*".length());						
					}
					input=BracketFileManager.replaceSubstring(input, "*newline*", "\n");
					String res=DemoEngine.tagLine(input,tagger1,tagger2);
					int len=0;
					StringTokenizer st=new StringTokenizer(res);
					StringBuffer output=new StringBuffer();
					while(st.hasMoreTokens()){
						String s=st.nextToken();
						output.append(" "+s);
						len+=s.length();
					}		
					System.out.println(BracketFileManager.replaceSubstring(output.toString(), "\n", " "));
				}
			}
			if(args[0].equalsIgnoreCase("-test")) {
                NETester.test(args[1], args[2]);
            }
			if(args[0].equalsIgnoreCase("-train")) {
                LearningCurve.getLearningCurve(args[1],args[3]);
            }
			if(args[0].equalsIgnoreCase("-trainSaveShapeClassifier")){
				 System.out.println("Training shape tagger predictions"); 
				 ShapeClassifierManager.trainLocalTagger();
				 ShapeClassifierManager.save();
			}
		}catch(Exception e){
			System.out.println("Exception caught: ");
			e.printStackTrace();
			System.out.println("The problem might be the usage: use one of the below:");
			System.out.println("*)java -classpath $LBJ2.jar:LBJ2Library.jar:bin -Xmx1000m -train <traingFile> -test <testFile> <-b/-r> <pathToConfigFile>");
			System.out.println("\tThis command will learn the classifier and print the training curve, the last parameter specifies the file " +
					"format; use -b for brackets and -r for raw (plain) text; ");
			System.out.println("*)java -classpath $LBJ2.jar:LBJ2Library.jar:bin -Xmx1000m -annotate <rawInputFile> <outFile>  <pathToConfigFile>");
			System.out.println("\tThis one takes a plain text, tags it, and outputs the the specified file in brackets format");
			System.out.println("*)java -classpath $LBJ2.jar:LBJ2Library.jar:bin -Xmx1000m -test <goldFile> <format(-c/-r)>  <pathToConfigFile>");
			System.out.println("\tWill output phrase-level F1 score on the file (recall that I love other measures for comparing taggers, I want to use this primary as sanity check)");
		}		
	}
}
