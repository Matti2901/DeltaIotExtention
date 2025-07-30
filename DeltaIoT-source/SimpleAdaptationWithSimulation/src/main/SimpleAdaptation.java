package main;

import java.util.ArrayList;

import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.client.SimulationClient;
import deltaiot.gui.DeltaIoTEmulatorMain;
import domain.Mote;
import mapek.FeedbackLoop;
import simulator.QoS;
import simulator.Simulator;

public class SimpleAdaptation {
	
	SimulationClient networkMgmt;
	public SimpleAdaptation() {
		networkMgmt = new SimulationClient();
	}
	public void start(int runCount) {
	    FeedbackLoop feedbackLoop = new FeedbackLoop();

	    Probe probe = networkMgmt.getProbe();
	    Effector effector = networkMgmt.getEffector();

	    feedbackLoop.setProbe(probe);
	    feedbackLoop.setEffector(effector);

	    Simulator sim = networkMgmt.getSimulator();
	    sim.getQosValues().clear();
	    
	    for (int i = 0; i < runCount; i++) {
	    	System.out.println();
	    	System.out.println("------------------------------------------------------------");
	    	System.out.println("Start Adaptation Cycle");
	        sim.doAdaptation(feedbackLoop); // All logic encapsulated in Simulator
	        System.out.println("------------------------------------------------------------");
	    }
	    System.out.println("Qos");
	    System.out.println("Run, PacketLoss, EnergyConsumption");
	    for (QoS qos : sim.getQosValues()) {
	        System.out.println(qos);
	    }
	    System.out.println("------------------------------------------------------------");
	}

	

	public Simulator getSimulator() {
		return networkMgmt.getSimulator();
	}
}
