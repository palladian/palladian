package ws.palladian.external.lbj.Tagger;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

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

public class PatternExtractor {
	public static final int contextSize=50; //in what area to look for the patterns
	public static final int appearanceThres=3; // what is the minimal number of times a pattern should appear to be called a parrten

	
	public static void annotate(Vector<LinkedVector> data,boolean useGoldLabel,boolean debug){
		Hashtable<NEWord,Vector<String>> patterns = getPatterns(data, useGoldLabel);
		//clear the patterns extracted from the previous iterations!!!
		for(int i=0;i<data.size();i++) {
            for(int j=0;j<data.elementAt(i).size();j++) {
                ((NEWord)data.elementAt(i).get(j)).activePatterns=new OccurrenceCounter();
            }
        }

				
		for(int i=0;i<data.size();i++){
			Vector<String> matched=new Vector<String>();
			for(int j=0;j<data.elementAt(i).size();j++){
				//find the patterns that appear near the word
				OccurrenceCounter nearbyPatterns=new OccurrenceCounter();
				NEWord w=(NEWord)data.elementAt(i).get(j);
				for(int k=0;k<contextSize&&w!=null;k++){
					if(patterns.containsKey(w)){
						Vector<String> v=patterns.get(w);
						for(int l=0;l<v.size();l++) {
                            nearbyPatterns.addToken(v.elementAt(l));
                        }
					}
					w=w.nextIgnoreSentenceBoundary;
				}
				w=(NEWord)data.elementAt(i).get(j);
				for(int k=0;k<contextSize&&w!=null;k++){
					if(patterns.containsKey(w)){
						Vector<String> v=patterns.get(w);
						for(int l=0;l<v.size();l++) {
                            nearbyPatterns.addToken(v.elementAt(l));
                        }
					}
					w=w.previousIgnoreSentenceBoundary;
				}
				//check what frequent patterns can be applied to a word, and apply them 
				w=(NEWord)data.elementAt(i).get(j);
				for(Iterator<String> iter=nearbyPatterns.getTokensIterator();iter.hasNext();){
					String pattern=iter.next();
					if(nearbyPatterns.getCount(pattern)>=appearanceThres){
						String pat=annotateWithPattern(w,pattern,nearbyPatterns.getCount(pattern));
						if(pat!=null&&debug) {
                            matched.addElement(pat+" : "+ nearbyPatterns.getCount(pattern));
                        }
					}
				}
			}
			if(debug){
				for(int j=0;j<data.elementAt(i).size();j++) {
                    System.out.print(((NEWord)data.elementAt(i).get(j)).form+" ");
                }
				System.out.println("");
				for(int j=0;j<matched.size();j++) {
                    System.out.println(matched.elementAt(j));
                }
			}
		}
	}
	
	private static String annotateWithPattern(NEWord word,String pattern,double weight){
	    
		StringTokenizer st=new StringTokenizer(pattern);
		String patLeft=st.nextToken();
		String patLabel=st.nextToken();
		String patRight=st.nextToken();
		String matchedPattern=patLeft+" ["+patLabel+" "; // this is for debugging purposes
		String left1="*null*";
		String left2="*null*";
		if(word.previous!=null) {
            left1=normalizeDigits(((NEWord)word.previous).form);
        }
		if(word.previousIgnoreSentenceBoundary!=null) {
            left2=normalizeDigits((word.previousIgnoreSentenceBoundary).form);
        }
		if(!left1.equals(patLeft)&&!left2.equals(patLeft))
         {
            return null;//there is no way we can match this pattern
        }
		
		//we're going to run for up to 5 words, and we're going to
		//stop as soon as we find a pattern
		NEWord last=word;
		for(int i=0;i<5&&last!=null;i++){
			String right1="*null*";
			String right2="*null*";
			if(last.next!=null) {
                right1=normalizeDigits(((NEWord)last.next).form);
            }
			if(last.nextIgnoreSentenceBoundary!=null) {
                right2=normalizeDigits((last.nextIgnoreSentenceBoundary).form);
            }
			if(patRight.equals(right1)||patRight.equals(right2)){
				NEWord temp=word;
				while(temp!=last){
					//check if we can minimize the pattern on the left
					//if we can move the left bound, return null, because this 
					//pattern will be discovered when we look at the next word
					temp=temp.nextIgnoreSentenceBoundary;
					left1="*null*";
					left2="*null*";
					if(temp.previous!=null) {
                        left1=normalizeDigits(((NEWord)temp.previous).form);
                    }
					if(temp.previousIgnoreSentenceBoundary!=null) {
                        left2=normalizeDigits((temp.previousIgnoreSentenceBoundary).form);
                    }
					if(left1.equals(patLeft)||left2.equals(patLeft))
                     {
                        return null;//we know that the right side matches, now we successfully moved the left side
                    }
				}
				
				//if we reached here, we're indeed in the minimal pattern we can grab
				NEWord w=word;				
				matchedPattern += " "+w.form;
				w.activePatterns.addToken(pattern,weight);
				while(w!=last){
					w=w.nextIgnoreSentenceBoundary;
					w.activePatterns.addToken(pattern,weight);
					matchedPattern += " "+w.form;
				}
				
				return matchedPattern+=" ] " +patRight+"\t";
			}
			last=last.nextIgnoreSentenceBoundary;
			}
		return null;
	}
	
