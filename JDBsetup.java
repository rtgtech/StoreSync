import java.io.File;
import java.sql.*;

public class JDBSetup {
    static String url = "jdbc:sqlite:storesync.db";

    public static void buildNewTable() {
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                int day = LocalDate.now().getDayOfMonth();
                String today_stat_table = "CREATE TABLE IF NOT EXISTS today_stats ("
                        + "id INTEGER,sales REAL,exps REAL,"
                        + "profit REAL,tot_goods_sld INTEGER,"
                        + "cus_count INTEGER);";

                String monthly_stat_table = "CREATE TABLE IF NOT EXISTS monthly_stats ("
                        + "id INTEGER,sales REAL,exps REAL,"
                        + "profit REAL,tot_goods_sld INTEGER,"
                        + "cus_count INTEGER);";

                String upto_date_stat_table = "CREATE TABLE IF NOT EXISTS upto_date_stats ("
                        + "id INTEGER,sales REAL,exps REAL,"
                        + "profit REAL,tot_goods_sld INTEGER,"
                        + "cus_count INTEGER);";

                String inventory_table = "CREATE TABLE IF NOT EXISTS inventory ("
                        + "id INTEGER,item TEXT,count INTEGER,"
                        + "price REAL);";

                String current = "CREATE TABLE IF NOT EXISTS current ("
                        + "id INTEGER, curr_id INTEGER, curr_date INTEGER);";

                Statement stmt = conn.createStatement();
                System.out.println("Creating storesync.db ...");
                stmt.execute(today_stat_table);
                stmt.execute(monthly_stat_table);
                stmt.execute(upto_date_stat_table);
                stmt.execute(inventory_table);
                stmt.execute(current);
                System.out.println("Created Tables ...");

                String init_current = "INSERT INTO current (id, curr_id, curr_date) VALUES (1, 1, " + day + ");";
                String init_1 = "INSERT INTO today_stats (id, sales, exps, profit, tot_goods_sld, cus_count) VALUES (1, 0.0, 0.0, 0.0, 0, 0);";
                String init_2 = "INSERT INTO monthly_stats (id, sales, exps, profit, tot_goods_sld, cus_count) VALUES (1, 0.0, 0.0, 0.0, 0, 0);";
                String init_3 = "INSERT INTO upto_date_stats (id, sales, exps, profit, tot_goods_sld, cus_count) VALUES (1, 0.0, 0.0, 0.0, 0, 0);";
                stmt.execute(init_current);
                stmt.execute(init_1);
                stmt.execute(init_2);
                stmt.execute(init_3);
                System.out.println("Initialized Tables ...");
                
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.out.println("Program abnormally terminated ...");
            System.exit(1);
        }

    }

    public static void main(String[] args) {
        File file = new File("storesync.db");
        if (!file.exists()){
            System.out.println("storesync.db not found. Creating storesync.db ...");
            buildNewTable();
        }
        
    }
}
