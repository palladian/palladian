package ws.palladian.external.lbj.ClassifiersAndUtils;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import ws.palladian.external.lbj.IO.InFile;


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

public class Document
{
	
	public int classID=-1;
	public Vector<String> words;
	public int[] activeFeatures=null;
	public Document brother=null;//this is a dirty hack remove it ASAP
	//public Hashtable<String, Boolean> wordsHash=new Hashtable<String, Boolean>();
	
	
	public Document(Document d)
	{
		this.classID=d.classID;
		words=new Vector<String>(d.words.size());
		for(int i=0;i<d.words.size();i++) {
            words.add(d.words.elementAt(i));
        }
	}
	public Document(Vector<String> _words,int _classID){
		classID=_classID;
		words=_words;
		words.trimToSize();
		//for(int i=0;i<words.size();i++)
		//	wordsHash.put(words.elementAt(i), true);
	}

	public Document(String[] _words,int _classID){
		classID=_classID;
		words=new Vector<String>();
		for(int i=0;i<_words.length;i++) {
            words.addElement(_words[i]);
        }
		words.trimToSize();
		//for(int i=0;i<words.size();i++)
		//	wordsHash.put(words.elementAt(i), true);
	}
	
	/*
	 * will read all the lines in the file inot a single document
	 */
	public Document(String filename, int _classID,StopWords stops) {
		InFile in=new InFile(filename);
		this.classID=_classID;
		words = new Vector<String>();
		Vector<String> currentWords = in.readLineTokens();
		while (currentWords != null) {
			if (stops != null) {
                currentWords = stops.filterStopWords(currentWords);
            }
			for (int j = 0; j < currentWords.size(); j++) {
                words.addElement(currentWords.elementAt(j));
            }
			currentWords = in.readLineTokens();
		}
		words.trimToSize();
	}
	
	public boolean containsWord(String w)
	{
		for(int i=0;i<words.size();i++) {
            if(words.elementAt(i).equalsIgnoreCase(w)) {
                return true;
            }
        }
		return false;
		//return wordsHash.containsKey(w);
	}
	
	public int[] getActiveFid(FeatureMap map){
		Hashtable<Integer,Boolean> activeFids=new Hashtable<Integer, Boolean>(map.dim);
		for(int i=0;i<words.size();i++)
		{
			if(map.wordToFid.containsKey(words.elementAt(i)))
			{
				int fid=map.wordToFid.get(words.elementAt(i));
				if(!activeFids.containsKey(fid)) {
                    activeFids.put(fid,true);
                }
			}
		}
		int[] res=new int[activeFids.size()];
		Iterator<Integer> iter=activeFids.keySet().iterator();
		for(int i=0;i<res.length;i++) {
            res[i]=iter.next();
        }
		return res;
	}
	
	public double[] getFeatureVec(FeatureMap map){
		double[] res=new double[map.dim];
		for(int i=0;i<words.size();i++) {
            if(map.wordToFid.containsKey(words.elementAt(i))) {
                res[map.wordToFid.get(words.elementAt(i))]++;
            }
        }
		return res;
	}
	
	public void toCompactFeatureRep(FeatureMap map){
		if(words==null)
		{
			activeFeatures=null;
			return;
		}
		this.activeFeatures=getActiveFid(map);
		this.words=null;
	}
	
	public void tokenize(){
		StringTokenizer st=new StringTokenizer(tokenize(this.toString()));
		words=new Vector<String>();
		while(st.hasMoreTokens()) {
            words.addElement(st.nextToken());
        }
	}
	
	public static String tokenize(String s)
	{
		String delims=",.?!;:<>-*&^%$#[]{}()/\\";
		StringBuffer res=new StringBuffer((int)(s.length()*1.5));
		for(int i=0;i<s.length();i++)
		{
			if(delims.indexOf(s.charAt(i))>-1) {
                res.append(' ');
            }
			res.append(s.charAt(i));
			if(delims.indexOf(s.charAt(i))>-1) {
                res.append(' ');
            }
		}
		s=res.toString();
		delims="'`";
		res=new StringBuffer((int)(s.length()*1.5));
		for(int i=0;i<s.length();i++)
		{
			if(delims.indexOf(s.charAt(i))>-1) {
                res.append(' ');
            }
			res.append(s.charAt(i));
		}
		return res.toString();
	}

	
	public String toString() {
		StringBuffer res=new StringBuffer(words.size()*10);
		for(int i=0;i<words.size();i++) {
            res.append(words.elementAt(i)+ " ");
        }
		return res.toString();
	}
	public String toString(FeatureMap map,boolean verbose) {
		StringBuffer res=new StringBuffer(words.size()*10);
		for(int i=0;i<words.size();i++){
			if(map.wordToFid.containsKey(words.elementAt(i))) {
                res.append(words.elementAt(i)+ " ");
            } else
				if(verbose) {
                    res.append("(?"+words.elementAt(i)+ "?) ");
                }
		}
		return res.toString();
	}
}