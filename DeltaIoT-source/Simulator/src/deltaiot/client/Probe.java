package deltaiot.client;

import java.util.ArrayList;

import deltaiot.services.Mote;
import simulator.QoS;

public interface Probe {
	
	public ArrayList<Mote> getAllMotes();
	
	public double getMoteEnergyLevel(int moteId);
	
	public double getMoteTrafficLoad(int moteId);
	
	public int getLinkPowerSetting(int src, int dest);
	
	public int getLinkSpreadingFactor(int src, int dest);
	
	public double getLinkSignalNoise(int src, int dest);
	
	public double getLinkDistributionFactor(int src, int dest);
	
	public ArrayList<QoS> getNetworkQoS(int period);
	
	//Mote for manage Uncertain Local congestion in mote
	public int getQueueFilling(int moteId);
	public int getSizeOfQueue(int moteId);
}
