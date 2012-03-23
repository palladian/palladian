package ws.palladian.retrieval.wiki;

import net.sourceforge.jwbf.mediawiki.actions.util.VersionException;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import org.apache.log4j.Logger;

import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.wiki.data.Revision;
import ws.palladian.retrieval.wiki.data.WikiDescriptor;
import ws.palladian.retrieval.wiki.data.WikiPage;
import ws.palladian.retrieval.wiki.persistence.MediaWikiDatabase;
import ws.palladian.retrieval.wiki.queries.RevisionsByTitleQuery;

/**
 * Fetches all revisions newer than the newest in data base for the given page from the API and stores them in the
 * database. A revision is identified by its revisionID, duplicate revisionIDs (revisionIDs already contained in
 * database) are written to error log.
 * 
 * @deprecated Caution! This class might not be up to date since the development of the crawler using multiple threads
 *             has been ceased.
 */
@Deprecated
public class RevisionThread implements Runnable {

    /** The global logger */
    private static final Logger LOGGER = Logger.getLogger(RevisionThread.class);

    /** The database used to persist results */
    private final MediaWikiDatabase mwDatabase;

    private final WikiDescriptor mwDescriptor;

    /** The jwbf bot that does all the communication with the MediaWiki API. */
    private final MediaWikiBot bot;

    /** The page to crawl revisions for. */
    private final WikiPage page;

    /**
     * @param bot The jwbf bot that does all the communication with the MediaWiki API.
     * @param mwDescriptor The Wiki the PAGE is in.
     * @param page The page to crawl revisions for.
     */
    public RevisionThread(MediaWikiBot bot, WikiDescriptor mwDescriptor, WikiPage page, MediaWikiDatabase mwDatabase) {
        this.mwDatabase = mwDatabase;
        this.mwDescriptor = mwDescriptor;
        this.bot = bot;
        this.page = page;
    }

    @Override
    public void run() {

        // prepare query for revisions
        RevisionsByTitleQuery rbtq = null;
        try {
            rbtq = new RevisionsByTitleQuery(bot, page.getTitle(), page.getNewestRevisionID());
        } catch (VersionException e) {
            LOGGER.fatal("Fetching page revisions is not supported by this Wiki version. " + e);
        }

        // run query and process results
        int revisionsAdded = 0;
        int revisionsSkipped = 0;
        for (Revision revision : rbtq) {

            // write revision to database
            boolean revisionAdded = mwDatabase.addRevision(mwDescriptor.getWikiID(), page.getPageID(), revision);
            if (revisionAdded) {
                revisionsAdded++;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("add wikiID:" + mwDescriptor.getWikiID() + " pageID:" + page.getPageID()
                            + " revisionID:" + revision.getRevisionID() + " " + revision.getTimestamp());
                }
            } else {
                revisionsSkipped++;
                LOGGER.error("skip wikiID:" + mwDescriptor.getWikiID() + " pageID:" + page.getPageID() + " revisionID:"
                        + revision.getRevisionID() + " " + revision.getTimestamp());
            }
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Processed " + (revisionsAdded + revisionsSkipped) + " revision(s) for page \""
                    + page.getTitle() + "\" : added " + revisionsAdded + " , skipped " + revisionsSkipped);
        }
    }

}
