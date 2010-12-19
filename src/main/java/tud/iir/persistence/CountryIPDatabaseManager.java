package tud.iir.persistence;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.MathHelper;
import tud.iir.helper.StopWatch;
import tud.iir.web.Crawler;

/**
 * <p>
 * Manage the IP database. The data comes from ip2country and is structured in the following format:<br>
 * # IP FROM IP TO REGISTRY ASSIGNED CTRY CNTRY COUNTRY<br>
 * # "1346797568","1346801663","ripencc","20010601","il","isr","Israel"<br>
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class CountryIPDatabaseManager {

    /** the instance of this class */
    private final static CountryIPDatabaseManager INSTANCE = new CountryIPDatabaseManager();

    /** The logger for this class. */
	private static final Logger LOGGER = Logger.getLogger(CountryIPDatabaseManager.class);
	
	private DatabaseManager dbm = DatabaseManager.getInstance();
	private PreparedStatement psAddEntry;
	private PreparedStatement psTruncate;
    private PreparedStatement psGetInformationByIP;

    private CountryIPDatabaseManager() {
		try {
			psAddEntry = dbm.getConnection().prepareStatement("INSERT INTO ip2country SET ipFrom=?,ipTo=?,registry=?,assignedAt=?,countryCode=?,countryName=?");
            psTruncate = dbm.getConnection().prepareStatement("TRUNCATE ip2country");
            psGetInformationByIP = dbm.getConnection().prepareStatement(
                    "SELECT countryCode, countryName FROM ip2country WHERE ? <= ipTo AND ? >= ipFrom");
		} catch (SQLException e) {
			LOGGER.error(e.getMessage());
		}		
	}
	
    public static CountryIPDatabaseManager getInstance() {
        return INSTANCE;
    }

    /**
     * Get the country code and country name from a given IP address.
     * 
     * @param ipAddress The IP address.
     * @return An array with two entries, the two-letter country code and the country name.
     */
    public String[] getInformationByIP(String ipAddress) {
        String[] information = new String[2];

        Long ipLong = MathHelper.ipToNumber(ipAddress);

        try {
            psGetInformationByIP.setLong(1, ipLong);
            psGetInformationByIP.setLong(2, ipLong);
            ResultSet rs = dbm.runQuery(psGetInformationByIP);
            if (rs.next()) {
                information[0] = rs.getString("countryCode");
                information[1] = rs.getString("countryName");
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return information;
    }

    /**
     * Refresh the IP database.
     */
	public void refreshDB() {
		
        StopWatch sw = new StopWatch();

        LOGGER.info("start refreshing IP database");

		// download db
        Crawler.downloadBinaryFile("http://software77.net/geo-ip/?DL=1", "data/temp/iptocountry.gz");
				
		// unzip file
        FileHelper.unzipFile("data/temp/iptocountry.gz");
		
		// import
        List<String> lines = FileHelper.readFileToArray("data/temp/iptocountry");
		
		dbm.runUpdate(psTruncate);
		for (String line : lines) {
			
			if (line.startsWith("#")) {
				continue;
			}
			
			String[] parts = line.split(",");
			for (int i = 0; i < parts.length; i++) {
				String part = parts[i].replaceAll("\"","");
				parts[i] = part;
			}
			
			try {
                psAddEntry.setLong(1, Long.valueOf(parts[0]));
                psAddEntry.setLong(2, Long.valueOf(parts[1]));
				psAddEntry.setString(3, parts[2]);
                psAddEntry.setTimestamp(4, new Timestamp(1000l * Long.valueOf(parts[3])));
				psAddEntry.setString(5, parts[4]);
                psAddEntry.setString(6, parts[6]);

				dbm.runUpdate(psAddEntry);
			} catch (NumberFormatException e) {
				LOGGER.error(e.getMessage());
			} catch (SQLException e) {
				LOGGER.error(e.getMessage());
			}
			
		}

        // clean up
        FileHelper.delete("data/temp/iptocountry.gz");
        FileHelper.delete("data/temp/iptocountry");

        LOGGER.info("refreshed IP database in " + sw.getElapsedTimeString());
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CountryIPDatabaseManager ipdb = new CountryIPDatabaseManager();
        // ipdb.refreshDB();
        CollectionHelper.print(ipdb.getInformationByIP("92.206.9.232"));
	}

}