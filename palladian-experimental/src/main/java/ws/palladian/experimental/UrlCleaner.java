package ws.palladian.experimental;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public final class UrlCleaner {

    @SuppressWarnings("unused")
    private final static Logger LOGGER = Logger.getLogger(UrlCleaner.class);
    private final static String INSERT_STATEMENT = "INSERT INTO feeds (feedUrl,siteUrl,format,textType,newestItemHash) VALUES (?,\"\",0,0,\"\");";

    // private final static String[] duplicateFeeds = { "http://216.109.136.224/RSS.aspx",
    // "http://AFRICARVTOURS.COM/feed", "http://ALLNEWSHYPE.COM/feed/",
    // "http://bigredtoday.com/apps/pbcs.dll/section?category=rss&c=BIGR" };

    /**
     * @param args
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        FileInputStream input = new FileInputStream(args[0]);
        List<String> urls = IOUtils.readLines(input);
        // String start = "INSERT INTO feeds (feedUrl,siteUrl,format,textType,newestItemHash) VALUES (\"";
        // String end = "\",\"\",0,0,\"\");";
        // List<String> duplicateFeedsList = Arrays.asList(duplicateFeeds);
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DriverManager
                .getConnection("jdbc:mysql://localhost/tudiirdb2?user=palladian&password=palladian");
        conn.createStatement().execute("TRUNCATE TABLE feeds;");
        PreparedStatement ps = conn.prepareStatement(INSERT_STATEMENT);

        int duplicates = 0;
        int tooLong = 0;
        for (String url : urls) {
            if (url.endsWith("\\")) {
                url = url.replaceAll("\\\\", "\\\\");
            }
            url = url.replaceAll(";", "\\;");
            try {
                ps.setString(1, url);
                ps.execute();
            } catch (com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException e) {
                LOGGER.error("Duplicate found.", e);
                duplicates++;
            } catch (com.mysql.jdbc.MysqlDataTruncation e) {
                LOGGER.error("URL " + url + " too long.", e);
                tooLong++;
            } catch (Exception e) {
                LOGGER.error("Error while processing URL: " + url);
                throw e;
            }
        }
        IOUtils.closeQuietly(input);
        LOGGER.info("Ignored " + duplicates + " Duplicates.");
        LOGGER.info(tooLong + " urls where too long to insert.");
    }
}
