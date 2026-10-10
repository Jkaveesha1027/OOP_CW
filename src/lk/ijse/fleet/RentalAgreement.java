package lk.ijse.fleet;

import java.util.Locale;

public class RentalAgreement {
    private static int counter = 1000;

    private final String agreementId;
    private final Vehicle vehicle;     // composition (has-a)
    private final Customer customer;   // composition (has-a)
    private final int rentalDays;
    private final double totalCost;
    private int daysElapsed;
    private boolean returned;          // set once the vehicle is returned (stops overdue checks)

    public RentalAgreement(Vehicle vehicle, Customer customer, int rentalDays) {
        this.vehicle = vehicle;
        this.customer = customer;
        this.rentalDays = rentalDays;
        double subtotal = vehicle.calculateRentalCost(rentalDays);
        this.totalCost = customer.applyDiscount(subtotal);
        this.agreementId = "AGR-" + (++counter);
        this.daysElapsed = 0;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Customer getCustomer() {
        return customer;
    }

    public int getRentalDays() {
        return rentalDays;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public int getDaysElapsed() {
        return daysElapsed;
    }

    public void setDaysElapsed(int daysElapsed) {
        this.daysElapsed = daysElapsed;
    }

    public boolean isReturned() {
        return returned;
    }

    void markReturned() {
        this.returned = true;
    }

    private static String typeLabel(Object o, String suffixToStrip) {
        String name = o.getClass().getSimpleName();
        if (!suffixToStrip.isEmpty() && name.endsWith(suffixToStrip) && name.length() > suffixToStrip.length()) {
            name = name.substring(0, name.length() - suffixToStrip.length());
        }
        return name;
    }

    @Override
    public String toString() {
        String line = "================================================================";
        StringBuilder sb = new StringBuilder();
        sb.append(line).append("\n");
        sb.append("RENTAL AGREEMENT RECEIPT\n");
        sb.append(line).append("\n");
        sb.append("Agreement ID : ").append(agreementId).append("\n");
        sb.append("Vehicle      : ").append(vehicle.getVehicleId()).append(" - ")
                .append(vehicle.getBrand()).append(" ").append(vehicle.getModel())
                .append(" (").append(typeLabel(vehicle, "")).append(")\n");
        sb.append("Customer     : ").append(customer.getCustomerId()).append(" - ")
                .append(customer.getFullName())
                .append(" (").append(typeLabel(customer, "Customer")).append(")\n");
        sb.append("Rental Days  : ").append(rentalDays).append("\n");
        sb.append("Total Cost   : ").append(String.format(Locale.US, "%,.2f", totalCost)).append("\n");
        sb.append(line);
        return sb.toString();
    }
}
