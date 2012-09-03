package ws.palladian.external.lbj.Tagger;

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

public class BrownClusters {
	public static Hashtable<String,String> wordToPath=null;
	public static final int[] prefixLengths ={4,6,10,20}; 
	
	public static void init(){
		wordToPath=new Hashtable<String, String>();
        InFile in = new InFile("data/models/illinoisner/data/BrownHierarchicalWordClusters/brownBllipClusters");
		String line=in.readLine();
		int wordsAdded=0;
		while(line!=null){
			StringTokenizer st=new StringTokenizer(line);
			String path=st.nextToken();
			String word=st.nextToken();
			int occ=Integer.parseInt(st.nextToken());
			if(occ>=5){
				wordToPath.put(word,path);
				//System.out.println(word);
				wordsAdded++;
			}
			line=in.readLine();
		}
		System.out.println(wordsAdded+" words added");
	}	
	
	public static String[] getPrefixes(String word){
		if(wordToPath==null||!wordToPath.containsKey(word)) {
            return new String[0];
        }
		Vector<String> v = new Vector<String>();
		String path=wordToPath.get(word);
		v.addElement(path.substring(0,Math.min(path.length(), prefixLengths[0])));
		for(int i=1;i<prefixLengths.length;i++) {
            if(prefixLengths[i-1]<path.length()) {
                v.addElement(path.substring(0,Math.min(path.length(), prefixLengths[i])));
            }
        }
		String[] res=new String[v.size()];
		for(int i=0;i<v.size();i++) {
            res[i]=v.elementAt(i);
        }
		return res;
	}
	
	public static void printArr(String[] arr){
		for(int i=0;i<arr.length;i++) {
            System.out.print(" "+arr[i]);
        }
		System.out.println("");
	}
	
	public static void main(String[] args){
		init();
		System.out.println("finance ");
		printArr(getPrefixes("finance"));
		System.out.println("help ");
		printArr(getPrefixes("help"));
		System.out.println("resque ");
		printArr(getPrefixes("resque"));
		System.out.println("assist ");
		printArr(getPrefixes("assist"));
		System.out.println("assistance ");
		printArr(getPrefixes("assistance"));
		System.out.println("guidance ");
		printArr(getPrefixes("guidance"));
	}
}
