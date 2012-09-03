package ws.palladian.retrieval.wiki.queries;

/**
 * This file is based on net.sourceforge.jwbf.mediawiki.actions.queries.AllPageTitles
 * 
 * Copyright 2007 Tobias Knerr.
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
 * Contributors to net.sourceforge.jwbf.mediawiki.actions.queries.AllPageTitles:
 * Tobias Knerr
 * 
 * Contributers to modifies version:
 * Sandro Reichert
 */

import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_13;
import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_16;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sourceforge.jwbf.core.actions.Get;
import net.sourceforge.jwbf.core.actions.util.HttpAction;
import net.sourceforge.jwbf.mediawiki.actions.MediaWiki;
import net.sourceforge.jwbf.mediawiki.actions.queries.TitleQuery;
import net.sourceforge.jwbf.mediawiki.actions.util.MWAction;
import net.sourceforge.jwbf.mediawiki.actions.util.SupportedBy;
import net.sourceforge.jwbf.mediawiki.actions.util.VersionException;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.StringInputStream;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.wiki.data.Revision;
import ws.palladian.retrieval.wiki.data.WikiPage;

/**
 * Action class using the MediaWiki-api's "prop = revisions", compatibility tested with MW 1.13 and 1.16, others might
 * also work
 * 
 * @author Tobias Knerr
 * @author Thomas Stock
 * @author Sandro Reichert
 * 
 */
@SupportedBy({ MW1_13, MW1_16 })
public class RecentChanges extends TitleQuery<WikiPage> {

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(RevisionsByTitleQuery.class);

    /** Pattern to parse returned page, @see {@link #parseHasMore(String)}. */
    private static final Pattern HAS_MORE_PATTERN = Pattern.compile(
            "<query-continue>.*?<recentchanges *rcstart=\"(.*?)\" */>.*?</query-continue>", Pattern.DOTALL
                    | Pattern.MULTILINE);

    /** Constant value for the aplimit-parameter. **/
    private static final int LIMIT = 500;

    /** The jwbf bot that does all the communication with the MediaWiki API. */
    private MediaWikiBot bot = null;

    /** The namespace IDs to get the recent changes for. */
    private int[] namespaces = null;

    /** timestamp in Wiki format to start enumerating from, like '2010-12-21T14:08:22Z' */
    private String rcstart = null;

    /** Which types of changes to show. Values (separate with '|'): edit, new */
    private String rctype = null;

    /** The properties to get for each changed item. */
    private String rcprop = "title|ids|timestamp|user";

    /** distinguish between first call and additional calls to process paged results */
    private boolean isFirstCall = true;

    /**
     * The public constructor. It will have an MediaWiki-request generated,
     * which is then added to msgs. When it is answered, the method
     * processAllReturningText will be called (from outside this class). For the
     * parameters, see {@link AllPageTitles#generateRequest(String, String, boolean, boolean, String)}
     * 
     * @param bot
     *            The MediaWiki bot to perform the action
     * @param rcstart
     *            Earliest timestamp to get changes for.
     * @param namespaces
     *            the namespace(s) that will be searched for changes; if null, this parameter is
     *            omitted and the API's default is used (default: all namespaces).
     * @param rctype
     *            Which types of changes to show. Values (separate with '|'): edit, new. If null, this parameter is
     *            omitted and the API's default is used (default: all types).
     * @throws VersionException if version is incompatible
     */
    public RecentChanges(final MediaWikiBot bot, final String rcstart, final int[] namespaces, final String rctype)
            throws VersionException {
        super(bot);

        this.bot = bot;
        this.namespaces = namespaces;
        this.rcstart = rcstart;
        this.rctype = rctype;
    }

