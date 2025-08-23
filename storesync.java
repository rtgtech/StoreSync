import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.time.LocalDate;
import java.awt.geom.RoundRectangle2D;

class BlueButton extends JButton {
    private int cornerRadius = 20;

    BlueButton(String text, int x, int y, int w, int h) {
        super(text);
        this.setBounds(x, y, w, h);
        this.setFont(new Font("Eras Demi ITC", Font.PLAIN, 28));
        this.setForeground(Color.WHITE);
        this.setBackground(new Color(51, 102, 255));
        this.setFocusPainted(false);
        this.setContentAreaFilled(false); 
        this.setBorderPainted(false);
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (getModel().isArmed()) {
            g2.setColor(getBackground().darker());
        } else if (getModel().isRollover()) {
            g2.setColor(getBackground().brighter());
        } else {
            g2.setColor(getBackground());
        }
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        super.paintComponent(g); // default text painting
        g2.dispose();
    }
    @Override
    protected void paintBorder(Graphics g) {}
    @Override
    public boolean contains(int x, int y) {
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        return shape.contains(x, y);
    }
}

class Item {
    int id;
    String name;
    int quantiy;
    double price;

    Item(int id, String s, int q, double p) {
        this.id = id;
        name = s;
        quantiy = q;
        price = p;
    }
}

class Table {
    ArrayList<Item> items;

    Table() {
        items = new ArrayList<>();
    }

    String[] getAsString() {
        String[] row = new String[20];

        return row;
    }

    void print() {
        for (Item i : items) {
            System.out.println("[" + i.id + "|" + i.name + "|" + i.quantiy + "|" + i.price + "]");
        }
    }

    String[] returnAsStirngL() {
        String[] rows = new String[items.size()];
        int i = 0;
        for (Item it : items) {
            rows[i] = String.format("%-14s", it.name).substring(0, 14) + " | Qty: " + String.valueOf(it.quantiy)
                    + " | price: " + String.valueOf(it.price);
            i++;
        }
        return rows;
    }
}

class MyList extends JList<String> {
    int size;

    MyList(String[] list) {
        super(list);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                // Set custom font and size

                label.setFont(new Font("Eras Demi ITC", Font.PLAIN, 30)); // Font: Arial, Size: 16
                return label;
            }
        });
    }
}

class ScrollPane extends JScrollPane {
    ScrollPane(MyList list, int x, int y, int w, int h) {
        super(list);

        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) { // Prevent double events
                    String selectedItem = list.getSelectedValue();
                    if (selectedItem != null) {
                        // Call the user-defined function
                    }
                }
            }
        });

        this.setBounds(x, y, w, h);
    }
}

class SqlOperations {
    String url = "jdbc:sqlite:storesync-db.db";
    Connection conn;
    Statement stmt;

