package ws.palladian.extraction.location.persistences.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * H2 DB functions. (internal)
 * 
 * @author Philipp Katz
 * @since 2.0
 */
public class DBFunctions {
	public static String getAncestorIds(Connection connection, int locationId) throws SQLException {
		PreparedStatement statement = connection.prepareStatement("WITH RECURSIVE location_ancestors(id, parentId) AS (" //
				+ "  SELECT id, parentId FROM locations WHERE id = ? UNION ALL " //
				+ "  SELECT l.id, l.parentId FROM locations l JOIN location_ancestors p ON l.id = p.parentId" //
				+ ") " //
				+ "SELECT parentId FROM location_ancestors WHERE parentId IS NOT NULL");
		statement.setInt(1, locationId);
		ResultSet resultSet = statement.executeQuery();
		List<String> ancestorIds = new ArrayList<>();
		while (resultSet.next()) {
			ancestorIds.add(resultSet.getString(1));
		}
		Collections.reverse(ancestorIds);
		return "/" + String.join("/", ancestorIds) + "/";
	}

	private DBFunctions() {
		// only static
	}
}
