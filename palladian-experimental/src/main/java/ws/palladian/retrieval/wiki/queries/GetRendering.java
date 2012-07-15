/*
 * file based on package net.sourceforge.jwbf.mediawiki.actions.misc.GetRendering, changed Get-Object from
 * "/api.php?action=parse&text=" to "/api.php?action=parse&page=" to fetch a complete parsed page, including used
 * templates which are omitted when fetching page as "text".
 */

package ws.palladian.retrieval.wiki.queries;


import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_12;
import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_13;
import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_14;
import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_15;
import static net.sourceforge.jwbf.mediawiki.actions.MediaWiki.Version.MW1_16;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import net.sourceforge.jwbf.core.actions.Get;
import net.sourceforge.jwbf.core.actions.util.ActionException;
import net.sourceforge.jwbf.core.actions.util.HttpAction;
import net.sourceforge.jwbf.core.actions.util.ProcessException;
import net.sourceforge.jwbf.mediawiki.actions.MediaWiki;
import net.sourceforge.jwbf.mediawiki.actions.util.MWAction;
import net.sourceforge.jwbf.mediawiki.actions.util.SupportedBy;
import net.sourceforge.jwbf.mediawiki.actions.util.VersionException;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.InputSource;

/**
 * 
 * Implements function to render wikitext on remote
 * <a href="http://www.mediawiki.org/wiki/API:Expanding_templates_and_rendering#parse">parse</a>.
 * 
 * @author Thomas Stock
 * @author Sandro Reichert
 * 
 */
@SupportedBy({ MW1_12, MW1_13, MW1_14, MW1_15, MW1_16 })
public class GetRendering extends MWAction {

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(GetRendering.class);

    private final Get msg;

    /** The page title to get the rendered text for. */
    private final String title;

    /** The html representation of the page */
    private String html = "";

    /** {@link MediaWikiBot} that executes the request. */
    private final MediaWikiBot bot;

    /** This is a self-executing request, so there is no need to execute it from outside (by the caller). */
    private boolean isSelfEx = true;

    /**
     * 
     * @param bot {@link MediaWikiBot} that executes the request.
     * @param title The title of the page to get parsed.
     * @throws VersionException if not supported
     */
    public GetRendering(final MediaWikiBot bot, final String title) throws VersionException {
        super(bot.getVersion());
        this.bot = bot;
        this.title = title;
        msg = new Get("/api.php?action=parse&page=" + MediaWiki.encode(title) + "&titles=API&format=xml");

    }

    /**
     * {@inheritDoc}
     * 
     * @deprecated see super
     */
    @Deprecated
    @Override
    public boolean isSelfExecuter() {
        return isSelfEx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpAction getNextMessage() {
        return msg;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String processAllReturningText(final String wikiResponse) throws ProcessException {
        try {
        html = findElement("text", wikiResponse).getTextTrim();
        } catch (NoSuchElementException e) {
            LOGGER.warn("MediaWiki API response for page \"" + title
                    + "\" did not contain a text element, page content could not be found.");
        }
        html = html.replace("\n", "");
        switch (bot.getVersion()) {
            case MW1_12:
                break;
            default:
                int last = html.lastIndexOf("<!--");
                if (last != -1) {
                    html = html.substring(0, last);
                }
        }
        return "";
    }


    protected Element findElement(final String elementName, final String xml) {
        final SAXBuilder builder = new SAXBuilder();
        Element root = null;
        try {
            final Reader stringReader = new StringReader(xml);
            final Document doc = builder.build(new InputSource(stringReader));

            root = doc.getRootElement();

        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (root != null) {
            return findContent(root, elementName);
        } else {
            return null;
        }
    }

    private Element findContent(final Element parent, final String name) {
        Element found = null;
        @SuppressWarnings("unchecked")
        final Iterator<Element> childrenIT = parent.getChildren().iterator();
        while (childrenIT.hasNext()) {
            final Element child = childrenIT.next();

            if (child.getQualifiedName().equalsIgnoreCase(name)) {
                // System.out.println(element.getQualifiedName());
                return child;

            } else {
                found = findContent(child, name);
            }

        }
        if (found == null) {
            throw new NoSuchElementException();
        }
        return found;
    }

    private void update() {
        try {
            isSelfEx = false;
            bot.performAction(this);

        } catch (ActionException e) {
            e.printStackTrace();
        } catch (ProcessException e) {
            e.printStackTrace();
        } finally {
            isSelfEx = true;
        }
    }

    /**
     * 
     * @return the
     */
    public String getHtml() {
        if (html.length() < 1) {
            update();
        }
        return html;
    }

}
