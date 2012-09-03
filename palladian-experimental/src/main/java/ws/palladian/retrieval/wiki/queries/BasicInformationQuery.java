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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sourceforge.jwbf.core.actions.Get;
import net.sourceforge.jwbf.core.actions.util.HttpAction;
import net.sourceforge.jwbf.mediawiki.actions.MediaWiki;
import net.sourceforge.jwbf.mediawiki.actions.queries.TitleQuery;
import net.sourceforge.jwbf.mediawiki.actions.util.VersionException;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.StringInputStream;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.wiki.data.WikiPage;

/**
 * Action class using the MediaWiki-api's "prop = info", compatibility tested with 1.17, others might also work,
 * 1.13 does not support inprop=url
 * 
 * @author Tobias Knerr
 * @author Thomas Stock
 * @author Sandro Reichert
 * 
 */
// @SupportedBy({ MW1_16 })
public class BasicInformationQuery extends TitleQuery<WikiPage> {

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(BasicInformationQuery.class);

    // /** Pattern to parse returned page, @see {@link #parseHasMore(String)}. */
    // private static final Pattern HAS_MORE_PATTERN = Pattern.compile("<query-continue>.*?</query-continue>",
    // Pattern.DOTALL | Pattern.MULTILINE);

    /** Constant value for the aplimit-parameter. **/
    private static final int LIMIT = 500;

    /** The jwbf bot that does all the communication with the MediaWiki API. */
    private MediaWikiBot bot = null;

    /** page Title to get Information for */
    private String title = null;

    /** The properties to get . */
    private final String inprop = "url";

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
     * @param title
     *            The title of the page to get information about.
     * @throws VersionException if version is incompatible
     */
    public BasicInformationQuery(final MediaWikiBot bot, final String title) throws VersionException {
        super(bot);
        this.bot = bot;
        this.title = title;
    }

    /**
     * Generates the next MediaWiki-request (GetMethod) and adds it to msgs.
     * 
     * @param title
     *            The title of the page to get information about.
     * @return a
     */
    private Get generateRequest(final String title) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("enter InfoQuery");
        }

        final String query = "/api.php?action=query&prop=info" + "&inprop=" + inprop + "&titles="
                + MediaWiki.encode(title) + "&rclimit=" + LIMIT + "&format=xml";
        return new Get(query);
    }

    /**
     * Wrapper to parse the recent changes. Since we use a {@link TitleQuery} as superclass, the naming
     * "parseArticleTitles" is a bit misleading, instead of titles, recent changes are parsed. See
     * {@link #parsePageInformation(String)} for details.
     * 
     * @param XML The XML representation of the Wiki's recent changes, fetched from Wiki API.
     */
    @Override
    protected Collection<WikiPage> parseArticleTitles(final String XML) {
        return parsePageInformation(XML);
    }

    /**
     * Picks for each page the pageID, title, lastRevisionID and fullurl from a MediaWiki API response. If multiple
     * revisions for the same page are received, they are all added to the same {@link WikiPage} object.
     * 
     * @param XML The XML representation of the page, fetched from Wiki API.
     * @return a collection of {@link WikiPage}s, containing basic page information returned by the API.
     */
    private Collection<WikiPage> parsePageInformation(final String XML) {
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

        for (Node page : XPathHelper.getNodes(document, "//page")) {
            // check whether page exists
            if (page.getAttributes().getNamedItem("missing") != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Page \"" + title + "\" does not exist! (Might be removed or has never existed.)");
                }
                continue;
            }
            WikiPage wikiPage = new WikiPage();
            wikiPage.setPageID(Integer.parseInt(page.getAttributes().getNamedItem("pageid").getTextContent()));
            wikiPage.setNewestRevisionID(Long
                    .parseLong(page.getAttributes().getNamedItem("lastrevid").getTextContent()));

            if (page.getAttributes().getNamedItem("fullurl") != null) {
                final String fullurl = page.getAttributes().getNamedItem("fullurl").getTextContent();
                try {
                    wikiPage.setPageURL(new URL(fullurl));
                } catch (MalformedURLException e) {
                    LOGGER.error("Could not create URL from \"" + fullurl + "\". ", e);
                }
            } else {
                LOGGER.warn("Could not fetch prop=url from API, page \"" + title + "\".");
            }

            // timestampTouched nor used, could be added
            // Date timestampTouched = null;
            // try {
            // timestampTouched = (DateGetterHelper.findDate(page.getAttributes().getNamedItem("touched")
            // .getTextContent())).getNormalizedDate();
            // } catch (Exception e) {
            // LOGGER.error(
            // "Error parsing Wiki touched timestamp \""
            // + MediaWiki.decode(page.getAttributes().getNamedItem("touched").getTextContent())
            // + "\", timestamp has not been added. ", e);
            // }

            pages.put(wikiPage.getPageID(), wikiPage);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("processed basic page information. XML file:\n" + XML + "\n");
        }
        return pages.values();
    }

    /**
     * Gets the information about a follow-up page from a provided api response.
     * Since follow-up pages are not required for this type of query, an empty string is returned.
     * 
     * @param wikiResponse
     *            text for parsing
     * @return empty String since follow-up pages are not required for this type of query.
     */
    @Override
    protected String parseHasMore(final String wikiResponse) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("enter InfoQuery.parseHasMore(String)");
        }
        return "";
        // final Matcher matcher = HAS_MORE_PATTERN.matcher(wikiResponse);
        // if (matcher.find()) {
        // return matcher.group(1);
        // } else {
        // return null;
        // }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HttpAction prepareCollection() {
        if (isFirstCall) {
            isFirstCall = false;
            return generateRequest(title != null ? title : "");
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
            return new BasicInformationQuery(bot, title);
        } catch (VersionException e) {
            throw new CloneNotSupportedException(e.getLocalizedMessage());
        }
    }

}
