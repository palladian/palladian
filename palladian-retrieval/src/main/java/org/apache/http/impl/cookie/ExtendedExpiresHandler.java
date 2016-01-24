package org.apache.http.impl.cookie;

import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.CommonCookieAttributeHandler;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;
import org.apache.http.impl.cookie.AbstractCookieAttributeHandler;
import org.apache.http.util.Args;

import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;

final class ExtendedExpiresHandler extends AbstractCookieAttributeHandler implements CommonCookieAttributeHandler {

	public static final ExtendedExpiresHandler INSTANCE = new ExtendedExpiresHandler();

	private ExtendedExpiresHandler() {
		// singleton
	}

	@Override
	public void parse(final SetCookie cookie, final String value) throws MalformedCookieException {
		Args.notNull(cookie, "Cookie");
		if (value == null) {
			throw new MalformedCookieException("Missing value for 'expires' attribute");
		}
		ExtractedDate parsedDate = DateParser.parseDate(value);
		if (parsedDate == null) {
			throw new MalformedCookieException("Could not parse 'expires' attribute: " + value);
		}
		cookie.setExpiryDate(parsedDate.getNormalizedDate());
	}

	@Override
	public String getAttributeName() {
		return ClientCookie.EXPIRES_ATTR;
	}

}
