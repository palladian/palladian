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

public class StopWords {
	public Hashtable<String, Boolean> h=new Hashtable<String, Boolean>();
	
	public StopWords(String filename)
	{
		InFile in=new InFile(filename);
		Vector<String> words=in.readLineTokens();
		while(words!=null)
		{
			for(int i=0;i<words.size();i++) {
                this.h.put(words.elementAt(i).toLowerCase(), true);
            }
			words=in.readLineTokens();
		}
	}
	
	public Vector<String> filterStopWords(Vector<String> words)
	{
		if(words==null) {
            return null;
        }
		Vector<String> res=new Vector<String>();
		for(int i=0;i<words.size();i++) {
            if(!h.containsKey(words.elementAt(i).toLowerCase())) {
                res.addElement(words.elementAt(i));
            }
        }
		return res;
	}
	
	public boolean isStopWord(String s){
		return h.containsKey(s.toLowerCase());
	}
	
	public Vector<String> extractStopWords(Vector<String> words)
	{
		if(words==null) {
            return null;
        }
		Vector<String> res=new Vector<String>();
		for(int i=0;i<words.size();i++) {
            if(h.containsKey(words.elementAt(i).toLowerCase())) {
                res.addElement(words.elementAt(i));
            }
        }
		return res;
	}
	
}
