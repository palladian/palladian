package ws.palladian.external.lbj.ClassifiersAndUtils;


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

public abstract class Classifier {
	public String methodName=null;
	public int classesN;
	public abstract double[] getPredictionConfidence(Document doc);
	public abstract int classify(Document doc,double thres);
	public abstract String getExtendedFeatures(Document d);
}
