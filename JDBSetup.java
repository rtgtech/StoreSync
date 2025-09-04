import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

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

                String users = "CREATE TABLE IF NOT EXISTS users ("
                        + "name TEXT, pass TEXT);";

                Statement stmt = conn.createStatement();
                System.out.println("Creating storesync.db ...");
                stmt.execute(today_stat_table);
                stmt.execute(monthly_stat_table);
                stmt.execute(upto_date_stat_table);
                stmt.execute(inventory_table);
                stmt.execute(current);
                stmt.execute(users);
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

    public static void checkIntegrity() {
        String dbUrl = "jdbc:sqlite:storesync.db"; 
        Map<String, List<String>> expectedTables = new HashMap<>();
        expectedTables.put("today_stats", Arrays.asList("id", "sales", "exps", "profit", "tot_goods_sld", "cus_count"));
        expectedTables.put("monthly_stats", Arrays.asList("id", "sales", "exps", "profit", "tot_goods_sld", "cus_count"));
        expectedTables.put("upto_date_stats", Arrays.asList("id", "sales", "exps", "profit", "tot_goods_sld", "cus_count"));
        expectedTables.put("inventory", Arrays.asList("id", "item", "count", "price"));
        expectedTables.put("current", Arrays.asList("id", "curr_id", "curr_date"));
        expectedTables.put("users", Arrays.asList("name", "pass"));
        
        boolean corrupted = false;
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            if (conn == null) {
                System.out.println("Corrupted"); 
                return;
            }

            DatabaseMetaData meta = conn.getMetaData();

            for (String tableName : expectedTables.keySet()) {
                boolean tableExists = false;
                try (ResultSet tables = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
                    tableExists = tables.next();
                }

                if (!tableExists) {
                    corrupted = true;
                    break;
                }
                Set<String> actualColumns = new HashSet<>();
                try (ResultSet columns = meta.getColumns(null, null, tableName, null)) {
                    while (columns.next()) {
                        actualColumns.add(columns.getString("COLUMN_NAME"));
                    }
                }

                for (String expectedColumn : expectedTables.get(tableName)) {
                    if (!actualColumns.contains(expectedColumn)) {
                        corrupted = true;
                        break;
                    }
                }

                if (corrupted) break;
            }

        } catch (SQLException e) {
            corrupted = true;
        }

        if (corrupted) {
            System.out.println("DB Corrupted! Delete storesync.db and Re - run JDBSetup");
            System.exit(1);
        } else {
            System.out.println("No issue");
        }
    }

    public static void addNewUser() {
        Scanner ip = new Scanner(System.in);
        try {
            String n, p;
            Connection conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();
            System.out.print("Name : ");
            n = ip.nextLine();
            System.out.print("Password : ");
            p = ip.nextLine();
            stmt.execute("INSERT INTO users (name, pass) VALUES ('"+ n+ "', '"+p +"');");
            System.out.println("New User added!");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        ip.close();
    }

    public static void main(String[] args) {
        File file = new File("storesync.db");
        if (!file.exists()) {
            System.out.println("storesync.db not found. Creating storesync.db ...");
            buildNewTable();
            addNewUser();
        } else {
            Scanner ip = new Scanner(System.in);
            System.out.print("Run Auto Checkup ? [y/n] : ");
            char res = Character.toLowerCase(ip.next().charAt(0));
            ip.nextLine();
            if (res == 'y') {
                checkIntegrity();
            }
            System.out.println("===> INFO <===");
            System.out.println(">>> add // Add new user");
            System.out.println(">>> reset // Delete All Users and Data");
            System.out.print("storesync(setup) >>> ");
            String s_res = ip.nextLine().toLowerCase();
            if (s_res.equals("add")) {
                addNewUser();
            } else if (s_res.equals("reset")) {
                file.delete();
                buildNewTable();
                addNewUser();
            } else {
                System.out.println();
            }
            ip.close();
        }

    }
}
