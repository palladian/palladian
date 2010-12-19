package tud.iir.extraction.event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import tud.iir.persistence.DatabaseManager;

public class EventDatabase {

    /** The instance of this class. */
    private final static EventDatabase INSTANCE = new EventDatabase();

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(EventDatabase.class);

    /** The database connection. */
    private Connection connection;

    private PreparedStatement psAddEvent;

    private EventDatabase() {
        try {
            connection = DatabaseManager.getInstance().getConnection();
            prepareStatements();
        } catch (SQLException e) {
            LOGGER.error("SQLException ", e);
        }
    }

    public static EventDatabase getInstance() {
        return INSTANCE;
    }

    private void prepareStatements() throws SQLException {

        Connection connection = DatabaseManager.getInstance().getConnection();
        psAddEvent = connection
                .prepareStatement("INSERT INTO events SET `url` = ?, `title` = ?, `text` = ?, `who` = ?, `what` = ?, `where` = ?, `when` = ?, `why` = ?, `how` = ?");

    }

    public void addEvent(Event event) {

        try {
            psAddEvent.setString(1, event.getUrl());
            psAddEvent.setString(2, event.getTitle());
            psAddEvent.setString(3, event.getText());
            psAddEvent.setString(4, event.getWho());
            psAddEvent.setString(5, event.getWhat());
            psAddEvent.setString(6, event.getWhere());
            psAddEvent.setString(7, event.getWhen());
            psAddEvent.setString(8, event.getWhy());
            psAddEvent.setString(9, event.getHow());

            DatabaseManager.getInstance().runUpdate(psAddEvent);

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        }

    }

}
