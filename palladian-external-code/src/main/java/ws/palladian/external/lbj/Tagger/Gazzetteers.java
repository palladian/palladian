package ws.palladian.external.lbj.Tagger;



import java.io.File;
import java.util.Hashtable;
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

public class Gazzetteers{
	
	public static Vector<String> dictNames=new Vector<String>();
	public  static Vector<Hashtable<String,Boolean>> dictionaries=null;
	public static Vector<Hashtable<String, Boolean>> dictionariesIgnoreCase = null;
	public static Vector<Hashtable<String, Boolean>> dictionariesOneWordIgnorePunctuation = null;

	public static void init(String pathToDictionaries){
		dictNames=new Vector<String>();
		dictionaries=null;
		dictionariesIgnoreCase = null;
		dictionariesOneWordIgnorePunctuation = null;
		System.out.println("loading dazzetteers....");
		Vector<String> filenames=new Vector<String>();
		String[] allfiles=new File(pathToDictionaries).list();
		for(int i=0;i<allfiles.length;i++) {
            if(new File(pathToDictionaries+"/"+allfiles[i]).isFile()){
				filenames.addElement(pathToDictionaries+"/"+allfiles[i]);
				dictNames.addElement(allfiles[i]);
			}
        }
		dictionaries=new Vector<Hashtable<String,Boolean>>(filenames.size());
		dictionariesIgnoreCase=new Vector<Hashtable<String,Boolean>>(filenames.size());
		dictionariesOneWordIgnorePunctuation=new Vector<Hashtable<String,Boolean>>(filenames.size());
		
		for(int i=0;i<filenames.size();i++)
		{
			System.out.println("\tloading gazzetteer:...."+filenames.elementAt(i));
			dictionaries.addElement(new Hashtable<String,Boolean>());
			dictionariesIgnoreCase.addElement(new Hashtable<String,Boolean>());
			dictionariesOneWordIgnorePunctuation.addElement(new Hashtable<String,Boolean>());
			InFile in=new InFile(filenames.elementAt(i));
			String line=in.readLine();
			while(line!=null){
				dictionaries.elementAt(i).put(line,true);
				if(!line.equalsIgnoreCase("in")&&!line.equalsIgnoreCase("on")&&!line.equalsIgnoreCase("us")&&!line.equalsIgnoreCase("or")&&!line.equalsIgnoreCase("am")) {
                    dictionariesIgnoreCase.elementAt(i).put(line.toLowerCase(),true);
                }
				StringTokenizer st=new StringTokenizer(line," ");
				while(st.hasMoreTokens()){
                    String s = ws.palladian.external.lbj.StringStatisticsUtils.MyString.cleanPunctuation(st.nextToken());
					if(s.length()>=5&&Character.isUpperCase(s.charAt(0))){
						dictionariesOneWordIgnorePunctuation.elementAt(i).put(s,true);
					}
				}
				line=in.readLine();
			}
			in.close();
		}
		System.out.println("found "+dictionaries.size()+" gazetteers");
	}
	
