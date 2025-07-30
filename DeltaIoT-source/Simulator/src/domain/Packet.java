package domain;

public class Packet {
	
	private Mote source;
	private int number;
	
	private Gateway destination;
	
	public Packet(Mote source, Gateway destination, int number) {
		this.source = source;
		this.destination = destination;
		this.number = number;
	}

	public Mote getSource() {
		return source;
	}
	
	public Gateway getDestination() {
		return destination;
	}
	void setDestination(Gateway destination) {
		this.destination = destination;
	}
	
	public int getNumber() {
		return number;
	}
	
	public Packet clone() {
		return new Packet(source, destination, number);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + number;
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Packet other = (Packet) obj;
		if (number != other.number)
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}
}
