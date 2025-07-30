package deltaiot.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import main.SimpleAdaptation;
import simulator.QoS;
import simulator.Simulator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import domain.Mote;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Dialog;

public class DeltaIoTEmulatorMain extends Application {
	@FXML
	private Button runEmulator, btnSaveResults, btnAdaptationLogic, btnClearResults, btnDisplay;

	@FXML
	private LineChart<?, ?> chartPacketLoss, chartEnergyConsumption;

	@FXML
	private ProgressBar progressBar;

	@FXML
	private Label lblProgress;

	public static final Path filePath = Paths.get(System.getProperty("user.dir"), "data", "data.csv");
	List<String> data = new LinkedList<String>();
	Stage primaryStage;
	Simulator simul;
	public static String selectedUncertainty = "Other";  
	public static String localCongestionMote = "Local Congestion Mote";

	public static int runNumber = 10;

	Service serviceEmulation = new Service() {

		@Override
		protected void succeeded() {
			displayData("Without adaptation", 0);
		}

		@Override
		protected Task createTask() {
			return new Task() {
				@Override
				protected Object call() throws Exception {
					btnDisplay.setDisable(true);
					simul = deltaiot.DeltaIoTSimulator.createSimulatorForDeltaIoT(selectedUncertainty); // IN REALTA VIENE FATTO PARTIRE QUESTO
					for (int i = 0; i < runNumber; i++)
						simul.doSingleRun();
					btnDisplay.setDisable(false);
					return null;
					
				}
			};
		}
	};
	@FXML
	void runEmulatorClicked(ActionEvent event) {
		
		btnClearResults(null);  //
		selectedUncertainty = showUncertaintyPopup();
		System.out.println("Selected uncertainty: " + selectedUncertainty);
		if (!serviceEmulation.isRunning()) {
		    serviceEmulation.restart();
		    serviceProgress.restart();
		}
		// Hide or show UI based on selected uncertainty
		if (selectedUncertainty.equals("Local Congestion Mote")) {
		    //btnDisplay.setVisible(false);
		    //btnDisplay.setManaged(false);

		    chartEnergyConsumption.setVisible(false);
		    chartEnergyConsumption.setManaged(false);
		} else {
		    //btnDisplay.setVisible(true);
		    //btnDisplay.setManaged(true);

		    chartEnergyConsumption.setVisible(true);
		    chartEnergyConsumption.setManaged(true);
		}
	}



	Service serviceProgress = new Service() {
		@Override
		protected void succeeded() {

		}

		@Override
		protected Task createTask() {
			return new Task() {
				@Override
				protected Object call() throws Exception {
					int run;
					do {
						run = simul.getRunInfo().getRunNumber();

						updateProgress(run, runNumber);
						updateMessage("(" + run + "\\"+runNumber + ")");

						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

					} while (run < runNumber);
					
					return null;
				}
			};
		}
	};

	Service serviceAdaptation = new Service() {

		@Override
		protected void succeeded() {
			displayData("With adaptation", 1);
		}

		@Override
		protected Task createTask() {
			return new Task() {
				@Override
				protected Object call() throws Exception {
					btnDisplay.setDisable(true);
					SimpleAdaptation client = new SimpleAdaptation();
					client.start(runNumber);
					simul = client.getSimulator();
					btnDisplay.setDisable(false);
					return null;
				}
			};
		}
	};

	@FXML
	void btnAdaptationLogic(ActionEvent event) {
		if (!serviceAdaptation.isRunning()) {
			serviceAdaptation.restart();
			serviceProgress.restart();
		}
	}

