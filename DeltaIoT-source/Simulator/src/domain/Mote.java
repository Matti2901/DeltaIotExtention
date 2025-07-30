package domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Mote extends Node {

	private double batteryCapacity;
	private double batteryRemaining;
	
	private int load; // = number of packets to send in a turn
	private Profile<Double> activationProbability = new Constant<>(1.0); // = chance this mote will send packets
	
	private List<Link> links = new ArrayList<>();
	private List<Packet> packetQueue = new ArrayList<>();
	private int lastPacketNumber = 0;
	private final int MAXQUEUESIZE;
	
	public Mote(int id, double batteryCapacity, int load, Position position,int maxQueue) {
		super(id, position);
		this.batteryCapacity = batteryCapacity;
		this.batteryRemaining = batteryCapacity;
		this.load = load;
		this.MAXQUEUESIZE = maxQueue;
	}

	public Mote(int id, double batteryCapacity, int load,int maxQueue) {
		this(id, batteryCapacity, load, null,maxQueue);
	}
		
	public int getQueueSize() {
		return MAXQUEUESIZE;
	}
	public double getBatteryCapacity() {
		return batteryCapacity;
	}
	public double getBatteryRemaining() {
		return batteryRemaining;
	}
	public void setBatteryRemaining(double batteryRemaining) {
		this.batteryRemaining = batteryRemaining;
	}
	public int getLoad() {
		return load;
	}
	public Profile<Double> getActivationProbability() {
		return activationProbability;
	}
	public void setActivationProbability(Profile<Double> actProbProf) {
		this.activationProbability = actProbProf;
	}
	
	public void addLinkTo(Node to, Gateway direction, int power, int distribution) {
		Link link = new Link(this, to, direction, power, distribution);
		links.add(link);
	}
	
	public Link getLinkTo(Node to) {
		for(Link link: links) {
			if(link.getTo() == to) {
				return link;
			}
		}
		return null;
	}
	
	public List<Link> getLinks() {
		return Collections.unmodifiableList(links);
	}

	/**
	 * Handles the turn of this mote for the emulation
	 * 
	 * 1. It might send some packets of its own (based on load and activation probability)
	 * 		a. It will increment the expected packets for the chosen destination gateway (to calculate packet loss)
	 * 2. It will send any packets it has queued from other motes
	 * 3. It will reduce its battery based on the number of packets sent
	 */
	public void handleTurn(RunInfo runInfo) {
	    List<Packet> myPackets = new ArrayList<>();
	    boolean shouldSend = Math.random() < activationProbability.get(runInfo.getRunNumber());
	    if (shouldSend) {
	        for (int i = 0; i < load; i++) {
	            if (packetQueue.size() < MAXQUEUESIZE) {
	                lastPacketNumber++;
	                Packet p = new Packet(this, null, lastPacketNumber);
	                myPackets.add(p);
	                packetQueue.add(p);
	            } else {
	                break;
	            }
	        }
	    }

	    int totalDistribution = links.stream().mapToInt(Link::getDistribution).sum();
	    for (Packet packet : myPackets) {
	        int rand = (int) Math.round(Math.random() * totalDistribution);
	        int countDistribution = 0;
	        for (Link link : links) {
	            countDistribution += link.getDistribution();
	            if (countDistribution >= rand) {
	                packet.setDestination(link.getDirection());
	                break;
	            }
	        }
	        packet.getDestination().addPacketToExpect();
	    }

	    List<Packet> sentPackets = new ArrayList<>();
	    Map<Link, Integer> sentPerLink = new HashMap<>();

	    for (Packet packet : packetQueue) {
	        List<Link> possibleLinks = links.stream()
	            .filter(link -> link.getDirection() == packet.getDestination())
	            .collect(Collectors.toList());

	        if (possibleLinks.isEmpty()) continue;

	        int totalDist = possibleLinks.stream().mapToInt(Link::getDistribution).sum();

	        if (totalDist > 100) {
	            for (Link link : possibleLinks) {
	                sendPacketOver(link, packet, runInfo);
	                sentPackets.add(packet);
	                sentPerLink.merge(link, 1, Integer::sum);
	            }
	        } else {
	            int rand = (int) Math.round(Math.random() * totalDist);
	            int countDist = 0;
	            for (Link link : possibleLinks) {
	                countDist += link.getDistribution();
	                if (countDist >= rand) {
	                    sendPacketOver(link, packet, runInfo);
	                    sentPackets.add(packet);
	                    sentPerLink.merge(link, 1, Integer::sum);
	                    break;
	                }
	            }
	        }
	    }

	    System.out.println("-------------------");
		System.out.println("MOTE OPERATION:");
		System.out.println();
	    System.out.println("Mote " + getId() + " sent " + sentPackets.size() + " packets:");
	    for (Map.Entry<Link, Integer> entry : sentPerLink.entrySet()) {
	        Link l = entry.getKey();
	        int count = entry.getValue();
	        int fromId = l.getFrom().getId();
	        int toId = l.getTo().getId();
	        String warning = (l.getDistribution() == 0) ? "⚠️ LINK WITH DIST = 0!" : "";
	        System.out.println("   ↪️ " + count + " packets to " + toId + " (dist=" + l.getDistribution() + "%) " + warning);
	    }

	    packetQueue.removeAll(sentPackets);
	}


	
	void sendPacketOver(Link link, Packet packet, RunInfo runInfo) {
		assert links.contains(link);
		
		// Send packet over a link
		link.sendPacket(packet, runInfo);

		// Subtract battery usage
		double batteryUsage = link.getSfTime() * (link.getPowerConsumptionRate() / DomainConstants.coulomb);
		batteryRemaining -= batteryUsage;
		packet.getDestination().reportPowerConsumed(batteryUsage);
	}

	@Override
	void receivePacket(Packet packet) {
	    if (packetQueue.size() < MAXQUEUESIZE) {
	        packetQueue.add(packet);
	        // Logging removed (was printing packet receipt and queue size)
	    } else {
	        // Logging removed (was printing packet drop due to full queue)
	    }

	    // Battery logic
	    double batteryUsage = DomainConstants.receptionTime *
	                          (DomainConstants.receptionCost / DomainConstants.coulomb);
	    batteryRemaining -= batteryUsage;
	    packet.getDestination().reportPowerConsumed(batteryUsage);
	}


	@Override
	public String toString() {
		return "Mote " + String.format("%2d", getId()) + " [battery=" + String.format("%5.1f", batteryRemaining) + "/" + String.format("%5.1f", batteryCapacity) 
		+ ", load=" + load + ", queue=" + packetQueue.size() + "]";
	}
	
	// Added to manage Uncertain Local congestion in mote
	public List<Packet> getPacketQueue() {
	    return packetQueue;
	}
	
}
