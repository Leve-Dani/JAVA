import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.Random;

public class app extends JFrame {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ticket_system"; // Update with your DB URL
    private static final String USER = "root"; // Replace with your MySQL username
    private static final String PASS = "sql!2366"; // Replace with your MySQL password

    private JTextField sourceField;
    private JTextField destinationField;
    private JTextField trainIdField;
    private JTextField ticketsField;
    private JTextField nameField;
    private JTextField phoneField;
    private JTextField ticketNumberField;

    public app() {
        setTitle("Train Ticket Booking System");
        setSize(400, 500); // Increased size for better layout
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(0, 2));

        // Input fields
        add(new JLabel("Source Station:"));
        sourceField = new JTextField();
        add(sourceField);

        add(new JLabel("Destination Station:"));
        destinationField = new JTextField();
        add(destinationField);

        add(new JLabel("Train ID:"));
        trainIdField = new JTextField();
        add(trainIdField);

        add(new JLabel("Number of Tickets:"));
        ticketsField = new JTextField();
        add(ticketsField);

        add(new JLabel("Your Name:"));
        nameField = new JTextField();
        add(nameField);

        add(new JLabel("Phone Number:"));
        phoneField = new JTextField();
        add(phoneField);

        add(new JLabel("Ticket Number (for cancellation):"));
        ticketNumberField = new JTextField();
        add(ticketNumberField);

        // Buttons
        JButton viewTrainsButton = new JButton("View Available Trains");
        viewTrainsButton.addActionListener(e -> viewAvailableTrains());
        add(viewTrainsButton);

        JButton bookTicketButton = new JButton("Book Ticket");
        bookTicketButton.addActionListener(e -> bookTicket());
        add(bookTicketButton);

        JButton cancelTicketButton = new JButton("Cancel Ticket");
        cancelTicketButton.addActionListener(e -> cancelTicket());
        add(cancelTicketButton);

        JButton viewBookedTicketsButton = new JButton("View Booked Tickets");
        viewBookedTicketsButton.addActionListener(e -> viewBookedTickets());
        add(viewBookedTicketsButton);
    }

    private void viewAvailableTrains() {
        String source = sourceField.getText();
        String destination = destinationField.getText();
        
        String query = "SELECT * FROM Trains WHERE source_station = ? AND destination_station = ? AND available_seats > 0";
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
             
            preparedStatement.setString(1, source);
            preparedStatement.setString(2, destination);
            ResultSet resultSet = preparedStatement.executeQuery();

            StringBuilder availableTrains = new StringBuilder("Available Trains:\n");
            while (resultSet.next()) {
                availableTrains.append("Train ID: ").append(resultSet.getInt("train_id"))
                        .append(", Name: ").append(resultSet.getString("train_name"))
                        .append(", Departure: ").append(resultSet.getTime("departure_time"))
                        .append(", Arrival: ").append(resultSet.getTime("arrival_time"))
                        .append(", Available Seats: ").append(resultSet.getInt("available_seats"))
                        .append("\n");
            }
            JOptionPane.showMessageDialog(this, availableTrains.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void bookTicket() {
        int trainId = Integer.parseInt(trainIdField.getText());
        int ticketsToBook = Integer.parseInt(ticketsField.getText());
        String name = nameField.getText();
        String phoneNumber = phoneField.getText();

        String selectQuery = "SELECT available_seats FROM Trains WHERE train_id = ?";
        String updateQuery = "UPDATE Trains SET available_seats = available_seats - ? WHERE train_id = ?";
        String insertPassengerQuery = "INSERT INTO Passengers (name, phone_number, ticket_number, train_id) VALUES (?, ?, ?, ?)";
        String insertTicketNumberQuery = "INSERT INTO TicketNumbers (ticket_number, train_id) VALUES (?, ?)";
        
        double ticketCost = getTicketCost(trainId);
        double totalCost = ticketCost * ticketsToBook;

        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
             PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
             PreparedStatement insertPassengerStatement = connection.prepareStatement(insertPassengerQuery);
             PreparedStatement insertTicketNumberStatement = connection.prepareStatement(insertTicketNumberQuery)) {

            // Check available seats
            selectStatement.setInt(1, trainId);
            ResultSet resultSet = selectStatement.executeQuery();
            if (resultSet.next()) {
                int availableSeats = resultSet.getInt("available_seats");
                if (availableSeats >= ticketsToBook) {
                    // Generate random ticket number
                    Random random = new Random();
                    int ticketNumber = random.nextInt(100000); // Generate a random ticket number

                    // Update available seats
                    updateStatement.setInt(1, ticketsToBook);
                    updateStatement.setInt(2, trainId);
                    updateStatement.executeUpdate();

                    // Insert passenger details
                    insertPassengerStatement.setString(1, name);
                    insertPassengerStatement.setString(2, phoneNumber);
                    insertPassengerStatement.setInt(3, ticketNumber);
                    insertPassengerStatement.setInt(4, trainId);
                    insertPassengerStatement.executeUpdate();

                    // Insert ticket number into TicketNumbers table
                    insertTicketNumberStatement.setInt(1, ticketNumber);
                    insertTicketNumberStatement.setInt(2, trainId);
                    insertTicketNumberStatement.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Successfully booked " + ticketsToBook + " tickets! Your ticket number is: " + ticketNumber + "\nTotal cost: " + totalCost);
                } else {
                    JOptionPane.showMessageDialog(this, "Not enough available seats. Only " + availableSeats + " seats left.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void cancelTicket() {
        int ticketNumber = Integer.parseInt(ticketNumberField.getText());

        String selectQuery = "SELECT train_id FROM Passengers WHERE ticket_number = ?";
        String updateQuery = "DELETE FROM Passengers WHERE ticket_number = ?";
        String updateSeatsQuery = "UPDATE Trains SET available_seats = available_seats + 1 WHERE train_id = ?";

        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
             PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
             PreparedStatement updateSeatsStatement = connection.prepareStatement(updateSeatsQuery)) {

            selectStatement.setInt(1, ticketNumber);
            ResultSet resultSet = selectStatement.executeQuery();
            if (resultSet.next()) {
                int trainId = resultSet.getInt("train_id");

                // Delete passenger record
                updateStatement.setInt(1, ticketNumber);
                updateStatement.executeUpdate();

                // Update available seats
                updateSeatsStatement.setInt(1, trainId);
                updateSeatsStatement.executeUpdate();

                JOptionPane.showMessageDialog(this, "Successfully canceled ticket number: " + ticketNumber);
            } else {
                JOptionPane.showMessageDialog(this, "Ticket number not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void viewBookedTickets() {
        String query = "SELECT * FROM Passengers";
        StringBuilder bookedTickets = new StringBuilder("Booked Tickets:\n");

        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement preparedStatement = connection.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                bookedTickets.append("Ticket Number: ").append(resultSet.getInt("ticket_number"))
                        .append(", Name: ").append(resultSet.getString("name"))
                        .append(", Phone: ").append(resultSet.getString("phone_number"))
                        .append(", Train ID: ").append(resultSet.getInt("train_id"))
                        .append("\n");
            }
            JOptionPane.showMessageDialog(this, bookedTickets.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private double getTicketCost(int trainId) {
        String query = "SELECT cost FROM TicketCosts WHERE train_id = ?";
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
           
            preparedStatement.setInt(1, trainId);
            ResultSet resultSet = preparedStatement.executeQuery();
 if (resultSet.next()) {
                return resultSet.getDouble("cost");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0; // Return 0 if not found
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            app app = new app();
            app.setVisible(true);
        });
    }
}