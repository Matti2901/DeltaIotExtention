package simulator;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import domain.*;
import mapek.FeedbackLoop;

public class Simulator {
	
	private List<Mote> motes = new ArrayList<>();
	private List<Gateway> gateways = new ArrayList<>();
	private List<Integer> turnOrder = new ArrayList<>();
	private RunInfo runInfo = new RunInfo();
	private List<QoS> qosValues = new ArrayList<>();
	
	// Constructor
	public Simulator() { }
	
	// Pre-build simulators
	
	public static Simulator createBaseCase() {
		Simulator simul = new Simulator();
		final int MAX_QUEUE_SIZE = 10000;
		// Motes
		double battery = 11880;
		int load = 10;
		Mote mote1 = new Mote(1, battery, load,MAX_QUEUE_SIZE);
		Mote mote12 = new Mote(12, battery, load,MAX_QUEUE_SIZE);
		Mote mote2 = new Mote(2, battery, load,MAX_QUEUE_SIZE);
		simul.addMotes(mote1, mote12, mote2);
		
		// Gateways
		// I use the convention to give gateways negative ids
		// Nothing enforces this, but all ids have to be unique between all nodes (= motes & gateways)
		Gateway gateway1 = new Gateway(-1);
		gateway1.setView(mote1, mote12);
		Gateway gateway2 = new Gateway(-2);
		gateway2.setView(mote2, mote12);
		simul.addGateways(gateway1, gateway2);
		
		// Links
		int power = 15;
		int distribution = 100;
		mote1.addLinkTo(gateway1, gateway1, power, distribution);
		mote2.addLinkTo(gateway2, gateway2, power, distribution);
		mote12.addLinkTo(mote1, gateway1, power, distribution);
		mote12.addLinkTo(mote2, gateway2, power, distribution);
		
		simul.setTurnOrder(mote12, mote1, mote2);
		
		return simul;
	}
	
	public static Simulator createBaseCase2() {
		Simulator simul = new Simulator();
		final int MAX_QUEUE_SIZE = 10000;
		// Motes
		double battery = 11880;
		int load = 10;
		Mote mote0 = new Mote(0, battery, load,MAX_QUEUE_SIZE);
		Mote mote11 = new Mote(11, battery, load,MAX_QUEUE_SIZE);
		Mote mote12 = new Mote(12, battery, load,MAX_QUEUE_SIZE);
		Mote mote21 = new Mote(21, battery, load,MAX_QUEUE_SIZE);
		Mote mote22 = new Mote(22, battery, load,MAX_QUEUE_SIZE);
		simul.addMotes(mote0, mote11, mote12, mote21, mote22);
		
		// Gateways
		// I use the convention to give gateways negative ids
		// Nothing enforces this, but all ids have to be unique between all nodes (= motes & gateways)
		Gateway gateway1 = new Gateway(-1);
		gateway1.setView(mote11, mote12, mote0);
		Gateway gateway2 = new Gateway(-2);
		gateway2.setView(mote21, mote22, mote0);
		simul.addGateways(gateway1, gateway2);
		
		// Links
		int power = 15;
		int distribution = 100;
		mote0.addLinkTo(mote11, gateway1, power, distribution);
		mote0.addLinkTo(mote12, gateway1, power, distribution);
		mote0.addLinkTo(mote21, gateway2, power, distribution);
		mote0.addLinkTo(mote22, gateway2, power, distribution);
		
		mote11.addLinkTo(gateway1, gateway1, power, distribution);
		mote12.addLinkTo(gateway1, gateway1, power, distribution);

		mote21.addLinkTo(gateway2, gateway2, power, distribution);
		mote22.addLinkTo(gateway2, gateway2, power, distribution);
		
		simul.setTurnOrder(mote0, mote11, mote12, mote21, mote22);
		
		return simul;
	}
	
	// Creation API
	
	public void addMotes(Mote... motes) {
		Collections.addAll(this.motes, motes);
	}
	
	public void addGateways(Gateway... gateways) {
		Collections.addAll(this.gateways, gateways);
	}
	
	public void setTurnOrder(Mote... motes) {
		Integer[] ids = new Integer[motes.length];
		for(int i = 0; i < motes.length; ++i) {
			ids [i] = motes[i].getId();
		}
		setTurnOrder(ids);
	}
	public void setTurnOrder(Integer... ids) {
		this.turnOrder = Arrays.asList(ids);
	}
	
	// Simulation API
	
