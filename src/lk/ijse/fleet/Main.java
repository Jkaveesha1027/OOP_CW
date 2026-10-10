package lk.ijse.fleet;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {
    private static final String LINE = "========================================================";

    private final FleetManager fleetManager = new FleetManager();
    private final UserManager userManager = new UserManager();
    private final Scanner scanner = new Scanner(System.in);
    private SystemUser loggedInUser = null;
    private boolean running = true;

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        // Lambda implementing the RentalEventListener functional interface
        fleetManager.setRentalEventListener(agreement ->
                System.out.println("[EVENT] New rental created: " + agreement.getAgreementId())
        );

        Thread statusThread = new Thread(new VehicleStatusMonitor(fleetManager));
        statusThread.setDaemon(true);
        statusThread.start();

        Thread overdueThread = new Thread(new OverdueReminderWorker(fleetManager));
        overdueThread.setDaemon(true);
        overdueThread.start();

        try {
            while (running) {
                if (loggedInUser == null) {
                    showAuthMenu();
                }
                if (running && loggedInUser != null) {
                    showMainMenu();
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed. Exiting.");
        }
        System.out.println("Goodbye!");
    }

    // ---------------------------------------------------------------- auth

    public void showAuthMenu() {
        while (running && loggedInUser == null) {
            System.out.println(LINE);
            System.out.println("FLEET RENTAL MANAGEMENT SYSTEM (FRMS)");
            System.out.println(LINE);
            System.out.println("[1] Login");
            System.out.println("[2] Register New User");
            System.out.println("[0] Exit");
            System.out.println(LINE);
            String choice = prompt("Enter choice > ");
            switch (choice) {
                case "1":
                    String username = prompt("Username: ");
                    String password = prompt("Password: ");
                    SystemUser user = userManager.login(username, password);
                    if (user != null) {
                        loggedInUser = user;
                    } else {
                        System.out.println("Invalid username or password");
                    }
                    break;
                case "2":
                    String newUsername = promptNonEmpty("Username: ");
                    String newPassword = promptNonEmpty("Password: ");
                    String fullName = promptNonEmpty("Full name: ");
                    userManager.registerUser(newUsername, newPassword, fullName);
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 1, 2 or 0.");
            }
        }
    }

    // ---------------------------------------------------------------- main menu

    public void showMainMenu() {
        System.out.println("\nWelcome, " + loggedInUser.getFullName() + "! Loading fleet dashboard...");
        while (loggedInUser != null) {
            System.out.println(LINE);
            System.out.println("FRMS MAIN MENU | Logged in as: " + loggedInUser.getFullName());
            System.out.println(LINE);
            System.out.println("[1] Add Vehicle");
            System.out.println("[2] Add Customer");
            System.out.println("[3] View Available Vehicles");
            System.out.println("[4] Create Rental Agreement");
            System.out.println("[5] Return Vehicle");
            System.out.println("[6] View Revenue Report");
            System.out.println("[7] View Vehicles by Fuel Type");
            System.out.println("[8] View Combined Fleet & Customer Report");
            System.out.println("[9] View All Customers");
            System.out.println("[10] View All Rental Agreements");
            System.out.println("[0] Logout");
            System.out.println(LINE);
            String choice = prompt("Enter choice > ");
            try {
                switch (choice) {
                    case "1": addVehicle(); break;
                    case "2": addCustomer(); break;
                    case "3": printVehicles(fleetManager.getAvailableVehicles(), "No available vehicles."); break;
                    case "4": createRental(); break;
                    case "5": returnVehicle(); break;
                    case "6": showRevenueReport(); break;
                    case "7": showVehiclesByFuelType(); break;
                    case "8": fleetManager.printAllReportables(); break;
                    case "9": printCustomers(); break;
                    case "10": printAgreements(); break;
                    case "0":
                        loggedInUser = null;
                        System.out.println("Logged out.");
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter a number from the menu.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number entered. Please try again.");
            }
        }
    }

    // ---------------------------------------------------------------- operations

    private void addVehicle() {
        System.out.println("Vehicle type: [1] Car  [2] Van  [3] Truck");
        int type = readInt("Choose type > ");
        if (type < 1 || type > 3) {
            System.out.println("Invalid vehicle type.");
            return;
        }
        String reg = promptNonEmpty("Registration number: ");
        String brand = promptNonEmpty("Brand: ");
        String model = promptNonEmpty("Model: ");
        double rate = readPositiveDouble("Daily rate: ");
        FuelType fuel = readFuelType();
        if (fuel == null) {
            return;
        }
        Vehicle v;
        if (type == 1) {
            v = new Car(reg, brand, model, rate, fuel);
        } else if (type == 2) {
            int seats = readPositiveInt("Passenger capacity: ");
            v = new Van(reg, brand, model, rate, fuel, seats);
        } else {
            double tons = readPositiveDouble("Cargo capacity (tons): ");
            v = new Truck(reg, brand, model, rate, fuel, tons);
        }
        fleetManager.addVehicle(v);
        System.out.println("Vehicle added: " + v.getVehicleId());
    }

    private void addCustomer() {
        System.out.println("Customer type: [1] Individual  [2] Corporate");
        int type = readInt("Choose type > ");
        if (type != 1 && type != 2) {
            System.out.println("Invalid customer type.");
            return;
        }
        String name = promptNonEmpty("Full name: ");
        String contact = promptNonEmpty("Contact number: ");
        Customer c;
        if (type == 1) {
            int points = readNonNegativeInt("Loyalty points: ");
            c = new IndividualCustomer(name, contact, points);
        } else {
            double limit = readPositiveDouble("Credit limit: ");
            c = new CorporateCustomer(name, contact, limit);
        }
        fleetManager.addCustomer(c);
        System.out.println("Customer added: " + c.getCustomerId());
    }

    private void createRental() {
        String vehicleId = promptNonEmpty("Vehicle ID (e.g. VEH-1001): ");
        String customerId = promptNonEmpty("Customer ID (e.g. CUS-1001): ");
        int days = readPositiveInt("Rental days: ");
        try {
            RentalAgreement agreement = fleetManager.createRentalAgreement(vehicleId, customerId, days);
            if (agreement != null) {
                System.out.println(agreement);
            }
        } catch (VehicleNotAvailableException | InsufficientCreditException e) {
            System.out.println("Rental failed: " + e.getMessage());
        }
    }

    private void returnVehicle() {
        String id = promptNonEmpty("Agreement ID (e.g. AGR-1001): ");
        fleetManager.returnVehicle(id);
    }

    private void showRevenueReport() {
        String line = "================================================================";
        System.out.println(line);
        System.out.println("FLEET REVENUE REPORT");
        System.out.println(line);
        System.out.println("Total Revenue: " + String.format(Locale.US, "%,.2f", fleetManager.generateRevenueReport()));
        System.out.println("----------------------------------------------------------------");
    }

    private void showVehiclesByFuelType() {
        FuelType fuel = readFuelType();
        if (fuel == null) {
            return;
        }
        printVehicles(fleetManager.getVehiclesByFuelType(fuel), "No vehicles with fuel type " + fuel + ".");
    }

    private void printVehicles(List<Vehicle> list, String emptyMessage) {
        if (list.isEmpty()) {
            System.out.println(emptyMessage);
            return;
        }
        for (Vehicle v : list) {
            System.out.println(v);
        }
    }

    private void printCustomers() {
        List<Customer> list = fleetManager.getCustomersSortedByName();
        if (list.isEmpty()) {
            System.out.println("No customers registered.");
            return;
        }
        for (Customer c : list) {
            System.out.println(c);
        }
    }

    private void printAgreements() {
        List<RentalAgreement> list = fleetManager.getAllAgreements();
        if (list.isEmpty()) {
            System.out.println("No rental agreements yet.");
            return;
        }
        for (RentalAgreement a : list) {
            System.out.println(a);
            System.out.println("Days elapsed: " + a.getDaysElapsed()
                    + (a.isReturned() ? "  (RETURNED)" : "  (ACTIVE)"));
        }
    }

    // ---------------------------------------------------------------- input helpers

    private String prompt(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    private String promptNonEmpty(String message) {
        while (true) {
            String s = prompt(message);
            if (!s.isEmpty()) {
                return s;
            }
            System.out.println("Value cannot be empty.");
        }
    }

    private int readInt(String message) {
        return Integer.parseInt(prompt(message));
    }

    private int readPositiveInt(String message) {
        while (true) {
            try {
                int n = Integer.parseInt(prompt(message));
                if (n > 0) {
                    return n;
                }
                System.out.println("Please enter a number greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }

    private int readNonNegativeInt(String message) {
        while (true) {
            try {
                int n = Integer.parseInt(prompt(message));
                if (n >= 0) {
                    return n;
                }
                System.out.println("Please enter 0 or a positive number.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }

    private double readPositiveDouble(String message) {
        while (true) {
            try {
                double d = Double.parseDouble(prompt(message));
                if (d > 0) {
                    return d;
                }
                System.out.println("Please enter a number greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }

    private FuelType readFuelType() {
        FuelType[] values = FuelType.values();
        System.out.println("Fuel types:");
        for (int i = 0; i < values.length; i++) {
            System.out.println("  [" + (i + 1) + "] " + values[i]);
        }
        String input = prompt("Choose fuel type (number or name) > ");
        try {
            int n = Integer.parseInt(input);
            if (n >= 1 && n <= values.length) {
                return values[n - 1];
            }
        } catch (NumberFormatException e) {
            for (FuelType f : values) {
                if (f.name().equalsIgnoreCase(input)) {
                    return f;
                }
            }
        }
        System.out.println("Invalid fuel type.");
        return null;
    }
}
