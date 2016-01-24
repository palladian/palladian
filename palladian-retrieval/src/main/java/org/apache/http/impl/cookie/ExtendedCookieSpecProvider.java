package org.apache.http.impl.cookie;

import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.protocol.HttpContext;

public final class ExtendedCookieSpecProvider implements CookieSpecProvider {
	
	public static ExtendedCookieSpecProvider INSTANCE = new ExtendedCookieSpecProvider();
	
	private ExtendedCookieSpecProvider() {
		// singleton
	}
	
	private static final DefaultCookieSpec COOKIE_SPEC;

	static {
        final RFC2965Spec strict = new RFC2965Spec(false,
                new RFC2965VersionAttributeHandler(),
                new BasicPathHandler(),
                PublicSuffixDomainFilter.decorate(
                        new RFC2965DomainAttributeHandler(), PublicSuffixMatcherLoader.getDefault()),
                new RFC2965PortAttributeHandler(),
                new BasicMaxAgeHandler(),
                new BasicSecureHandler(),
                new BasicCommentHandler(),
                new RFC2965CommentUrlAttributeHandler(),
                new RFC2965DiscardAttributeHandler());
        final RFC2109Spec obsoleteStrict = new RFC2109Spec(false,
                new RFC2109VersionHandler(),
                new BasicPathHandler(),
                PublicSuffixDomainFilter.decorate(
                        new RFC2109DomainHandler(), PublicSuffixMatcherLoader.getDefault()),
                new BasicMaxAgeHandler(),
                new BasicSecureHandler(),
                new BasicCommentHandler());
        final NetscapeDraftSpec netscapeDraft = new NetscapeDraftSpec(
                PublicSuffixDomainFilter.decorate(
                        new BasicDomainHandler(), PublicSuffixMatcherLoader.getDefault()),
                new BasicPathHandler(),
                new BasicSecureHandler(),
                new BasicCommentHandler(),
                ExtendedExpiresHandler.INSTANCE);
        COOKIE_SPEC = new DefaultCookieSpec(strict, obsoleteStrict, netscapeDraft);
	}

	@Override
	public CookieSpec create(HttpContext context) {
		return COOKIE_SPEC;
	}

}