	/**
	 * Do a single simulation run.
	 * This will simulate the sending of packets through the network to the gateways.
	 * Each gateway will aggregate information about packet-loss and power-consumption.
	 * To get this information, use gateway.calculatePacketLoss and gateway.getPowerConsumed respectively.
	 */
	public void doSingleRun() {
		// Reset the gateways aggregated values, so we can start a new window to see packet loss and power consumption
		resetGatewaysAggregatedValues();
		
		// Do the actual run, this will give all motes a turn
		// Give each mote a turn, in the given order
		for(Integer id: turnOrder) {
			Mote mote = getMoteWithId(id);
			// Let mote handle its turn
			mote.handleTurn(runInfo); //return value doesn't include packets send for other motes, only its own packets
		}
		
		//QoS
		QoS qos = new QoS();
		qos.setEnergyConsumption(gateways.get(0).getPowerConsumed());
		qos.setPacketLoss(gateways.get(0).calculatePacketLoss());
		qos.setPeriod("" + runInfo.getRunNumber());
		qosValues.add(qos);

		// Increase run number
		runInfo.incrementRunNumber();
	}
	public void doAdaptation(FeedbackLoop feedbackLoop) {
		
	    // Reset the gateways aggregated values at the beginning of a new run
	    resetGatewaysAggregatedValues();

	    // Process each mote one by one, and run MAPE after each turn
	    for (Integer id : turnOrder) {
	        Mote mote = getMoteWithId(id);
	        mote.handleTurn(runInfo);

	        // Call reactive adaptation logic after each mote
	        feedbackLoop.start();
	    }

	    // Compute QoS at the end of the run
	    QoS qos = new QoS();
	    qos.setEnergyConsumption(gateways.get(0).getPowerConsumed());
	    qos.setPacketLoss(gateways.get(0).calculatePacketLoss());
	    qos.setPeriod("" + runInfo.getRunNumber());
	    qosValues.add(qos);

	    // Advance the run counter
	    runInfo.incrementRunNumber();
	}
	
	private void resetGatewaysAggregatedValues() {
		// Reset gateways' packetstore and expected packet count, so the packetloss for this run can be calculated easily
		// Also reset the consumed power, so this is correctly aggregated for this run
		for(Gateway gateway: gateways) {
			gateway.resetPacketStoreAndExpectedPacketCount();
			gateway.resetPowerConsumed();
		}
	}
	
	// Alteration and inspection API
	
	public Mote getMoteWithId(int id) {
		for(Mote mote: motes) {
			if (mote.getId() == id) return mote;
		}
		return null;
	}
	
	public Gateway getGatewayWithId(int id) {
		for(Gateway gw: gateways) {
			if (gw.getId() == id) return gw;
		}
		return null;
	}
	
	public List<Integer> getTurnOrder() {
		return Collections.unmodifiableList(turnOrder);
	}
	
	/**
	 * Gets the Node with a specified id if one exists
	 * This can be both a Mote or a Gateway
	 * 
	 * @param id The id
	 * @return The node with the given id (either a mote or gateway) if one exists (null otherwise)
	 */
	public Node getNodeWithId(int id) {
		Mote mote = getMoteWithId(id);
		if (mote == null) {
			Gateway gw = getGatewayWithId(id);
			return gw;
		}
		else return mote;
	}
	
	public List<Gateway> getGateways() {
		return Collections.unmodifiableList(gateways);
	}
	
	public List<Mote> getMotes() {
		return Collections.unmodifiableList(motes);
	}
	
	public RunInfo getRunInfo() {
		return runInfo;
	}
	
	public List<QoS> getQosValues() {
		return qosValues;
	}

	public static Simulator createCongestionScenario() {
		Simulator simul = new Simulator();
	    double battery = 11880;

	    Mote mote1         = new Mote(1, battery, 95, new Position(0, 0), 1000);     
	    Mote mote3         = new Mote(3, battery, 99, new Position(0, 1), 2000); 
	    Mote mote4         = new Mote(4, battery, 99, new Position(2, 0), 1000);    
	    Mote mote2congest  = new Mote(2, battery, 0, new Position(1, 0), 100);                 
	    Mote mote5         = new Mote(5, battery, 30, new Position(1, 1), 1000);         
	    Mote moteSupport6  = new Mote(6, battery, 20, new Position(3, 0), 2000);       
	    Mote moteSupport7  = new Mote(7, battery, 0, new Position(2, 1), 2000);                 
	    Mote moteGateway8  = new Mote(8, battery, 0, new Position(3, 1), 200000);                 

	    simul.addMotes( mote1, mote3, mote4,mote2congest,
		        mote5, moteSupport6, moteSupport7, moteGateway8);
	    Gateway gateway = new Gateway(-1);
	    gateway.setView( mote1, mote3, mote4,mote2congest,
                mote5, moteSupport6, moteSupport7, moteGateway8);

	    simul.addGateways(gateway);

	    int power = 15;


	    mote1.addLinkTo(mote2congest, gateway, power, 90);    
	    mote1.addLinkTo(moteSupport6, gateway, power, 10);   
	    
	    mote2congest.addLinkTo(moteGateway8, gateway, power, 100);
	    
	    mote3.addLinkTo(mote2congest, gateway, power, 100);     
	    mote3.addLinkTo(moteSupport6, gateway, power, 0);  
	    

	    mote4.addLinkTo(mote2congest, gateway, power, 100);
	    mote4.addLinkTo(moteSupport7, gateway, power, 0);    

	    mote5.addLinkTo(mote2congest, gateway, power, 100);   
	    mote5.addLinkTo(moteSupport7, gateway, power, 0);    

	    moteSupport6.addLinkTo(moteGateway8, gateway, power,100);    

	    moteSupport7.addLinkTo(moteGateway8, gateway, power, 100);  

	    moteGateway8.addLinkTo(gateway, gateway, power, 100);  

	    simul.setTurnOrder(
	    		 mote1,  mote3, mote4,mote2congest,
			        mote5, moteSupport6, moteSupport7, moteGateway8
	    );

	    // SNR
	    for (Mote mote : simul.getMotes()) {
	        for (Link link : mote.getLinks()) {
	            if (link.getSnrEquation() == null) {
	                link.setSnrEquation(new SNREquation(0.1, 2.0));
	            }
	        }
	    }
	    return simul;
	}



}