# DeltaIoT Simulator — Project Course Self Adaptive System

This repository contains a Java-based simulator implementing a self-adaptive system based on DeltaIoT, extended to support 3 new different types of runtime uncertainties.

## Branches Overview

Each branch of the repository represents a specific uncertainty scenario:

- `BatteryHardwareDegradation`: models the natural decay of battery capacity in IoT motes over time.
- `LocalCongestionMotes`: models congestion due to traffic spikes that saturate mote buffers.
- `MalfunctionEnvironmentalData`: models faulty motes sending invalid or unusable environmental data.

Switch to the appropriate branch depending on the uncertainty you want to analyze.

## Requirements

- JDK 1.8
- Java 1.8 Runtime
- Eclipse IDE (recommended for setup and execution)

Note: The simulator is designed and tested to work specifically with Java 1.8. Later versions may cause compatibility issues.

## How to Run the Simulator

1. Clone the repository:
   ```bash
   git clone [repository-url]
   cd [repository-folder]
   ```

2. Checkout one of the branches:
   ```bash
   git checkout BatteryHardwareDegradation
   # or:
   git checkout LocalCongestionMotes
   # or:
   git checkout MalfunctionEnvironmentalData
   ```

3. Import the project into Eclipse:
   - Open Eclipse.
   - Go to File > Import > Existing Projects into Workspace.
   - Select the root folder of the repository.
   - Finish the import.

4. Run the simulator:
   - Open the file:  
     `SimulatorGUI/src/deltaiot/gui/DeltaIoTEmulatorMain.java`
   - Right-click on it → Run As > Java Application.

   This will launch the graphical user interface of the DeltaIoT simulator.

## Project Structure

```
.
├── SimpleAdaptationWithSimulation/
├── Simulator/
├── SimulatorGUI/
│   └── src/
│       └── deltaiot/
│           └── gui/
│               ├── DeltaIoTEmulatorMain.java  # main class to run
│               └── DeltaIoTClientMain.java    # optional client mode
```

## Notes

- The DeltaIoTEmulatorMain.java class is the main entry point for running simulations via GUI.
- Each branch includes specific modifications to the MAPE-K loop, UML logic, and adaptation behavior.
