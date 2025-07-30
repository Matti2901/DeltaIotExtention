package domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Gateway extends Node {

	private List<Mote> view;
	private List<Packet> packetStore = new ArrayList<>();
	
	private int expectedPacketCount = 0;
	private double powerConsumed = 0;
	
	public Gateway(int id) {
		this(id, null);
	}
	
	public Gateway(int id, Position position) {
		super(id, position);
	}
	
	public void setView(Mote...motes) {
		this.view = Arrays.asList(motes);
	}
	
	public List<Mote> getView() {
		return Collections.unmodifiableList(view);
	}

	@Override
	void receivePacket(Packet packet) {
		packetStore.add(packet);
	}

	void addPacketToExpect() {
		expectedPacketCount += 1;
	}
	
	public void resetPacketStoreAndExpectedPacketCount() {
		packetStore.clear();
		expectedPacketCount = 0;
	}
	
	void reportPowerConsumed(double amount) {
		powerConsumed += amount;
	}
	
	public void resetPowerConsumed() {
		powerConsumed = 0;
	}
	
	public double calculatePacketLoss() {
		long packetStoreSizeWithoutDuplicates = packetStore.stream().distinct().count();
		return 1 - (double)packetStoreSizeWithoutDuplicates / (double)expectedPacketCount;
	}
	
	public double getPowerConsumed() {
		return powerConsumed;
	}
	
	// Debugging helpers
	public void printInfoPacketLoss() {
		long packetStoreSizeWithoutDuplicates = packetStore.stream().distinct().count();
		double packetloss = 1 - (double)packetStoreSizeWithoutDuplicates / (double)expectedPacketCount;
		
		System.out.println("GW" + getId() + " packetloss: 1 - " 
				+ packetStoreSizeWithoutDuplicates + "/" + expectedPacketCount + " = " + packetloss);
	}
	
	public void printInfoPacketStore() {
		System.out.println("GW" + getId() + " PacketStore: ");
		for (Packet packet: packetStore) {
			System.out.println("\tnumber " + packet.getNumber() + " from " + packet.getSource().getId() + " to " + packet.getDestination().getId());
		}
	}

	@Override
	public String toString() {
		double packetloss = calculatePacketLoss();
		return "Gateway " + String.format("%2d", getId()) 
			+ " [storedPackets=" + packetStore.size()
			+ ", expectedPackets=" + expectedPacketCount
			+ ", packetloss=" + String.format("%2d", Math.round(packetloss * 100)) + ", powerConsumed=" + String.format("%.2f", powerConsumed) + "]";
	}
	
}
