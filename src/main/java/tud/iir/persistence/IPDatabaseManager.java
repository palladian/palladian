package tud.iir.persistence;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import tud.iir.helper.FileHelper;
import tud.iir.helper.MathHelper;
import tud.iir.helper.StopWatch;
import tud.iir.web.Crawler;

/**
 * <p>
 * Manage the IP database. The data comes from Maxmind.<br>
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class IPDatabaseManager {

    /** the instance of this class */
    private final static IPDatabaseManager INSTANCE = new IPDatabaseManager();

    /** The logger for this class. */
	private static final Logger LOGGER = Logger.getLogger(IPDatabaseManager.class);
	
	private DatabaseManager dbm = DatabaseManager.getInstance();
	private PreparedStatement psAddEntry;
	private PreparedStatement psTruncate;
    private PreparedStatement psGetInformationByIP;

    private IPDatabaseManager() {
		try {
            psAddEntry = dbm
                    .getConnection()
                    .prepareStatement(
                            "INSERT INTO ip2location SET ipStart=?,countryCode=?,countryName=?,regionCode=?,regionName=?,city=?,zipCode=?,latitude=?,longitude=?,metrocode=?");
            psTruncate = dbm.getConnection().prepareStatement("TRUNCATE ip2location");
            psGetInformationByIP = dbm.getConnection().prepareStatement(
                    "SELECT * FROM `ip2location` WHERE `ipStart` <= INET_ATON(?) ORDER BY ipStart DESC LIMIT 1");
		} catch (SQLException e) {
			LOGGER.error(e.getMessage());
		}		
	}
	
    public static IPDatabaseManager getInstance() {
        return INSTANCE;
    }

    /**
     * Get the country code and country name from a given IP address.
     * 
     * @param ipAddress The IP address.
     * @return An array with two entries, the two-letter country code and the country name.
     */
    public Location getLocationByIP(String ipAddress) {

        Location location = new Location();

        try {
            psGetInformationByIP.setString(1, ipAddress);

            ResultSet rs = dbm.runQuery(psGetInformationByIP);
            if (rs.next()) {
                location.setCountryCode(rs.getString("countryCode"));
                location.setCountryName(rs.getString("countryName"));
                location.setRegionCode(rs.getInt("regionCode"));
                location.setRegionName(rs.getString("regionName"));
                location.setCity(rs.getString("city"));
                location.setZipCode(rs.getInt("zipCode"));
                location.setLatitude(rs.getDouble("latitude"));
                location.setLongitude(rs.getDouble("longitude"));
                location.setMetrocode(rs.getInt("metrocode"));
            }
        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

        return location;
    }

    /**
     * Refresh the IP database.
     */
	public void refreshDB() {
		
        StopWatch sw = new StopWatch();

        LOGGER.info("start refreshing IP database");

        String targetFilename = "data/temp/iptolocation.zip";
        String targetFileUnzip = "data/temp/ip_group_city.csv";

		// download db
        LOGGER.info("downloading ip2location database...");
        Crawler.downloadBinaryFile(
                "http://mirrors.ipinfodb.com/ipinfodb/ip_database/current/ipinfodb_one_table_full.csv.zip",
                targetFilename);
        LOGGER.info("...download completed");
				
		// unzip file
        LOGGER.info("unzipping file...");
        boolean unzipSuccess = FileHelper.unzipFile(targetFilename);

        if (!unzipSuccess) {
            LOGGER.error("...could not unzip file");
            return;
        }
        LOGGER.info("...file unzipped successfully");
		
        int numberOfLines = FileHelper.getNumberOfLines(targetFileUnzip);

        LOGGER.info("start importing data in IP database");
        dbm.runUpdate(psTruncate);
        try {
            FileReader in = new FileReader(targetFileUnzip);
            BufferedReader br = new BufferedReader(in);

            String line = "";
            int c = 0;
            do {
                line = br.readLine();
                if (line == null) {
                    break;
                }

                if (line.startsWith("#") || line.startsWith("\"ip_start")) {
                    continue;
                }

                String[] parts = line.split(";");
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i].replaceAll("\"", "");
                    parts[i] = part;
                }

                try {
                    psAddEntry.setLong(1, Long.valueOf(parts[0]));

                    if (parts[1].length() == 0) {
                        psAddEntry.setNull(2, java.sql.Types.NULL);
                    } else {
                        psAddEntry.setString(2, parts[1]);
                    }

                    if (parts[2].length() == 0) {
                        psAddEntry.setNull(3, java.sql.Types.NULL);
                    } else {
                        psAddEntry.setString(3, parts[2]);
                    }

                    if (parts[3].length() == 0) {
                        psAddEntry.setNull(4, java.sql.Types.NULL);
                    } else {
                        psAddEntry.setString(4, parts[3]);
                    }

                    if (parts[4].length() == 0) {
                        psAddEntry.setNull(5, java.sql.Types.NULL);
                    } else {
                        psAddEntry.setString(5, parts[4]);
                    }

                    if (parts[5].length() == 0) {
                        psAddEntry.setNull(6, java.sql.Types.NULL);
                    } else {
                        psAddEntry.setString(6, parts[5]);
                    }

                    if (parts[6].length() == 0) {
                        psAddEntry.setNull(7, java.sql.Types.NULL);
                    } else {
                        psAddEntry.setString(7, parts[6]);
                    }

                    if (parts[7].length() == 0) {
                        psAddEntry.setNull(8, java.sql.Types.NULL);
                    } else {
                        psAddEntry.setDouble(8, Double.valueOf(parts[7]));
                    }

                    if (parts[8].length() == 0) {
                        psAddEntry.setNull(9, java.sql.Types.NULL);
                    } else {
                        psAddEntry.setDouble(9, Double.valueOf(parts[8]));
                    }

                    if (parts.length == 9 || parts[9].length() == 0) {
                        psAddEntry.setNull(10, java.sql.Types.NULL);
                    } else {
                        psAddEntry.setInt(10, Integer.valueOf(parts[9]));
                    }

                    dbm.runUpdate(psAddEntry);
                } catch (NumberFormatException e) {
                    LOGGER.error(e.getMessage());
                } catch (SQLException e) {
                    LOGGER.error(e.getMessage());
                }

                if (++c % 200000 == 0) {
                    LOGGER.info("still importing, " + MathHelper.round(100 * c / numberOfLines, 2) + "% done");
                }

            } while (line != null);

            in.close();
            br.close();

        } catch (FileNotFoundException e) {
            LOGGER.error(targetFileUnzip + ", " + e.getMessage());
        } catch (IOException e) {
            LOGGER.error(targetFileUnzip + ", " + e.getMessage());
        } catch (OutOfMemoryError e) {
            LOGGER.error(targetFileUnzip + ", " + e.getMessage());
        }

        // clean up
        LOGGER.info("import complete, cleaning up now");
        FileHelper.delete(targetFilename);
        FileHelper.delete(targetFileUnzip);
        FileHelper.delete(FileHelper.getFilePath(targetFileUnzip) + "ip_group_country.csv");

        LOGGER.info("refreshed IP database in " + sw.getElapsedTimeString());
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IPDatabaseManager ipdb = new IPDatabaseManager();
        // ipdb.refreshDB();
        System.out.println(ipdb.getLocationByIP("92.206.9.232"));
	}

}