package maui.main;

import java.sql.SQLException;
import java.util.* ;
import java.text.* ;

import org.wikipedia.miner.model.Anchor;
import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.util.SortedVector;

public class Context {
	
	HashMap<String, Double> cachedRelatedness = new HashMap<String, Double>() ;
	
	Vector<Article> contextArticles ;

	public Context() {
		contextArticles = new Vector<Article>() ;
	}
	
	public void addSense(Anchor.Sense sense) {
		contextArticles.add(sense) ;
	}


	public String toString() {
		String result = "";
		for (Article a : contextArticles) {
			result += a + "\n";
		}
		return result;
	}
	
	

	public double getRelatednessTo(Article art) throws SQLException {

		double relatedness = 0 ;

		for (Article contextArt: contextArticles) 
			relatedness = relatedness + art.getRelatednessTo(contextArt) ;	

		return relatedness / contextArticles.size() ;
	}
	
	private boolean isDate(Article art) {
		SimpleDateFormat sdf = new SimpleDateFormat("MMMM d") ;
		Date date = null ;
		
		try {
			date = sdf.parse(art.getTitle()) ;
		} catch (ParseException e) {
			return false ;
		}

		return (date != null) ;		
	}
}

