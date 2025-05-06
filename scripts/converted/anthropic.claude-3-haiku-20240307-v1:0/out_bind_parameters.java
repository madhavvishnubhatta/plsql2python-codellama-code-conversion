import java.sql.*;

public class test_pg {

    public static void calc_stats_new1(Connection db_conn, int a, int b, int[] result) {
        try {
            int sum = a + b;
            result[0] = sum;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

