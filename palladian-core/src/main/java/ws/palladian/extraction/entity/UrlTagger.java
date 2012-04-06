package ws.palladian.extraction.entity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Tag URLs in a text.
 * </p>
 * TODO also recognize "cinefreaks.com" (without www and http)
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class UrlTagger {

	/** The tag name for URLs. */
	public static final String URI_TAG_NAME = "URI";
	
	// http://www.faqs.org/rfcs/rfc1738.html
	
	private static final String DOMAIN_ALLOWED_CHARACTERS = "[^;/?:@=&\\s]";
	private static final String PATH_ALLOWED_CHARACTERS = "[^\\s]";
	private static final String NEGLECTED_ENDINGS = "[.,?!;\\[\\](){}\"\\s]";
	
	// TODO to extend, see http://en.wikipedia.org/wiki/List_of_Internet_top-level_domains
	private static final String TOP_LEVEL_DOMAINS = "(de|com|cc|tv|us|net|org|gov|mil|edu|fr|it|com.au|co.uk|ws)";
	
	/** The URL regular expression. */
//	private static final String URL_REGEXP = "((http://|https://|www.).*?(?=[.,;?!]?(\\s|\\]|\\))|[.,;?!]?$))|([A-Za-z.0-9-]*?\\.(de|com|cc|tv|us|net|org|gov|mil|edu|fr|it|com.au|co.uk)[/A-Za-z0-9-]*(\\.[A-Za-z]{2,5})?)";
//    private static final String URL_REGEXP = "((http://|https://|www.).{0,63}?(?=[.,;?!]?(\\s|\\]|\\))|[.,;?!]?$))|([A-Za-z.0-9-]{0,63}?\\.(de|com|cc|tv|us|net|org|gov|mil|edu|fr|it|com.au|co.uk)[/A-Za-z0-9-\\?=&\\+]{0,1024}(\\.[A-Za-z]{2,5})?)";
//	private static final String URL_REGEXP = "(http(s)?://)?([A-Za-z0-9-.]*?\\.(de|com|cc|tv|us|net|org|gov|mil|edu|fr|it|com.au|co.uk)((/[/A-Za-z0-9-?=&+]*(\\.[A-Za-z?]{2,5})?)|(?=(\\s|\\.|\\)|\\]|\\?|\\!|\\,))))";
//	private static final String URL_REGEXP = "(http(s)?://)?([A-Za-z0-9-.]{0,63}?\\.(de|com|cc|tv|us|net|org|gov|mil|edu|fr|it|com.au|co.uk)((/[/A-Za-z0-9-?=&+]{0,255}(\\.[A-Za-z?]{2,5})?)|(?=(\\s|\\.|\\)|\\]|\\?|\\!|\\,))))";
	private static final String URL_REGEXP = "(http(s)?://)?("+DOMAIN_ALLOWED_CHARACTERS+"{0,63}?\\."+TOP_LEVEL_DOMAINS+"((/"+PATH_ALLOWED_CHARACTERS+"{0,255}(\\.[A-Za-z?]{2,5})?)|(?="+NEGLECTED_ENDINGS+")))";

	private final Pattern urlPattern;

	public UrlTagger() {
		urlPattern = Pattern.compile(URL_REGEXP);
	}

	public Annotations tagUrls(String inputText) {

		Annotations annotations = new Annotations();
		
		Matcher matcher = urlPattern.matcher(inputText);

		while (matcher.find()) {
			Annotation annotation = new Annotation(matcher.start(),matcher.group(0),URI_TAG_NAME,annotations);
			annotations.add(annotation);
		}

		return annotations;
	}

}
