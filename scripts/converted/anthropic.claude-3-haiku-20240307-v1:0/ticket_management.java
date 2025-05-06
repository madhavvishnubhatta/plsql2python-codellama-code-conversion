import java.sql.*;
import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SellTickets {

    public static void sellTickets(Connection db_conn, int person_id, int event_id, int quantity) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            // Get event details
            EventDetails event_rec = getEventDetails(db_conn, event_id);
            
            // Find available adjacent seats
            String query = "SELECT seat_level, seat_section, seat_row "
                         + "FROM sporting_event_ticket "
                         + "WHERE sporting_event_id = ? "
                         + "AND ticketholder_id IS NULL "
                         + "GROUP BY seat_level, seat_section, seat_row "
                         + "HAVING COUNT(*) >= ? "
                         + "FETCH FIRST 1 ROWS ONLY";
            stmt = db_conn.prepareStatement(query);
            stmt.setInt(1, event_id);
            stmt.setInt(2, quantity);
            rs = stmt.executeQuery();
            
            if (!rs.next()) {
                throw new SQLException("Sorry, there aren't " + quantity + " adjacent seats for event: "
                                     + event_rec.homeTeamName + " VS " + event_rec.awayTeamName + " (" + event_rec.sportName + ")"
                                     + "\n   " + event_rec.homeField + ": " + event_rec.dateTime);
            }
            
            int seatLevel = rs.getInt("seat_level");
            String seatSection = rs.getString("seat_section");
            String seatRow = rs.getString("seat_row");
            
            // Lock and update adjacent seats
            query = "SELECT * "
                  + "FROM sporting_event_ticket "
                  + "WHERE sporting_event_id = ? "
                  + "AND seat_level = ? "
                  + "AND seat_section = ? "
                  + "AND seat_row = ? "
                  + "ORDER BY seat_level, seat_section, seat_row "
                  + "FOR UPDATE OF ticketholder_id";
            stmt = db_conn.prepareStatement(query);
            stmt.setInt(1, event_id);
            stmt.setInt(2, seatLevel);
            stmt.setString(3, seatSection);
            stmt.setString(4, seatRow);
            rs = stmt.executeQuery();
            
            for (int i = 0; i < quantity; i++) {
                if (!rs.next()) {
                    throw new SQLException("Unexpected error occurred while locking seats.");
                }
                
                int ticketId = rs.getInt("id");
                updateTicketHolder(db_conn, ticketId, person_id, rs.getDouble("ticket_price"));
            }
            
            db_conn.commit();
        } catch (SQLException e) {
            db_conn.rollback();
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    private static EventDetails getEventDetails(Connection db_conn, int eventId) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            String query = "SELECT e.home_team_name, e.away_team_name, e.sport_name, e.home_field, e.date_time "
                         + "FROM sporting_event e "
                         + "WHERE e.id = ?";
            stmt = db_conn.prepareStatement(query);
            stmt.setInt(1, eventId);
            rs = stmt.executeQuery();
            
            if (!rs.next()) {
                throw new SQLException("Event not found.");
            }
            
            EventDetails event = new EventDetails();
            event.homeTeamName = rs.getString("home_team_name");
            event.awayTeamName = rs.getString("away_team_name");
            event.sportName = rs.getString("sport_name");
            event.homeField = rs.getString("home_field");
            event.dateTime = rs.getTimestamp("date_time");
            
            return event;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    private static void updateTicketHolder(Connection db_conn, int ticketId, int personId, double purchasePrice) throws SQLException {
        PreparedStatement stmt = null;
        
        try {
            String query = "UPDATE sporting_event_ticket "
                         + "SET ticketholder_id = ? "
                         + "WHERE id = ?";
            stmt = db_conn.prepareStatement(query);
            stmt.setInt(1, personId);
            stmt.setInt(2, ticketId);
            stmt.executeUpdate();
            
            query = "INSERT INTO ticket_purchase_hist (sporting_event_ticket_id, purchased_by_id, transaction_date_time, purchase_price) "
                  + "VALUES (?, ?, CURRENT_TIMESTAMP, ?)";
            stmt = db_conn.prepareStatement(query);
            stmt.setInt(1, ticketId);
            stmt.setInt(2, personId);
            stmt.setDouble(3, purchasePrice);
            stmt.executeUpdate();
        } finally {
            if (stmt != null) stmt.close();
        }
    }

    private static class EventDetails {
        public String homeTeamName;
        public String awayTeamName;
        public String sportName;
        public String homeField;
        public Timestamp dateTime;
    }
}

