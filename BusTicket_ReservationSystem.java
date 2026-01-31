import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;


public class BusReservationSystem extends JFrame {
    private JTextField sourceField, destinationField, nameField, contactField, ticketIdField;
    private JTextArea outputArea;
    private Connection conn;

    public BusReservationSystem() {bj
        setTitle("Bus Reservation System");
        setSize(750, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Connect to DB
        try {
            conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/bus_reservation", "root", "robika@mysql123"
            );
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Database Connection Failed: " + e.getMessage());
            System.exit(0);
        }

        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Enter Details"));

        inputPanel.add(new JLabel("Boarding Point:"));
        sourceField = new JTextField();
        inputPanel.add(sourceField);

        inputPanel.add(new JLabel("Destination:"));
        destinationField = new JTextField();
        inputPanel.add(destinationField);

        inputPanel.add(new JLabel("Passenger Name:"));
        nameField = new JTextField();
        inputPanel.add(nameField);

        inputPanel.add(new JLabel("Contact:"));
        contactField = new JTextField();
        inputPanel.add(contactField);

        inputPanel.add(new JLabel("Ticket ID (for Cancel):"));
        ticketIdField = new JTextField();
        inputPanel.add(ticketIdField);

        add(inputPanel, BorderLayout.NORTH);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton searchButton = new JButton("Search Bus");
        JButton bookButton = new JButton("Book Seat");
        JButton viewButton = new JButton("View My Bookings");
        JButton cancelButton = new JButton("Cancel Booking");

        buttonPanel.add(searchButton);
        buttonPanel.add(bookButton);
        buttonPanel.add(viewButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Button Actions
        searchButton.addActionListener(e -> searchBus());
        bookButton.addActionListener(e -> bookSeat());
        viewButton.addActionListener(e -> viewBookings());
        cancelButton.addActionListener(e -> cancelBooking());
    }

    private void searchBus() {
        String source = sourceField.getText().trim();
        String destination = destinationField.getText().trim();

        if (source.isEmpty() || destination.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter source and destination!");
            return;
        }

        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM buses WHERE source=? AND destination=?"
            );
            ps.setString(1, source);
            ps.setString(2, destination);
            ResultSet rs = ps.executeQuery();

            outputArea.setText("");
            boolean found = false;

            while (rs.next()) {
                found = true;
                outputArea.append("Bus No: " + rs.getInt("bus_no") + "\n");
                outputArea.append("Source: " + rs.getString("source") + "\n");
                outputArea.append("Destination: " + rs.getString("destination") + "\n");
                outputArea.append("Available Seats: " + rs.getInt("available_seats") + "\n");
                outputArea.append("------------------------------------\n");
            }

            if (!found)
                outputArea.setText("No buses found for this route.\n");

        } catch (Exception ex) {
            outputArea.setText("Error: " + ex.getMessage());
        }
    }

    private void bookSeat() {
        String source = sourceField.getText().trim();
        String destination = destinationField.getText().trim();
        String name = nameField.getText().trim();
        String contact = contactField.getText().trim();

        if (source.isEmpty() || destination.isEmpty() || name.isEmpty() || contact.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter all passenger details!");
            return;
        }

        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM buses WHERE source=? AND destination=? AND available_seats > 0 LIMIT 1"
            );
            ps.setString(1, source);
            ps.setString(2, destination);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int busNo = rs.getInt("bus_no");
                int available = rs.getInt("available_seats");

                PreparedStatement ps2 = conn.prepareStatement(
                    "INSERT INTO bookings (passenger_name, passenger_contact, source, destination, bus_no) VALUES (?, ?, ?, ?, ?)"
                );
                ps2.setString(1, name);
                ps2.setString(2, contact);
                ps2.setString(3, source);
                ps2.setString(4, destination);
                ps2.setInt(5, busNo);
                ps2.executeUpdate();

                PreparedStatement ps3 = conn.prepareStatement(
                    "UPDATE buses SET available_seats = ? WHERE bus_no = ?"
                );
                ps3.setInt(1, available - 1);
                ps3.setInt(2, busNo);
                ps3.executeUpdate();

                outputArea.setText("Seat booked successfully!\nBus No: " + busNo);
            } else {
                outputArea.setText("No available seats for this route.\n");
            }

        } catch (Exception ex) {
            outputArea.setText("Error: " + ex.getMessage());
        }
    }

    private void viewBookings() {
        String contact = contactField.getText().trim();
        if (contact.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter contact to view your bookings!");
            return;
        }

        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM bookings WHERE passenger_contact=?"
            );
            ps.setString(1, contact);
            ResultSet rs = ps.executeQuery();

            outputArea.setText("");
            boolean found = false;

            while (rs.next()) {
                found = true;
                outputArea.append("Ticket ID: " + rs.getInt("ticket_id") + "\n");
                outputArea.append("Name: " + rs.getString("passenger_name") + "\n");
                outputArea.append("Bus No: " + rs.getInt("bus_no") + "\n");
                outputArea.append("Route: " + rs.getString("source") + " → " + rs.getString("destination") + "\n");
                outputArea.append("------------------------------------\n");
            }

            if (!found)
                outputArea.setText("No bookings found for this contact.\n");

        } catch (Exception ex) {
            outputArea.setText("Error: " + ex.getMessage());
        }
    }

    private void cancelBooking() {
        String ticketId = ticketIdField.getText().trim();
        if (ticketId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter ticket ID to cancel booking!");
            return;
        }

        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT bus_no FROM bookings WHERE ticket_id=?"
            );
            ps.setInt(1, Integer.parseInt(ticketId));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int busNo = rs.getInt("bus_no");

                PreparedStatement ps2 = conn.prepareStatement(
                    "DELETE FROM bookings WHERE ticket_id=?"
                );
                ps2.setInt(1, Integer.parseInt(ticketId));
                ps2.executeUpdate();

                PreparedStatement ps3 = conn.prepareStatement(
                    "UPDATE buses SET available_seats = available_seats + 1 WHERE bus_no=?"
                );
                ps3.setInt(1, busNo);
                ps3.executeUpdate();

                outputArea.setText("Booking cancelled successfully!\n");
            } else {
                outputArea.setText("Invalid Ticket ID.\n");
            }

        } catch (Exception ex) {
            outputArea.setText("Error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BusReservationSystem().setVisible(true));
    }
}
