package tud.iir.web;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import tud.iir.helper.ThreadHelper;
import tud.iir.persistence.DatabaseManager;


/**
 * Cache for {@link URLRankingServices}. As those APIs have a considerable latency, we cache their results for a specific time.
 * 
 * 
 * CREATE TABLE `ranking_features` (
 *   `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
 *   `url` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
 *   `service` tinyint(4) NOT NULL,
 *   `ranking` float NOT NULL,
 *   `updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *   PRIMARY KEY (`id`),
 *   UNIQUE KEY `url_service_unique` (`url`,`service`)
 * ) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=1 ;
 * 
 * CREATE PROCEDURE `add_update_ranking`(IN url VARCHAR(255), IN service INTEGER, IN ranking FLOAT)
 * BEGIN
 *     IF (EXISTS (SELECT * FROM ranking_features AS t WHERE t.url = url AND t.service = service)) THEN
 *         UPDATE ranking_features AS t SET t.ranking = ranking, t.updated = CURRENT_TIMESTAMP WHERE t.url = url AND t.service = service;
 *     ELSE
 *         INSERT INTO ranking_features (url, service, ranking) VALUES (url, service, ranking);
 *     END IF;
 * END
 * 
 * 
 * @author Philipp Katz
 *
 */
public class URLRankingCache {
    
    private static final Logger LOGGER = Logger.getLogger(URLRankingCache.class);
    
    private int ttlSeconds = 60 * 60 * 24;
    
    private Connection connection;
    
    public URLRankingCache() {
        connection = DatabaseManager.getInstance().getConnection();
    }
    
    public float get(String url, URLRankingServices.Service service) {
        
        float result = -1;
        
        try {
            
            PreparedStatement ps = connection.prepareStatement("SELECT ranking FROM ranking_features WHERE url = ? AND service = ? AND CURRENT_TIMESTAMP - updated < ?");
            ps.setString(1, url);
            ps.setInt(2, service.getServiceId());
            ps.setInt(3, ttlSeconds);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = rs.getFloat(1);
                LOGGER.debug("cache hit for " + url + " " + service + " : " + result);
            } else {
                LOGGER.debug("cache fail for " + url + " " + service);
            }
            
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return result;
    }
    
    public void add(String url, URLRankingServices.Service service, float ranking) {
        
        try {
            
            CallableStatement call = connection.prepareCall("{CALL add_update_ranking(?, ?, ?)}");
            call.setString(1, url);
            call.setInt(2, service.getServiceId());
            call.setFloat(3, ranking);
            call.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
    }
    
    public void setTtlSeconds(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
    
    public static void main(String[] args) {
        
        URLRankingCache cache = new URLRankingCache();
        cache.setTtlSeconds(10);
        
        cache.add("http://cnn.com/", URLRankingServices.Service.BITLY_CLICKS, 100);
        cache.get("http://cnn.com/", URLRankingServices.Service.BITLY_CLICKS);
        cache.add("http://cnn.com/", URLRankingServices.Service.BITLY_CLICKS, 120);
        ThreadHelper.sleep(5000);
        cache.get("http://cnn.com/", URLRankingServices.Service.BITLY_CLICKS);
        ThreadHelper.sleep(5000);
        cache.get("http://cnn.com/", URLRankingServices.Service.BITLY_CLICKS);
    }


}
