package tud.iir.web.ranking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import tud.iir.helper.StopWatch;
import tud.iir.persistence.DatabaseManager;
import tud.iir.web.ranking.RankingRetriever.Service;

/**
 * Cache for {@link RankingRetriever}. As those APIs have a considerable latency, we cache their results for a
 * specific time in the database.
 * 
 * TODO caching ttl sometimes does not work correctly.
 * 
 * @author Philipp Katz
 * 
 */
public class RankingCacheDB extends RankingCache {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(RankingCacheDB.class);

    /** Prepared statements to get ranking with TTL. */
    private PreparedStatement getRankings;

    /** Prepared statement to get ranking without TTL. */
    private PreparedStatement getRankings2;

    /** Prepared statement to add ranking. */
    private PreparedStatement addRanking;

    public RankingCacheDB() {
        StopWatch sw = new StopWatch();
        Connection connection = DatabaseManager.getInstance().getConnection();
        try {
            getRankings = connection
            .prepareStatement("SELECT ranking, service FROM rankingCache WHERE url = ? AND CURRENT_TIMESTAMP - updated < ?");
            getRankings2 = connection.prepareStatement("SELECT ranking, service FROM rankingCache WHERE url = ?");
            addRanking = connection
            .prepareStatement("INSERT INTO rankingCache (url, service, ranking) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE ranking = VALUES(ranking), updated = CURRENT_TIMESTAMP");
        } catch (SQLException e) {
            LOGGER.error(e);
        }
        LOGGER.trace("<init> RankingCacheDB:" + sw.getElapsedTime());
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.web.URLRankingCache#get(tud.iir.knowledge.Source)
     */
    @Override
    public Map<Service, Float> get(String url) {

        Map<Service, Float> result = new HashMap<Service, Float>();

        try {

            PreparedStatement ps;
            if (getTtlSeconds() == -1) {
                // System.out.println("without ttl");
                ps = getRankings2;
            } else {
                // System.out.println("with ttl");
                ps = getRankings;
                ps.setInt(2, getTtlSeconds());
            }
            ps.setString(1, url);
            LOGGER.trace(ps.toString());

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                float ranking = rs.getFloat(1);
                Service service = Service.getById(rs.getInt(2));
                result.put(service, ranking);
                LOGGER.debug("cache hit for " + url + " : " + service + ":" + ranking);
            }
            rs.close();

        } catch (SQLException e) {
            LOGGER.error(e);
        }

        return result;

    }

    /*
     * (non-Javadoc)
     * @see tud.iir.web.URLRankingCache#add(tud.iir.knowledge.Source, java.util.Map)
     */
    @Override
    public void add(String url, Map<Service, Float> rankings) {

        try {

            for (Entry<Service, Float> ranking : rankings.entrySet()) {
                addRanking.setString(1, url);
                addRanking.setInt(2, ranking.getKey().getServiceId());
                addRanking.setFloat(3, ranking.getValue());
                addRanking.executeUpdate();
            }

        } catch (SQLException e) {
            LOGGER.error(e);
        }

    }

    private void clear() {
        new DatabaseManager().runUpdate("TRUNCATE TABLE rankingCache");
    }

    public static void main(String[] args) {

        RankingCacheDB cache = new RankingCacheDB();
        cache.clear();

        // cache.setTtlSeconds(10);
        //
        // cache.add("http://cnn.com/", RankingRetriever.Service.BITLY_CLICKS, 100);
        // cache.get("http://cnn.com/", RankingRetriever.Service.BITLY_CLICKS);
        // cache.add("http://cnn.com/", RankingRetriever.Service.BITLY_CLICKS, 120);
        // ThreadHelper.sleep(5000);
        // cache.get("http://cnn.com/", RankingRetriever.Service.BITLY_CLICKS);
        // ThreadHelper.sleep(5000);
        // cache.get("http://cnn.com/", RankingRetriever.Service.BITLY_CLICKS);
    }

}
