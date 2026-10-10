# Fleet Rental Management System (FRMS)

ITS1033 Object-Oriented Programming coursework, HDSE 77, Institute of Software Engineering.
A terminal-based Java application for managing a vehicle rental fleet, customers and rental agreements.

## Requirements

- JDK 25 or later (the code uses only features available in Java 8+, so older JDKs also work)
- A terminal (or any Java IDE)

## Project structure

All types live in the single package `lk.ijse.fleet`:

```
src/lk/ijse/fleet/
  FuelType, RentalStatus                      (enums)
  SystemUser, UserManager                     (login / registration)
  Reportable, RentalEventListener             (interfaces)
  Vehicle, Car, Van, Truck                    (vehicle hierarchy)
  Customer, IndividualCustomer, CorporateCustomer
  VehicleNotAvailableException, InsufficientCreditException
  RentalAgreement
  FleetManager (+ inner class FleetSnapshot)
  VehicleStatusMonitor, OverdueReminderWorker (daemon workers)
  Main
```

## Build and run

From the project root (the folder containing `src`):

```bash
javac -d bin $(find src -name "*.java")
java -cp bin lk.ijse.fleet.Main
```

On Windows PowerShell:

```powershell
javac -d bin (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp bin lk.ijse.fleet.Main
```

## Using the application

1. Choose **[2] Register New User**, then **[1] Login**.
2. Add at least one vehicle (**[1]**) and one customer (**[2]**). IDs are generated automatically
   (`VEH-1001`, `CUS-1001`, `AGR-1001`, ...).
3. Create a rental (**[4]**) using the vehicle and customer IDs, then return it with **[5]**.
4. Reports: **[6]** revenue, **[7]** vehicles by fuel type, **[8]** combined Reportable report.
5. **[0] Logout** returns to the login menu without exiting.

Every 5 seconds two daemon threads print the fleet status and advance/check overdue rentals
(each tick simulates one day passing for active agreements).

## Concepts demonstrated

- Inheritance, abstraction, polymorphism: abstract `Vehicle` / `Customer` with concrete subclasses
- Encapsulation and Java Bean conventions on every class
- Composition / association: `RentalAgreement` has a `Vehicle` and `Customer`; `FleetManager` owns the lists;
  `Main` holds `FleetManager` and `UserManager`
- Rich enum (`FuelType`), plain enum (`RentalStatus`)
- Static counters for auto-generated IDs
- Custom checked exceptions thrown in `createRentalAgreement` and caught in the menu
- Functional interface `RentalEventListener` implemented with a lambda in `Main`
- Non-static inner class `FleetManager.FleetSnapshot`
- Multithreading: `Runnable` workers started as daemon threads; synchronized methods
- Collections and Streams (`filter`, `sorted`, `mapToDouble`)
- Interface polymorphism via `Reportable` implemented directly by `Vehicle` and `Customer`

## Implementation notes

- `RentalAgreement` has one extra private field, `returned`, so that overdue checks stop for
  agreements whose vehicle has already been returned.
- Collection-touching methods in `FleetManager` are `synchronized` (not only the two required ones) because
  the background threads and the menu thread share the same lists.
- Passwords are stored in plain text, as required by the brief. This is not suitable for production.

