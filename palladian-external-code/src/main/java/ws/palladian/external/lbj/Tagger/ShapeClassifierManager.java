package ws.palladian.external.lbj.Tagger;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import lbj.NETypeTagger;

import ws.palladian.external.lbj.IO.InFile;
import ws.palladian.external.lbj.IO.Keyboard;
import LBJ2.classify.Classifier;
import LBJ2.classify.ScoreSet;
import LBJ2.nlp.Word;
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

public class ShapeClassifierManager {
	
	public static NETypeTagger shapeClassifier=new NETypeTagger();
	public final static int minWordLen=4;//the minimum word length we're willing to run the shape classifier on
	
	public static Vector randomize(Vector all,int sampleSize){
		System.out.println("Randomizing...");
		Vector res=new Vector(sampleSize);
		boolean[] used=new boolean[sampleSize]; 
		while(res.size()<sampleSize){
			int pos=(int)(Math.random()*all.size());
			if(pos>=all.size()) {
                pos=all.size()-1;
            }
			res.addElement(all.elementAt(pos));
		}
		return res;
	}
	
	public static Vector<NEWord> getEntities(String pathToFile,int numWordsToRead,String label){
		System.out.println("Getting entitites for "+label);
		Vector<String> all=new Vector<String>(numWordsToRead*10);
		InFile in=new InFile(pathToFile);
		String line=in.readLine();
		while(line!=null){
			if(line.indexOf(':')==-1&&line.indexOf('(')==-1&&line.indexOf('(')==-1){
				StringTokenizer st=new StringTokenizer(line);
				while(st.hasMoreTokens())
				{
					String s=st.nextToken();
					if(s.length()>=minWordLen) {
                        all.addElement(s);
                    }
				}
			}
			line=in.readLine();
		}
		System.out.println(all.size()+" words of type "+label + " are available for shape recognition learning");
		all=randomize(all,numWordsToRead);
		Vector<NEWord> res=new Vector<NEWord>();
		for(int i=0;i<numWordsToRead&&i<all.size();i++){
			NEWord w=new NEWord(new Word(all.elementAt(i)),null,label);
			w.parts=new String[0];
			res.addElement(w);
		}
		return res;
	}
	
	public static void trainLocalTagger(){
		shapeClassifier.forget();
		int size=200000;
		shapeClassifier.forget();
		Vector<NEWord> pers=getEntities("Data/KnownLists/WikiPeople.lst",size,"PER");
		Vector<NEWord> locs=getEntities("Data/KnownLists/WikiLocations.lst",size,"LOC");
		Vector<NEWord> orgs=getEntities("Data/KnownLists/WikiOrganizations.lst",size,"ORG");
		 
		Vector<NEWord> all=new Vector<NEWord>(pers.size()+locs.size()+orgs.size());
		for(int i=0;i<size;i++){
			all.addElement(pers.elementAt(i));
			all.addElement(locs.elementAt(i));
			all.addElement(orgs.elementAt(i));
		}
		all=randomize(all,size*3);
		
		//training on the first 9/10 of the docs; 10 rounds
		for(int round=0;round<20;round++){
			System.out.println("Training round "+round);
			for(int i=0;i<all.size()*0.9;i++){
				shapeClassifier.learn(all.elementAt(i));
			}
			//testing on the last 1/10 of thee data
			double correct=0;
			double total=0;
			for(int i=(int)(all.size()*0.9+1);i<all.size();i++){
				total++;
				if(all.elementAt(i).neLabel.equals(shapeClassifier.discreteValue(all.elementAt(i)))) {
                    correct++;
                }
			}			
			System.out.println("Round: "+round+"  local prediction acc: "+correct/total);
		}
		
	}
	
	public static void annotateShapeTagger(Vector<LinkedVector> data){
		for(int i=0;i<data.size();i++){
			for(int j=0;j<data.elementAt(i).size();j++){
				NEWord w=(NEWord)data.elementAt(i).get(j);
				if(w.form.length()>=minWordLen&&Character.isUpperCase(w.form.charAt(0))){
					ScoreSet scores= shapeClassifier.scores(w);
					//Score[] arr=scores.toArray();
					//for(int k=0;k<arr.length;k++)
					//	System.out.println(arr[k].value+" "+arr[k].score);
					//System.out.println("classification: "+shapeClassifier.discreteValue(w));
					w.shapePredPer=scores.get("PER");
					w.shapePredOrg=scores.get("ORG");
					w.shapePredLoc=scores.get("LOC");
					double min=Math.min(Math.min(w.shapePredPer, w.shapePredOrg),w.shapePredLoc);
					double sum=Math.exp(w.shapePredPer-min)+Math.exp(w.shapePredOrg-min)+Math.exp(w.shapePredLoc-min);
					w.shapePredPer=Math.exp(w.shapePredPer-min)/sum;
					w.shapePredOrg=Math.exp(w.shapePredOrg-min)/sum;
					w.shapePredLoc=Math.exp(w.shapePredLoc-min)/sum;					
				} else {
                    w.shapePredPer=w.shapePredOrg=w.shapePredLoc=0;
                }
			}
		}
	}
	
	public static void save(){
		System.out.println("saving shape classifier");
		NETypeTagger.getInstance().binaryWrite("Data/Models/shapeClassifier");
		System.out.println("Done-saving shape classifier");
	}

	public static void load(){
		System.out.println("loading shape classifier");
		shapeClassifier=(NETypeTagger)Classifier.binaryRead("Data/Models/shapeClassifier");
		System.out.println("Done loading shape classifier");
	}
	
	public static void main(String[] args) throws IOException{
		load();
		String s="";
		while(!s.equals("quit")){
			System.out.println("Enter NER");
			s=Keyboard.readLine();
			NEWord w=new NEWord(new Word(s),null,"unlabeled");
			LinkedVector v=new LinkedVector();
			v.add(w);
			Vector<LinkedVector> v2=new Vector<LinkedVector>();
			v2.addElement(v);
			annotateShapeTagger(v2);
			System.out.println("\tPer: "+ w.shapePredPer+"\n\tOrg: "+w.shapePredOrg+"\n\tLoc: "+w.shapePredLoc);
		}
	}
}
