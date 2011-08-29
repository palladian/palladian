package ws.palladian.extraction.entity.ner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Tag URLs in a text.
 * </p>
 * 
 * @author David Urbansky
 */
public class UrlTagger {

	/** The tag name for URLs. */
	public static final String URI_TAG_NAME = "URI";
	
	/** The URL regular expression. */
	private static final String URL_REGEXP = "(http://|www\\.).*?(?=\\s|$)";

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