	@FXML
	void btnDisplay(ActionEvent event) {
		if(selectedUncertainty.equals(localCongestionMote)) {
			  // Se l'incertezza è "Local Congestion Mote", mostra solo immagine
	        try {
	            Stage stage = new Stage();
	            stage.setTitle("Topology Scenario");
	            
	            Path imagePath = Paths.get(System.getProperty("user.dir"), "DeltaIoT-source", "SimulatorGUI", "resources", "TopologyScenario.png");
	            File imageFile = imagePath.toFile();
	
	            ImageView imageView = new ImageView(new Image(imageFile.toURI().toString()));
	            imageView.setPreserveRatio(true);
	            imageView.setFitWidth(800);

	            // (Facoltativo) Adatta dinamicamente alla finestra
	            imageView.setSmooth(true);
	            StackPane root = new StackPane(imageView);
	            Scene scene = new Scene(root, 800, 600);

	            stage.setScene(scene);
	            stage.setResizable(true);
	            stage.show();
	        } catch (Exception e) {
	            System.err.println(" Error displaying TopologyScenario.png:");
	            e.printStackTrace();
	        }
		}
		else {
		try {
			// Fix Windows path compatibility for FXML resources
			URL fxmlUrl = getFXMLResource("DeltaIoTModel.fxml");
			
			if (fxmlUrl == null) {
				System.err.println("ERROR: DeltaIoTModel.fxml not found in resources");
				return;
			}
			
			FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
			Parent root1 = (Parent) fxmlLoader.load();
			DeltaIoTClientMain main = fxmlLoader.getController();
			main.setSimulationClient(simul);
			Stage stage = new Stage();
			stage.setScene(new Scene(root1));
			stage.setResizable(false);
			stage.setTitle("DeltaIoT topology");
			stage.setAlwaysOnTop(true);
			stage.showAndWait();
		} catch (Exception e) {
			System.err.println("ERROR opening topology window:");
			e.printStackTrace();
			
			// Fallback: show user-friendly error message
			Platform.runLater(() -> {
				javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
				alert.setTitle("Display error");
				alert.setHeaderText("Unable to open topology window");
				alert.setContentText("Please verify that FXML files are present in the resources folder.");
				alert.showAndWait();
			});
		}
		}
	}

	@FXML
	void initialize() {
		assert progressBar != null : "fx:id=\"progressBar\" was not injected: check your FXML file 'Progress.fxml'.";
		lblProgress.textProperty().bind(serviceProgress.messageProperty());
		progressBar.progressProperty().bind(serviceProgress.progressProperty());
	}

	@FXML
	void btnSaveResults(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();

		// Set extension filter for CSV files
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
		fileChooser.getExtensionFilters().add(extFilter);

		// Show save file dialog
		File file = fileChooser.showSaveDialog(primaryStage);

		if (file != null) {
			saveFile(file);
		}
	}
	private String showUncertaintyPopup() {
	    Dialog<String> dialog = new Dialog<>();
	    dialog.setTitle("Select Uncertainty");
	    dialog.setHeaderText("Which new uncertainty do you want to try");

	    VBox buttonBox = new VBox(10);
	    buttonBox.setPrefWidth(300);

	    Button btn2 = new Button(localCongestionMote);
	    Button btnOther = new Button("Other");

	  
	    btn2.setMaxWidth(Double.MAX_VALUE);

	    btnOther.setMaxWidth(Double.MAX_VALUE);

	    final String[] selected = {null};

	   
	    btn2.setOnAction(e -> { selected[0] = localCongestionMote; dialog.close(); });
	 
	    btnOther.setOnAction(e -> { selected[0] = "Other"; dialog.close(); });

	    buttonBox.getChildren().addAll(btn2,btnOther);
	    dialog.getDialogPane().setContent(buttonBox);

	    // Required to prevent default dialog handling
	    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
	    dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(false);

	    dialog.showAndWait();

	    return selected[0] != null ? selected[0] : "Other";
	}




