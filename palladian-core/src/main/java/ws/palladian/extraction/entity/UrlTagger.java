package ws.palladian.extraction.entity;

import java.util.regex.Matcher;

import ws.palladian.helper.UrlHelper;

/**
 * <p>
 * Tag URLs in a text.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class UrlTagger {

	/** The tag name for URLs. */
	public static final String URI_TAG_NAME = "URI";

	public Annotations tagUrls(String inputText) {

		Annotations annotations = new Annotations();
		
		Matcher matcher = UrlHelper.URL_PATTERN.matcher(inputText);

		while (matcher.find()) {
			Annotation annotation = new Annotation(matcher.start(),matcher.group(0),URI_TAG_NAME);
			annotations.add(annotation);
		}

		return annotations;
	}

}