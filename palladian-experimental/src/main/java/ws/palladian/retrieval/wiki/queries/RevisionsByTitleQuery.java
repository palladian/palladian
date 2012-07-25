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

package ws.palladian.retrieval.wiki.queries;

import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_13;
import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_16;

import java.util.Collection;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sourceforge.jwbf.core.actions.Get;
import net.sourceforge.jwbf.core.actions.util.HttpAction;
import net.sourceforge.jwbf.mediawiki.actions.MediaWiki;
import net.sourceforge.jwbf.mediawiki.actions.queries.TitleQuery;
import net.sourceforge.jwbf.mediawiki.actions.util.SupportedBy;
import net.sourceforge.jwbf.mediawiki.actions.util.VersionException;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.date.DateGetterHelper;
import ws.palladian.helper.date.dates.DateParser;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.StringInputStream;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.wiki.data.Revision;

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
public class RevisionsByTitleQuery extends TitleQuery<Revision> {

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(RevisionsByTitleQuery.class);

    /** Pattern to parse returned page, @see {@link #parseHasMore(String)}. */
    private static final Pattern HAS_MORE_PATTERN = Pattern.compile(
            "<query-continue>.*?<revisions *rvstartid=\"([1-9][0-9]*)\" */>.*?</query-continue>", Pattern.DOTALL
                    | Pattern.MULTILINE);

    /** Constant value for the aplimit-parameter. **/
    private static final int LIMIT = 500;

    /** The jwbf bot that does all the communication with the MediaWiki API. */
    private final MediaWikiBot bot;

    /** The title of the page to get the revisions for */
    private final String pageTitle;

    /** The first revision ID to fetch */
    private Long rvStartID = null;

    /** the properties to get for each revision */
    private String[] properties = { "ids", "user", "timestamp" };

    /** distinguish between first call and additional calls to process paged results */
    private boolean isFirstCall = true;

    /**
     * The public constructor. It will have an MediaWiki-request generated,
     * which is then added to msgs. When it is answered, the method
     * processAllReturningText will be called (from outside this class). For the
     * parameters, see {@link #generateRequest(String)}
     * 
     * @param bot
     *            The MediaWiki bot to perform the action
     * @param pageTitle
     *            Name of a single page to retrieve revisions from
     * @param rvStartID
     *            revision ID to start from, may be null to return all revisions
     * @throws VersionException if version is incompatible
     */
    public RevisionsByTitleQuery(final MediaWikiBot bot, final String pageTitle, final Long rvStartID)
            throws VersionException {
        super(bot);

        this.bot = bot;
        this.rvStartID = rvStartID;
        this.pageTitle = pageTitle;
    }

    /**
     * Generates the next MediaWiki-request (GetMethod) and adds it to msgs.
     * 
     * @param rvStartID
     *            revision ID to start from, may be null to start from most recent
     * @return a
     */
    private Get generateRequest(final String rvStartID) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("enter RevisionByTitleQuery.generateRequest");
        }

        String rvprop = "";
        for (String property : properties) {
            rvprop += property + "|";
        }
        rvprop = rvprop.substring(0, rvprop.length() - 1);

        final String query = "/api.php?action=query&prop=revisions&titles=" + MediaWiki.encode(pageTitle)
                + ((properties != null && rvprop.length() > 0) ? ("&rvprop=" + MediaWiki.encode(rvprop)) : "")
                + ((rvStartID != null && rvStartID.length() > 0) ? ("&rvstartid=" + rvStartID) : "") + "&rvdir=newer"
                + "&rvlimit=" + LIMIT + "&format=xml";

        return new Get(query);
    }

    /**
     * Wrapper to parse the article revisions. Since we use a {@link TitleQuery} as superclass, the naming
     * "parseArticleTitles" is a bit misleading, instead of titles, revisions are parsed. See
     * {@link #parseArticleRevisions(String)} for details.
     * 
     * @param XML The XML representation of the page's revisions, fetched from Wiki API.
     */
    @Override
    protected Collection<Revision> parseArticleTitles(final String XML) {
        return parseArticleRevisions(XML);
    }

    /**
     * Picks the article revision (revisionID, author, time stamp) from a MediaWiki API response.
     * 
     * @param XML The XML representation of the page's revisions, fetched from Wiki API.
     * @return a collection of {@link Revision}s, containing the requested revisions, see
     *         {@link RevisionsByTitleQuery#RevisionsByTitleQuery(MediaWikiBot, String, Long, String[])} for details on
     *         pageTitle, requested revisions, etc.
     */
    private Collection<Revision> parseArticleRevisions(final String XML) {
        final Collection<Revision> revisions = new Vector<Revision>();

        try {
            final Document document = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new StringInputStream(StringHelper.stripNonValidXMLCharacters(StringHelper
                            .removeNonAsciiCharacters(XML))));

            // try to get asian characters to work:
            // StringInputStream dataStream = new StringInputStream(XML);
            // XmlReader xmlReader = new XmlReader(dataStream);
            // Xml10FilterReader filterReader = new Xml10FilterReader(xmlReader);
            // InputSource is = new InputSource(filterReader);
            //
            // final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

            for (Node revision : XPathHelper.getNodes(document, "//rev")) {
                final long revisionID = Long.parseLong(MediaWiki.decode(revision.getAttributes().getNamedItem("revid")
                        .getTextContent()));

                final Node userNode = revision.getAttributes().getNamedItem("user");
                String author = (userNode == null) ? "User has been deleted!" : MediaWiki.decode(userNode
                        .getTextContent());
                if (author.length() == 0) {
                    author = "Illeagal user name, contained non-ASCII characters only.";
                }

                try {
                    final Date timestamp = DateParser.findDate(
                            revision.getAttributes().getNamedItem("timestamp").getTextContent()).getNormalizedDate();
                    revisions.add(new Revision(revisionID, timestamp, author));
                } catch (Exception e) {
                    LOGGER.error("Error parsing Wiki timestamp \""
                            + MediaWiki.decode(revision.getAttributes().getNamedItem("timestamp").getTextContent())
                            + "\", revisionID " + revisionID + " has not been added. Error: " + e.getMessage());
                    if ((author == null || author.length() == 0) && userNode != null) {
                        LOGGER.error("userNode.toString() = \"" + userNode.toString()
                                + "\", userNode.getTextContent() = \"" + userNode.getTextContent() + "\"");
                    }

                    if (author == null || author.length() == 0) {

                    }

                }
            }
        } catch (Exception e) {
            LOGGER.error("could not process revisions for page \"" + pageTitle + "\". XML file:\n" + XML + "\n", e);
            return revisions;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("processed revisions for page \"" + pageTitle + "\". XML file:\n" + XML + "\n");
        }
        return revisions;
    }

    /**
     * Gets the information about a follow-up page from a provided api response.
     * If there is one, a new request is added to msgs by calling
     * generateRequest. If no exists, the string is empty.
     * 
     * @param wikiResponse
     *            text for parsing
     * @return The revisionID to start a follow-up request for.
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
     * Wrapper to get the next revisionID if the API's result is paged.
     * 
     * @return The next revisionID to start a new query from.
     */
    private final String getNextRevisionID() {
        return getNextPageInfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HttpAction prepareCollection() {
        if (isFirstCall) {
            isFirstCall = false;
            return generateRequest(rvStartID != null ? rvStartID.toString() : "");
        } else {
            return generateRequest(getNextRevisionID());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        try {
            return new RevisionsByTitleQuery(bot, pageTitle, rvStartID);
        } catch (VersionException e) {
            throw new CloneNotSupportedException(e.getLocalizedMessage());
        }
    }

}
