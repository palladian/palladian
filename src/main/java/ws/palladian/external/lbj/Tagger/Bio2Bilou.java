package ws.palladian.external.lbj.Tagger;


import java.util.Hashtable;
import java.util.Vector;

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

public class Bio2Bilou {
	public static void Bio2BilouLabels(Vector<LinkedVector> data){
		for(int i=0;i<data.size();i++){
			LinkedVector v=data.elementAt(i);
			for(int j=0;j<v.size();j++){
				NEWord w=(NEWord)v.get(j);
				if(!w.neLabel.equalsIgnoreCase("O")){
					String label=w.neLabel.substring(2);
					NEWord prev=(NEWord)w.previous;
					NEWord next=(NEWord)w.next;
					String nextLabel="null";
					String prevLabel="null";
					if(next!=null) {
                        nextLabel=next.neLabel;
                    }
					if(nextLabel.indexOf("-")>-1) {
                        nextLabel=nextLabel.substring(2);
                    }
					if(prev!=null) {
                        prevLabel=prev.neLabel;
                    }
					if(prevLabel.indexOf("-")>-1) {
                        prevLabel=prevLabel.substring(2);
                    }
					if(!nextLabel.equalsIgnoreCase(label)&&!prevLabel.equalsIgnoreCase(label)) {
                        w.neLabel="U-"+label;
                    }
					if(!nextLabel.equalsIgnoreCase(label)&&prevLabel.equalsIgnoreCase(label)) {
                        w.neLabel="L-"+label;
                    }
				}
			}
		}
	}

	public static void Bio2BilouPredictionsLevel1(Vector<LinkedVector> data){
		for(int i=0;i<data.size();i++){
			LinkedVector v=data.elementAt(i);
			for(int j=0;j<v.size();j++){
				NEWord w=(NEWord)v.get(j);
				if(w.neTypeLevel1!=null&&!w.neTypeLevel1.equalsIgnoreCase("O")){
					String label=w.neTypeLevel1.substring(2);
					NEWord prev=(NEWord)w.previous;
					NEWord next=(NEWord)w.next;
					String nextLabel="null";
					String prevLabel="null";
					if(next!=null) {
                        nextLabel=next.neTypeLevel1;
                    }
					if(nextLabel.indexOf("-")>-1) {
                        nextLabel=nextLabel.substring(2);
                    }
					if(prev!=null) {
                        prevLabel=prev.neTypeLevel1;
                    }
					if(prevLabel.indexOf("-")>-1) {
                        prevLabel=prevLabel.substring(2);
                    }
					if(!nextLabel.equalsIgnoreCase(label)&&!prevLabel.equalsIgnoreCase(label)) {
                        w.neTypeLevel1="U-"+label;
                    }
					if(!nextLabel.equalsIgnoreCase(label)&&prevLabel.equalsIgnoreCase(label)) {
                        w.neTypeLevel1="L-"+label;
                    }
				}
			}
		}
	}

	public static void Bio2BilouPredictionsLevel2(Vector<LinkedVector> data){
		for(int i=0;i<data.size();i++){
			LinkedVector v=data.elementAt(i);
			for(int j=0;j<v.size();j++){
				NEWord w=(NEWord)v.get(j);
				if(w.neTypeLevel2!=null&&!w.neTypeLevel2.equalsIgnoreCase("O")){
					String label=w.neTypeLevel2.substring(2);
					NEWord prev=(NEWord)w.previous;
					NEWord next=(NEWord)w.next;
					String nextLabel="null";
					String prevLabel="null";
					if(next!=null) {
                        nextLabel=next.neTypeLevel2;
                    }
					if(nextLabel.indexOf("-")>-1) {
                        nextLabel=nextLabel.substring(2);
                    }
					if(prev!=null) {
                        prevLabel=prev.neTypeLevel2;
                    }
					if(prevLabel.indexOf("-")>-1) {
                        prevLabel=prevLabel.substring(2);
                    }
					if(!nextLabel.equalsIgnoreCase(label)&&!prevLabel.equalsIgnoreCase(label)) {
                        w.neTypeLevel2="U-"+label;
                    }
					if(!nextLabel.equalsIgnoreCase(label)&&prevLabel.equalsIgnoreCase(label)) {
                        w.neTypeLevel2="L-"+label;
                    }
				}
			}
		}
	}

