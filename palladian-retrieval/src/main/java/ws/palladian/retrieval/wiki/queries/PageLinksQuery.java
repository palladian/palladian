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

import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_13;

import java.util.Collection;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.jwbf.core.actions.Get;
import net.sourceforge.jwbf.core.actions.util.HttpAction;
import net.sourceforge.jwbf.mediawiki.actions.MediaWiki;
import net.sourceforge.jwbf.mediawiki.actions.queries.TitleQuery;
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
@SupportedBy({ MW1_13 })
public class PageLinksQuery extends TitleQuery<WikiPage> {

    /** the logger for this class */
    private static final Logger LOG = Logger.getLogger(PageLinksQuery.class);

    /** store log Level */
    private static final boolean TRACE = LOG.isTraceEnabled();

    /** Pattern to parse returned page, @see {@link #parseHasMore(String)}. */
    private static final Pattern HAS_MORE_PATTERN = Pattern.compile(
            "<query-continue>.*?<allpages *apfrom=\"([^\"]*)\" */>.*?</query-continue>", Pattern.DOTALL
                    | Pattern.MULTILINE);

    /** Pattern to parse returned page, @see {@link #parseArticleTitles(String)} */
    private static final Pattern LINK_PATTERN = Pattern.compile("<pl ns=\"(.*?)\" title=\"(.*?)\" />");

    /** Constant value for the pllimit-parameter. **/
    private static final int LIMIT = 500;

    /** The Wiki's namespaceIDs to get all links to pages for, like */
    private Collection<Integer> namespaces;

    /** {@link MediaWikiBot} that executes the request. */
    private MediaWikiBot bot;

    /** Page title to get links for. */
    private final String pageTitle;

    /**
     * The public constructor. It will have an MediaWiki-request generated, which is then added to msgs. When it is
     * answered, the method processAllReturningText will be called (from outside this class).
     * 
     * @param bot The bot to execute the request.
     * @param pageTitle The page title to get outgoing links for.
     * @param namespaces The Wiki's namespaceID to get all pages for.
     * @throws VersionException if version is incompatible
     */
    public PageLinksQuery(final MediaWikiBot bot, final String pageTitle, final Collection<Integer> namespaces)
            throws VersionException {
        super(bot);
        this.bot = bot;
        this.pageTitle = pageTitle;
        this.namespaces = namespaces;
    }

    /**
     * Generates the next MediaWiki-request (GetMethod) and adds it to msgs.
     * 
     * @param queryContinue optional parameter to continue a paged query
     * @return a {@link Get} object representing the query.
     */
    private Get generateRequest(final String queryContinue) {
        if (TRACE) {
            LOG.trace("enter PageLinks.generateRequest");
        }

        final String query = "/api.php?action=query&prop=links&titles="
                + MediaWiki.encode(pageTitle)
                + "&plnamespace="
                + getEncodeNamespaces()
                + ((queryContinue != null && queryContinue.length() > 0) ? ("&plcontinue=" + MediaWiki
                        .encode(queryContinue)) : "") + "&pllimit=" + LIMIT + "&format=xml";
        return new Get(query);
    }

    /**
     * Wrapper to parse the page links. Since we use a {@link TitleQuery} as superclass, the naming
     * "parseArticleTitles" is a bit misleading, instead of titles, links are parsed. See
     * {@link #parsePageTitles(String)} for details.
     * 
     * @param XML The XML representation of the page's revisions, fetched from Wiki API.
     */
    @Override
    protected Collection<WikiPage> parseArticleTitles(final String XML) {
        return parsePageLinks(XML);
    }

    /**
     * Picks the page title, pageID and namespaceID from the String (called with a MediaWiki API response).
     * 
     * @param wikiResponse Text for parsing, usually a MediaWiki API response.
     * @return a {@link Collection} of {@link WikiPage}s contained in the API response, for each {@link WikiPage},
     *         title and namespaceID are set.
     */
    protected Collection<WikiPage> parsePageLinks(final String wikiResponse) {
        if (TRACE) {
            LOG.trace("enter PageLinks.parseArticleTitles(String)");
        }
        final Collection<WikiPage> pages = new Vector<WikiPage>();
        final Matcher matcher = LINK_PATTERN.matcher(wikiResponse);
        WikiPage wikiPage = null;
        while (matcher.find()) {
            final int namespaceDest = Integer.parseInt(MediaWiki.decode(matcher.group(1)));
            final String titleDest = MediaWiki.decode(matcher.group(2));
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found link from page \"" + pageTitle + "\" to page \"" + titleDest + "\".");
            }
            wikiPage = new WikiPage();
            wikiPage.setNamespaceID(namespaceDest);
            wikiPage.setTitle(titleDest);
            pages.add(wikiPage);
        }
        return pages;
    }

    /**
     * Returns the namespaces in encoded query syntax, e.g. a set {0,1,3} as "0%7C1%7C3" (human readable: "0|1|3").
     * 
     * @return String with encodedNamespaces like "0%7C1%7C3" ("0|1|3").
     */
    private String getEncodeNamespaces() {
        String namespacesString = "";
        for (Integer namespace : namespaces) {
            namespacesString += namespace + "|";
        }
        namespacesString = namespacesString.substring(0, namespacesString.length() - 1);
        return MediaWiki.encode(namespacesString);
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
        if (TRACE) {
            LOG.trace("enter PageLinksQuery.parseHasMore(String)");
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
        return generateRequest(getNextPageInfo());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        try {
            return new PageLinksQuery(bot, pageTitle, namespaces);
        } catch (VersionException e) {
            throw new CloneNotSupportedException(e.getLocalizedMessage());
        }
    }

}
