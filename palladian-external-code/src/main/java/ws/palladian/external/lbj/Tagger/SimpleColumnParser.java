package ws.palladian.external.lbj.Tagger;

import java.util.Vector;

import LBJ2.nlp.ColumnFormat;
import LBJ2.nlp.SentenceSplitter;
import LBJ2.nlp.Word;
import LBJ2.nlp.WordSplitter;
import LBJ2.parse.LinkedVector;
import LBJ2.parse.Parser;


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
public class SimpleColumnParser extends ColumnFormat
{
  /**
    * Constructs this parser to parse the given file.
    *
    * @param file The name of the file to parse.
   **/
	
	String filename=null;
  public SimpleColumnParser(String file) { super(file);filename=file; }


  /**
    * Produces the next object parsed from the input file; in this case, that
    * object is guaranteed to be a <code>LinkedVector</code> populated by
    * <code>NEWord</code>s representing a sentence.
   **/
  /**
   * Produces the next object parsed from the input file; in this case, that
   * object is guaranteed to be a <code>LinkedVector</code> populated by
   * <code>Token</code>s representing a sentence.
  **/
 @Override
public Object next()
 {	 
	   String[] line = (String[]) super.next();
        while (line != null && line.length == 0) {
        line = (String[]) super.next();
    }
	   if (line == null) {
        return null;
    }

	   LinkedVector res=new LinkedVector(); 
        NEWord w = new NEWord(new Word(line[0]), null, line[1]);
	   Vector<NEWord> v=splitWord(w);
	   
	   if(Parameters.tokenizationScheme.equalsIgnoreCase(Parameters.DualTokenizationScheme)){
		   w.parts=new String[v.size()];
		   for(int i=0;i<v.size();i++) {
            w.parts[i]=v.elementAt(i).form;
        }
		   res.add(w);
	   }
	   else{
		  if(Parameters.tokenizationScheme.equalsIgnoreCase(Parameters.LbjTokenizationScheme)){
			  for(int j=0;j<v.size();j++) {
                res.add(v.elementAt(j));
            }
		  }
		  else{
			  System.out.println("Fatal error in BracketFileManager.readAndAnnotate - unrecognized tokenization scheme: "+Parameters.tokenizationScheme);
			  System.exit(0);
		  }					  
	   }
	   
	   for (line = (String[]) super.next(); line != null && line.length > 0;
	        line = (String[]) super.next())
	   {

            int lastIndex = line.length - 1;

            // the content before the tag
            String tokenContent = "";
            for (int i = 0; i < lastIndex; i++) {
                if (i > 0) {
                    tokenContent += " ";
                }
                tokenContent += line[i];
            }

            w = new NEWord(new Word(tokenContent), null, line[lastIndex]);
		   v=splitWord(w);
		   if(Parameters.tokenizationScheme.equalsIgnoreCase(Parameters.DualTokenizationScheme)){
			   w.parts=new String[v.size()];
			   for(int i=0;i<v.size();i++) {
                w.parts[i]=v.elementAt(i).form;
            }
			   res.add(w);
		   }
		   else{
			   if(Parameters.tokenizationScheme.equalsIgnoreCase(Parameters.LbjTokenizationScheme)){
				   for(int j=0;j<v.size();j++) {
                    res.add(v.elementAt(j));
                }				   
			   }else{
				   System.out.println("Fatal error in BracketFileManager.readAndAnnotate - unrecognized tokenization scheme: "+Parameters.tokenizationScheme);	
				   System.exit(0);				   
			   }
		   }
	   }
	   if(res.size()==0) {
        return null;
    }
	      
	   return res;
 }



  public static Vector<NEWord> splitWord(NEWord word){
	  //System.out.println("------lala: "+word.form+" ");
	  String[] sentence={word.form+" "};
	  Parser parser = new WordSplitter(new SentenceSplitter(sentence));
	  LinkedVector words=(LinkedVector) parser.next();
	  Vector<NEWord> res=new Vector<NEWord>(); 
	  String label=word.neLabel;
	  for(int i=0;i<words.size();i++){
		  if(label.indexOf("B-")>-1&&i>0) {
            label="I-"+label.substring(2);
        }
		  NEWord w=new NEWord(new Word(((Word)words.get(i)).form),null,label);
		  w.originalSpelling=word.form;
		  res.addElement(w);
	  }
	  // for(int i=0;i<words.size();i++)
		//   System.out.println(((NEWord)res.elementAt(i)));
	  return res;
  }

  
  public Vector<LinkedVector> readAndAnnotate(){
	    System.out.println("Reading and annotating the file: "+fileName);
	    Vector<LinkedVector> res=new Vector<LinkedVector>();
	    for (LinkedVector vector = (LinkedVector) next(); vector != null; vector = (LinkedVector) next()) {
            res.addElement(vector);
        }
	    annotate(res);
	    System.out.println("Done reading and annotating the corpus");
	    return res;
  }
  
  public static void annotate(Vector<LinkedVector> res){
	    
	    //fixing skips across sentence boundaries, for the nonlocal features;
	    for(int i=0;i<res.size();i++){
	    	LinkedVector vector=res.elementAt(i);
			if(Parameters.featuresToUse!=null){
	  			if(Parameters.featuresToUse.containsKey("GazetteersFeatures")){
	  				for(int j=0;j<vector.size();j++) {
                        Gazzetteers.annotate((NEWord)vector.get(j));
                    }
	  			}
			}
  		}
	    for(int i=0;i<res.size();i++)
	    {
	    	for(int j=0;j<res.elementAt(i).size();j++){
	    		NEWord w=(NEWord)res.elementAt(i).get(j);
	    		w.previousIgnoreSentenceBoundary=(NEWord)w.previous;
	    		w.nextIgnoreSentenceBoundary=(NEWord)w.next;
	    	}
	    	if(i>0&&res.elementAt(i).size()>0)
	    	{
	    		NEWord w=(NEWord)res.elementAt(i).get(0);
	    		w.previousIgnoreSentenceBoundary=(NEWord)res.elementAt(i-1).get(res.elementAt(i-1).size()-1);
	    	}
	    	if(i<res.size()-1&&res.elementAt(i).size()>0)
	    	{
	    		NEWord w=(NEWord)res.elementAt(i).get(res.elementAt(i).size()-1);
	    		w.nextIgnoreSentenceBoundary=(NEWord)res.elementAt(i+1).get(0);
	    	}
	    }
	    //annotating the nonlocal features;
	    for(int i=0;i<res.size();i++) {
            for(int j=0;j<res.elementAt(i).size();j++) {
                GlobalFeatures.annotate((NEWord)res.elementAt(i).get(j));
            }
        }
	    
	    //annotating shape classifiactions
		if(Parameters.featuresToUse.containsKey("NEShapeTaggerFeatures")) {
            ShapeClassifierManager.annotateShapeTagger(res);
        }
  }
}