    SqlOperations() {
        try {
            conn = DriverManager.getConnection(url);
            stmt = conn.createStatement();

            int day = LocalDate.now().getDayOfMonth();
            ResultSet rs = stmt.executeQuery("SELECT * FROM current WHERE id = 1;");
            rs.next();
            int curr_day = rs.getInt(3);
            rs.close();
            if (day != curr_day) {
                stmt.execute("UPDATE current SET curr_date = " + day + " WHERE id = 1;");
                stmt.execute(
                        "UPDATE today_stats SET sales = 0.0, exps = 0.0, profit = 0.0, tot_goods_sld = 0, cus_count = 0 WHERE id = 1;");
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    void delete_inv_itm(int id) {
        try {
            stmt.execute("DELETE FROM inventory WHERE id = " + id + ";");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    int get_count_inv() {
        int n = -1;
        try {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM inventory WHERE count > 0;");
            n = rs.getInt(1);
            rs.close();
            return n;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return n;
    }

    void update_itm_qty(Item i, int n) {
        try {
            stmt.execute("UPDATE today_stats SET exps = exps + " + (i.price * (n - i.quantiy)) + " WHERE id = 1;");
            stmt.execute("UPDATE monthly_stats SET exps = exps + " + (i.price * (n - i.quantiy)) + " WHERE id = 1;");
            stmt.execute("UPDATE upto_date_stats SET exps = exps + " + (i.price * (n - i.quantiy)) + " WHERE id = 1;");
            stmt.execute("UPDATE inventory SET count = " + n + " WHERE id = " + i.id + ";");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    MyList get_daily_stats() {
        String[] rows = new String[5];
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM today_stats");
            while (rs.next()) {
                rows[0] = "Sales : " + rs.getString(2);
                rows[1] = "Expense : " + rs.getString(3);
                rows[2] = "Profit : " + rs.getString(4);
                rows[3] = "Total no : " + rs.getString(5);
                rows[4] = "customers : " + rs.getString(6);
            }
            rs.close();
        } catch (Exception e) {

        }

        return new MyList(rows);
    }

    MyList get_monthly_stats() {
        String[] rows = new String[5];
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM monthly_stats");
            while (rs.next()) {
                rows[0] = "Sales : " + rs.getString(2);
                rows[1] = "Expense : " + rs.getString(3);
                rows[2] = "Profit : " + rs.getString(4);
                rows[3] = "Total no : " + rs.getString(5);
                rows[4] = "customers : " + rs.getString(6);
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return new MyList(rows);
    }

    MyList get_all_stats() {
        String[] rows = new String[5];
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM upto_date_stats");
            while (rs.next()) {
                rows[0] = "Sales : " + rs.getString(2);
                rows[1] = "Expense : " + rs.getString(3);
                rows[2] = "Profit : " + rs.getString(4);
                rows[3] = "Total no : " + rs.getString(5);
                rows[4] = "customers : " + rs.getString(6);
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return new MyList(rows);
    }

    MyList get_inv_items() {
        return new MyList(get_inv_itemStrings());
    }

    String[] get_inv_itemStrings() {
        ArrayList<String> list = new ArrayList<>();
        String name;
        String title = "";
        int i = 0;
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM inventory");
            while (rs.next()) {
                name = rs.getString(2);
                if (name.length() > 15) {
                    title = name.substring(0, 14) + "...";
                } else {
                    title = String.format("%-17s", name);
                }
                title += String.format(" | Qty:%4d", rs.getInt(3)) + String.format(" | Price:%5.2f", rs.getFloat(4));
                list.add(title);
            }
            rs.close();
        } catch (SQLException e) {

        }
        String[] rows = new String[list.size()];
        for (String s: list){
            rows[i++] = s;
        }
        return rows;
    }

    String[] get_inv_item_names() {
        ArrayList<String> list = new ArrayList<>();
        int i=0;
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM inventory");
            while (rs.next()) {
                list.add(rs.getString(2));
            }
            rs.close();
        } catch (SQLException e) {

        }
        String[] rows = new String[list.size()];
        for (String s: list){
            rows[i++] = s;
        }
        return rows;
    }

    Item getSpecificItem(String s) {
        Item item = new Item(0, "", 0, 0);
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM inventory");
            while (rs.next()) {
                if (String.format("%-14s", rs.getString(2)).substring(0, 14).equals(s)) {
                    item.id = rs.getInt(1);
                    item.name = rs.getString(2);
                    item.quantiy = rs.getInt(3);
                    item.price = rs.getDouble(4);
                    break;
                }
            }
            rs.close();
        } catch (Exception e) {
        }
        return item;
    }

    MyList get_low_inv_items() {
        String[] items = new String[20];
        MyList list;
        int i = 0;
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM inventory WHERE count < 3;");
            while (rs.next()) {
                items[i] = rs.getString(2);
                i++;
            }
            rs.close();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        list = new MyList(items);
        list.size = i;
        return list;
    }

    int get_current_id() {
        int i = 0;
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM current WHERE id = 1");
            rs.next();
            i = rs.getInt(2);
            rs.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return i;
    }

    void add_new_item(String s, int q, double p) {
        try {
            stmt.execute("UPDATE current SET curr_id = curr_id + 1 WHERE id = 1");
            stmt.execute("INSERT INTO inventory (id, item, count, price) VALUES (" + get_current_id() + ", '" + s
                    + "', " + q + ", " + p + ");");
            stmt.execute("UPDATE today_stats SET exps = exps + " + (q * p) + " WHERE id = 1;");
            stmt.execute("UPDATE monthly_stats SET exps = exps + " + (q * p) + " WHERE id = 1;");
            stmt.execute("UPDATE upto_date_stats SET exps = exps + " + (q * p) + " WHERE id = 1;");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    void update_db(Table t) {
        int tot_quantity = 0;
        double total_profit = 0;
        double total_sales = 0;
        for (Item i : t.items) {
            try {
                stmt.execute("UPDATE inventory SET count = count - " + i.quantiy + " WHERE id = " + i.id + ";");
                tot_quantity += i.quantiy;
                total_profit += (i.price / 10) * i.quantiy;
                total_sales += i.price * 1.1 * i.quantiy;
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        try {
            stmt.execute("UPDATE today_stats SET sales = sales + " + total_sales + " WHERE id = 1;");
            stmt.execute("UPDATE today_stats SET profit = profit + " + total_profit + " WHERE id = 1;");
            stmt.execute("UPDATE today_stats SET tot_goods_sld = tot_goods_sld + " + tot_quantity + " WHERE id = 1;");
            stmt.execute("UPDATE today_stats SET cus_count = cus_count + 1  WHERE id = 1;");

            stmt.execute("UPDATE monthly_stats SET sales = sales + " + total_sales + " WHERE id = 1;");
            stmt.execute("UPDATE monthly_stats SET profit = profit + " + total_profit + " WHERE id = 1;");
            stmt.execute("UPDATE monthly_stats SET tot_goods_sld = tot_goods_sld + " + tot_quantity + " WHERE id = 1;");
            stmt.execute("UPDATE monthly_stats SET cus_count = cus_count + 1  WHERE id = 1;");

            stmt.execute("UPDATE upto_date_stats SET sales = sales + " + total_sales + " WHERE id = 1;");
            stmt.execute("UPDATE upto_date_stats SET profit = profit + " + total_profit + " WHERE id = 1;");
            stmt.execute(
                    "UPDATE upto_date_stats SET tot_goods_sld = tot_goods_sld + " + tot_quantity + " WHERE id = 1;");
            stmt.execute("UPDATE upto_date_stats SET cus_count = cus_count + 1  WHERE id = 1;");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println(tot_quantity + "|" + total_profit + "|" + total_sales);
    }

    boolean areLowStocks() {
        return get_low_inv_items().size > 0;
    }
}

class AuthPanel extends JPanel implements ActionListener {
    Image image;
    BlueButton submit_btn;
    JPasswordField pass_box;
    JTextField usr_nm_bx;
    MyWindow mom;

    AuthPanel(MyWindow mom) {
        this.mom = mom;

        image = new ImageIcon("bg.png").getImage();

        usr_nm_bx = new JTextField();
        pass_box = new JPasswordField();
        submit_btn = new BlueButton("Submit", 710, 460, 150, 40);

        setSize(1550, 823);
        this.setLayout(null);

        usr_nm_bx.setFont(new Font("Eras Demi ITC", 0, 24)); // NOI18N
        usr_nm_bx.setForeground(new Color(51, 102, 255));
        usr_nm_bx.setBorder(BorderFactory.createTitledBorder(new LineBorder(new Color(51, 102, 255), 1, true),
                "username", TitledBorder.LEFT, TitledBorder.TOP, new Font("Eras Demi ITC", 0, 18),
                new Color(51, 153, 255))); // NOI18N
        usr_nm_bx.setCaretColor(new Color(51, 102, 255));
        usr_nm_bx.setCursor(new Cursor(Cursor.TEXT_CURSOR));
        usr_nm_bx.setBounds(575, 300, 400, 60);

        pass_box.setFont(new Font("Eras Demi ITC", 0, 24)); // NOI18N
        pass_box.setForeground(new Color(51, 102, 255));
        pass_box.setBorder(BorderFactory.createTitledBorder(new LineBorder(new Color(51, 153, 255), 1, true),
                "password", TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION, new Font("Eras Demi ITC", 0, 18),
                new Color(51, 153, 255))); // NOI18N
        pass_box.setBounds(575, 380, 400, 60);

        submit_btn.addActionListener(this);
        this.add(usr_nm_bx);
        this.add(pass_box);
        this.add(submit_btn);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == submit_btn) {
            String usr = usr_nm_bx.getText();
            String pass = new String(pass_box.getPassword());
            if (usr.equals("RTG") && pass.equals("rtg4ever")) {
                JOptionPane.showMessageDialog(null, "RTG Succesfully Logged in", "RTG Login",
                        JOptionPane.INFORMATION_MESSAGE);
                mom.toHomePanel();
                //add as many as else ifs to add user names
            } else if (usr.equals("YOUR-NAME") && pass.equals("YOUR_PASS")) {
                JOptionPane.showMessageDialog(null, "Rohit Succesfully Logged in", "Rohit Login",
                        JOptionPane.INFORMATION_MESSAGE);
                mom.toHomePanel();
            } else {
                JOptionPane.showMessageDialog(null, "Wrong Username or Password", "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
            usr_nm_bx.setText("");
            pass_box.setText("");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
    }

}

class HomePanel extends JPanel implements ActionListener {
    BlueButton btn1, btn2, btn3, btn4, btn5, logout_btn;
    MyWindow mom;
    SqlOperations sql;

    HomePanel(MyWindow mom) {
        this.mom = mom;

        this.setBackground(Color.WHITE);
        this.setLayout(null);
        this.setSize(1550, 823);

        sql = new SqlOperations();

        btn2 = new BlueButton("Business Statistics", 220, 75, 350, 200);
        btn3 = new BlueButton("Manage Finances", 520, 75, 250, 200);
        btn4 = new BlueButton("Manage Sales", 610, 75, 350, 200);
        btn5 = new BlueButton("<html>Manage <br>Inventory</html>", 1020, 75, 350, 200);
        logout_btn = new BlueButton("Logout", 65, 75, 140, 50);

        btn2.addActionListener(this);
        btn3.addActionListener(this);
        btn4.addActionListener(this);
        btn5.addActionListener(this);
        logout_btn.addActionListener(this);

        this.add(btn2);
        // this.add(btn3);
        this.add(btn4);
        this.add(btn5);
        this.add(logout_btn);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btn2) {
            mom.setContentPane(new BussinessStatisticsPanel(mom));
        } else if (e.getSource() == btn3) {
            // mom.setContentPane(mom.manageFinancesPanel);
        } else if (e.getSource() == btn4) {
            mom.setContentPane(new ManageSalesPanel(mom));
            remindLowStock();
        } else if (e.getSource() == btn5) {
            mom.setContentPane(new ManageInventoryPanel(mom));
            remindLowStock();
        } else if (e.getSource() == logout_btn) {
            mom.setContentPane(mom.authPanel);
        }
        mom.revalidate();
        mom.repaint();
    }

    void remindLowStock() {
        if (!sql.areLowStocks()) {
            return;
        }
        JFrame frame = new JFrame("Low Stock Reminder");
        frame.setSize(500, 500);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setLayout(null);
        frame.setIconImage(mom.getIconImage());

        ScrollPane pane = new ScrollPane(sql.get_low_inv_items(), 10, 10, 400, 400);
        frame.add(pane);
    }

}

class BussinessStatisticsPanel extends JPanel implements ActionListener {
    BlueButton back_btn;
    JLabel label1, label2, label3;
    ScrollPane pane1, pane2, pane3;
    MyWindow mom;
    SqlOperations sql;

    BussinessStatisticsPanel(MyWindow mom) {
        this.mom = mom;

        sql = new SqlOperations();

        back_btn = new BlueButton("<", 75, 75, 75, 50);
        label1 = new JLabel("Today");
        label2 = new JLabel("This Month");
        label3 = new JLabel("Up to Date");

        pane1 = new ScrollPane(sql.get_daily_stats(), 200, 150, 350, 200);
        pane2 = new ScrollPane(sql.get_monthly_stats(), 600, 150, 350, 200);
        pane3 = new ScrollPane(sql.get_all_stats(), 1000, 150, 350, 200);

        this.setLayout(null);

        back_btn.addActionListener(this);

        label1.setFont(back_btn.getFont());
        label1.setBounds(200, 75, 300, 50);

        label2.setFont(back_btn.getFont());
        label2.setBounds(600, 75, 300, 50);

        label3.setFont(back_btn.getFont());
        label3.setBounds(1000, 75, 300, 50);

        this.add(label1);
        this.add(label2);
        this.add(label3);
        this.add(pane1);
        this.add(pane2);
        this.add(pane3);
        this.add(back_btn);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == back_btn) {
            mom.toHomePanel();
        }
    }
}

/*
 * class ManageFinancesPanel extends JPanel implements ActionListener {
 * BlueButton back_btn;
 * JComboBox<String> cbox;
 * JLabel label1;
 * JLabel label2;
 * MyWindow mom;
 * JScrollPane pane1, pane2;
 * 
 * ManageFinancesPanel(MyWindow mom) {
 * this.mom = mom;
 * 
 * String[] ops = { "Today", "This Month", "Up To Date" };
 * Font font = new Font("Eras Demi ITC", 0, 24);
 * 
 * cbox = new JComboBox<>(ops);
 * back_btn = new BlueButton("<", 75, 75, 75, 50);
 * label1 = new JLabel("Profits");
 * label2 = new JLabel("Expenditure");
 * 
 * cbox.setFont(font); // NOI18N
 * cbox.setBorder(new LineBorder(new Color(51, 102, 255), 1, true));
 * cbox.setFocusable(false);
 * cbox.setBounds(250, 75, 300, 50);
 * 
 * back_btn.addActionListener(this);
 * 
 * pane1 = new JScrollPane();
 * pane2 = new JScrollPane();
 * 
 * pane1.setBounds(250, 210, 350, 500);
 * pane2.setBounds(750, 210, 350, 500);
 * 
 * label1.setFont(font); // NOI18N
 * label1.setBounds(250, 150, 300, 50);
 * 
 * label2.setFont(font); // NOI18N
 * label2.setBounds(750, 150, 300, 50);
 * 
 * this.setLayout(null);
 * this.add(back_btn);
 * this.add(cbox);
 * this.add(label1);
 * this.add(label2);
 * this.add(pane1);
 * this.add(pane2);
 * }
 * 
 * @Override
 * public void actionPerformed(ActionEvent e) {
 * if (e.getSource() == back_btn) {
 * mom.toHomePanel();
 * }
 * }
 * }
 */

class ManageSalesPanel extends JPanel implements ActionListener {
    BlueButton back_btn, btn1, btn2, btn3, btn4, btn5, btn6;
    JScrollPane scrollPane;
    JLabel label;
    Font font = new Font("Eras Demi ITC", 0, 24);
    MyWindow mom;
    SqlOperations sql;
    JComboBox<String> cbox;
    Item item, newItem;
    Table table;
    MyList list;
    int selectedItem;
    boolean added_btn6;

    ManageSalesPanel(MyWindow mom) {
        this.mom = mom;

        this.setLayout(null);

        added_btn6 = false;

        table = new Table();

        list = new MyList(new String[1]);
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) { 
                    int selectedIndex = list.getSelectedIndex();
                    if (selectedIndex != -1) {
                        setSelectedItem(selectedIndex);
                        add_btn6();
                    }
                }
            }
        });

        back_btn = new BlueButton("<", 75, 75, 75, 50);
        btn1 = new BlueButton("New Item", 300, 200, 300, 50);
        btn2 = new BlueButton("+", 300, 400, 100, 50);
        btn3 = new BlueButton("-", 500, 400, 100, 50);
        btn4 = new BlueButton("Add Item", 300, 460, 300, 50);
        btn5 = new BlueButton("Proceed", 700, 700, 200, 50);
        btn6 = new BlueButton("Remove Item", 300, 340, 300, 50);

        label = new JLabel();
        label.setFont(font);
        label.setBounds(410, 400, 80, 50);

        sql = new SqlOperations();

        scrollPane = new JScrollPane(list);
        scrollPane.setBounds(700, 80, 600, 600);

        cbox = new JComboBox<>(sql.get_inv_item_names());
        cbox.setBounds(300, 270, 300, 50);
        cbox.setFont(font);
        cbox.addActionListener(this);

        back_btn.addActionListener(this);
        btn1.addActionListener(this);
        btn2.addActionListener(this);
        btn3.addActionListener(this);
        btn4.addActionListener(this);
        btn5.addActionListener(this);
        btn6.addActionListener(this);

        this.add(back_btn);
        this.add(btn1);
        this.add(scrollPane);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == back_btn) {
            mom.toHomePanel();
        }
        if (e.getSource() == btn1) {
            if (sql.get_count_inv() == 0) {
                JOptionPane.showMessageDialog(null, "No items in inventory", "Empty Inventory",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                this.add(cbox);
            }
        }
        if (e.getSource() == cbox) {

            item = sql.getSpecificItem(String.format("%-14s", cbox.getSelectedItem()).substring(0, 14));

            label.setText("1");

            this.add(btn2);
            this.add(btn3);
            this.add(btn4);
            this.add(label);

        }
        if (e.getSource() == btn2) {
            int i = Integer.parseInt(label.getText());
            if (i < item.quantiy) {
                i++;
                label.setText(String.valueOf(i));
            }
        }
        if (e.getSource() == btn3) {
            int i = Integer.parseInt(label.getText());
            if (i != 1) {
                i--;
                label.setText(String.valueOf(i));
            }
        }
        if (e.getSource() == btn4) {
            newItem = new Item(item.id, item.name, Integer.parseInt(label.getText()), item.price);
            table.items.add(newItem);
            list.setListData(table.returnAsStirngL());
            this.add(btn5);
        }

        if (e.getSource() == btn5) {
            sql.update_db(table);
            JOptionPane.showMessageDialog(null, "Succesful", "Confirm Message", JOptionPane.INFORMATION_MESSAGE);
            mom.setContentPane(new ManageSalesPanel(mom));
            mom.revalidate();
            mom.repaint();
        }

        if (e.getSource() == btn6) {
            table.items.remove(selectedItem);
            list.setListData(table.returnAsStirngL());
            this.remove(btn6);
            added_btn6 = false;
        }
        mom.revalidate();
        mom.repaint();
    }

    void setSelectedItem(int n) {
        selectedItem = n;
    }

    void add_btn6() {
        if (!added_btn6) {
            this.add(btn6);
            added_btn6 = true;
            this.revalidate();
            this.repaint();
        }
    }
}

class ManageInventoryPanel extends JPanel implements ActionListener {
    BlueButton back_btn, btn1, btn11, btn2, btn3, btn21, btn22, btn23;
    Font font;
    JScrollPane scrollPane;
    MyWindow mom;
    SqlOperations sql;
    MyList list;
    String name;
    MyLabel label, num;
    Item currentItem;
    JTextField box1, box2, box3;
    JFrame frame, frame2;

    ManageInventoryPanel(MyWindow mom) {
        this.mom = mom;

        this.setLayout(null);

        sql = new SqlOperations();

        list = sql.get_inv_items();

        ScrollPane scrollPane = new ScrollPane(list, 600, 100, 800, 500);

        font = new Font("Eras Demi ITC", 0, 28);

        back_btn = new BlueButton("<", 75, 75, 75, 50);
        btn1 = new BlueButton("Add new Item", 200, 130, 300, 50);
        btn2 = new BlueButton("Modify Item", 200, 230, 300, 50);
        btn3 = new BlueButton("Remove Item", 200, 330, 300, 50);
        btn21 = new BlueButton("+", 210, 50, 70, 50);
        btn22 = new BlueButton("-", 400, 50, 70, 50);
        btn23 = new BlueButton("Set", 480, 50, 70, 50);

        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) { // Prevent double events
                    String selectedItem = list.getSelectedValue();
                    if (selectedItem != null) {
                        // Call the user-defined function
                        setSelectedString(selectedItem);
                    }
                }
            }
        });

        back_btn.addActionListener(this);
        btn1.addActionListener(this);
        btn2.addActionListener(this);
        btn21.addActionListener(this);
        btn22.addActionListener(this);
        btn23.addActionListener(this);
        btn3.addActionListener(this);

        this.add(scrollPane);
        this.add(back_btn);
        this.add(btn1);
        this.add(btn2);
        this.add(btn3);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == back_btn) {
            mom.toHomePanel();
        }
        if (e.getSource() == btn1) {
            frame = new JFrame("Add New Item");
            frame.setLayout(null);
            frame.setSize(600, 350);
            frame.setLocationRelativeTo(null);
            frame.setIconImage(mom.getIconImage());
            frame.setResizable(false);

            JLabel label1 = new JLabel("Name :");
            JLabel label2 = new JLabel("Quantity :");
            JLabel label3 = new JLabel("Price :");
            label1.setFont(font);
            label2.setFont(font);
            label3.setFont(font);
            label1.setBounds(30, 30, 200, 50);
            label2.setBounds(30, 100, 200, 50);
            label3.setBounds(30, 170, 200, 50);

            box1 = new JTextField();
            box2 = new JTextField();
            box3 = new JTextField();

            box1.setFont(font);
            box2.setFont(font);
            box3.setFont(font);
            box1.setBounds(250, 30, 300, 50);
            box2.setBounds(250, 100, 300, 50);
            box3.setBounds(250, 170, 300, 50);

            btn11 = new BlueButton("Confirm", 200, 250, 200, 50);
            btn11.addActionListener(this);

            frame.add(label1);
            frame.add(label2);
            frame.add(label3);

            frame.add(box1);
            frame.add(box2);
            frame.add(box3);

            frame.add(btn11);

            frame.setVisible(true);
        }
        if (e.getSource() == btn11) {
            if (box1.getText().equals("") || box2.getText().equals("") || box3.getText().equals("")) {
                return;
            } else {
                frame.dispose();
                sql.add_new_item(box1.getText(), Integer.parseInt(box2.getText()), Double.parseDouble(box3.getText()));
                list.setListData(sql.get_inv_itemStrings());
                list.revalidate();
                list.repaint();
            }
        }
        if (e.getSource() == btn2) {
            try {
                if (sql.get_count_inv() == 0){
                    throw new Exception();
                }

                currentItem = getItem();


                frame2 = new JFrame("Modify Item");
                frame2.setLayout(null);
                frame2.setSize(580, 200);
                frame2.setLocationRelativeTo(null);
                frame2.setVisible(true);

                label = new MyLabel("Quantity", 50, 50, 150, 50);
                num = new MyLabel(String.valueOf(currentItem.quantiy), 300, 50, 70, 50);

                frame2.add(btn21);
                frame2.add(btn22);
                frame2.add(btn23);
                frame2.add(label);
                frame2.add(num);
            } catch (Exception e1) {
                if (sql.get_count_inv() == 0){
                    JOptionPane.showMessageDialog(null, "No items to modify", "Empty Inventory", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Please select an Item from the list beside!", "No item Selected!", JOptionPane.WARNING_MESSAGE);
                }
            }
        }
        if (e.getSource() == btn21) {
            int i = Integer.parseInt(num.getText());
            i++;
            num.setText(String.valueOf(i));
            num.repaint();
        }
        if (e.getSource() == btn22) {
            int i = Integer.parseInt(num.getText());
            if (i > 0) {
                i--;
            }
            num.setText(String.valueOf(i));
            num.repaint();
        }
        if (e.getSource() == btn23) {
            frame2.dispose();
            sql.update_itm_qty(currentItem, Integer.parseInt(num.getText()));
            list.setListData(sql.get_inv_itemStrings());
            list.revalidate();
            list.repaint();
        }
        if (e.getSource() == btn3) {
            currentItem = getItem();
            sql.delete_inv_itm(currentItem.id);
            list.setListData(sql.get_inv_itemStrings());
            list.revalidate();
            list.repaint();
        }
    }

    void setSelectedString(String s) {
        name = s;
    }

    String getSelectedString() {
        return name.substring(0, 14);
    }

    Item getItem() {
        Item item = sql.getSpecificItem(getSelectedString());
        return item;
    }
}

class MyLabel extends Label {
    MyLabel(String s, int x, int y, int w, int h) {
        super(s);
        this.setBounds(x, y, w, h);
        super.setFont(new Font("Eras Demi ITC", Font.BOLD, 28));
    }
}

class MyWindow extends JFrame {
    ImageIcon icon;
    AuthPanel authPanel;
    HomePanel homePanel;
    // ManageFinancesPanel manageFinancesPanel;

    MyWindow() {
        super("StoreSync®™");

        authPanel = new AuthPanel(this);
        homePanel = new HomePanel(this);
        // manageFinancesPanel = new ManageFinancesPanel(this);

        icon = new ImageIcon("ss_50.png");
        this.setIconImage(icon.getImage());
        this.setLayout(null);
        this.setSize(1550, 823);
        this.setLocation(-7, 0);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setContentPane(authPanel);
        this.setVisible(true);
    }

    void toHomePanel() {
        this.setContentPane(homePanel);
        this.revalidate();
        this.repaint();
    }
}

public class storesync {
    public static void main(String[] args) {
        new MyWindow();
    }
}