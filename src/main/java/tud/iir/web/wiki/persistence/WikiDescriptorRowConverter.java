package tud.iir.web.wiki.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;

import tud.iir.persistence.RowConverter;
import tud.iir.web.wiki.data.WikiDescriptor;

public class WikiDescriptorRowConverter implements RowConverter<WikiDescriptor> {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WikiDescriptorRowConverter.class);

    @Override
    public WikiDescriptor convert(ResultSet resultSet) throws SQLException {
        WikiDescriptor wd = new WikiDescriptor();
        wd.setWikiID(resultSet.getInt(1));
        wd.setWikiName(resultSet.getString(2));
        wd.setWikiURL(resultSet.getString(3));
        wd.setPathToAPI(resultSet.getString(4));
        if (resultSet.getString(5) != null && !resultSet.getString(5).equalsIgnoreCase("NULL")) {
            Date lastCheck = null;
            try {
                lastCheck = MediaWikiDatabase.convertSQLDateTimeToDate(resultSet.getString(5));
            } catch (Exception e) {
                LOGGER.error(
                        "Could not process the timestamp the wiki has been checked for new pages the last time. Wiki \""
                        + resultSet.getString(2) + "\", timestamp: " + resultSet.getString(5) + " ", e);
            }
            wd.setLastCheckForModifications(lastCheck);
        }
        wd.setCrawlerUserName(resultSet.getString(6));
        wd.setCrawlerPassword(resultSet.getString(7));
        return wd;
    }

}
