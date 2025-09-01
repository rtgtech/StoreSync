import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.awt.geom.RoundRectangle2D;
import java.io.File;

//=======================>
//Modified GUI components

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
    protected void paintBorder(Graphics g) {
    }

    @Override
    public boolean contains(int x, int y) {
        Shape shape = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        return shape.contains(x, y);
    }
}

class ScrollPane extends JScrollPane {
    ScrollPane(CustomTable table, int x, int y, int w, int h) {
        super(table);
        this.setBounds(x, y, w, h);
    }

    ScrollPane(JList<String> list, int x, int y, int w, int h) {
        super(list);
        this.setBounds(x, y, w, h);
    }

    ScrollPane(int x, int y, int w, int h) {
        this.setBounds(x, y, w, h);
    }

}

class MyLabel extends Label {
    MyLabel(String s, int x, int y, int w, int h) {
        super(s);
        this.setBounds(x, y, w, h);
        super.setFont(new Font("Eras Demi ITC", Font.BOLD, 28));
    }
}

class CustomTable extends JTable {
    private final DefaultTableModel model;

    public CustomTable() {
        // Added "ID" column (first, hidden)
        String[] columnNames = { "ID", "Name", "Quantity", "Price" };

        // Create model with no rows and non-editable cells
        model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        this.setModel(model);
        this.setFont(new Font("Eras Demi ITC", Font.PLAIN, 24));
        this.setRowHeight(30);

        // Set row selection only
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Fill the entire viewport width (no horizontal scroll)
        this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Hide the ID column (index 0)
        TableColumn idCol = this.getColumnModel().getColumn(0);
        this.getColumnModel().removeColumn(idCol);
    }

    public void setColumnWidths(int x, int y, int z) {
        // Columns are shifted left after removing ID column
        TableColumn nameCol = this.getColumnModel().getColumn(0); // now "Name"
        TableColumn qtyCol = this.getColumnModel().getColumn(1); // now "Quantity"
        TableColumn priceCol = this.getColumnModel().getColumn(2); // now "Price"

        // nameCol.setPreferredWidth(nameColWidth);
        nameCol.setPreferredWidth(x);
        qtyCol.setPreferredWidth(y);
        priceCol.setPreferredWidth(z);
    }

    public void addRow(int id, String name, int quantity, double price) {
        model.addRow(new Object[] { id, name, quantity, price });
    }

    public int deleteSelectedRow() {
        int selectedRow = this.getSelectedRow();
        if (selectedRow >= 0) {
            model.removeRow(convertRowIndexToModel(selectedRow));
            return selectedRow;
        }
        return -1;
    }

    public int getSelectedRowIndex() {
        return this.getSelectedRow();
    }

