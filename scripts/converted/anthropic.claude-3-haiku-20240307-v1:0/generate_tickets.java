import java.sql.*;
import java.util.Random;

public class generate_tickets {

    public static void generateTickets(Connection db_conn, int P_event_id) throws SQLException {
        double standard_price;

        // Get the standard price
        standard_price = new Random().nextDouble() * (50 - 30) + 30;

        try (PreparedStatement event_stmt = db_conn.prepareStatement(
                "SELECT id, location_id FROM sporting_event WHERE ID = ?")) {
            event_stmt.setInt(1, P_event_id);
            try (ResultSet event_rs = event_stmt.executeQuery()) {
                while (event_rs.next()) {
                    int event_id = event_rs.getInt("id");
                    int location_id = event_rs.getInt("location_id");

                    try (PreparedStatement seat_stmt = db_conn.prepareStatement(
                            "INSERT INTO sporting_event_ticket (id, sporting_event_id, sport_location_id, seat_level, seat_section, seat_row, seat, ticket_price) " +
                            "SELECT sporting_event_ticket_seq.nextval, ?, seat.sport_location_id, seat.seat_level, seat.seat_section, seat.seat_row, seat.seat, " +
                            "CASE " +
                            "  WHEN seat.seat_type = 'luxury' THEN 3 * ? " +
                            "  WHEN seat.seat_type = 'premium' THEN 2 * ? " +
                            "  WHEN seat.seat_type = 'standard' THEN ? " +
                            "  WHEN seat.seat_type = 'sub-standard' THEN 0.8 * ? " +
                            "  WHEN seat.seat_type = 'obstructed' THEN 0.5 * ? " +
                            "  WHEN seat.seat_type = 'standing' THEN 0.5 * ? " +
                            "END " +
                            "FROM seat " +
                            "WHERE seat.sport_location_id = ? AND sporting_event.id = ?")) {
                        seat_stmt.setInt(1, event_id);
                        seat_stmt.setDouble(2, standard_price);
                        seat_stmt.setDouble(3, standard_price);
                        seat_stmt.setDouble(4, standard_price);
                        seat_stmt.setDouble(5, standard_price);
                        seat_stmt.setDouble(6, standard_price);
                        seat_stmt.setDouble(7, standard_price);
                        seat_stmt.setInt(8, location_id);
                        seat_stmt.setInt(9, event_id);
                        seat_stmt.executeUpdate();
                    }
                }
            }
        }
    }
}