	public static void Bilou2BioLabels(Vector<LinkedVector> data){
		for(int i=0;i<data.size();i++){
			LinkedVector v=data.elementAt(i);
			for(int j=0;j<v.size();j++){
				NEWord w=(NEWord)v.get(j);
				if(!w.neLabel.equalsIgnoreCase("O")){
					if(w.neLabel.startsWith("U-")) {
                        w.neLabel="B-"+w.neLabel.substring(2);
                    }
					if(w.neLabel.startsWith("L-")) {
                        w.neLabel="I-"+w.neLabel.substring(2);
                    }
				}
			}
		}	
	}


	public static void bilou2BioPredictionsLevel1(Vector<LinkedVector> data){
		for(int i=0;i<data.size();i++){
			LinkedVector v=data.elementAt(i);
			for(int j=0;j<v.size();j++){
				NEWord w=(NEWord)v.get(j);
				if(w.neTypeLevel1!=null&&!w.neTypeLevel1.equalsIgnoreCase("O")){
					if(w.neTypeLevel1.startsWith("U-")) {
                        w.neTypeLevel1="B-"+w.neTypeLevel1.substring(2);
                    }
					if(w.neTypeLevel1.startsWith("L-")) {
                        w.neTypeLevel1="I-"+w.neTypeLevel1.substring(2);
                    }
				}
			}
		}	
	}

	public static void Bilou2BioPredictionsLevel2(Vector<LinkedVector> data){
		for(int i=0;i<data.size();i++){
			LinkedVector v=data.elementAt(i);
			for(int j=0;j<v.size();j++){
				NEWord w=(NEWord)v.get(j);
				if(w.neTypeLevel2!=null&&!w.neTypeLevel2.equalsIgnoreCase("O")){
					if(w.neTypeLevel2.startsWith("U-")) {
                        w.neTypeLevel2="B-"+w.neTypeLevel2.substring(2);
                    }
					if(w.neTypeLevel2.startsWith("L-")) {
                        w.neTypeLevel2="I-"+w.neTypeLevel2.substring(2);
                    }
				}
			}
		}	
	}


	public static void main(String[] args){
		Parameters.tokenizationScheme=Parameters.DualTokenizationScheme;// should be either LbjTokenizationScheme or DualTokenizationScheme
		Parameters.taggingScheme=Parameters.BILOU;// should be either BIO or BILOU		
		Parameters.featuresToUse=new Hashtable<String, Boolean>();

		Reuters2003Parser parser = new Reuters2003Parser("Data/GoldData/Reuters/BIO.testa");
		Vector<LinkedVector> testData1 = parser.readAndAnnotate();
		parser = new Reuters2003Parser("Data/GoldData/Reuters/BIO.testa");
		Vector<LinkedVector> testData2 = parser.readAndAnnotate();
		Bio2BilouLabels(testData1);
		Bilou2BioLabels(testData1);
		for(int i=0;i<testData1.size();i++){
			for(int j=0;j<testData1.elementAt(i).size();j++){
				NEWord w1=(NEWord)testData1.elementAt(i).get(j);
				NEWord w2=(NEWord)testData2.elementAt(i).get(j);
				System.out.println(w1.form+"\t"+w1.neLabel+"\t"+w2.neLabel);
				if(!w1.neLabel.equals(w2.neLabel)){
					System.out.println("Bug with BIO to BILOU & back to BIO conversion");
					System.exit(0);
				}
			}
			System.out.println("");
		}
	}
}
