package ws.palladian.external.lbj.Tagger;


import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import ws.palladian.external.lbj.StringStatisticsUtils.OccurrenceCounter;
import LBJ2.nlp.Word;


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

public class NEWord extends Word
{
	

	
  /** This field is used to store a computed named entity type tag. */
  public String neTypeLevel1;
  public String neTypeLevel2;
  /** This field stores the named entity type tag found in labeled data. */
  public String neLabel;
  
  public String originalSpelling="";
  public String[] parts;

  public double shapePredPer=0;
  public double shapePredOrg=0;
  public double shapePredLoc=0;
  
  
  public Vector<String> gazetteers; 
    public Vector<String> matchedMultiTokenGazEntries=new Vector<String>();
    public Vector<String> matchedMultiTokenGazEntryTypes=new Vector<String>();//this is the name of the dictionary rather than entity type!
    public Vector<String> matchedMultiTokenGazEntriesIgnoreCase=new Vector<String>();
    public Vector<String> matchedMultiTokenGazEntryTypesIgnoreCase=new Vector<String>();//this is the name of the dictionary rather than entity type!
    public OccurrenceCounter gazetteerMatchAggregationFeatures=new OccurrenceCounter();
  


  public Hashtable<String,Integer> nonLocalFeatures=new Hashtable<String,Integer>();
  private String[] nonLocFeatArray=null;
  public NEWord nextIgnoreSentenceBoundary=null;
  public NEWord previousIgnoreSentenceBoundary=null;
  public OccurrenceCounter activePatterns=new OccurrenceCounter();
  public OccurrenceCounter mostFrequentLevel1Prediction=new OccurrenceCounter();
  public OccurrenceCounter mostFrequentLevel1PredictionType=new OccurrenceCounter();
  public OccurrenceCounter mostFrequentLevel1NotOutsidePrediction=new OccurrenceCounter();
  public OccurrenceCounter mostFrequentLevel1NotOutsidePredictionType=new OccurrenceCounter();
  
  public String entity;
  public String entityType;
  public OccurrenceCounter mostFrequentLevel1TokenInEntityType=new OccurrenceCounter();
  public OccurrenceCounter mostFrequentLevel1ExactEntityType=new OccurrenceCounter();
  public OccurrenceCounter mostFrequentLevel1SuperEntityType=new OccurrenceCounter();

  

  /**
    * An <code>NEWord</code> can be constructed from a <code>Word</code>
    * object representing the same word, an <code>NEWord</code> representing
    * the previous word in the sentence, and the named entity type label found
    * in the data.
    *
    * @param w    Represents the same word as the <code>NEWord</code> being
    *             constructed.
    * @param p    The previous word in the sentence.
    * @param type The named entity type label for this word from the data.
   **/
  public NEWord(Word w, NEWord p, String type)
  {
    super(w.form, w.partOfSpeech, w.lemma, w.wordSense, p, w.start, w.end);
    neLabel = type;
    neTypeLevel1=null;
  }


  /**
    * Produces a simple <code>String</code> representation of this word in
    * which the <code>neLabel</code> field appears followed by the word's part
    * of speech and finally the form (i.e., spelling) of the word all
    * surrounded by parentheses.
   **/
  @Override
public String toString()
  {
    return "(" + neLabel + " " + partOfSpeech + " " + form + ")";
  }
  
  public String[] getAllNonlocalFeatures(){
	  if(nonLocFeatArray==null){
		  Vector<String> v=new Vector<String>();
		  for(Iterator<String> i=nonLocalFeatures.keySet().iterator();i.hasNext();v.addElement(i.next())) {
            ;
        }
		  nonLocFeatArray=new String[v.size()];
		  for(int i=0;i<v.size();i++) {
            nonLocFeatArray[i]=v.elementAt(i);
        }
	  }
	  return nonLocFeatArray;
  }
  
  public int getNonLocFeatCount(String nonLocFeat)
  {
	  return nonLocalFeatures.get(nonLocFeat).intValue();
  }
}

