package ws.palladian.external.lbj.Tagger;

import java.util.Vector;

import lbj.NETaggerLevel1;
import lbj.NETaggerLevel2;

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

public class DemoEngine
{
  
    public static String tagLine(String line,NETaggerLevel1 tagger1,NETaggerLevel2 tagger2)
    {
    	Vector<LinkedVector> data=BracketFileManager.parseText(line);
    	NETester.annotateBothLevels(data,tagger1,tagger2);
        StringBuffer res=new StringBuffer();
        for(int i=0;i<data.size();i++){
            LinkedVector vector = data.elementAt(i);
            boolean open=false;
            String[] predictions=new String[vector.size()];
            String[] words=new String[vector.size()];
            for(int j=0;j<vector.size();j++){
            	predictions[j] = bilou2bio(((NEWord)vector.get(j)).neTypeLevel2);
            	words[j]=((NEWord)vector.get(j)).form;
            }
            for(int j=0;j<vector.size();j++)
            { 
            	if (predictions[j].startsWith("B-")
            			|| 
            			j>0&&predictions[j].startsWith("I-") && !predictions[j-1].endsWith(predictions[j].substring(2))){
            		res.append("[" + predictions[j].substring(2) + " ");
            		open=true;
            	}
            	res.append(words[j]+ " ");
            	if(open){
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
            			res.append(" ] ");
            			open=false;
            		}
            	}
            }
        }
        return insertHtmlColors(res.toString());
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
    
    public static String insertHtmlColors(String annotatedText){
    	String res=BracketFileManager.replaceSubstring(annotatedText, "[PER", "<font style=\"color:red\">[PER");
    	res=BracketFileManager.replaceSubstring(res, "[LOC", "<font style=\"color:blue\">[LOC");
    	res=BracketFileManager.replaceSubstring(res, "[ORG", "<font style=\"color:green\">[ORG");
    	res=BracketFileManager.replaceSubstring(res, "[MISC", "<font style=\"color:brown\">[MISC");
    	
    	res=BracketFileManager.replaceSubstring(res, "]", "]</font>");
    	return res;
    }
}

