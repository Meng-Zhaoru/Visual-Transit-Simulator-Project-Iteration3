package edu.umn.cs.csci3081w.project.webserver;

import edu.umn.cs.csci3081w.project.model.Bus;
import edu.umn.cs.csci3081w.project.model.BusFactory;
import edu.umn.cs.csci3081w.project.model.Counter;
import edu.umn.cs.csci3081w.project.model.Line;
import edu.umn.cs.csci3081w.project.model.StorageFacility;
import edu.umn.cs.csci3081w.project.model.Train;
import edu.umn.cs.csci3081w.project.model.TrainFactory;
import edu.umn.cs.csci3081w.project.model.Vehicle;
import edu.umn.cs.csci3081w.project.model.VehicleConcreteSubject;
import edu.umn.cs.csci3081w.project.model.VehicleFactory;
import java.util.ArrayList;
import java.util.List;

public class VisualTransitSimulator {

  private static boolean LOGGING = false;
  private int numTimeSteps = 0;
  private int simulationTimeElapsed = 0;
  private Counter counter;
  private List<Line> lines;
  private List<Vehicle> activeVehicles;
  private List<Vehicle> completedTripVehicles;
  private List<Integer> vehicleStartTimings;
  private List<Integer> timeSinceLastVehicle;
  private StorageFacility storageFacility;
  private WebServerSession webServerSession;
  private VehicleFactory busFactory;
  private VehicleFactory trainFactory;
  private VehicleConcreteSubject vehicleConcreteSubject;

  /**
   * Constructor for Simulation.
   *
   * @param configFile       file containing the simulation configuration
   * @param webServerSession session associated with the simulation
   */
  public VisualTransitSimulator(String configFile, WebServerSession webServerSession) {
    this.webServerSession = webServerSession;
    this.counter = new Counter();
    ConfigManager configManager = new ConfigManager();
    configManager.readConfig(counter, configFile);
    this.lines = configManager.getLines();
    this.activeVehicles = new ArrayList<Vehicle>();
    this.completedTripVehicles = new ArrayList<Vehicle>();
    this.vehicleStartTimings = new ArrayList<Integer>();
    this.timeSinceLastVehicle = new ArrayList<Integer>();
    this.storageFacility = configManager.getStorageFacility();
    if (this.storageFacility == null) {
      this.storageFacility = new StorageFacility(Integer.MAX_VALUE, Integer.MAX_VALUE,
          Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
    vehicleConcreteSubject = new VehicleConcreteSubject(webServerSession);

    if (VisualTransitSimulator.LOGGING) {
      System.out.println("////Simulation Lines////");
      for (int i = 0; i < lines.size(); i++) {
        lines.get(i).report(System.out);
      }
    }
  }

  /**
   * Initializes vehicle factory classes for the simulation.
   *
   * @param time time when the simulation was started
   */
  public void setVehicleFactories(int time) {
    this.busFactory = new BusFactory(storageFacility, counter, time);
    this.trainFactory = new TrainFactory(storageFacility, counter, time);
  }

  /**
   * Starts the simulation.
   *
   * @param vehicleStartTimings start timings of bus
   * @param numTimeSteps        number of time steps
   */
  public void start(List<Integer> vehicleStartTimings, int numTimeSteps) {
    this.vehicleStartTimings = vehicleStartTimings;
    this.numTimeSteps = numTimeSteps;
    for (int i = 0; i < vehicleStartTimings.size(); i++) {
      this.timeSinceLastVehicle.add(i, 0);
    }
    simulationTimeElapsed = 0;
  }

  /**
   * Updates the simulation at each step.
   */
  public void update() {
    simulationTimeElapsed++;
    if (simulationTimeElapsed > numTimeSteps) {
      return;
    }
    System.out.println("~~~~The simulation time is now at time step "
        + simulationTimeElapsed + "~~~~");
    // generate vehicles
    for (int i = 0; i < timeSinceLastVehicle.size(); i++) {
      Line line = lines.get(i);
      if (timeSinceLastVehicle.get(i) <= 0) {
        Vehicle generatedVehicle = null;
        if (line.getType().equals(Line.BUS_LINE) && !line.isIssueExist()) {
          generatedVehicle = busFactory.generateVehicle(line.shallowCopy());
        } else if (line.getType().equals(Line.TRAIN_LINE) && !line.isIssueExist()) {
          generatedVehicle = trainFactory.generateVehicle(line.shallowCopy());
        }
        if (line.getType().equals(Line.TRAIN_LINE) || line.getType().equals(Line.BUS_LINE)) {
          if (generatedVehicle != null && !line.isIssueExist()) {
            activeVehicles.add(generatedVehicle);
          }
          timeSinceLastVehicle.set(i, vehicleStartTimings.get(i));
          timeSinceLastVehicle.set(i, timeSinceLastVehicle.get(i) - 1);
        }
      } else {
        if (!line.isIssueExist()) {
          timeSinceLastVehicle.set(i, timeSinceLastVehicle.get(i) - 1);
        }
      }
    }
    // update vehicles
    for (int i = activeVehicles.size() - 1; i >= 0; i--) {
      Vehicle currVehicle = activeVehicles.get(i);
      currVehicle.update();
      if (currVehicle.isTripComplete()) {
        Vehicle completedTripVehicle = activeVehicles.remove(i);
        completedTripVehicles.add(completedTripVehicle);
        if (completedTripVehicle instanceof Bus) {
          busFactory.returnVehicle(completedTripVehicle);
        } else if (completedTripVehicle instanceof Train) {
          trainFactory.returnVehicle(completedTripVehicle);
        }
      } else {
        if (VisualTransitSimulator.LOGGING) {
          currVehicle.report(System.out);
        }
      }
    }
    // update lines
    for (int i = 0; i < lines.size(); i++) {
      Line currLine = lines.get(i);
      currLine.update();
      if (VisualTransitSimulator.LOGGING) {
        currLine.report(System.out);
      }
    }
    vehicleConcreteSubject.notifyObservers();
  }

  public List<Line> getLines() {
    return lines;
  }

  public List<Vehicle> getActiveVehicles() {
    return activeVehicles;
  }

  /**
   * Registers an observer into the vehicle subject.
   *
   * @param vehicle the vehicle that starts observing
   */
  public void addObserver(Vehicle vehicle) {
    vehicleConcreteSubject.attachObserver(vehicle);
  }
}
