import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Random;

public class GenerateMlbSeason {
    public static void generateMibSeason(Connection dbConnection) {
        String sportTypeName = null;
        LocalDate eventDate;

        int dateOffset = 0;

        try (Statement statement = dbConnection.createStatement()) {
            // Get the sport type name for 'baseball'
            ResultSet rs = statement.executeQuery("SELECT name FROM sport_type WHERE LOWER(name) = 'baseball'");
            if (rs.next()) {
                sportTypeName = rs.getString("name");
            }
            rs.close();

            // Iterate over the home teams
            try (ResultSet homeTeams = statement.executeQuery(
                    "SELECT id, home_field_id FROM sport_team " +
                    "WHERE sport_league_short_name = 'MLB' AND sport_type_name = 'baseball' " +
                    "ORDER BY id")) {
                while (homeTeams.next()) {
                    int homeTeamId = homeTeams.getInt("id");
                    int homeFieldId = homeTeams.getInt("home_field_id");

                    // Calculate the start date for the home team's games
                    eventDate = LocalDate.parse(String.format("%04d-03-31", LocalDate.now().getYear()))
                            .with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                            .plusDays(7 * dateOffset);

                    // Iterate over the away teams
                    try (ResultSet awayTeams = statement.executeQuery(
                            "SELECT id, home_field_id FROM sport_team " +
                            "WHERE id > ? AND sport_league_short_name = 'MLB' AND sport_type_name = 'baseball' " +
                            "ORDER BY id", homeTeamId)) {
                        while (awayTeams.next()) {
                            int awayTeamId = awayTeams.getInt("id");
                            int awayFieldId = awayTeams.getInt("home_field_id");

                            // Add a random time offset to the event date
                            eventDate = eventDate.plusHours((long) (new Random().nextDouble() * 7) + 12);

                            // Insert the sporting event
                            statement.executeUpdate(
                                    "INSERT INTO sporting_event (sport_type_name, home_team_id, away_team_id, location_id, start_date_time) " +
                                    "VALUES (?, ?, ?, ?, ?)",
                                    sportTypeName, homeTeamId, awayTeamId, homeFieldId, eventDate);

                            // Move the event date to the next week
                            eventDate = eventDate.plusDays(7);
                        }
                    }

                    // Calculate the start date for the home team's second set of games
                    eventDate = eventDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));

                    // Iterate over the away teams in reverse order
                    try (ResultSet awayTeams = statement.executeQuery(
                            "SELECT id, home_field_id FROM sport_team " +
                            "WHERE id < ? AND sport_league_short_name = 'MLB' AND sport_type_name = 'baseball' " +
                            "ORDER BY id DESC", homeTeamId)) {
                        while (awayTeams.next()) {
                            int awayTeamId = awayTeams.getInt("id");
                            int awayFieldId = awayTeams.getInt("home_field_id");

                            // Add a random time offset to the event date
                            eventDate = eventDate.minusDays(7).plusHours((long) (new Random().nextDouble() * 7) + 12);

                            // Insert the sporting event
                            statement.executeUpdate(
                                    "INSERT INTO sporting_event (sport_type_name, home_team_id, away_team_id, location_id, start_date_time) " +
                                    "VALUES (?, ?, ?, ?, ?)",
                                    sportTypeName, homeTeamId, awayTeamId, homeFieldId, eventDate);
                        }
                    }

                    dateOffset++;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