import java.sql.*;
import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TicketTransfer {

    public static void transferTicket(Connection db_conn, int ticket_id, int new_ticketholder_id, boolean transfer_all, Double price) throws SQLException {
        int xferall = 0;
        int old_ticketholder_id;
        Timestamp last_txn_date;

        if (transfer_all) {
            xferall = 1;
        }

        try (PreparedStatement stmt = db_conn.prepareStatement(
                "SELECT MAX(h.transaction_date_time) AS transaction_date_time, t.ticketholder_id AS ticketholder_id " +
                "FROM ticket_purchase_hist h, sporting_event_ticket t " +
                "WHERE t.id = ? " +
                "AND h.purchased_by_id = t.ticketholder_id " +
                "AND (h.sporting_event_ticket_id = ? OR ? = 1) " +
                "GROUP BY t.ticketholder_id")) {
            stmt.setInt(1, ticket_id);
            stmt.setInt(2, ticket_id);
            stmt.setInt(3, xferall);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    last_txn_date = rs.getTimestamp("transaction_date_time");
                    old_ticketholder_id = rs.getInt("ticketholder_id");
                }
            }
        }

        try (PreparedStatement stmt = db_conn.prepareStatement(
                "SELECT * FROM ticket_purchase_hist " +
                "WHERE purchased_by_id = ? " +
                "AND transaction_date_time = ?")) {
            stmt.setInt(1, old_ticketholder_id);
            stmt.setTimestamp(2, last_txn_date);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int sporting_event_ticket_id = rs.getInt("sporting_event_ticket_id");
                    try (PreparedStatement updateStmt = db_conn.prepareStatement(
                            "UPDATE sporting_event_ticket " +
                            "SET ticketholder_id = ? " +
                            "WHERE id = ?")) {
                        updateStmt.setInt(1, new_ticketholder_id);
                        updateStmt.setInt(2, sporting_event_ticket_id);
                        updateStmt.executeUpdate();
                    }
                    try (PreparedStatement insertStmt = db_conn.prepareStatement(
                            "INSERT INTO ticket_purchase_hist(sporting_event_ticket_id, purchased_by_id, transferred_from_id, transaction_date_time, purchase_price) " +
                            "VALUES(?, ?, ?, ?, ?)")) {
                        insertStmt.setInt(1, sporting_event_ticket_id);
                        insertStmt.setInt(2, new_ticketholder_id);
                        insertStmt.setInt(3, old_ticketholder_id);
                        insertStmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                        insertStmt.setDouble(5, price != null ? price : rs.getDouble("purchase_price"));
                        insertStmt.executeUpdate();
                    }
                }
            }
        }

        db_conn.commit();
    }
}

import java.sql.*;
import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TicketActivityGenerator {

    public static void generateTicketActivity(Connection db_conn, int transaction_delay, int max_transactions) {
        int txn_count = 0;

        try {
            while (txn_count < max_transactions) {
                sellRandomTickets(db_conn);
                txn_count++;
                Thread.sleep(transaction_delay * 1000); // Convert delay from seconds to milliseconds
            }
        } catch (SQLException e) {
            // Handle SQL exceptions
            e.printStackTrace();
        } catch (InterruptedException e) {
            // Handle thread sleep interruptions
            e.printStackTrace();
        }
    }

    private static void sellRandomTickets(Connection db_conn) throws SQLException {
        // Call the existing Java function to sell random tickets
        TicketSalesService.sellRandomTickets(db_conn);
    }
}

import java.sql.*;
import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseTransferActivity {

    public static void generateTransferActivity(Connection db_conn, int transaction_delay, int max_transactions) {
        int txn_count = 0;
        int min_tik_id, max_tik_id, tik_id;
        int new_ticketholder;
        boolean xfer_all, chg_price;
        double new_price;

        PreparedStatement stmt1, stmt2, stmt3;

        try {
            while (txn_count < max_transactions) {
                // Get min and max ticket IDs
                stmt1 = db_conn.prepareStatement("SELECT MIN(sporting_event_ticket_id), MAX(sporting_event_ticket_id) FROM ticket_purchase_hist");
                ResultSet rs1 = stmt1.executeQuery();
                if (rs1.next()) {
                    min_tik_id = rs1.getInt(1);
                    max_tik_id = rs1.getInt(2);
                }
                rs1.close();
                stmt1.close();

                // Get a random ticket ID
                stmt2 = db_conn.prepareStatement("SELECT MAX(sporting_event_ticket_id) FROM ticket_purchase_hist WHERE sporting_event_ticket_id <= ?");
                stmt2.setDouble(1, Math.random() * (max_tik_id - min_tik_id) + min_tik_id);
                ResultSet rs2 = stmt2.executeQuery();
                if (rs2.next()) {
                    tik_id = rs2.getInt(1);
                }
                rs2.close();
                stmt2.close();

                // Get a random new ticketholder
                new_ticketholder = (int) Math.floor(Math.random() * (g_max_person_id - g_min_person_id + 1)) + g_min_person_id;

                // Decide whether to transfer all tickets
                xfer_all = (Math.round(Math.random() * 4) < 4); // 80% of the time

                // Decide whether to change the ticket price
                chg_price = (Math.round(Math.random() * 2) == 0); // 30% of the time
                if (chg_price) {
                    stmt3 = db_conn.prepareStatement("SELECT ticket_price FROM sporting_event_ticket WHERE id = ?");
                    stmt3.setInt(1, tik_id);
                    ResultSet rs3 = stmt3.executeQuery();
                    if (rs3.next()) {
                        new_price = rs3.getDouble(1) * (0.8 + 0.4 * Math.random());
                    }
                    rs3.close();
                    stmt3.close();
                } else {
                    new_price = 0;
                }

                transferTicket(db_conn, tik_id, new_ticketholder, xfer_all, new_price);

                txn_count++;
                Thread.sleep(transaction_delay * 1000);
            }
        } catch (SQLException | InterruptedException e) {
            Logger logger = Logger.getLogger(DatabaseTransferActivity.class.getName());
            logger.log(Level.SEVERE, "Error in generateTransferActivity", e);
        }
    }

    private static void transferTicket(Connection db_conn, int tik_id, int new_ticketholder, boolean xfer_all, double new_price) {
        // Call the transferTicket Java function here
    }

    private static final int g_min_person_id = 1;
    private static final int g_max_person_id = 1000000;
}

