package ws.palladian.external.lbj.StringStatisticsUtils;
import java.util.*;


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
public class CharacteristicObjects {
	int maxSize;
	public Vector<Object> topObjects=new Vector<Object>();
	Vector<Double> topScores=new Vector<Double>();

	public CharacteristicObjects(int capacity)
	{
		maxSize=capacity;
	}
	public void addElement(Object o,double score){
		topObjects.addElement(o);
		topScores.addElement(score);
		if(topObjects.size()>maxSize){
			int minId=0;
			for(int i=0;i<topScores.size();i++)
				if(topScores.elementAt(minId)>topScores.elementAt(i))
					minId=i;
			topScores.removeElementAt(minId);
			topObjects.removeElementAt(minId);
		}
	}
	
	public String toString()
	{
		String res="";
		for(int i=0;i<topScores.size();i++)
			res+=(topObjects.elementAt(i).toString()+ "\t-\t"+topScores.elementAt(i)+"\n");
		return res;
	}
}
