package deltaiot.services;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.annotations.Expose;

public class Mote {
	
	@Expose private int MoteID;
	@Expose private int Load;
	@Expose private double BatteryRemaining;
	@Expose private double BatteryConsumed;
    @Expose private int NumParents;
    @Expose private int DataProbability;
    @Expose private List<Link> Links = new LinkedList<Link>();

    public Mote() { }
    
    
    public Mote(Integer moteid, int load, Double battery,Integer parents, int dataProbability, List<Link> links) {
		this.MoteID = moteid;
		this.Load = load;
		this.BatteryRemaining = battery;
		this.NumParents = parents;
		this.DataProbability = dataProbability;
		this.Links = links;
	}

	public void setMoteid(Integer moteid) {
		this.MoteID = moteid;
	}
    
    public Integer getMoteid() {
		return MoteID;
	}
    
    /**
     * 
     * @return
     *     The load
     */
    public int getLoad() {
        return Load;
    }

    /**
     * 
     * @param load
     *     The load
     */
    public void setLoad(int load) {
        this.Load = load;
    }

    /**
     * 
     * @return
     *     The statistics
     */
    public List<Link> getLinks() {
       return Links;
    	//return null;
    }

    /**
     * 
     * @param statistics
     *     The statistics
     */
    public void setLinks(List<Link> links) {
       this.Links = links;
    }

    /**
     * 
     * @return
     *     The battery
     */
    public double getBattery() {
        return BatteryRemaining;
    }

    /**
     * 
     * @param battery
     *     The battery
     */
    public void setBattery(double battery) {
        this.BatteryRemaining = battery;
    }

    /**
     * 
     * @return
     *     The parents
     */
    public int getParents() {
        return NumParents;
    }

    /**
     * 
     * @param parents
     *     The parents
     */
    public void setParents(int parents) {
        this.NumParents = parents;
    }
    
    public int getDataProbability() {
		return DataProbability;
	}
    
    public void setDataProbability(int dataProbability) {
		DataProbability = dataProbability;
	}

	public Link getLink(int index){
		return Links.get(index);
	}
	
	public Link getLinkWithDest(int destination){
		for(Link link: Links)
			if (link.getDest() == destination)
				return link;
		
		return null;
	}

	public void addLink(Link link) {
		Links.add(link);
		NumParents++;
	}
    
    @Override
    public String toString() {
    	return String.format("MoteId=%d, Parents=%d, Battery=%f, Load=%d, DataProbability=%d", MoteID, NumParents, BatteryRemaining, Load, DataProbability);
    }
    
    @Override
    public Object clone() {
    	List<Link> linksCopy = new LinkedList<Link>();
    	
    	for(Link link: Links)
    		linksCopy.add((Link) link.clone());
    	return new Mote(MoteID, Load, BatteryRemaining, NumParents, DataProbability,linksCopy);
    }
}
