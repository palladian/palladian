package ws.palladian.retrieval.wiki.queries;

/**
 * This file is based on net.sourceforge.jwbf.mediawiki.actions.editing.AllPageTitles, created by Tobias Knerr. All
 * modifications are done by Sandro Reichert
 * 
 * Copyright 2007 Tobias Knerr.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Contributors:
 * Tobias Knerr
 * 
 */

import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_09;
import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_10;
import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_11;
import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_12;
import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_13;
import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_14;
import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_15;
import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_16;

import java.util.Collection;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.jwbf.core.actions.Get;
import net.sourceforge.jwbf.core.actions.util.HttpAction;
import net.sourceforge.jwbf.mediawiki.actions.MediaWiki;
import net.sourceforge.jwbf.mediawiki.actions.queries.TitleQuery;
import net.sourceforge.jwbf.mediawiki.actions.util.RedirectFilter;
import net.sourceforge.jwbf.mediawiki.actions.util.SupportedBy;
import net.sourceforge.jwbf.mediawiki.actions.util.VersionException;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import org.apache.log4j.Logger;

import ws.palladian.retrieval.wiki.data.WikiPage;

/**
 * Action class using the MediaWiki-api's "list=allpages".
 * 
 * @author Tobias Knerr
 * @author Thomas Stock
 * @author Sandro Reichert
 */
@SupportedBy({ MW1_09, MW1_10, MW1_11, MW1_12, MW1_13, MW1_14, MW1_15, MW1_16 })
public class AllPageTitles extends TitleQuery<WikiPage> {

    /** the logger for this class */
    private static final Logger LOG = Logger.getLogger(AllPageTitles.class);

    /** Pattern to parse returned page, @see {@link #parseHasMore(String)}. */
    private static final Pattern HAS_MORE_PATTERN = Pattern.compile(
            "<query-continue>.*?<allpages *apfrom=\"([^\"]*)\" */>.*?</query-continue>", Pattern.DOTALL
            | Pattern.MULTILINE);

    /** Pattern to parse returned page, @see {@link #parseArticleTitles(String)} */
    private static final Pattern TITLE_PATTERN = Pattern
    .compile("<p pageid=\"(.*?)\" ns=\"(.*?)\" title=\"(.*?)\" />");

    /** Constant value for the aplimit-parameter. **/
    private static final int LIMIT = 500;

    /** Information given in the constructor, necessary for creating next action. */
    private String prefix;

    /** The Wiki's namespaceID to get all pages for. */
    private int namespace;

    /** {@link MediaWikiBot} that executes the request. */
    private MediaWikiBot bot;

    /** page title to start from, may be null. */
    private String from;

    /** Defines whether redirect pages should be included or not. */
    private RedirectFilter redirectFilter;

    /** distinguish between first call and additional calls to process paged results */
    private boolean isFirstCall = true;


    /**
     * Calls {@link #AllPageTitles(MediaWikiBot, String, String, RedirectFilter, int)} as this(bot, null, null,
     * RedirectFilter.all, namespace)
     * 
     * @param bot The bot to execute the request.
     * @param namespaces The Wiki's namespaceID to get all pages for.
     * @throws VersionException if version is incompatible
     */
    public AllPageTitles(final MediaWikiBot bot, final int namespace) throws VersionException {
        this(bot, null, null, RedirectFilter.all, namespace);

    }

    /**
     * The public constructor. It will have an MediaWiki-request generated, which is then added to msgs. When it is
     * answered, the method processAllReturningText will be called (from outside this class). For the parameters, see
     * {@link AllPageTitles#generateRequest(String, String, boolean, boolean, String)}
     * 
     * @param from The page title to start from, may be null.
     * @param prefix Restricts search to titles that begin with this value, may be null.
     * @param rf Include redirects in the list or not.
     * @param bot The bot to execute the request.
     * @param namespace The Wiki's namespaceID to get all pages for.
     * @throws VersionException if version is incompatible
     */
    public AllPageTitles(final MediaWikiBot bot, final String from, final String prefix, final RedirectFilter rf,
            final int namespace)
    throws VersionException {
        super(bot);

        this.bot = bot;
        this.redirectFilter = rf;
        this.prefix = prefix;
        this.namespace = namespace;
        this.from = from;
        generateRequest(from, prefix, rf, namespace);

    }

