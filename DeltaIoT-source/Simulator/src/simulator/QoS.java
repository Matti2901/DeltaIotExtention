package simulator;

public class QoS {
	private double PacketLoss;
	private double PowerConsumption;
	private String period;
	
	public double getPacketLoss() {
		return PacketLoss;
	}
	
	public double getEnergyConsumption() {
		return PowerConsumption;
	}
	
	public String getPeriod() {
		return period;
	}
	
	public void setPacketLoss(double packetLoss) {
		this.PacketLoss = packetLoss;
	}
	
	public void setEnergyConsumption(double energyConsumption) {
		this.PowerConsumption = energyConsumption;
	}
	
	public void setPeriod(String period) {
		this.period = period;
	}
	
	@Override
	public String toString() {
		return String.format("%s, %f, %f", period, PacketLoss, PowerConsumption);
	}
}