	public static void annotate(NEWord w)
	{
		w.gazetteers=new Vector<String>();
		
	       	for(int j=0;j<dictionaries.size();j++)
		{
            if (dictionariesOneWordIgnorePunctuation.elementAt(j).containsKey(
                    ws.palladian.external.lbj.StringStatisticsUtils.MyString.cleanPunctuation(w.form))) {
			    //BE CAREFULE WITH THE "PART-" PREFIX! IT'S USED ELSEWHERE!!!
				w.gazetteers.addElement("Part-"+dictNames.elementAt(j));
			}
		}
	       
		NEWord start=w;
		NEWord endWord=(NEWord)w.next;
		String expression=w.form;
		boolean changeEnd=true;
		for(int i=0;i<5&&changeEnd;i++)
		{
			changeEnd=false;
			for(int j=0;j<dictionaries.size();j++)
			{
				if(dictionaries.elementAt(j).containsKey(expression))
				{
					NEWord temp=start;
					if(temp.gazetteers==null) {
                        temp.gazetteers=new Vector<String>();
                    }
					if(i==0){
						temp.gazetteers.addElement("U-"+dictNames.elementAt(j));
					}
					else{
						int loc=0;
						while(temp!=endWord){
							if(temp.gazetteers==null){
								temp.gazetteers=new Vector<String>();
							}
							if(loc==0){
								temp.gazetteers.addElement("B-"+dictNames.elementAt(j));
								temp.matchedMultiTokenGazEntries.addElement(expression);
								temp.matchedMultiTokenGazEntryTypes.addElement("B-"+dictNames.elementAt(j));
							}
							if(loc>0&&loc<i){
								temp.gazetteers.addElement("I-"+dictNames.elementAt(j));
								temp.matchedMultiTokenGazEntries.addElement(expression);
								temp.matchedMultiTokenGazEntryTypes.addElement("I-"+dictNames.elementAt(j));
							}
							if(loc==i){
								temp.gazetteers.addElement("L-"+dictNames.elementAt(j));
								temp.matchedMultiTokenGazEntries.addElement(expression);
								temp.matchedMultiTokenGazEntryTypes.addElement("L-"+dictNames.elementAt(j));
							}
							temp=(NEWord)temp.next;
							loc++;
						}
					}
				}
				if(dictionariesIgnoreCase.elementAt(j).containsKey(expression.toLowerCase()))
				{
					NEWord temp=start;
					if(temp.gazetteers==null) {
                        temp.gazetteers=new Vector<String>();
                    }
					if(i==0){
						temp.gazetteers.addElement("U-"+dictNames.elementAt(j)+"(IC)");
					}
					else{
						int loc=0;
						while(temp!=endWord){
							if(temp.gazetteers==null){
								temp.gazetteers=new Vector<String>();
							}
							if(loc==0){
								temp.gazetteers.addElement("B-"+dictNames.elementAt(j)+"(IC)");
								temp.matchedMultiTokenGazEntriesIgnoreCase.addElement(expression.toLowerCase());
								temp.matchedMultiTokenGazEntryTypesIgnoreCase.addElement("B-"+dictNames.elementAt(j)+"(IC)");
							}
							if(loc>0&&loc<i){
								temp.gazetteers.addElement("I-"+dictNames.elementAt(j)+"(IC)");
								temp.matchedMultiTokenGazEntriesIgnoreCase.addElement(expression.toLowerCase());
								temp.matchedMultiTokenGazEntryTypesIgnoreCase.addElement("I-"+dictNames.elementAt(j)+"(IC)");
							}
							if(loc==i){
								temp.gazetteers.addElement("L-"+dictNames.elementAt(j)+"(IC)");
								temp.matchedMultiTokenGazEntriesIgnoreCase.addElement(expression.toLowerCase());
								temp.matchedMultiTokenGazEntryTypesIgnoreCase.addElement("L-"+dictNames.elementAt(j)+"(IC)");
							}
							temp=(NEWord)temp.next;
							loc++;
						}
					}
				}
			} //dictionaries
			if(endWord!=null)
			{
				expression+=" "+endWord.form;
				endWord=(NEWord)endWord.next;
				changeEnd=true;
			}
		} //i
	}

	public static boolean hasPunctuation(String s){
		return s.indexOf('.')>-1||s.indexOf(',')>-1||s.indexOf(':')>-1||s.indexOf(';')>-1||s.indexOf('-')>-1||s.indexOf('/')>-1||s.indexOf('?')>-1||
		s.indexOf('!')>-1||s.indexOf('\\')>-1||s.indexOf('\"')>-1||s.indexOf('`')>-1||s.indexOf('\'')>-1||s.indexOf('[')>-1||s.indexOf(']')>-1||s.indexOf('{')>-1||s.indexOf('}')>-1||s.indexOf('(')>-1||s.indexOf(')')>-1;
	}
}
