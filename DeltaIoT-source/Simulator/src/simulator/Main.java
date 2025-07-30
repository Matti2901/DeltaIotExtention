package simulator;

import deltaiot.DeltaIoTSimulator;
import domain.Gateway;

public class Main {
	
	public static void main(String [ ] args) {
		run();
	}
	
	public static void run() {
		Simulator simul = DeltaIoTSimulator.createSimulatorForDeltaIoT("Other");
		
		// Do logic
	    for (int i = 0; i < 96; ++i) {
	    	simul.doSingleRun();
			//simul.doMultipleRuns(96);
	    	
	    	for(Gateway gateway: simul.getGateways()) {
				System.out.println(gateway);
			}
			/*for(Mote mote: motes) {
				System.out.println(mote);
			}*/
		}
	}
	
}
