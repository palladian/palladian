package ws.palladian.external.lbj.ClassifiersAndUtils;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import ws.palladian.external.lbj.IO.InFile;
import ws.palladian.external.lbj.IO.OutFile;




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

public class FeatureMap {
	public Hashtable<String, Integer> wordToFid=new Hashtable<String, Integer>();
	public Hashtable<Integer,String > fidToWord=new Hashtable<Integer,String>();
	public int dim=0;
	
	public FeatureMap(){
		wordToFid=new Hashtable<String, Integer>();
		fidToWord=new Hashtable<Integer,String>();	
		dim=0;
	}
	
	public void save(String file){
		OutFile out=new OutFile(file);
		out.println(String.valueOf(dim));
		for(Iterator<String> iter=wordToFid.keySet().iterator();iter.hasNext();){
			String w=iter.next();
			out.println(w);
			out.println(String.valueOf(wordToFid.get(w)));
		}
		out.close();
	}
	
	public  FeatureMap(String filename){
		wordToFid=new Hashtable<String, Integer>();
		fidToWord=new Hashtable<Integer, String>();
		InFile in=new InFile(filename);
		dim=Integer.parseInt(in.readLine());
		for(int i=0;i<dim;i++){
			String w=in.readLine();
			int fid=Integer.parseInt(in.readLine());
			wordToFid.put(w,fid);
			fidToWord.put(fid, w);
		}
		in.close();
	}
	
	public FeatureMap(FeatureMap _map){
		wordToFid=new Hashtable<String, Integer>();
		fidToWord=new Hashtable<Integer,String>();	
		dim=_map.dim;
		for(Iterator<String> iter=_map.wordToFid.keySet().iterator();iter.hasNext();)
		{
			String w=iter.next();
			wordToFid.put(w, _map.wordToFid.get(w));
			fidToWord.put(_map.wordToFid.get(w), w);
		}
			
	}
	public void readFromFile(String countFiles,int thres){
		InFile in=new InFile(countFiles);
		Vector<String> tokens=in.readLineTokens();
		while(tokens!=null){
			int count=Integer.parseInt(tokens.elementAt(0));
			if(count>=thres)
			{
				wordToFid.put(tokens.elementAt(1), dim);
				fidToWord.put(dim, tokens.elementAt(1));
				dim++;
			}
			tokens=in.readLineTokens();
		}
	}
	
	/*
	 * if countRepsWithinDoc is false, we basically require the word to appear in
	 * at least appearanceThres documents
	 */	
	public void addDocs(DocumentCollection docs,int appearanceThres,boolean countRepsWithinDoc)
	{
		UnigramStatistics stat=new UnigramStatistics(docs,countRepsWithinDoc);	
		for(Iterator<String> iter=stat.wordCounts.keySet().iterator();iter.hasNext();)
		{
			String w=iter.next();
			if(stat.wordCounts.get(w)>=appearanceThres)
			{
				wordToFid.put(w, dim);
				fidToWord.put(dim,w);
				dim++;
			}
		}
		/*System.out.println("Building a feature map");
		for(int i=0;i<docs.docs.size();i++)
		{
			Vector<String> words=docs.docs.elementAt(i).words;
			for(int j=0;j<words.size();j++)
				if((!wordToFid.containsKey(words.elementAt(j)))&&
						(stat.wordCounts.get(words.elementAt(j))>=appearanceThres))
				{
					wordToFid.put(words.elementAt(j), dim);
					fidToWord.put(dim,words.elementAt(j));
					dim++;
				}
		}*/			
		System.out.println("Done building a feature map, the dimension is: "+dim);
	}	
	public void addMoreDocsIgnoreAppearanceThres(DocumentCollection docs)
	{
		for(int i=0;i<docs.docs.size();i++)
		{
			Vector<String> words=docs.docs.elementAt(i).words;
			for(int j=0;j<words.size();j++) {
                if(!wordToFid.containsKey(words.elementAt(j)))
				{
					wordToFid.put(words.elementAt(j), dim);
					fidToWord.put(dim,words.elementAt(j));
					dim++;
				}
            }
		}			
		System.out.println("Done adding docs to a feature map, the dimension is: "+dim);
	}	
	
	public void addDimension(String dimensionName){
		if(!wordToFid.containsKey(dimensionName))
		{
			wordToFid.put(dimensionName, dim);
			fidToWord.put(dim,dimensionName);
			dim++;
		}		
	}
}