    /**
     * Generates the next MediaWiki-request (GetMethod) and adds it to msgs.
     * 
     * @param rcstart
     *            timestamp to start from, may be null to start from oldest one.
     * @param prefix
     *            restricts search to titles that begin with this value, may be
     *            null
     * @param rf include redirects in the list
     * @param namespace
     *            the namespace(s) that will be searched for links, as a string
     *            of numbers separated by '|'; if null, this parameter is
     *            omitted
     * @return a
     */
    private Get generateRequest(final String rcstart) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("enter RecentChangePages.generateRequest");
        }

        final String query = "/api.php?action=query&list=recentchanges"
                + "&rcprop="
                + MediaWiki.encode(rcprop)
                + ((rcstart != null && rcstart.length() > 0) ? ("&rcstart=" + rcstart + "&rcdir=newer") : "")
                + ((rctype != null && rctype.length() > 0) ? ("&rctype=" + MediaWiki.encode(rctype)) : "")
                + "&rclimit="
                + LIMIT
                + ((namespaces != null && namespaces.length > 0) ? ("&rcnamespace=" + MediaWiki.encode(MWAction
                        .createNsString(namespaces))) : "") + "&format=xml";

        return new Get(query);
    }

    /**
     * Wrapper to parse the recent changes. Since we use a {@link TitleQuery} as superclass, the naming
     * "parseArticleTitles" is a bit misleading, instead of titles, recent changes are parsed. See
     * {@link #parseRecentChanges(String)} for details.
     * 
     * @param XML The XML representation of the Wiki's recent changes, fetched from Wiki API.
     */
    @Override
    protected Collection<WikiPage> parseArticleTitles(final String XML) {
        return parseRecentChanges(XML);
    }

    /**
     * Picks for each recent change the namespaceID, pageID, title, revisionID, author and timestamp from a MediaWiki
     * API response. If multiple revisions for the same page are received, they are all added to the same
     * {@link WikiPage} object.
     * 
     * @param XML The XML representation of the page's recent changes, fetched from Wiki API.
     * @return a collection of {@link WikiPage}s, containing the recent changes returned by the API.
     */
    private Collection<WikiPage> parseRecentChanges(final String XML) {
        final Map<Integer, WikiPage> pages = new HashMap<Integer, WikiPage>();

        Document document = null;
        try {
            document = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new StringInputStream(StringHelper.stripNonValidXMLCharacters(StringHelper
                            .removeNonAsciiCharacters(XML))));
        } catch (Exception e) {
            LOGGER.error("could not process recent changes! XML file:\n" + XML + "\n", e);
            return null;
        }

        for (Node recentChange : XPathHelper.getNodes(document, "//rc")) {
            WikiPage wikiPage = new WikiPage();
            wikiPage.setPageID(Integer.parseInt(recentChange.getAttributes().getNamedItem("pageid").getTextContent()));

            Revision revision = null;
            final Long revisionID = Long.parseLong(MediaWiki.decode(recentChange.getAttributes().getNamedItem("revid")
                    .getTextContent()));
            final String author = MediaWiki.decode(recentChange.getAttributes().getNamedItem("user").getTextContent());
            try {
                final Date timestamp = (DateParser.findDate(recentChange.getAttributes()
                        .getNamedItem("timestamp").getTextContent())).getNormalizedDate();
                revision = new Revision(revisionID, timestamp, author);
            } catch (Exception e) {
                LOGGER.error("Error parsing Wiki timestamp \""
                        + MediaWiki.decode(recentChange.getAttributes().getNamedItem("timestamp").getTextContent())
                        + "\", revisionID " + revisionID + " has not been added. Error:" + e.getMessage());
            }

            if (pages.containsKey(wikiPage.getPageID())) {
                // we already found a new revision for this page, add the current revision to the page
                wikiPage = pages.get(wikiPage.getPageID());
            } else {
                // first new revision, create the WikiPage object and add the revision
                wikiPage.setNamespaceID(Integer.parseInt(recentChange.getAttributes().getNamedItem("ns")
                        .getTextContent()));
                wikiPage.setTitle(recentChange.getAttributes().getNamedItem("title").getTextContent());
            }

            if (revision != null) {
                wikiPage.addRevision(revision);
            }
            pages.put(wikiPage.getPageID(), wikiPage);

        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("processed recent changes. XML file:\n" + XML + "\n");
        }
        return pages.values();
    }

    /**
     * Gets the information about a follow-up page from a provided api response.
     * If there is one, a new request is added to msgs by calling
     * generateRequest. If no exists, the string is empty.
     * 
     * @param wikiResponse
     *            text for parsing
     * @return The timestamp in Wiki format to start a follow-up request.
     */
    @Override
    protected String parseHasMore(final String wikiResponse) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("enter GetAllPagetitles.parseHasMore(String)");
        }
        final Matcher matcher = HAS_MORE_PATTERN.matcher(wikiResponse);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HttpAction prepareCollection() {
        if (isFirstCall) {
            isFirstCall = false;
            return generateRequest(rcstart != null ? rcstart : "");
        } else {
            return generateRequest(getNextPageInfo());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        try {
            return new RecentChanges(bot, rcstart, namespaces, rctype);
        } catch (VersionException e) {
            throw new CloneNotSupportedException(e.getLocalizedMessage());
        }
    }

}
