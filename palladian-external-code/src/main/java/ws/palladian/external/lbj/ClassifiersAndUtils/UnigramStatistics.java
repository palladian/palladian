package ws.palladian.external.lbj.ClassifiersAndUtils;
import java.util.Hashtable;
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

public class UnigramStatistics {
	public Hashtable<String, Integer> wordCounts=new Hashtable<String, Integer>();
	boolean countRepsWithinDocs=false;
	public int totalWordCount=0;
	/*
	 * if countRepsWithinDoc is false, increase occurrence count
	 * only when the word appears in distince documents
	 */
	public  UnigramStatistics(String filename,FeatureMap map){
		InFile in=new InFile(filename);
		Vector<String> tokens=in.readLineTokens();
		while(tokens!=null){
			for(int i=0;i<tokens.size();i++) {
                if(map.wordToFid.containsKey(tokens.elementAt(i))) {
                    addWord(tokens.elementAt(i));
                }
            }
			tokens=in.readLineTokens();
		}
		in.close();
	}

	public  UnigramStatistics(DocumentCollection docs,boolean _countRepsWithinDocs)
	{
		countRepsWithinDocs=_countRepsWithinDocs;
		System.out.println("Building unigram statistics");
		for(int i=0;i<docs.docs.size();i++)
		{
			addDoc(docs.docs.elementAt(i));
		}			
		System.out.println("Done building unigram statistics");
	}
	/*
	 * if countRepsWithinDoc is false, increase occurrence count
	 * only when the word appears in distince documents
	 */		
	public  UnigramStatistics(boolean _countRepsWithinDocs)
	{
		countRepsWithinDocs=_countRepsWithinDocs;
	}
	
	public void addDoc(Document doc)
	{
		Hashtable<String, Boolean> alreadyAppreared=new Hashtable<String, Boolean>();
		Vector<String> words=doc.words;
		for(int j=0;j<words.size();j++)
		{
			if(countRepsWithinDocs||!alreadyAppreared.containsKey(words.elementAt(j)))
			{
				addWord(words.elementAt(j));
				alreadyAppreared.put(words.elementAt(j), true);
			}
		}		
	}
	
	public void addWord(String w){
		totalWordCount++;
		if(!wordCounts.containsKey(w))
		{
			wordCounts.put(w, 1);
		}
		else
		{
			int count=wordCounts.get(w);
			wordCounts.remove(w);
			wordCounts.put(w, count+1);
		}
	}
}
