package tud.iir.wiki;

import net.sourceforge.jwbf.mediawiki.actions.util.VersionException;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;

import org.apache.log4j.Logger;

import tud.iir.wiki.data.Revision;
import tud.iir.wiki.data.WikiDescriptor;
import tud.iir.wiki.data.WikiPage;
import tud.iir.wiki.persistence.MediaWikiDatabase;
import tud.iir.wiki.queries.RevisionsByTitleQuery;

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

    private final MediaWikiDatabase MW_DATABASE;

    private final WikiDescriptor MW_DESCRIPTOR;

    private final MediaWikiBot BOT;

    private final WikiPage PAGE;

    /**
     * @param MW_DESCRIPTOR The Wiki the PAGE is in.
     * @param PAGE The page to crawl revisions for.
     */
    public RevisionThread(MediaWikiBot bot, final WikiDescriptor MW_DESCRIPTOR, final WikiPage PAGE) {
        this.MW_DATABASE = MediaWikiDatabase.getInstance();
        this.MW_DESCRIPTOR = MW_DESCRIPTOR;
        this.BOT = bot;
        this.PAGE = PAGE;
    }

    @Override
    public void run() {

        // prepare query for revisions
        RevisionsByTitleQuery rbtq = null;
        try {
            rbtq = new RevisionsByTitleQuery(BOT, PAGE.getTitle(), PAGE.getNewestRevisionID());
        } catch (VersionException e) {
            LOGGER.fatal("Fetching page revisions is not supported by this Wiki version. " + e);
        }

        // run query and process results
        int revisionsAdded = 0;
        int revisionsSkipped = 0;
        for (Revision revision : rbtq) {

            // write revision to database
            boolean revisionAdded = MW_DATABASE.addRevision(MW_DESCRIPTOR.getWikiID(), PAGE.getPageID(), revision);
            if (revisionAdded) {
                revisionsAdded++;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("add wikiID:" + MW_DESCRIPTOR.getWikiID() + " pageID:" + PAGE.getPageID()
                            + " revisionID:" + revision.getRevisionID() + " " + revision.getTimestamp());
                }
            } else {
                revisionsSkipped++;
                LOGGER.error("skip wikiID:" + MW_DESCRIPTOR.getWikiID() + " pageID:" + PAGE.getPageID()
                        + " revisionID:" + revision.getRevisionID() + " " + revision.getTimestamp());
            }
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Processed " + (revisionsAdded + revisionsSkipped) + " revision(s) for page \""
                    + PAGE.getTitle() + "\" : added " + revisionsAdded + " , skipped " + revisionsSkipped);
        }
    }

}