	//
	// Will initialize the patterns like " ( ORG ," etc.
	//	
	public static Hashtable<NEWord,Vector<String>> getPatterns(Vector<LinkedVector> data,boolean useGoldLabel){
		Hashtable<NEWord,Vector<String>> patterns=new Hashtable<NEWord, Vector<String>>();
		for(int i=0;i<data.size();i++){
			for(int j=0;j<data.elementAt(i).size();j++){
				NEWord w=(NEWord)data.elementAt(i).get(j);
	   			String label=w.neLabel;
	   			if(!useGoldLabel) {
                    label=w.neTypeLevel1;
                }
	   			if(label.startsWith("B-")||label.startsWith("U-")){
	   				Vector<String> prev=new Vector<String>();
	   				if(w.previous==null) {
                        prev.addElement("*null*");
                    }
	   				if(w.previousIgnoreSentenceBoundary!=null){
	   					String s=w.previousIgnoreSentenceBoundary.form;
	   					if(hasNoLetters(s)) {
                            prev.addElement(normalizeDigits(s));
                        }
	   				}
	   				if(useGoldLabel){
	   					while(w.next!=null&&(((NEWord)w.next).neLabel.startsWith("I-")||((NEWord)w.next).neLabel.startsWith("L-"))) {
                            w=(NEWord)w.next;
                        }
	   				}else{
	   					while(w.next!=null&&(((NEWord)w.next).neTypeLevel1.startsWith("I-")||((NEWord)w.next).neTypeLevel1.startsWith("L-"))) {
                            w=(NEWord)w.next;
                        }
	   				}
	   				Vector<String> next=new Vector<String>();
	   				if(w.next==null) {
                        next.addElement("*null*");
                    }
	   				if(w.nextIgnoreSentenceBoundary!=null){
	   					String s=w.nextIgnoreSentenceBoundary.form;
	   					if(hasNoLetters(s)) {
                            next.addElement(normalizeDigits(s));
                        }
	   				}
	   				Vector<String> v=new Vector<String>();
					label=label.substring(2,label.length());
					if(useGoldLabel&&Math.random()<0.3) {
                        label=ParametersForLbjCode.patternLabelRandomGenerator.randomType();
                    }
					if(!label.equals("O")){
					    for(int l=0;l<prev.size();l++) {
                            for(int k=0;k<next.size();k++) {
                                v.addElement(prev.elementAt(l)+"\t"+label+"\t"+next.elementAt(k));
                            }
                        }
					    patterns.put((NEWord)data.elementAt(i).get(j),v);
					}
	   			}
			}
		}

		return patterns;
	}
	private static boolean hasNoLetters(String s){
		for(int i=0;i<s.length();i++) {
            if(Character.isLetter(s.charAt(i))) {
                return false;
            }
        }
		return true;
	}
	
	private static String normalizeDigits(String s){
		//if more than two non-digits and non-punc, return s
		int count=0;
		for(int i=0;i<s.length();i++){
			char c=s.charAt(i);
			if(!Character.isDigit(c)&&c!='.'&&c!=',') {
                count++;
            }
		}
		if(count>1) {
            return s;
        }
		for(int i=0;i<s.length();i++) {
            if(Character.isDigit(s.charAt(i))) {
                return "*D*";
            }
        }
		return s;
	}
	
	public static void main(String[] args){
		Parameters.readConfigAndLoadExternalData("Config/baselineFeatures.config");
		Reuters2003Parser parser=new Reuters2003Parser("Data/GoldData/Reuters/BIO.testb");
		Vector<LinkedVector> data=parser.readAndAnnotate();
		annotate(data, true, true);
	}
	
}