    /**
     * Generates the next MediaWiki-request (GetMethod) and adds it to msgs.
     * 
     * @param from The page title to start from, may be null.
     * @param prefix Restricts search to titles that begin with this value, may be null
     * @param rf Include redirects in the list or not.
     * @param namespace The Wiki's namespaceID to get all pages for.
     * @return a {@link Get} object representing the query.
     */
    private Get generateRequest(final String from, String prefix, final RedirectFilter rf, final int namespace) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("enter GetAllPagetitles.generateRequest" + "(String,String,boolean,boolean,String)");
        }

        String apfilterredir;
        if (rf == RedirectFilter.all) {
            apfilterredir = "all";
        } else if (rf == RedirectFilter.redirects) {
            apfilterredir = "redirects";
        } else {
            apfilterredir = "nonredirects";
        }

        final String query = "/api.php?action=query&list=allpages"
            + ((from != null && from.length() > 0) ? ("&apfrom=" + MediaWiki.encode(from)) : "")
            + ((prefix != null) ? ("&apprefix=" + MediaWiki.encode(prefix)) : "") + "&apnamespace=" + namespace
            + "&apfilterredir=" + apfilterredir + "&aplimit=" + LIMIT + "&format=xml";
        return new Get(query);

    }

    /**
     * Picks the page title, pageID and namespaceID from the String (called with a MediaWiki API response).
     * 
     * @param wikiResponse Text for parsing, usually a MediaWiki API response.
     * @return a {@link Collection} of {@link WikiPage}s contained in the API response, for each {@link WikiPage},
     *         title, pageID and namespaceID are set.
     */
    @Override
    protected Collection<WikiPage> parseArticleTitles(final String wikiResponse) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("enter GetAllPagetitles.parseArticleTitles(String)");
        }
        final Collection<WikiPage> pages = new Vector<WikiPage>();
        final Matcher matcher = TITLE_PATTERN.matcher(wikiResponse);
        WikiPage wikiPage = null;
        while (matcher.find()) {
            final int pageID = Integer.parseInt(MediaWiki.decode(matcher.group(1)));
            final int namespace = Integer.parseInt(MediaWiki.decode(matcher.group(2)));
            final String title = MediaWiki.decode(matcher.group(3));
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found page title: \"" + title + "\"");
            }
            wikiPage = new WikiPage();
            wikiPage.setPageID(pageID);
            wikiPage.setNamespaceID(namespace);
            wikiPage.setTitle(title);
            pages.add(wikiPage);
        }
        return pages;
    }

    /**
     * Gets the information about a follow-up page from a provided api response.
     * If there is one, a new request is added to msgs by calling
     * generateRequest. If no exists, the string is empty.
     * 
     * @param wikiResponse text for parsing, usually a MediaWiki API response.
     * @return the title of the next page.
     */
    @Override
    protected String parseHasMore(final String wikiResponse) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("enter GetAllPagetitles.parseHasMore(String)");
        }
        final Matcher matcher = HAS_MORE_PATTERN.matcher(wikiResponse);
        if (matcher.find()) {
            return MediaWiki.decode(matcher.group(1));
        } else {
            return "";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HttpAction prepareCollection() {
        if (isFirstCall) {
            isFirstCall = false;
            return generateRequest(from, prefix, redirectFilter, namespace);
        }
        return generateRequest(getNextPageInfo(), prefix, redirectFilter, namespace);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        try {
            return new AllPageTitles(bot, from, prefix, redirectFilter, namespace);
        } catch (VersionException e) {
            throw new CloneNotSupportedException(e.getLocalizedMessage());
        }
    }

}
