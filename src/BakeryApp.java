import java.sql.*;
import java.util.Scanner;

public class BakeryApp {

    public static void main(String[] args) {
        // JDBC connection settings; update these if necessary.
        String url = "jdbc:db2://winter2025-comp421.cs.mcgill.ca:50000/comp421";
        String your_userid = "u";  // Replace with your user ID
        String your_password = "p"; // Replace with your password

        Connection con = null;
        Scanner scanner = new Scanner(System.in);

        try {
            // Register the driver
            DriverManager.registerDriver(new com.ibm.db2.jcc.DB2Driver());

            // Connect to the DB2 database
            con = DriverManager.getConnection(url, your_userid, your_password);
            System.out.println("Connected to the database successfully!");

            // Create a Statement object that we'll reuse.
            Statement stmt = con.createStatement();

            // Main menu loop
            boolean quit = false;
            while (!quit) {
                System.out.println("\n----- Bakery Management Menu -----");
                System.out.println("1. Look up Employee by ID (Query)");
                System.out.println("2. Add a New Customer (Modification)");
                System.out.println("3. Place an Order (Multiple Statements)");
                System.out.println("4. List Baked Goods (Sub-Menu)");
                System.out.println("5. Cancel an Order (Modification with multiple steps)");
                System.out.println("6. Quit");
                System.out.print("Select an option (1-6): ");
                String choice = scanner.nextLine();

                switch (choice) {
                    case "1":
                        // Option 1: Look up Employee by ID
                        System.out.print("Enter Employee ID: ");
                        String empIdStr = scanner.nextLine();
                        try {
                            int empId = Integer.parseInt(empIdStr);
                            String query = "SELECT eId, name, email, salary, status FROM Employee WHERE eId = " + empId;
                            ResultSet rs = stmt.executeQuery(query);
                            if (rs.next()) {
                                System.out.println("Employee ID: " + rs.getInt("eId"));
                                System.out.println("Name: " + rs.getString("name"));
                                System.out.println("Email: " + rs.getString("email"));
                                System.out.println("Salary: " + rs.getDouble("salary"));
                                System.out.println("Status: " + rs.getString("status"));
                            } else {
                                System.out.println("No employee found with ID " + empId);
                            }
                            rs.close();
                        } catch (NumberFormatException nfe) {
                            System.out.println("Invalid ID format.");
                        } catch (SQLException e) {
                            System.out.println("SQL Error: " + e.getMessage());
                        }
                        break;

                    case "2":
                        // Option 2: Add a new customer
                        System.out.print("Enter new Customer ID: ");
                        String custId = scanner.nextLine();
                        System.out.print("Enter Customer phone number: ");
                        String phone = scanner.nextLine();
                        String insertCust = "INSERT INTO Customer (cId, phoneNumber) VALUES ('" + custId + "', '" + phone + "')";
                        try {
                            int rows = stmt.executeUpdate(insertCust);
                            if (rows > 0) {
                                System.out.println("Customer added successfully!");
                            } else {
                                System.out.println("Customer insertion failed.");
                            }
                        } catch (SQLException e) {
                            System.out.println("SQL Error: " + e.getMessage());
                        }
                        break;

                    case "3":
                        // Option 3: Place an order (with two statements)
                        // First, insert a new order record and then insert details into the Contains table.
                        System.out.print("Enter new Order ID: ");
                        String orderId = scanner.nextLine();
                        System.out.print("Enter Cashier Employee ID: ");
                        String cashierId = scanner.nextLine();
                        System.out.print("Enter Customer ID: ");
                        String orderCustId = scanner.nextLine();
                        System.out.print("Enter Order Date (YYYY-MM-DD): ");
                        String orderDate = scanner.nextLine();
                        System.out.print("Enter Order price: ");
                        String orderPrice = scanner.nextLine();
                        System.out.print("Enter Baked Good Name: ");
                        String bakedName = scanner.nextLine();
                        System.out.print("Enter quantity: ");
                        String quantity = scanner.nextLine();

                        try {
                            // Insert into Orders table
                            String insertOrder = "INSERT INTO Orders (oId, eId, cId, oDate, status, price) VALUES ("
                                    + orderId + ", " + cashierId + ", '" + orderCustId + "', DATE('" + orderDate + "'), 'Placed', " + orderPrice + ")";
                            int rows1 = stmt.executeUpdate(insertOrder);

                            // Insert into Contains table (associating order with baked good)
                            String insertContains = "INSERT INTO Contains (oId, bName, quant, price) VALUES ("
                                    + orderId + ", '" + bakedName + "', " + quantity + ", " + orderPrice + ")";
                            int rows2 = stmt.executeUpdate(insertContains);

                            if (rows1 > 0 && rows2 > 0) {
                                System.out.println("Order placed successfully!");
                            } else {
                                System.out.println("Failed to place order.");
                            }
                        } catch (SQLException e) {
                            System.out.println("SQL Error: " + e.getMessage());
                        }
                        break;

                    case "4":
                        // Option 4: List baked goods with a sub-menu
                        try {
                            String queryBaked = "SELECT bName, price, stock FROM BakedGood";
                            ResultSet rsBaked = stmt.executeQuery(queryBaked);
                            System.out.println("----- Available Baked Goods -----");
                            while (rsBaked.next()) {
                                System.out.println("Name: " + rsBaked.getString("bName")
                                        + " | Price: " + rsBaked.getDouble("price")
                                        + " | Stock: " + rsBaked.getInt("stock"));
                            }
                            rsBaked.close();
                            // Now, provide a sub-menu to select one for more details.
                            System.out.print("Enter a Baked Good name to view details (or press Enter to return to main menu): ");
                            String selectedBaked = scanner.nextLine();
                            if (!selectedBaked.isEmpty()) {
                                // For simplicity, we just re-run the query filtering by name.
                                String detailQuery = "SELECT * FROM BakedGood WHERE bName = '" + selectedBaked + "'";
                                ResultSet rsDetail = stmt.executeQuery(detailQuery);
                                if (rsDetail.next()) {
                                    System.out.println("Details for " + selectedBaked + ":");
                                    System.out.println("Price: " + rsDetail.getDouble("price"));
                                    System.out.println("Stock: " + rsDetail.getInt("stock"));
                                    // Add any additional details you might have in the table.
                                } else {
                                    System.out.println("No details found for " + selectedBaked);
                                }
                                rsDetail.close();
                            }
                        } catch (SQLException e) {
                            System.out.println("SQL Error: " + e.getMessage());
                        }
                        break;

                    case "5":
                        // Option 5: Cancel an order. This example will update the order status and adjust baked good stock.
                        System.out.print("Enter Order ID to cancel: ");
                        String cancelOrderId = scanner.nextLine();
                        try {
                            // Update order status to 'Cancelled'
                            String updateOrder = "UPDATE Orders SET status = 'Cancelled' WHERE oId = " + cancelOrderId;
                            int updatedRows = stmt.executeUpdate(updateOrder);

                            if (updatedRows > 0) {
                                System.out.println("Order cancelled successfully.");
                                // As an example of multiple statements: now update the baked goods stock.
                                // First, get the baked good name and quantity from the Contains table.
                                String queryContains = "SELECT bName, quant FROM Contains WHERE oId = " + cancelOrderId;
                                ResultSet rsCancel = stmt.executeQuery(queryContains);
                                while (rsCancel.next()) {
                                    String bName = rsCancel.getString("bName");
                                    int quant = rsCancel.getInt("quant");
                                    // Increase stock by the quantity in the cancelled order.
                                    String updateStock = "UPDATE BakedGood SET stock = stock + " + quant + " WHERE bName = '" + bName + "'";
                                    stmt.executeUpdate(updateStock);
                                }
                                rsCancel.close();
                            } else {
                                System.out.println("Order cancellation failed. Order may not exist.");
                            }
                        } catch (SQLException e) {
                            System.out.println("SQL Error: " + e.getMessage());
                        }
                        break;

                    case "6":
                        // Option 6: Quit the application
                        quit = true;
                        System.out.println("Exiting the application. Goodbye!");
                        break;

                    default:
                        System.out.println("Invalid option. Please choose between 1 and 6.");
                        break;
                }
            } // End of menu loop

            // Clean up: close the statement and connection.
            stmt.close();
            con.close();
            System.out.println("Database connection closed.");
        } catch (SQLException e) {
            System.err.println("Database error occurred: " + e.getMessage());
        } catch (Exception ex) {
            System.err.println("Unexpected error: " + ex.getMessage());
        } finally {
            // Ensure connection is closed in case of an exception.
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException sqle) {
                    System.err.println("Error closing connection: " + sqle.getMessage());
                }
            }
            scanner.close();
        }
    }
}
