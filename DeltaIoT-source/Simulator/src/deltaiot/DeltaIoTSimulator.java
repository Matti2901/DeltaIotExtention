package deltaiot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import deltaiot.gui.DeltaIoTEmulatorMain;
import domain.Link;
import domain.Mote;
import domain.Node;
import domain.Position;
import domain.SNREquation;
import simulator.Simulator;
import domain.Constant;
import domain.DoubleRange;
import domain.FileProfile;
import domain.Gateway;

public class DeltaIoTSimulator {

	final Lock lock = new ReentrantLock();
	final Condition adaptationCompleted = lock.newCondition();
	final static int GATEWAY_ID = 1;
	final static int PORT = 9888;

	Simulator simul;
	
	public DeltaIoTSimulator() {
		this.simul = createSimulatorForDeltaIoT("Other");
	}
	public static Simulator createSimulatorForDeltaIoT(String uncertain) {
	    if (uncertain.equals("Other")) {
	        return createDefaultScenario();
	    } else if (uncertain.equals(DeltaIoTEmulatorMain.localCongestionMote)) {
	        return Simulator.createCongestionScenario(); 
	    } else {
	        throw new IllegalArgumentException("Unknown uncertainty type: " + uncertain);
	    }
	}
	
	public static Simulator createDefaultScenario() {
	    Simulator simul = new Simulator();

	    // Motes
	    int load = 10;
	    double battery = 11880.0;
	    double posScale = 2;
	    int maxQueue = 100000;

	    Mote mote2  = new Mote(2 , battery, load, new Position(378 * posScale, 193 * posScale), maxQueue);
	    Mote mote3  = new Mote(3 , battery, load, new Position(365 * posScale, 343 * posScale), maxQueue);
	    Mote mote4  = new Mote(4 , battery, load, new Position(508 * posScale, 296 * posScale), maxQueue);
	    Mote mote5  = new Mote(5 , battery, load, new Position(603 * posScale, 440 * posScale), maxQueue);
	    Mote mote6  = new Mote(6 , battery, load, new Position(628 * posScale, 309 * posScale), maxQueue);
	    Mote mote7  = new Mote(7 , battery, load, new Position(324 * posScale, 273 * posScale), maxQueue);
	    Mote mote8  = new Mote(8 , battery, load, new Position(392 * posScale, 478 * posScale), maxQueue);
	    Mote mote9  = new Mote(9 , battery, load, new Position(540 * posScale, 479 * posScale), maxQueue);
	    Mote mote10 = new Mote(10, battery, load, new Position(694 * posScale, 356 * posScale), maxQueue);
	    Mote mote11 = new Mote(11, battery, load, new Position(234 * posScale, 232 * posScale), maxQueue);
	    Mote mote12 = new Mote(12, battery, load, new Position(221 * posScale, 322 * posScale), maxQueue);
	    Mote mote13 = new Mote(13, battery, load, new Position(142 * posScale, 170 * posScale), maxQueue);
	    Mote mote14 = new Mote(14, battery, load, new Position(139 * posScale, 293 * posScale), maxQueue);
	    Mote mote15 = new Mote(15, battery, load, new Position(128 * posScale, 344 * posScale), maxQueue);

	    Mote[] allMotes = new Mote[]{mote2, mote3, mote4, mote5, mote6, mote7, mote8, mote9, mote10, mote11, mote12, mote13, mote14, mote15};
	    simul.addMotes(allMotes);

	    // Gateway
	    Gateway gateway = new Gateway(GATEWAY_ID, new Position(482 * posScale, 360 * posScale));
	    gateway.setView(allMotes);
	    simul.addGateways(gateway);

	    // Links
	    int power = 15;
	    int distribution = 100;
	    mote2. addLinkTo(mote4,   gateway, power, distribution);
	    mote3. addLinkTo(gateway, gateway, power, distribution);
	    mote4. addLinkTo(gateway, gateway, power, distribution);
	    mote5. addLinkTo(mote9,   gateway, power, distribution);
	    mote6. addLinkTo(mote4,   gateway, power, distribution);
	    mote7. addLinkTo(mote2,   gateway, power, distribution);
	    mote7. addLinkTo(mote3,   gateway, power, distribution);
	    mote8. addLinkTo(gateway, gateway, power, distribution);
	    mote9. addLinkTo(gateway, gateway, power, distribution);
	    mote10.addLinkTo(mote6,   gateway, power, distribution);
	    mote10.addLinkTo(mote5,   gateway, power, distribution);
	    mote11.addLinkTo(mote7,   gateway, power, distribution);
	    mote12.addLinkTo(mote7,   gateway, power, distribution);
	    mote12.addLinkTo(mote3,   gateway, power, distribution);
	    mote13.addLinkTo(mote11,  gateway, power, distribution);
	    mote14.addLinkTo(mote12,  gateway, power, distribution);
	    mote15.addLinkTo(mote12,  gateway, power, distribution);

	    // Order
	    simul.setTurnOrder(8, 10, 13, 14, 15, 5, 6, 11, 12, 9, 7, 2, 3, 4);

	    // Profiles
	    mote2 .setActivationProbability(new Constant<>(0.85));
	    mote8 .setActivationProbability(new Constant<>(0.85));
	    mote10.setActivationProbability(new FileProfile("deltaiot/scenario_data/PIR1.txt", 1.0));
	    mote13.setActivationProbability(new FileProfile("deltaiot/scenario_data/PIR2.txt", 1.0));
	    mote14.setActivationProbability(new Constant<>(0.85));

	    mote12.getLinkTo(mote3).setInterference(new FileProfile("deltaiot/scenario_data/SNR2.txt", 0.0));

	    // SNR
	    mote2 .getLinkTo(mote4  ).setSnrEquation(new SNREquation( 0.0473684210526, -5.29473684211));
	    mote3 .getLinkTo(gateway).setSnrEquation(new SNREquation( 0.0280701754386,  4.25263157895));
	    mote4 .getLinkTo(gateway).setSnrEquation(new SNREquation( 0.119298245614 , -1.49473684211));
	    mote5 .getLinkTo(mote9  ).setSnrEquation(new SNREquation(-0.019298245614 ,  4.8));
	    mote6 .getLinkTo(mote4  ).setSnrEquation(new SNREquation( 0.0175438596491, -3.84210526316));
	    mote7 .getLinkTo(mote3  ).setSnrEquation(new SNREquation( 0.168421052632 ,  2.30526315789));
	    mote7 .getLinkTo(mote2  ).setSnrEquation(new SNREquation(-0.0157894736842,  3.77894736842));
	    mote8 .getLinkTo(gateway).setSnrEquation(new SNREquation( 0.00350877192982, 0.45263157895));
	    mote9 .getLinkTo(gateway).setSnrEquation(new SNREquation( 0.0701754385965,  2.89473684211));
	    mote10.getLinkTo(mote6 ).setSnrEquation(new SNREquation( 3.51139336547e-16,-2.21052631579));
	    mote10.getLinkTo(mote5 ).setSnrEquation(new SNREquation( 0.250877192982 , -1.75789473684));
	    mote11.getLinkTo(mote7 ).setSnrEquation(new SNREquation( 0.380701754386 , -2.12631578947));
	    mote12.getLinkTo(mote7 ).setSnrEquation(new SNREquation( 0.317543859649 ,  2.95789473684));
	    mote12.getLinkTo(mote3 ).setSnrEquation(new SNREquation(-0.0157894736842, -3.77894736842));
	    mote13.getLinkTo(mote11).setSnrEquation(new SNREquation(-0.0210526315789, -2.81052631579));
	    mote14.getLinkTo(mote12).setSnrEquation(new SNREquation( 0.0333333333333,  2.58947368421));
	    mote15.getLinkTo(mote12).setSnrEquation(new SNREquation( 0.0438596491228,  1.31578947368));

	    // Interference globale
	    simul.getRunInfo().setGlobalInterference(new DoubleRange(-5.0, 5.0));

	    return simul;
	}