    public int getSelectedRowId() {
        int viewRow = this.getSelectedRow();
        if (viewRow >= 0) {
            int modelRow = convertRowIndexToModel(viewRow);
            Object value = model.getValueAt(modelRow, 0); // 0 = ID column in model
            if (value instanceof Integer) {
                return (Integer) value;
            } else {
                try {
                    return Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    public void updateRow(int id, int q) {
        // Iterate through the rows in the model to find a matching ID
        for (int i = 0; i < model.getRowCount(); i++) {
            // Get the ID from the first column (index 0) of the current row in the model
            Object value = model.getValueAt(i, 0);

            if (value instanceof Integer && (Integer) value == id) {
                // Found the row, now update the quantity column
                // Quantity is at index 2 in the model
                model.setValueAt(q, i, 2);
                return; // Exit the loop once the row is found and updated
            }
        }
    }
}

class MyData {
    public int id;
    public String name;
    public int q;
    public double p;

    MyData(int i, String n, int _q, double _p) {
        id = i;
        name = n;
        q = _q;
        p = _p;
    }

    @Override
    public String toString() {
        return name;
    }

    public int getId() {
        return id;
    }
}

// ======================>
// Database components
class SqlOperations {
    String url = "jdbc:sqlite:storesync.db";
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

    public JList<String> getTodayStats() {
        JList<String> jls = new JList<>();
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM today_stats WHERE id = 1;");
            DefaultListModel<String> listModel = new DefaultListModel<>();
            while (rs.next()) {
                listModel.addElement("Sales : " + rs.getString(2));
                listModel.addElement("Expense : " + rs.getString(3));
                listModel.addElement("Profit : " + rs.getString(4));
                listModel.addElement("Total no : " + rs.getString(5));
                listModel.addElement("customers : " + rs.getString(6));
            }
            rs.close();
            jls = new JList<>(listModel);
            jls.setFont(new Font("Eras Demi ITC", Font.PLAIN, 30));
            jls.setEnabled(false);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return jls;
    }

    public JList<String> getMonthlyStats() {
        JList<String> jls = new JList<>();
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM monthly_stats WHERE id = 1;");
            DefaultListModel<String> listModel = new DefaultListModel<>();
            while (rs.next()) {
                listModel.addElement("Sales : " + rs.getString(2));
                listModel.addElement("Expense : " + rs.getString(3));
                listModel.addElement("Profit : " + rs.getString(4));
                listModel.addElement("Total no : " + rs.getString(5));
                listModel.addElement("customers : " + rs.getString(6));
            }
            rs.close();
            jls = new JList<>(listModel);
            jls.setFont(new Font("Eras Demi ITC", Font.PLAIN, 30));
            jls.setEnabled(false);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return jls;
    }

    public JList<String> getAllStats() {
        JList<String> jls = new JList<>();
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM upto_date_stats WHERE id = 1;");
            DefaultListModel<String> listModel = new DefaultListModel<>();
            while (rs.next()) {
                listModel.addElement("Sales : " + rs.getString(2));
                listModel.addElement("Expense : " + rs.getString(3));
                listModel.addElement("Profit : " + rs.getString(4));
                listModel.addElement("Total no : " + rs.getString(5));
                listModel.addElement("customers : " + rs.getString(6));
            }
            rs.close();
            jls = new JList<>(listModel);
            jls.setFont(new Font("Eras Demi ITC", Font.PLAIN, 30));
            jls.setEnabled(false);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return jls;
    }

    public CustomTable getInventory() {
        CustomTable table = new CustomTable();

        String query = "SELECT * FROM inventory";

        try {
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("item");
                int quantity = rs.getInt("count");
                double price = rs.getDouble("price");

                table.addRow(id, name, quantity, price);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace(); // or log it properly
        }

        return table;
    }

    public void updateInventoryQuantity(int id, int q) {
        try {
            stmt.execute("UPDATE inventory SET count = " + q + " WHERE id = " + id + ";");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public boolean isInventory() {
        int n = 0;
        try {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM inventory;");
            rs.next();
            n = rs.getInt(1);
            rs.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return n != 0;
    }

    public boolean isItems() {
        int n = 0;
        try {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM inventory WHERE count > 0;");
            rs.next();
            n = rs.getInt(1);
            rs.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return n != 0;
    }

    public void update(CustomTable table) {
        try {
            TableModel model = table.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                int id = (int) model.getValueAt(i, 0);
                int count = (int) model.getValueAt(i, 2);
                double p = (double) model.getValueAt(i, 3);
                double total_sales = count*p;
                double total_profit = total_sales/5;
                stmt.execute("UPDATE inventory SET count = count - " + count + " WHERE id = " + id + ";");

                stmt.execute("UPDATE today_stats SET sales = sales + " + total_sales + " WHERE id = 1;");
                stmt.execute("UPDATE today_stats SET profit = profit + " + total_profit + " WHERE id = 1;");
                stmt.execute( "UPDATE today_stats SET tot_goods_sld = tot_goods_sld + " + count + " WHERE id = 1;");
                stmt.execute("UPDATE today_stats SET cus_count = cus_count + 1  WHERE id = 1;");

                stmt.execute("UPDATE monthly_stats SET sales = sales + " + total_sales + " WHERE id = 1;");
                stmt.execute("UPDATE monthly_stats SET profit = profit + " + total_profit + " WHERE id = 1;");
                stmt.execute("UPDATE monthly_stats SET tot_goods_sld = tot_goods_sld + " + count + " WHERE id = 1;");
                stmt.execute("UPDATE monthly_stats SET cus_count = cus_count + 1  WHERE id = 1;");

                stmt.execute("UPDATE upto_date_stats SET sales = sales + " + total_sales + " WHERE id = 1;");
                stmt.execute("UPDATE upto_date_stats SET profit = profit + " + total_profit + " WHERE id = 1;");
                stmt.execute( "UPDATE upto_date_stats SET tot_goods_sld = tot_goods_sld + " + count
                                + " WHERE id = 1;");
                stmt.execute("UPDATE upto_date_stats SET cus_count = cus_count + 1  WHERE id = 1;");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void populateNameList(JComboBox<MyData> cbox) {
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM inventory WHERE count > 0;");
            cbox.removeAllItems();
            while (rs.next()) {
                cbox.addItem(new MyData(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getDouble(4)));
            }
            rs.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void deleteInventoryItem(int id) {
        try {
            stmt.execute("DELETE FROM inventory WHERE id = " + id + ";");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public int getCurrentId() {
        int curr_id = 0;
        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM current WHERE id = 1;");
            rs.next();
            curr_id = rs.getInt(2);
            rs.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return curr_id + 1;
    }

    public int getQuantity(int id) {
        int q = 0;
        try {
            ResultSet rs = stmt.executeQuery("SELECT count FROM inventory WHERE id = " + id + ";");
            rs.next();
            q = rs.getInt("count");
            rs.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return q;
    }

    public void addNewItem(String name, int q, double p) {
        try {
            stmt.execute("UPDATE current SET curr_id = curr_id + 1 WHERE id = 1");
            stmt.execute("INSERT INTO inventory (id, item, count, price) VALUES (" + getCurrentId() + ", '" + name
                    + "', " + q + ", " + p + ");");
            stmt.execute("UPDATE today_stats SET exps = exps + " + (q * p) + " WHERE id = 1;");
            stmt.execute("UPDATE monthly_stats SET exps = exps + " + (q * p) + " WHERE id = 1;");
            stmt.execute("UPDATE upto_date_stats SET exps = exps + " + (q * p) + " WHERE id = 1;");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}

// ======================>
// Panels
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
        // TO BE ADDED
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

        pane1 = new ScrollPane(sql.getTodayStats(), 200, 150, 350, 200);
        pane2 = new ScrollPane(sql.getMonthlyStats(), 600, 150, 350, 200);
        pane3 = new ScrollPane(sql.getAllStats(), 1000, 150, 350, 200);

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

class ManageSalesPanel extends JPanel implements ActionListener {
    BlueButton back_btn, btn1, btn2, btn3, btn4, btn5, btn6;
    ScrollPane scrollPane;
    JLabel label;
    Font font = new Font("Eras Demi ITC", 0, 24);
    MyWindow mom;
    SqlOperations sql;
    JComboBox<MyData> cbox;
    int selectedItem;
    boolean added_btn6;
    CustomTable table;

    ManageSalesPanel(MyWindow mom) {
        this.mom = mom;

        this.setLayout(null);

        added_btn6 = false;

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
        table = new CustomTable();
        table.setColumnWidths(350, 122, 125);
        scrollPane = new ScrollPane(table, 700, 80, 600, 600);

        cbox = new JComboBox<>();
        sql.populateNameList(cbox);
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
        MyData d = null;
        if (e.getSource() == back_btn) {
            mom.toHomePanel();
        }
        if (e.getSource() == btn1) {
            if (!sql.isItems()) {
                JOptionPane.showMessageDialog(null, "No items in inventory", "Empty inventory",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                this.add(cbox);
            }
        }
        if (e.getSource() == cbox) {
            d = (MyData) cbox.getSelectedItem();
            this.add(btn2);
            this.add(btn3);
            this.add(btn4);
            this.add(label);
            label.setText("1");
        }
        if (e.getSource() == btn2) {
            int i = Integer.parseInt(label.getText());
            d = (MyData) cbox.getSelectedItem();
            if (i < d.q) {
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
            d = (MyData) cbox.getSelectedItem();
            table.addRow(d.id, d.name, Integer.parseInt(label.getText()), d.p);
            this.add(btn6);
            this.add(btn5);
        }

        if (e.getSource() == btn5) {
            sql.update(table);
            JOptionPane.showMessageDialog(null, "Successful", "Payment Done", JOptionPane.INFORMATION_MESSAGE);
            mom.setContentPane(new ManageSalesPanel(mom));
        }

        if (e.getSource() == btn6) {
            table.deleteSelectedRow();
            this.remove(btn6);
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
    String name;
    MyLabel label, num;
    JTextField box1, box2, box3;
    JFrame frame, frame2;
    CustomTable table;

    ManageInventoryPanel(MyWindow mom) {
        this.mom = mom;

        this.setLayout(null);

        sql = new SqlOperations();

        table = sql.getInventory();
        table.setColumnWidths(497, 150, 150);

        ScrollPane scrollPane = new ScrollPane(table, 600, 100, 800, 500);

        font = new Font("Eras Demi ITC", 0, 28);

        back_btn = new BlueButton("<", 75, 75, 75, 50);
        btn1 = new BlueButton("Add new Item", 200, 130, 300, 50);
        btn2 = new BlueButton("Modify Item", 200, 230, 300, 50);
        btn3 = new BlueButton("Remove Item", 200, 330, 300, 50);
        btn21 = new BlueButton("+", 210, 50, 70, 50);
        btn22 = new BlueButton("-", 400, 50, 70, 50);
        btn23 = new BlueButton("Set", 480, 50, 70, 50);

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
        int q1 = 0;
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
            String name = box1.getText();
            String quantity = box2.getText();
            String price = box3.getText();
            if (name.equals("") || quantity.equals("") || price.equals("")) {
                return;
            } else {
                int q = Integer.parseInt(quantity);
                double p = Integer.parseInt(price);
                sql.addNewItem(name, q, p);
                table.addRow(sql.getCurrentId() - 1, name, q, p);
                frame.dispose();
            }
        }
        if (e.getSource() == btn2) {
            if (!sql.isInventory()) {
                JOptionPane.showMessageDialog(null, "Inventory is Empty!", "Empty Inventory",
                        JOptionPane.WARNING_MESSAGE);
            } else if (table.getSelectedRowId() == -1) {
                JOptionPane.showMessageDialog(null, "Please Select a Item!", "No Row Selected",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                int id = table.getSelectedRowId();
                q1 = sql.getQuantity(id);
                frame2 = new JFrame("Modify Item");
                frame2.setLayout(null);
                frame2.setSize(580, 200);
                frame2.setLocationRelativeTo(null);
                frame2.setVisible(true);

                label = new MyLabel("Quantity", 50, 50, 150, 50);
                num = new MyLabel(String.valueOf(q1), 300, 50, 70, 50);

                frame2.add(btn21);
                frame2.add(btn22);
                frame2.add(btn23);
                frame2.add(label);
                frame2.add(num);
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
            sql.updateInventoryQuantity(table.getSelectedRowId(), Integer.parseInt(num.getText()));
            table.updateRow(table.getSelectedRowId(), Integer.parseInt(num.getText()));
            frame2.dispose();
        }
        if (e.getSource() == btn3) {
            int id = table.getSelectedRowId();
            if (!sql.isInventory()) {
                JOptionPane.showMessageDialog(null, "Inventory is Empty!", "Empty Inventory",
                        JOptionPane.WARNING_MESSAGE);
            } else if (id == -1) {
                JOptionPane.showMessageDialog(null, "Please select an Item", "No Item Selected",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                table.deleteSelectedRow();
                sql.deleteInventoryItem(id);
            }
        }
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
        this.setContentPane(homePanel);
        this.setVisible(true);
    }

    void toHomePanel() {
        this.setContentPane(homePanel);
        this.revalidate();
        this.repaint();
    }

}

// ======================>
// Main function
public class Program {
    public static boolean checkDBFile() {
        File file = new File("storesync.db");
        if (!file.exists()) {
            JOptionPane.showMessageDialog(null, "Failure to open Database. Run JDBsetup.java", "DB Failure",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        if (checkDBFile()) {
            new MyWindow();
        } else {
            JOptionPane.showMessageDialog(null, "Failure to open and read storesync.db. Run JDBsetup.class", "DB init failure", JOptionPane.WARNING_MESSAGE);
        }

    }
}