	private void saveFile(File file) {
		try {
			FileWriter fileWriter = null;

			fileWriter = new FileWriter(file, true);

			for (String line : data)
				fileWriter.write(line + "\n");
			fileWriter.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	@FXML
	void btnClearResults(ActionEvent event) {
		chartEnergyConsumption.getData().clear();
		chartPacketLoss.getData().clear();
		data.clear();
	}

	void displayData(String setName, int index) {
	    XYChart.Series energyConsumptionSeries = new XYChart.Series();
	    XYChart.Series packetLossSeries = new XYChart.Series();
	    energyConsumptionSeries.setName(setName);
	    packetLossSeries.setName(setName);
	    List<QoS> qosList = simul.getQosValues();

	    int i = 0; // usare l'indice come X
	    for (QoS qos : qosList) {
	        data.add(qos + ", " + setName);
	        energyConsumptionSeries.getData()
	            .add(new XYChart.Data(i, qos.getEnergyConsumption()));
	        packetLossSeries.getData()
	            .add(new XYChart.Data(i, qos.getPacketLoss()));
	        i++;
	    }

	    chartEnergyConsumption.getData().add(energyConsumptionSeries);
	    chartPacketLoss.getData().add(packetLossSeries);
	}

	@Override
	public void start(Stage stage) throws Exception {
		// Configure JavaFX properties for Windows compatibility
		configureJavaFXForWindows();
		
		try {
			System.out.println("Attempting to load EmulatorGUI.fxml...");
			
			// Fix Windows FXML loading with improved resource detection
			URL fxmlUrl = getFXMLResource("EmulatorGUI.fxml");
			
			if (fxmlUrl == null) {
				System.err.println("EmulatorGUI.fxml not found, attempting fallback UI");
				createFallbackUI(stage);
				return;
			}
			
			System.out.println("Loading FXML from: " + fxmlUrl);
			Parent root = FXMLLoader.load(fxmlUrl);
			primaryStage = stage;
			Scene scene = new Scene(root, 900, 600);

			stage.setTitle("DeltaIoT Simulator");
			stage.setOnCloseRequest(new EventHandler() {
				@Override
				public void handle(Event arg0) {
					Platform.exit();
				}
			});
			stage.setScene(scene);
			stage.show();
			
			System.out.println("Application started successfully!");
			
		} catch (Exception e) {
			System.err.println("ERROR in main application startup:");
			e.printStackTrace();
			
			System.out.println("Attempting fallback UI...");
			createFallbackUI(stage);
		}
	}

	/**
	 * Configure JavaFX properties specifically for Windows
	 */
	private void configureJavaFXForWindows() {
		// Configuration to avoid rendering issues on Windows
		System.setProperty("prism.order", "sw");
		System.setProperty("prism.text", "t2k");
		System.setProperty("java.awt.headless", "false");
		System.setProperty("prism.forceGPU", "false");
		System.setProperty("prism.allowhidpi", "false");
		System.setProperty("file.encoding", "UTF-8");
		
		// Disable networking if not necessary
		System.setProperty("java.net.useSystemProxies", "false");
		System.setProperty("networkaddress.cache.ttl", "0");
	}

	/**
	 * Find FXML resources in a Windows-compatible way
	 */
	private URL getFXMLResource(String fxmlName) {
	    // Try first in the current class package
	    URL resource = getClass().getResource(fxmlName);
	    if (resource != null) {
	        System.out.println("Found " + fxmlName + " in current package: " + resource);
	        return resource;
	    }
	    
	    // Try in classpath root
	    resource = getClass().getResource("/" + fxmlName);
	    if (resource != null) {
	        System.out.println("Found " + fxmlName + " in classpath root: " + resource);
	        return resource;
	    }
	    
	    // Try in ClassLoader
	    resource = getClass().getClassLoader().getResource(fxmlName);
	    if (resource != null) {
	        System.out.println("Found " + fxmlName + " in ClassLoader: " + resource);
	        return resource;
	    }
	    
	    // Try in resources folder
	    resource = getClass().getClassLoader().getResource("resources/" + fxmlName);
	    if (resource != null) {
	        System.out.println("Found " + fxmlName + " in resources/: " + resource);
	        return resource;
	    }
	    
	    // Try relative paths in filesystem
	    try {
	        String userDir = System.getProperty("user.dir");
	        String[] possiblePaths = {
	            // Path for SimulatorGUI project
	            "DeltaIoT-source/SimulatorGUI/resources/" + fxmlName,
	            "DeltaIoT-source/SimulatorGUI/src/resources/" + fxmlName,
	            "DeltaIoT-source/SimulatorGUI/" + fxmlName,
	            // Generic paths
	            "resources/" + fxmlName,
	            "src/main/resources/" + fxmlName,
	            "src/resources/" + fxmlName,
	            fxmlName
	        };
	        
	        for (String pathStr : possiblePaths) {
	            Path path = Paths.get(userDir, pathStr);
	            File file = path.toFile();
	            System.out.println("Checking path: " + file.getAbsolutePath());
	            if (file.exists() && file.canRead()) {
	                System.out.println("Found " + fxmlName + " in filesystem: " + file.getAbsolutePath());
	                return file.toURI().toURL();
	            }
	        }
	    } catch (Exception e) {
	        System.err.println("Error in filesystem search for " + fxmlName + ": " + e.getMessage());
	    }
	    
	    System.err.println("WARNING: " + fxmlName + " not found in any location");
	    return null;
	}

	/**
	 * Create fallback UI if FXML doesn't work
	 */
	private void createFallbackUI(Stage stage) {
		try {
			javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(15);
			root.setPadding(new javafx.geometry.Insets(20));
			root.setAlignment(javafx.geometry.Pos.CENTER);
			root.setStyle("-fx-background-color: #f0f0f0;");
			
			javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("DeltaIoT simulator");
			titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
			
			javafx.scene.control.Label statusLabel = new javafx.scene.control.Label(
				"Simplified interface active\n" +
				"(FXML files not available)"
			);
			statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-text-alignment: center;");
			
			// Progress bar for simulation
			ProgressBar progressBar = new ProgressBar(0);
			progressBar.setPrefWidth(300);
			Label progressLabel = new Label("Ready for simulation");
			
			javafx.scene.control.Button btnRunSimulation = new javafx.scene.control.Button("Run complete simulation");
			btnRunSimulation.setStyle("-fx-font-size: 14px; -fx-padding: 10px 20px;");
			btnRunSimulation.setOnAction(e -> {
				btnRunSimulation.setDisable(true);
				runSimulationWithProgress(progressBar, progressLabel, btnRunSimulation);
			});
			
			javafx.scene.control.Button btnRunQuick = new javafx.scene.control.Button("Quick Test");
			btnRunQuick.setStyle("-fx-font-size: 14px; -fx-padding: 10px 20px;");
			btnRunQuick.setOnAction(e -> {
				btnRunQuick.setDisable(true);
				runQuickTest(progressLabel, btnRunQuick);
			});
			
			javafx.scene.control.Button btnExit = new javafx.scene.control.Button("Exit");
			btnExit.setStyle("-fx-font-size: 14px; -fx-padding: 10px 20px;");
			btnExit.setOnAction(e -> Platform.exit());
			
			root.getChildren().addAll(
				titleLabel, 
				statusLabel, 
				new javafx.scene.control.Separator(),
				progressLabel,
				progressBar,
				btnRunSimulation, 
				btnRunQuick,
				btnExit
			);
			
			Scene scene = new Scene(root, 600, 400);
			stage.setTitle("DeltaIoT simulator - Simplified mode");
			stage.setScene(scene);
			stage.show();
			
			System.out.println("Fallback UI created successfully");
			
		} catch (Exception e) {
			System.err.println("Error creating fallback UI:");
			e.printStackTrace();
			// Last resort: exit with message
			System.err.println("Unable to start any interface. Please check JavaFX installation.");
			Platform.exit();
		}
	}

	/**
	 * Run simulation with progress bar
	 */
	private void runSimulationWithProgress(ProgressBar progressBar, Label progressLabel, javafx.scene.control.Button btnRun) {
	    Task<Void> task = new Task<Void>() {
	        @Override
	        protected Void call() throws Exception {
	            try {
	                updateMessage("Initializing simulator...");
	                //simul = deltaiot.DeltaIoTSimulator.createSimulatorForDeltaIoT();
	               
	                if(selectedUncertainty.equals(localCongestionMote)) { //MATTIA
	                	System.out.println("SOno nel if");
	                	System.out.println("Start uncertain: "+localCongestionMote);
	                	simul = deltaiot.DeltaIoTSimulator.createSimulatorForDeltaIoT(localCongestionMote);
	                }
	             
                	else {
                		System.out.println("Start uncertain: base");
                    simul = deltaiot.DeltaIoTSimulator.createSimulatorForDeltaIoT("Other");
	                } 
	                int totalRuns = runNumber;
	                for (int i = 0; i < totalRuns; i++) {
	                    if (isCancelled()) break;
	                    
	                    simul.doSingleRun();
	                    updateProgress(i + 1, totalRuns);
	                    updateMessage("Cycle " + (i + 1) + "/" + totalRuns + " completed");
	                    
	                    // Small pause to allow UI updates
	                    Thread.sleep(50);
	                }
	                
	                updateMessage("Simulation completed successfully!");
	                
	            } catch (Exception e) {
	                updateMessage("Simulation error: " + e.getMessage());
	                e.printStackTrace();
	            }
	            return null;
	        }
	    };
	    
	    progressBar.progressProperty().bind(task.progressProperty());
	    progressLabel.textProperty().bind(task.messageProperty());
	    
	    task.setOnSucceeded(e -> {
	        btnRun.setDisable(false);
	        showResults();
	    });
	    
	    task.setOnFailed(e -> {
	        btnRun.setDisable(false);
	        progressLabel.setText("Simulation failed");
	    });
	    
	    new Thread(task).start();
	}

	/**
	 * Run quick test
	 */
	private void runQuickTest(Label statusLabel, javafx.scene.control.Button btnQuick) {
	    Task<Void> task = new Task<Void>() {
	        @Override
	        protected Void call() throws Exception {
	            try {
	                updateMessage("Quick test in progress...");
	                simul = deltaiot.DeltaIoTSimulator.createSimulatorForDeltaIoT("Other");
	                
	                for (int i = 0; i < 5; i++) {
	                    simul.doSingleRun();
	                    updateMessage("Test " + (i + 1) + "/5");
	                    Thread.sleep(200);
	                }
	                
	                updateMessage("Test completed!");
	                
	            } catch (Exception e) {
	                updateMessage("Test error: " + e.getMessage());
	                e.printStackTrace();
	            }
	            return null;
	        }
	    };
	    
	    statusLabel.textProperty().bind(task.messageProperty());
	    
	    task.setOnSucceeded(e -> btnQuick.setDisable(false));
	    task.setOnFailed(e -> btnQuick.setDisable(false));
	    
	    new Thread(task).start();
	}

	/**
	 * Show results in separate window
	 */
	private void showResults() {
	    try {
	        if (simul != null) {
	            List<QoS> qosList = simul.getQosValues();
	            if (qosList != null && !qosList.isEmpty()) {
	                
	                StringBuilder results = new StringBuilder();
	                results.append("DeltaIoT simulation results:\n\n");
	                
	                for (int i = 0; i < Math.min(10, qosList.size()); i++) {
	                    QoS qos = qosList.get(i);
	                    results.append(String.format("Cycle %d - Period: %s, Packet Loss: %.3f, Energy: %.3f\n",
	                        i + 1, qos.getPeriod(), qos.getPacketLoss(), qos.getEnergyConsumption()));
	                }
	                
	                if (qosList.size() > 10) {
	                    results.append(String.format("\n... and %d more results\n", qosList.size() - 10));
	                }
	                
	                // Create results window
	                Stage resultStage = new Stage();
	                javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(results.toString());
	                textArea.setEditable(false);
	                textArea.setPrefSize(500, 300);
	                
	                javafx.scene.layout.VBox layout = new javafx.scene.layout.VBox(10);
	                layout.setPadding(new javafx.geometry.Insets(10));
	                layout.getChildren().add(textArea);
	                
	                Scene scene = new Scene(layout);
	                resultStage.setTitle("Simulation results");
	                resultStage.setScene(scene);
	                resultStage.show();
	                
	                // Also print to console
	                System.out.println("\n" + results.toString());
	            }
	        }
	    } catch (Exception e) {
	        System.err.println("Error displaying results:");
	        e.printStackTrace();
	    }
	}

	public static void main(String[] args) {
		try {
			// Configure environment for Windows before launch
			System.setProperty("prism.order", "sw");
			System.setProperty("prism.text", "t2k");
			System.setProperty("java.awt.headless", "false");
			System.setProperty("prism.forceGPU", "false");
			System.setProperty("prism.allowhidpi", "false");
			
			// Debug information
			System.out.println("=== DeltaIoT simulator starting ===");
			System.out.println("OS: " + System.getProperty("os.name"));
			System.out.println("Java version: " + System.getProperty("java.version"));
			System.out.println("User dir: " + System.getProperty("user.dir"));
			System.out.println("JavaFX runtime: " + System.getProperty("javafx.runtime.version"));
			System.out.println("Class path: " + System.getProperty("java.class.path"));
			System.out.println("================================");
			
			// Verify presence of essential files
			String userDir = System.getProperty("user.dir");
			File deltaIoTSource = new File(userDir, "DeltaIoT-source");
			File deltaIoTExecutables = new File(userDir, "DeltaIoT-executables");
			
			System.out.println("Project structure verification:");
			System.out.println("DeltaIoT-source exists: " + deltaIoTSource.exists());
			System.out.println("DeltaIoT-executables exists: " + deltaIoTExecutables.exists());
			
			launch(args);
			
		} catch (Exception e) {
			System.err.println("FATAL ERROR starting DeltaIoT:");
			e.printStackTrace();
			
			// Try alternative startup
			System.err.println("Attempting alternative startup...");
			try {
				Application.launch(DeltaIoTEmulatorMain.class, args);
			} catch (Exception e2) {
				System.err.println("Alternative startup also failed:");
				e2.printStackTrace();
				
				System.err.println("\n=== TROUBLESHOOTING INSTRUCTIONS ===");
				System.err.println("1. Verify FXML files are present in:");
				System.err.println("   - DeltaIoT-source/SimulatorGUI/resources/");
				System.err.println("2. Check JavaFX is properly installed");
				System.err.println("3. Use Oracle JDK 8 if possible");
				System.err.println("4. Verify file read permissions");
				System.err.println("==========================================");
				
				System.exit(1);
			}
		}
	}    
}