	public void run() {
	    try {
		    System.out.println("--START--");
		    
	    	// Create socket
			ServerSocket serverSocket = new ServerSocket(PORT);
		    System.out.println("Waiting to connect...");
			Socket socket = serverSocket.accept();
			BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintStream output = new PrintStream(socket.getOutputStream());
			System.out.println("Connected.");
			
			// Create thread that listens for messages from the client
		    new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String fromFeedbackLoop = null;
						while ((fromFeedbackLoop = input.readLine()) != null) {
							applyNewSettings(fromFeedbackLoop);
						}
					} catch (Exception e) {
						e.printStackTrace(System.err);
					}
				}
			}).start();
		    
		    // Do logic
		    System.out.println("--SIMULATION--");
		    List<String> printsBackup = new ArrayList<>();
		    for (int i = 0; i < 96; ++i) {
				System.out.println("Run... " + i);
		    	
		    	// Do emulation
		    	simul.doSingleRun();
		    	String print = simul.getGatewayWithId(GATEWAY_ID).toString();
		    	printsBackup.add(print);
		    	System.out.println(print);
		    	
				
		    	// Send info to client
		    	String infoToSend = createInfoToSend();
		    	System.out.println("To FeedbackLoop: " + infoToSend);
		    	output.println(infoToSend);
				
				// Wait for client response
				lock.lock();
				try {
					adaptationCompleted.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					lock.unlock();
				}
			}
		    
		    // Report
		    System.out.println("--FINAL REPORT--");
		    for(String print: printsBackup) {
		    	System.out.println(print);
		    }
		    
		    // Cleanup
		    input.close();
		    output.close();
		    socket.close();
		    serverSocket.close();
        }
        catch (IOException e) {
           System.out.println(e);
        }
	}
	
	
	private String createInfoToSend() {
		deltaiot.DeltaIoT deltaIoT = new deltaiot.DeltaIoT();
		
		Gateway gw = simul.getGatewayWithId(GATEWAY_ID);
		deltaIoT.setPacketLoss(gw.calculatePacketLoss());
		deltaIoT.setPower(gw.getPowerConsumed());
		
		HashMap<Integer, deltaiot.services.Mote> afMotes = new HashMap<>();
		for (Mote mote: gw.getView()) {
			int moteid = mote.getId();
			deltaiot.services.Mote afMote = getAfMote(mote, simul);
			afMotes.put(moteid, afMote);
		}
		deltaIoT.Motes = afMotes;
		
		return deltaIoT.toJson();
	}
	
	public static deltaiot.services.Mote getAfMote(Mote mote, Simulator simul){
		int moteid = mote.getId();
		int load = mote.getLoad();
		double battery = mote.getBatteryRemaining();
		int parents = mote.getLinks().size();
		int dataProbability = (int)Math.round(mote.getActivationProbability().get(simul.getRunInfo().getRunNumber()) * 100);
		
		List<deltaiot.services.Link> afLinks = new LinkedList<>();
		for(Link link: mote.getLinks()) {
			double latency = 0; //unused
			int power = link.getPowerNumber();
			int packetLoss = link.calculatePacketLoss(simul.getRunInfo());
			int source = link.getFrom().getId();
			int dest = link.getTo().getId();
			double sNR = link.getSnrEquation().getSNR(link.getPowerNumber());
			int distribution = link.getDistribution();
			int sF = link.getSfTimeNumber();
			
			deltaiot.services.Link afLink = new deltaiot.services.Link(latency, power, packetLoss, source, dest, sNR, distribution, sF);
			afLinks.add(afLink);
		}
		
		deltaiot.services.Mote afMote = new deltaiot.services.Mote(moteid, load, battery, parents, dataProbability, afLinks);
		return afMote;
	}
	
	public void applyNewSettings(String msgSettings) {
		System.out.println("Message Received from Client:" + msgSettings);
		if (msgSettings.equalsIgnoreCase("NoAdaptationRequired")
				|| msgSettings.equalsIgnoreCase("AdaptationCompleted")) {
			lock.lock();
			try {

				synchronized (adaptationCompleted) {
					adaptationCompleted.signal();
				}
			} finally {
				lock.unlock();
			}
		} else {
			Gson gson = new Gson();
			deltaiot.services.Mote afMote = gson.fromJson(msgSettings, new TypeToken<deltaiot.services.Mote>() {}.getType());
			for (deltaiot.services.Link afLink : afMote.getLinks()) {
				
				Mote source = simul.getMoteWithId(afMote.getMoteid());
				Node destination = simul.getNodeWithId(afLink.getDest());
				Link actualLink = source.getLinkTo(destination);
				
				if (actualLink.getDistribution() != afLink.getDistribution()) {
					actualLink.setDistribution(afLink.getDistribution());
					System.out.println("Link distribution adapted: " + actualLink);
				}
				if (actualLink.getPowerNumber() != afLink.getPower()) {
					actualLink.setPowerNumber(afLink.getPower());
					System.out.println("Link power adapted:        " + actualLink);
				}
			}
		}
	}
	
}
