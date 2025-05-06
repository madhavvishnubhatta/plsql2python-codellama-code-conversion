import java.sql.*;
import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArtistManager {

    public static void GET_ARTIST_BY_ALBUM(Connection db_conn, String p_artist_id) {
        String v_artist_name = null;

        try {
            String query = "SELECT ART.NAME "
                        + "FROM ALBUM ALB "
                        + "JOIN ARTIST ART USING (ARTISTID) "
                        + "WHERE ALB.TITLE = ?";

            PreparedStatement stmt = db_conn.prepareStatement(query);
            stmt.setString(1, p_artist_id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                v_artist_name = rs.getString("NAME");
            }

            System.out.println("ArtistName: " + v_artist_name);

        } catch (SQLException e) {
            Logger logger = Logger.getLogger(ArtistManager.class.getName());
            logger.log(Level.SEVERE, "Error executing GET_ARTIST_BY_ALBUM", e);
        }
    }
}

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class CustInvoiceByYearAnalyze {

    public static void custInvoiceByYearAnalyze(Connection dbConn) {
        String vCustGenres;
        try (Statement stmt = dbConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT CUSTOMERID, CUSTNAME, LOW_YEAR, HIGH_YEAR, CUST_AVG FROM TMP_CUST_INVOICE_ANALYSE")) {
            while (rs.next()) {
                int customerId = rs.getInt("CUSTOMERID");
                String custName = rs.getString("CUSTNAME");
                String lowYear = rs.getString("LOW_YEAR");
                String highYear = rs.getString("HIGH_YEAR");
                double custAvg = rs.getDouble("CUST_AVG");

                if (Integer.parseInt(lowYear.substring(lowYear.length() - 4)) > Integer.parseInt(highYear.substring(highYear.length() - 4))) {
                    vCustGenres = getCustomerPreferredGenres(dbConn, customerId)
                            .stream()
                            .collect(Collectors.joining(","));
                    System.out.println("Customer: " + custName.toUpperCase() + " - Offer a Discount According To Preferred Genres: " + vCustGenres.toUpperCase());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static List<String> getCustomerPreferredGenres(Connection dbConn, int customerId) {
        List<String> genres = new ArrayList<>();
        try (Statement stmt = dbConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT FUNC_GENRE_BY_ID(TRC.GENREID) AS GENRE " +
                     "FROM TMP_CUST_INVOICE_ANALYSE TMPTBL " +
                     "JOIN INVOICE INV USING(CUSTOMERID) " +
                     "JOIN INVOICELINE INVLIN ON INV.INVOICEID = INVLIN.INVOICEID " +
                     "JOIN TRACK TRC ON TRC.TRACKID = INVLIN.TRACKID " +
                     "WHERE CUSTOMERID = " + customerId)) {
            while (rs.next()) {
                genres.add(rs.getString("GENRE"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return genres;
    }
}

