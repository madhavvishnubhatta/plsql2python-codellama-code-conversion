import java.sql.*;
import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MLBTeamsLoader {

    public static void loadMLBTeams(Connection dbConn) {
        String vDiv;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Create the SQL query to fetch distinct MLB team names and long names
            String query = "SELECT DISTINCT DECODE(TRIM(mlb_team), 'AAA', 'LAA', mlb_team) a_name, "
                    + "DECODE(TRIM(mlb_team_long), 'Anaheim Angels', 'Los Angeles Angels', mlb_team_long) l_name "
                    + "FROM mlb_data";

            stmt = dbConn.prepareStatement(query);
            rs = stmt.executeQuery();

            // Prepare the SQL statement to insert into the sport_team table
            String insertQuery = "INSERT INTO sport_team (name, abbreviated_name, sport_type_name, sport_league_short_name, sport_division_short_name) "
                    + "VALUES (?, ?, 'baseball', 'MLB', ?)";
            PreparedStatement insertStmt = dbConn.prepareStatement(insertQuery);

            while (rs.next()) {
                String aName = rs.getString("a_name");
                String lName = rs.getString("l_name");

                // Determine the division based on the team name
                if (aName.matches("(BAL|BOS|TOR|TB|NYY)")) {
                    vDiv = "AL East";
                } else if (aName.matches("(CLE|DET|KC|CWS|MIN)")) {
                    vDiv = "AL Central";
                } else if (aName.matches("(TEX|SEA|HOU|OAK|LAA)")) {
                    vDiv = "AL West";
                } else if (aName.matches("(WSH|MIA|NYM|PHI|ATL)")) {
                    vDiv = "NL East";
                } else if (aName.matches("(CHC|STL|PIT|MIL|CIN)")) {
                    vDiv = "NL Central";
                } else {
                    vDiv = "NL West";
                }

                // Insert the team information into the sport_team table
                insertStmt.setString(1, lName);
                insertStmt.setString(2, aName);
                insertStmt.setString(3, vDiv);
                insertStmt.executeUpdate();
            }
        } catch (SQLException e) {
            // Handle any SQL exceptions
            e.printStackTrace();
        } finally {
            // Close the database resources
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