import java.sql.*;
import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventDetailsRetriever {

    public static EventRecType getEventDetails(Connection dbConn, int eventId) throws SQLException {
        EventRecType eventRec = new EventRecType();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String sql = "SELECT e.sport_type_name, h.name, a.name, l.name, e.start_date_time "
                    + "FROM sporting_event e "
                    + "JOIN sport_team h ON e.home_team_id = h.id "
                    + "JOIN sport_team a ON e.away_team_id = a.id "
                    + "JOIN sport_location l ON e.location_id = l.id "
                    + "WHERE e.id = ?";

            stmt = dbConn.prepareStatement(sql);
            stmt.setInt(1, eventId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                eventRec.sport_name = rs.getString(1);
                eventRec.home_team_name = rs.getString(2);
                eventRec.away_team_name = rs.getString(3);
                eventRec.home_field = rs.getString(4);
                eventRec.date_time = rs.getTimestamp(5);
            }
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }

        return eventRec;
    }

    public static class EventRecType {
        public String sport_name;
        public String home_team_name;
        public String away_team_name;
        public String home_field;
        public Timestamp date_time;
    }
}

import java.sql.*;
import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseOperations {

    public static void sellRandomTickets(Connection db_conn) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // Get open events
            stmt = db_conn.prepareStatement("SELECT id FROM events WHERE is_open = 1");
            rs = stmt.executeQuery();
            EventTab event_tab = new EventTab();
            while (rs.next()) {
                event_tab.add(rs.getInt("id"));
            }

            // Select a random event
            int row_ct = event_tab.size();
            int event_idx = (int) Math.floor(Math.random() * (row_ct - 1) + 1);
            int event_id = event_tab.get(event_idx);

            // Select a random person
            int ticket_holder = (int) Math.floor(Math.random() * (g_max_person_id - g_min_person_id + 1)) + g_min_person_id;

            // Sell random number of tickets
            int quantity = (int) Math.floor(Math.random() * 5) + 1;
            sellTickets(db_conn, ticket_holder, event_id, quantity);
        } catch (SQLException e) {
            Logger logger = Logger.getLogger(DatabaseOperations.class.getName());
            logger.log(Level.SEVERE, "Error in sellRandomTickets", e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                Logger logger = Logger.getLogger(DatabaseOperations.class.getName());
                logger.log(Level.SEVERE, "Error closing resources", e);
            }
        }
    }

    private static void sellTickets(Connection db_conn, int ticket_holder, int event_id, int quantity) {
        // Call the sellTickets Java function
        SalesOperations.sellTickets(db_conn, ticket_holder, event_id, quantity);
    }

    private static class EventTab extends ArrayList<Integer> {
        // Custom class to hold event IDs
    }

    private static final int g_min_person_id = 1;
    private static final int g_max_person_id = 1000000;
}

import java.sql.*;
import java.util.*;

public class generate_mlb_season {

    public static List<SportingEvent> getOpenEvents(Connection db_conn) {
        List<SportingEvent> eventTab = new ArrayList<>();

        try (Statement stmt = db_conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM sporting_event WHERE sold_out = 0 ORDER BY start_date_time")) {
            while (rs.next()) {
                SportingEvent event = new SportingEvent();
                event.setEventId(rs.getInt("event_id"));
                event.setEventName(rs.getString("event_name"));
                event.setStartDateTime(rs.getTimestamp("start_date_time"));
                event.setSoldOut(rs.getBoolean("sold_out"));
                eventTab.add(event);
            }
        } catch (SQLException e) {
            // Handle database exceptions
            e.printStackTrace();
        }

        return eventTab;
    }
}

class SportingEvent {
    private int eventId;
    private String eventName;
    private Timestamp startDateTime;
    private boolean soldOut;

    // Getters and setters
    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Timestamp getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(Timestamp startDateTime) {
        this.startDateTime = startDateTime;
    }

    public boolean isSoldOut() {
        return soldOut;
    }

    public void setSoldOut(boolean soldOut) {
        this.soldOut = soldOut;
    }
}

