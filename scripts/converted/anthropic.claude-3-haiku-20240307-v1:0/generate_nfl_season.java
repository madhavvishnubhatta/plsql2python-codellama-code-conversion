import java.sql.*;
import java.io.*;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GenerateNFLSeason {

    public static void generateNFLSeason(Connection dbConn) {
        String sportTypeName = null;
        Date eventDate = null;
        int dateOffset = 0;

        try (Statement stmt = dbConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sport_type WHERE LOWER(name) = 'football'")) {
            if (rs.next()) {
                sportTypeName = rs.getString("name");
            }
        } catch (SQLException e) {
            // Handle exception
        }

        try (PreparedStatement divCurStmt = dbConn.prepareStatement(
                "SELECT DISTINCT sport_division_short_name " +
                "FROM sport_team " +
                "WHERE sport_type_name = 'football' " +
                "AND sport_league_short_name = 'NFL'");
             ResultSet divCurRS = divCurStmt.executeQuery()) {

            while (divCurRS.next()) {
                String division = divCurRS.getString("sport_division_short_name");
                dateOffset = 0;

                try (PreparedStatement team1Stmt = dbConn.prepareStatement(
                        "SELECT id, home_field_id " +
                        "FROM sport_team " +
                        "WHERE sport_division_short_name = ? " +
                        "AND sport_type_name = 'football' " +
                        "AND sport_league_short_name = 'NFL' " +
                        "ORDER BY id");
                     PreparedStatement team2Stmt = dbConn.prepareStatement(
                        "SELECT id, home_field_id " +
                        "FROM sport_team " +
                        "WHERE id > ? " +
                        "AND sport_division_short_name = ? " +
                        "AND sport_type_name = 'football' " +
                        "AND sport_league_short_name = 'NFL' " +
                        "ORDER BY id");
                     PreparedStatement team3Stmt = dbConn.prepareStatement(
                        "SELECT id, home_field_id " +
                        "FROM sport_team " +
                        "WHERE id < ? " +
                        "AND sport_division_short_name = ? " +
                        "AND sport_type_name = 'football' " +
                        "AND sport_league_short_name = 'NFL' " +
                        "ORDER BY id")) {

                    team1Stmt.setString(1, division);
                    try (ResultSet team1RS = team1Stmt.executeQuery()) {
                        while (team1RS.next()) {
                            int homeTeamId = team1RS.getInt("id");
                            int homeFieldId = team1RS.getInt("home_field_id");
                            eventDate = new Date(new Date("01-SEP-" + new SimpleDateFormat("yyyy").format(new Date())).getTime() + 7 * dateOffset * 86400000L);

                            team2Stmt.setInt(1, homeTeamId);
                            team2Stmt.setString(2, division);
                            try (ResultSet team2RS = team2Stmt.executeQuery()) {
                                while (team2RS.next()) {
                                    int awayTeamId = team2RS.getInt("id");
                                    eventDate = new Date(eventDate.getTime() + (long) (Math.random() * 7 * 86400000L) / 24);
                                    insertSportingEvent(dbConn, sportTypeName, homeTeamId, awayTeamId, homeFieldId, eventDate);
                                    eventDate = new Date(eventDate.getTime() + 7 * 86400000L);
                                }
                            }

                            eventDate = new Date(new Date(eventDate.getTime() + 7 * 86400000L).getTime() + (long) (Math.random() * 7 * 86400000L) / 24);

                            team3Stmt.setInt(1, homeTeamId);
                            team3Stmt.setString(2, division);
                            try (ResultSet team3RS = team3Stmt.executeQuery()) {
                                while (team3RS.next()) {
                                    int awayTeamId = team3RS.getInt("id");
                                    insertSportingEvent(dbConn, sportTypeName, homeTeamId, awayTeamId, homeFieldId, eventDate);
                                }
                            }

                            dateOffset++;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // Handle exception
        }

        try (PreparedStatement crossDivCurStmt = dbConn.prepareStatement(
                "SELECT rownum AS cur_row, " +
                "       DECODE(MOD(rownum, 2), 0, a.id, 1, b.id) AS home_id, " +
                "       a.id AS t2_id, a.home_field_id AS t2_field_id, " +
                "       b.id AS t1_id, b.home_field_id AS t1_field_id " +
                "FROM sport_team a, sport_team b " +
                "WHERE a.sport_division_short_name = ? " +
                "  AND b.sport_division_short_name = ? " +
                "ORDER BY a.name, b.name");
             PreparedStatement insertEventStmt = dbConn.prepareStatement(
                "INSERT INTO sporting_event(sport_type_name, home_team_id, away_team_id, location_id, start_date_time) " +
                "VALUES(?, ?, ?, ?, ?)")) {

            Map<Integer, String> nfcTab = new HashMap<>();
            Map<Integer, String> afcTab = new HashMap<>();
            Map<Integer, Date> dateTab = new HashMap<>();

            eventDate = new Date(new Date(eventDate.getTime() + 7 * 86400000L).getTime() + (long) (Math.random() * 7 * 86400000L) / 24);
            dateTab.put(1, eventDate);
            dateTab.put(6, eventDate);
            dateTab.put(11, eventDate);
            dateTab.put(16, eventDate);

            eventDate = new Date(new Date(eventDate.getTime() + 7 * 86400000L).getTime() + (long) (Math.random() * 7 * 86400000L) / 24);
            dateTab.put(2, eventDate);
            dateTab.put(7, eventDate);
            dateTab.put(12, eventDate);
            dateTab.put(13, eventDate);

            eventDate = new Date(new Date(eventDate.getTime() + 7 * 86400000L).getTime() + (long) (Math.random() * 7 * 86400000L) / 24);
            dateTab.put(3, eventDate);
            dateTab.put(8, eventDate);
            dateTab.put(9, eventDate);
            dateTab.put(14, eventDate);

            eventDate = new Date(new Date(eventDate.getTime() + 7 * 86400000L).getTime() + (long) (Math.random() * 7 * 86400000L) / 24);
            dateTab.put(4, eventDate);
            dateTab.put(5, eventDate);
            dateTab.put(10, eventDate);
            dateTab.put(15, eventDate);

            nfcTab.put(1, "NFC North");
            nfcTab.put(2, "NFC East");
            nfcTab.put(3, "NFC South");
            nfcTab.put(4, "NFC West");
            afcTab.put(1, "AFC North");
            afcTab.put(2, "AFC East");
            afcTab.put(3, "AFC South");
            afcTab.put(4, "AFC West");

            for (int i = 1; i <= 4; i++) {
                crossDivCurStmt.setString(1, nfcTab.get(i));
                crossDivCurStmt.setString(2, afcTab.get(i));
                try (ResultSet crossDivCurRS = crossDivCurStmt.executeQuery()) {
                    while (crossDivCurRS.next()) {
                        int homeId = crossDivCurRS.getInt("home_id");
                        int t2Id = crossDivCurRS.getInt("t2_id");
                        int t1Id = crossDivCurRS.getInt("t1_id");
                        int t2FieldId = crossDivCurRS.getInt("t2_field_id");
                        int t1FieldId = crossDivCurRS.getInt("t1_field_id");
                        int curRow = crossDivCurRS.getInt("cur_row");

                        insertEventStmt.setString(1, sportTypeName);
                        if (homeId == t2Id) {
                            insertEventStmt.setInt(2, t2Id);
                            insertEventStmt.setInt(3, t1Id);
                            insertEventStmt.setInt(4, t2FieldId);
                        } else {
                            insertEventStmt.setInt(2, t1Id);
                            insertEventStmt.setInt(3, t2Id);
                            insertEventStmt.setInt(4, t1FieldId);
                        }
                        insertEventStmt.setDate(5, dateTab.get(curRow));
                        insertEventStmt.executeUpdate();
                    }
                }
            }

            afcTab.put(1, "AFC West");
            afcTab.put(2, "AFC North");
            afcTab.put(3, "AFC East");
            afcTab.put(4, "AFC South");

            for (int i = 1; i <= 4; i++) {
                crossDivCurStmt.setString(1, nfcTab.get(i));
                crossDivCurStmt.setString(2, afcTab.get(i));
                try (ResultSet crossDivCurRS = crossDivCurStmt.executeQuery()) {
                    while (crossDivCurRS.next()) {
                        int homeId = crossDivCurRS.getInt("home_id");
                        int t2Id = crossDivCurRS.getInt("t2_id");
                        int t1Id = crossDivCurRS.getInt("t1_id");
                        int t2FieldId = crossDivCurRS.getInt("t2_field_id");
                        int t1FieldId = crossDivCurRS.getInt("t1_field_id");
                        int curRow = crossDivCurRS.getInt("cur_row");

                        insertEventStmt.setString(1, sportTypeName);
                        if (homeId == t2Id) {
                            insertEventStmt.setInt(2, t2Id);
                            insertEventStmt.setInt(3, t1Id);
                            insertEventStmt.setInt(4, t2FieldId);
                        } else {
                            insertEventStmt.setInt(2, t1Id);
                            insertEventStmt.setInt(3, t2Id);
                            insertEventStmt.setInt(4, t1FieldId);
                        }
                        insertEventStmt.setDate(5, dateTab.get(curRow));
                        insertEventStmt.executeUpdate();
                    }
                }
            }

            afcTab.put(1, "AFC South");
            afcTab.put(2, "AFC West");
            afcTab.put(3, "AFC North");
            afcTab.put(4, "AFC East");

            for (int i = 1; i <= 3; i++) {
                crossDivCurStmt.setString(1, nfcTab.get(i));
                crossDivCurStmt.setString(2, afcTab.get(i));
                try (ResultSet crossDivCurRS = crossDivCurStmt.executeQuery()) {
                    while (crossDivCurRS.next()) {
                        int homeId = crossDivCurRS.getInt("home_id");
                        int t2Id = crossDivCurRS.getInt("t2_id");
                        int t1Id = crossDivCurRS.getInt("t1_id");
                        int t2FieldId = crossDivCurRS.getInt("t2_field_id");
                        int t1FieldId = crossDivCurRS.getInt("t1_field_id");
                        int curRow = crossDivCurRS.getInt("cur_row");

                        insertEventStmt.setString(1, sportTypeName);
                        if (homeId == t2Id) {
                            insertEventStmt.setInt(2, t2Id);
                            insertEventStmt.setInt(3, t1Id);
                            insertEventStmt.setInt(4, t2FieldId);
                        } else {
                            insertEventStmt.setInt(2, t1Id);
                            insertEventStmt.setInt(3, t2Id);
                            insertEventStmt.setInt(4, t1FieldId);
                        }
                        insertEventStmt.setDate(5, dateTab.get(curRow));
                        insertEventStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            // Handle exception
        }
    }

    private static void insertSportingEvent(Connection dbConn, String sportTypeName, int homeTeamId, int awayTeamId, int locationId, Date startDateTime) {
        try (PreparedStatement insertEventStmt = dbConn.prepareStatement(
                "INSERT INTO sporting_event(sport_type_name, home_team_id, away_team_id, location_id, start_date_time) " +
                "VALUES(?, ?, ?, ?, ?)")) {
            insertEventStmt.setString(1, sportTypeName);
            insertEventStmt.setInt(2, homeTeamId);
            insertEventStmt.setInt(3, awayTeamId);
            insertEventStmt.setInt(4, locationId);
            insertEventStmt.setDate(5, startDateTime);
            insertEventStmt.executeUpdate();
        } catch (SQLException e) {
            // Handle exception
        }
    }
}

