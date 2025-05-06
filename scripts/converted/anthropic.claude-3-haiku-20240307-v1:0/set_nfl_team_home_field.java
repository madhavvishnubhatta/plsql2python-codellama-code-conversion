import java.sql.*;
import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SetNflTeamHomeField {

    public static void setNflTeamHomeField(Connection dbConn) {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Create the cursor
            String sql = "SELECT sport_location_id, team FROM nfl_stadium_data";
            stmt = dbConn.prepareStatement(sql);
            rs = stmt.executeQuery();

            // Iterate through the cursor
            while (rs.next()) {
                int sportLocationId = rs.getInt("sport_location_id");
                String team = rs.getString("team");

                // Update the sport_team table
                sql = "UPDATE sport_team s " +
                      "SET s.home_field_id = ? " +
                      "WHERE s.name = ? " +
                      "AND s.sport_league_short_name = 'NFL' " +
                      "AND s.sport_type_name = 'football'";
                PreparedStatement updateStmt = dbConn.prepareStatement(sql);
                updateStmt.setInt(1, sportLocationId);
                updateStmt.setString(2, team);
                updateStmt.executeUpdate();
                updateStmt.close();
            }
        } catch (SQLException e) {
            // Handle any SQL exceptions
            e.printStackTrace();
        } finally {
            // Close the resources
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

