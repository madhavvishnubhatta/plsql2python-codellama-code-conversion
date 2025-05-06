import java.sql.*;
import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class JsonTest {

    public static void p_json_test(Connection db_conn, String p_in_accounts_json, StringBuilder p_out_accunts_json) {
        try {
            // Prepare the SQL query
            String sql = "SELECT " +
                    "  JSON_OBJECT(" +
                    "    'accountCounts' VALUE JSON_ARRAYAGG(" +
                    "      JSON_OBJECT(" +
                    "        'businessUnitId' VALUE business_unit_id," +
                    "        'parentAccountNumber' VALUE parent_account_number," +
                    "        'accountNumber' VALUE account_number," +
                    "        'totalOnlineContactsCount' VALUE online_contacts_count," +
                    "        'countByPosition' VALUE" +
                    "      JSON_OBJECT(" +
                    "        'taxProfessionalCount' VALUE tax_count," +
                    "        'attorneyCount' VALUE attorney_count," +
                    "        'nonAttorneyCount' VALUE non_attorney_count," +
                    "        'clerkCount' VALUE clerk_count" +
                    "      )" +
                    "    )" +
                    "  )" +
                    "FROM (" +
                    "  SELECT" +
                    "    tab_data.business_unit_id," +
                    "    tab_data.parent_account_number," +
                    "    tab_data.account_number," +
                    "    COUNT(*) online_contacts_count," +
                    "    SUM(CASE WHEN tab_data.position_id = '0095' THEN 1 ELSE 0 END) tax_count," +
                    "    SUM(CASE WHEN tab_data.position_id = '0100' THEN 1 ELSE 0 END) attorney_count," +
                    "    SUM(CASE WHEN tab_data.position_id = '0090' THEN 1 ELSE 0 END) non_attorney_count," +
                    "    SUM(CASE WHEN tab_data.position_id = '0050' THEN 1 ELSE 0 END) clerk_count" +
                    "  FROM aws_test_table scco" +
                    "  CROSS JOIN JSON_TABLE(?, '$.accounts[*]' ERROR ON ERROR " +
                    "    COLUMNS (" +
                    "      parent_account_number PATH '$.parentAccountNumber'," +
                    "      account_number PATH '$.accountNumber'," +
                    "      business_unit_id PATH '$.businessUnitId'" +
                    "    )" +
                    "  ) AS static_data" +
                    "  CROSS JOIN JSON_TABLE(scco.json_doc, '$' ERROR ON ERROR " +
                    "    COLUMNS (" +
                    "      parent_account_number NUMBER PATH '$.data.account.parentAccountNumber'," +
                    "      account_number NUMBER PATH '$.data.account.accountNumber'," +
                    "      business_unit_id NUMBER PATH '$.data.account.businessUnitId'," +
                    "      position_id VARCHAR2(4) PATH '$.data.positionId'" +
                    "    )" +
                    "  ) AS tab_data" +
                    "  WHERE static_data.parent_account_number = tab_data.parent_account_number" +
                    "    AND static_data.account_number = tab_data.account_number" +
                    "    AND static_data.business_unit_id = tab_data.business_unit_id" +
                    "  GROUP BY" +
                    "    tab_data.business_unit_id," +
                    "    tab_data.parent_account_number," +
                    "    tab_data.account_number" +
                    ")";

            // Execute the query
            PreparedStatement stmt = db_conn.prepareStatement(sql);
            stmt.setString(1, p_in_accounts_json);
            ResultSet rs = stmt.executeQuery();

            // Convert the result set to a JSON string
            JsonObject result = new JsonObject();
            JsonArray accountCounts = new JsonArray();
            while (rs.next()) {
                JsonObject account = new JsonObject();
                account.addProperty("businessUnitId", rs.getInt("business_unit_id"));
                account.addProperty("parentAccountNumber", rs.getInt("parent_account_number"));
                account.addProperty("accountNumber", rs.getInt("account_number"));
                account.addProperty("totalOnlineContactsCount", rs.getInt("online_contacts_count"));

                JsonObject countByPosition = new JsonObject();
                countByPosition.addProperty("taxProfessionalCount", rs.getInt("tax_count"));
                countByPosition.addProperty("attorneyCount", rs.getInt("attorney_count"));
                countByPosition.addProperty("nonAttorneyCount", rs.getInt("non_attorney_count"));
                countByPosition.addProperty("clerkCount", rs.getInt("clerk_count"));
                account.add("countByPosition", countByPosition);

                accountCounts.add(account);
            }
            result.add("accountCounts", accountCounts);

            // Set the output parameter
            p_out_accunts_json = new StringBuilder(new Gson().toJson(result));
        } catch (SQLException e) {
            throw new RuntimeException("Error while running the JSON query", e);
        }
    }
}

