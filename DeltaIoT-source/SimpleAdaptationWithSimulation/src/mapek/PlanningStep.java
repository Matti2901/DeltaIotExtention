package mapek;

import java.util.ArrayList;
import java.util.List;

import deltaiot.services.Link;

public class PlanningStep {
    Step step;
    Link link;       
    int value;
    int moteId;
    public int getMoteId() {
		return moteId;
	}

	public List<Link> getOriginalLink() {
		return originalLink;
	}

	List<Link> originalLink = new ArrayList<>();

    private Link oldLink;    // per REDIRECT_TRAFFIC
    private Link newLink;

    public PlanningStep(Step step, Link link, int value) {
        this.step = step;
        this.link = link;
        this.value = value;
    }

    // REDIRECT TRAFFIC
    public PlanningStep(Step step, Link oldLink, Link newLink) {
        this.step = step;
        this.oldLink = oldLink;
        this.newLink = newLink;
    }
    //RECOVER_CONGESTION
    public PlanningStep(Step step,int moteId, List<Link> originalLinks) {
		this.originalLink = originalLinks;
		this.step = step;
		this.moteId = moteId;
		
		
	}

	public Step getStep() {
        return step;
    }

    public Link getLink() {
        return link;
    }

    public int getValue() {
        return value;
    }

    public Link getOldLink() {
        return oldLink;
    }

    public Link getNewLink() {
        return newLink;
    }

  

    @Override
    public String toString() {
        if (step == Step.REDIRECT_TRAFFIC) {
            return String.format("%s: %d → %d  →  %d", step, oldLink.getSource(), oldLink.getDest(), newLink.getDest());
        } else {
            return String.format("%s, %s, %d", step, link, value);
        }
    }
}

