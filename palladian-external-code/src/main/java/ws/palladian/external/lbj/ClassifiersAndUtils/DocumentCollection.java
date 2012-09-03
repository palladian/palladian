package ws.palladian.external.lbj.ClassifiersAndUtils;


import java.io.File;
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

public class DocumentCollection {
	public Vector<Document> docs=new Vector<Document>();
	
	public DocumentCollection(){
	}
	public void addDoc(Document doc){
		docs.addElement(doc);
	}
	public void addDocuments(Vector<Document> _docs)
	{
		for(int i=0;i<_docs.size();i++) {
            this.docs.addElement(_docs.elementAt(i));
        }
	}	
	/*
	 * This code assumes each line in a file contains a new document
	 */
	public void addDocuments(String filename,int classID,StopWords stops,boolean discardFirstToken){
		InFile in=new InFile(filename);
		Vector<String> words=in.readLineTokens();
		if(discardFirstToken&&words!=null&&words.size()>0) {
            words.removeElementAt(0);
        }
		if(stops!=null) {
            words=stops.filterStopWords(words);
        }
		while(words!=null)
		{
			if(words.size()>=0) {
                docs.addElement(new Document(words,classID));
            }
			words=in.readLineTokens();
			if(discardFirstToken&&words!=null&&words.size()>0) {
                words.removeElementAt(0);
            }
			if(stops!=null) {
                words=stops.filterStopWords(words);
            }
		}
	}
	
	/*
	 * This format assumes that the folder contains a bunch of files.
	 * each files is a single doc
	 */
	public void addFolder(String path,int classID,StopWords stops,boolean discardFirstToken){
		String[] files=new File(path).list();
		for(int i=0;i<files.length;i++)
		{
			InFile in=new InFile(path+"/"+files[i]);
			Vector<String> allWords=new Vector<String>();
			Vector<String> words=in.readLineTokens();
			if(discardFirstToken&&words!=null&&words.size()>0) {
                words.removeElementAt(0);
            }
			if(stops!=null) {
                words=stops.filterStopWords(words);
            }
			while(words!=null)
			{
				for(int j=0;j<words.size();j++) {
                    allWords.addElement(words.elementAt(j));
                }
				words=in.readLineTokens();
				if(discardFirstToken&&words!=null&&words.size()>0) {
                    words.removeElementAt(0);
                }
				if(stops!=null) {
                    words=stops.filterStopWords(words);
                }
			}
			docs.addElement(new Document(allWords,classID));
		}
	}

	public void tokenize()
	{
		for(int i=0;i<docs.size();i++) {
            docs.elementAt(i).tokenize();
        }
	}
}
