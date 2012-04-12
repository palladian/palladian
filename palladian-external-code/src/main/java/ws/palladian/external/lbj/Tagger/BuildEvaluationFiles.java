package ws.palladian.external.lbj.Tagger;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import ws.palladian.external.lbj.IO.OutFile;
import ws.palladian.external.lbj.StringStatisticsUtils.OccurrenceCounter;




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

public class BuildEvaluationFiles {
	
	public static void main(String[] args){
		buildEvaluationFile(args[0], args[1], args[2],Boolean.parseBoolean(args[3]));
	}
	
	public static void buildEvaluationFile(String goldFile,String taggedFile,String outFile,boolean removeNestedTags){
		String[] goldFiles={goldFile};
		String[] taggedFiles={taggedFile};
		buildEvaluationFile(goldFiles, taggedFiles, outFile, removeNestedTags);
	}
	public static void buildEvaluationFile(String[] goldFiles,String[] taggedFiles,String outFile,boolean removeNestedTags){
		OutFile outPhrase=new OutFile(outFile+".phraseLevel");
		OutFile outToken=new OutFile(outFile+".tokenLevel");
		for(int i=0;i<goldFiles.length;i++) {
            appendToEvaluationFile(goldFiles[i], taggedFiles[i], outPhrase,outToken, removeNestedTags);
        }
		outPhrase.close();
		outToken.close();
	}
	
	public static void appendToEvaluationFile(String goldFile,String taggedFile,OutFile outPhrase,OutFile outToken, boolean removeNestedTags){
		Vector<String> goldTags=new Vector<String>();
		Vector<String> goldWords=new Vector<String>();		
		BracketFileManager.parseBracketedFile(goldFile,goldTags,goldWords);
		if(removeNestedTags) {
            BracketFileManager.removeNestedTags(goldTags, goldWords);
        }
		Vector<String> tempgoldTags=new Vector<String>();
		Vector<String> tempgoldWords=new Vector<String>();
		Hashtable<Integer,Boolean> newlines=new Hashtable<Integer,Boolean>();
		for(int i=0;i<goldWords.size();i++){
			String s=cleanPunctuation(goldWords.elementAt(i));
			//if(goldWords.elementAt(i).indexOf('.')>-1||goldWords.elementAt(i).indexOf('!')>-1||goldWords.elementAt(i).indexOf('?')>-1)
			//	newlines.put(tempgoldTags.size(),true);
			if(s.length()>0){
				tempgoldWords.addElement(s);
				tempgoldTags.addElement(goldTags.elementAt(i));
			}
		}
		goldWords=tempgoldWords;
		goldTags=tempgoldTags;
		
		
		Vector<String> resTags=new Vector<String>();
		Vector<String> resWords=new Vector<String>();
		BracketFileManager.parseBracketedFile(taggedFile,resTags,resWords);
		if(removeNestedTags) {
            BracketFileManager.removeNestedTags(resTags, resWords);
        }
		Vector<String> tempresTags=new Vector<String>();
		Vector<String> tempresWords=new Vector<String>();
		for(int i=0;i<resWords.size();i++){
			String s=cleanPunctuation(resWords.elementAt(i));
			if(s.length()>0){
				tempresWords.addElement(s);
				tempresTags.addElement(resTags.elementAt(i));
			}
		}
		resWords=tempresWords;
		resTags=tempresTags;
		
		int gWordId=0,gCharId=0;
		int tWordId=0,tCharId=0;
		while(gWordId<goldWords.size()){
			String gw=goldWords.elementAt(gWordId).toLowerCase();
			String rw=resWords.elementAt(tWordId).toLowerCase();
			OccurrenceCounter resTagsForCurrentToken=new OccurrenceCounter();
			while(gCharId<gw.length()){
				if(tCharId>=rw.length()){
					tWordId++;
					tCharId=0;
					rw=resWords.elementAt(tWordId).toLowerCase();
				}
				if(gw.charAt(gCharId)!=rw.charAt(tCharId)){
					System.out.println("mismatched characters when building evaluation files");
					System.out.println("the words were '"+gw+"' and: '"+rw+"'  exiting");
					System.out.println("the characters were '"+gw.charAt(gCharId)+"' and: '"+rw.charAt(tCharId)+"'  exiting");
					outToken.close();
					outPhrase.close();
					System.exit(0);
				}
				else{
					if(gCharId==0){
						resTagsForCurrentToken.addToken(resTags.elementAt(tWordId));
					}
					//String lastTag=resTags.elementAt(tWordId);
					//resTagsForCurrentToken.addToken(lastTag);
					//System.out.println(gw.charAt(gCharId)+"-"+rw.charAt(tCharId));
				}
				gCharId++;
				tCharId++;
			}
			String maxLabel="";
			int maxCount=0;
			for(Iterator<String> iter=resTagsForCurrentToken.getTokensIterator();iter.hasNext();){
				String s=iter.next();
				if(maxCount<=resTagsForCurrentToken.getCount(s)){
					maxCount=(int)resTagsForCurrentToken.getCount(s);
					maxLabel=s;
				}
			}
			//if((maxLabel.indexOf("-")>-1)&&(goldTags.elementAt(gWordId).indexOf("-")>-1)
			//	&&(maxLabel.substring(2)).equalsIgnoreCase(goldTags.elementAt(gWordId).substring(2)))
			//	outPhrase.println(goldWords.elementAt(gWordId)+" "+goldTags.elementAt(gWordId)+" "+goldTags.elementAt(gWordId));
			//else
				outPhrase.println(goldWords.elementAt(gWordId)+" "+goldTags.elementAt(gWordId)+" "+maxLabel);

			String g=goldTags.elementAt(gWordId);
			if(g.indexOf('-')>-1) {
                g=g.substring(g.indexOf('-')+1);
            }
			if(maxLabel.indexOf('-')>-1) {
                maxLabel=maxLabel.substring(maxLabel.indexOf('-')+1);
            }
			outToken.println(goldWords.elementAt(gWordId)+" "+g+" "+maxLabel);
			if(newlines.containsKey(gWordId))
			{
				outPhrase.println("");
				outToken.println("");
			}
			gWordId++;
			gCharId=0;
			/*tCharId++;
			if(tCharId>=rw.length()){
				tWordId++;
				tCharId=0;
				if(tWordId<resWords.size())
					rw=resWords.elementAt(tWordId).toLowerCase();
			}*/
		}
	}

	public static String cleanPunctuation(String s){
		String res="";
		String punc="\"';:/?><,.!`~@#$%^&*()-_=+|\\/[]{}";
		int i=0;
		while(i<s.length()){
			if(punc.indexOf(s.charAt(i))==-1) {
                res=res+s.charAt(i);
            }
			i++;
		}
		return res;
	}
}
