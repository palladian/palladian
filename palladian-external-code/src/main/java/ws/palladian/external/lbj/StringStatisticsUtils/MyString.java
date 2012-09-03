package ws.palladian.external.lbj.StringStatisticsUtils;
import java.util.Date;


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
public class MyString {

	public static String cleanPunctuation(String s) {
		StringBuffer res=new StringBuffer(s.length());
		for(int i=0;i<s.length();i++)
		{
			char c=s.charAt(i);
			if(Character.isLetter(c)||Character.isDigit(c))
				res.append(c);
		}
		return res.toString();
	}

    
    /*
     * replace the source with the target within the line
     */
    public static String replaceSubstring(String line,String source,String target){
	int start=0;
	StringBuffer res=new StringBuffer(line.length()*2);
	while(line.indexOf(source,start)>-1){
	    int next=line.indexOf(source,start);
	    res.append(line.substring(start,next)+target);
	    start=next+source.length();
	}
	res.append(line.substring(start,line.length()));
	return res.toString();
    }

    public static String normalizeDigitsForFeatureExtraction(String s){
	String form=s;
	if(MyString.isDate(form))
	    form="*DATE*";
	if(MyString.hasDigits(form))
	    form= MyString.normalizeDigits(form);
        return form;
    }
    
    public static boolean isDate(String s){
	try{
	    Date.parse(replaceSubstring(replaceSubstring(s, "-", "/"),".","/"));
	    return true;
	}catch (Exception e) {
	    return false;
	}
    }
    
    public static String collapseDigits(String s){
	StringBuffer res=new StringBuffer(s.length()*2);
	for(int i=0;i<s.length();i++){
	    if(Character.isDigit(s.charAt(i))){
		while(i<s.length()&&Character.isDigit(s.charAt(i)))
		    i++;
		res.append("*D*");
		if(i<s.length())
		    res.append(s.charAt(i));
	    }
	    else{
		res.append(s.charAt(i));
	    }
	}
	return res.toString();
    }
    
    public static String normalizeDigits(String s){
	StringBuffer res=new StringBuffer(s.length()*2);
	for(int i=0;i<s.length();i++){
	    if(Character.isDigit(s.charAt(i))){
		res.append("*D*");
	    }
	    else{
		res.append(s.charAt(i));
	    }
	}
	return res.toString();
    }

    public static boolean hasDigits(String s){
	for(int i=0;i<s.length();i++)
	    if(Character.isDigit(s.charAt(i)))
		return true;
	return false;
    }
    
}
