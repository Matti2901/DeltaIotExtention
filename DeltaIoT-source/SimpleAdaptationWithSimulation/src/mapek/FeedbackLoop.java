package mapek;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.gui.DeltaIoTEmulatorMain;
import deltaiot.services.Link;
import deltaiot.services.LinkSettings;
import deltaiot.services.Mote;

public class FeedbackLoop {

    Probe probe;
    Effector effector;

    // Knowledge
    ArrayList<Mote> motes;
    List<PlanningStep> steps = new LinkedList<>();
    
    // Extension
    // Congestion-specific knowledge
    private Map<Integer, Integer> queueFilling = new HashMap<>();
    private List<Mote> congestedMotes = new ArrayList<>();
    private Map<Integer, List<Integer>> congestionSources = new HashMap<>();
    private Map<Integer, Integer> queueSize = new HashMap<>();
    private List<Integer> moteCongestAdaptedID = new ArrayList<>();
    private double loadFactor = 0.8;
    Map<Integer, List<Link>> originalLinksPerMote = new HashMap<>();
    private List<Integer> moteRecovered = new ArrayList<>();

    public void setProbe(Probe probe) {
        this.probe = probe;
    }

    public void setEffector(Effector effector) {
        this.effector = effector;
    }

    public void start() {
        for (int i = 0; i < DeltaIoTEmulatorMain.runNumber; i++) {
            try {
                monitor();
               
            } catch (Exception e) {
                System.err.println("❌ Error during adaptation:");
                e.printStackTrace();
            }
        }
    }

    void monitor() {
        motes = probe.getAllMotes();
        queueFilling.clear();
        System.out.println("-------------------");
        System.out.println("MONITOR");
        System.out.println();
        for (Mote mote : motes) {
            int id = mote.getMoteid();

            // Get the queue fill for the mote
            int queueFill = probe.getQueueFilling(mote.getMoteid());
            queueFilling.put(id, queueFill);
           
            System.out.println("Queue of mote " + id + " is: " + queueFill);

            // Get the maximum queue size for the mote
            int queueSizeSingle = probe.getSizeOfQueue(id);
            queueSize.put(mote.getMoteid(), queueSizeSingle);
        }

        System.out.println();
        // Perform analysis after monitoring
        analysis();
    }

    void analysis() {
    	System.out.println("-------------------");
    	System.out.println("ANALYSiS");
    	System.out.println();
        // Analyze all link settings and congestion levels
        boolean adaptationRequired = analyzeLinkSettings();
        boolean adaptationCongestion = analyzeQueueMote();
        
        // If any adaptation is required, invoke the planner
        if (adaptationRequired || adaptationCongestion) {
            planning();
        }
        else {
        	System.out.println("✅ No Uncertainties found");

        }
        
    }


    boolean analyzeQueueMote() {
        boolean adaptationNeeded = false;
        congestedMotes.clear();
        congestionSources.clear();
        
        for (Mote mote : motes) {
            int idDest = mote.getMoteid();
            int queueFill = queueFilling.getOrDefault(idDest, 0);
            int maxQueue = queueSize.getOrDefault(idDest, 10);
            int threshold = (int) (maxQueue * loadFactor);

            // 📦 Already adapted motes (moteCongestAdapted)
            /*
            System.out.print("📦 Already adapted motes (moteCongestAdapted): ");
            for (int id : moteCongestAdaptedID) {
                System.out.print(id + " ");
            }
            System.out.println();
            */

            if (queueFill >= threshold && !moteCongestAdaptedID.contains(idDest)) {
                System.out.println("⚠️ Congestion detected on mote " + idDest);
               
                List<Integer> incomingSenders = new ArrayList<>();

                for (Mote otherMote : motes) {
                    int idSrc = otherMote.getMoteid();
                    if (idSrc <= idDest) continue;
                    for (Link link : otherMote.getLinks()) {
                        if (link.getDest() == idDest) {
                            incomingSenders.add(idSrc);
                            break;
                        }
                    }
                }

                System.out.println("⤪ Incoming traffic to " + idDest + " from motes: " + incomingSenders);
                System.out.println("-------------------");
                congestedMotes.add(mote);
                congestionSources.put(idDest, incomingSenders);
                adaptationNeeded = true;

            } else {
                if (moteCongestAdaptedID.contains(idDest) && probe.getQueueFilling(mote.getMoteid()) == 0) {
                    System.out.println("⚠️ Mote " + idDest + " is no longer congested");
                    System.out.println("-------------------");
                    moteCongestAdaptedID.remove((Integer) idDest);
                    moteRecovered.add((Integer) idDest);
                    adaptationNeeded = true;
                }
            }
        }
        return adaptationNeeded;
    }

	boolean analyzeLinkSettings() {
		// analyze all links for possible adaptation options
		for (Mote mote : motes) {
			for (Link link : mote.getLinks()) {
				if (link.getSNR() > 0 && link.getPower() > 0 || link.getSNR() < 0 && link.getPower() < 15) {
					return true;
				}
			}
			if (mote.getLinks().size() == 2) {
				if (mote.getLinks().get(0).getPower() != mote.getLinks().get(1).getPower())
					return true;
			}
		}
		return false;
	}

	void planning() {
    	System.out.println("-------------------");
    	System.out.println("PLANNING STRATEGY");
		if (!congestedMotes.isEmpty()) {
			steps.addAll(handleCongestionPlanning());
		}
		if (!moteRecovered.isEmpty()) {
			//System.out.println();
		    //System.out.println("📦 moteRecovered : " + moteRecovered);
		    //System.out.println();
		    
		    System.out.println();
		    steps.addAll(handleRecoverCongestion());
		}

		boolean powerChanging = false;
		Link left, right;
		for (Mote mote : motes) {
			for (Link link : mote.getLinks()) {
				powerChanging = false;
				if (link.getSNR() > 0 && link.getPower() > 0) {
					steps.add(new PlanningStep(Step.CHANGE_POWER, link, link.getPower() - 1));
					powerChanging = true;
				} else if (link.getSNR() < 0 && link.getPower() < 15) {
					steps.add(new PlanningStep(Step.CHANGE_POWER, link, link.getPower() + 1));
					powerChanging = true;
				}
			}
			if (mote.getLinks().size() == 2 && !powerChanging) {
				left = mote.getLinks().get(0);
				right = mote.getLinks().get(1);
				if (left.getPower() != right.getPower()) {
					if (left.getDistribution() == 100 && right.getDistribution() == 100) {
						left.setDistribution(50);
						right.setDistribution(50);
					}
					if (left.getPower() > right.getPower() && left.getDistribution() < 100) {
						steps.add(new PlanningStep(Step.CHANGE_DIST, left, left.getDistribution() + 10));
						steps.add(new PlanningStep(Step.CHANGE_DIST, right, right.getDistribution() - 10));
					} else if (right.getDistribution() < 100) {
						steps.add(new PlanningStep(Step.CHANGE_DIST, right, right.getDistribution() + 10));
						steps.add(new PlanningStep(Step.CHANGE_DIST, left, left.getDistribution() - 10));
					}
				}
			}
		}

		if (!steps.isEmpty()) {
			execution();
		}
	}
	private List<PlanningStep> handleRecoverCongestion() {
	    List<PlanningStep> congestionSteps = new ArrayList<>();

	    if (!moteRecovered.isEmpty()) {
	        //System.out.println("📦 moteRecovered contains: " + moteRecovered);
	        // Temporary list to mark which motes must be removed
	        List<Integer> recoveredHandled = new ArrayList<>();
	     
	        for (Map.Entry<Integer, List<Link>> entry : originalLinksPerMote.entrySet()) {
	            int sourceMoteId = entry.getKey();
	            List<Link> originalLinks = entry.getValue();

	            System.out.println("Analyzing backup of mote " + sourceMoteId);
	            for (Link l : originalLinks) {
	                System.out.println("   🔗 link from " + l.getSource() + " to " + l.getDest());
	            }

	            boolean requiresRecovery = originalLinks.stream()
	                .anyMatch(link -> {
	                    boolean match = moteRecovered.contains(link.getDest()) && !isMoteCongested(link.getDest());
	                    if (match) {
	                        System.out.println("   ✅ Link to " + link.getDest() + " is in moteRecovered and free");
	                    }
	                    return match;
	                });

	            if (requiresRecovery) {
	                Mote sourceMote = findMote(sourceMoteId);
	                if (sourceMote != null) {
	                	System.out.println();
	                	System.out.println("Create adaptation step");
	                    congestionSteps.add(new PlanningStep(Step.RECOVER_CONGESTION, sourceMoteId, originalLinks));
	                    System.out.println("♻️ RECOVER_CONGESTION: planning step created for mote " + sourceMoteId);
	                    System.out.println();
	                    // Accumulate the motes to be removed after
	                    for (Link l : originalLinks) {
	                        int dest = l.getDest();
	                        if (moteRecovered.contains(dest) && !isMoteCongested(dest)) {
	                            recoveredHandled.add(dest);
	                        }
	                    }

	                } else {
	                    System.out.println("⚠️ Mote " + sourceMoteId + " not found in the network");
	                }
	            } else {
	                System.out.println("❌ No link to restore for mote " + sourceMoteId);
	            }
	        }

	        // ✅ Remove all recovered motes at the end
	        for (Integer id : recoveredHandled) {
	            moteRecovered.remove(id);
	            System.out.println("🧹 Removed mote " + id + " from moteRecovered after recovery");
	        }

	    } else {
	        System.out.println("⚠️ moteRecovered is empty");
	    }
	    return congestionSteps;
	}


	private List<PlanningStep> handleCongestionPlanning() {
	    List<PlanningStep> congestionSteps = new ArrayList<>();
	    System.out.println();
	    DebuggerMethod.debugPrintLinks(motes,"Before the adaptation");
	    Map<Integer, Integer> virtualFilling = new HashMap<>(queueFilling);
	    final int ESTIMATED_PACKET = 1;

	    for (Mote congestedMote : congestedMotes) {
	        int congestedId = congestedMote.getMoteid();

	        if (moteCongestAdaptedID.contains(congestedId)) {
	            continue; // Skip already handled
	        }

	        List<Integer> sources = congestionSources.get(congestedId);

	        for (Integer sourceId : sources) {
	            Mote sourceMote = findMote(sourceId);
	            if (sourceMote == null) continue;

	            Link oldLink = sourceMote.getLinkWithDest(congestedId);
	            if (oldLink == null || oldLink.getDistribution() == 0) {
	                continue; // Skip if already redirected
	            }

	            // Backup if not already done
	            if (!originalLinksPerMote.containsKey(sourceId)) {
	                List<Link> backupLinks = new ArrayList<>();
	                for (Link link : sourceMote.getLinks()) {
	                    backupLinks.add(cloneLink(link));
	                }
	                originalLinksPerMote.put(sourceId, backupLinks);
	                System.out.println("Create Backup Link:");
	    	        System.out.println();
	                System.out.println("📝 Backup of links for mote " + sourceId);
	                for (Link l : backupLinks) {
	                    System.out.println("   🔗 Saved link from " + l.getSource() + " to " + l.getDest());
	                }
	            }

	            Link bestNewLink = null;
	            int minQueueFill = Integer.MAX_VALUE;
	            int oldDistribution = oldLink.getDistribution();
	            int chosenDest = -1;

	            for (Link candidate : sourceMote.getLinks()) {
	                int destId = candidate.getDest();
	                if (destId == congestedId || isMoteCongested(destId)) continue;

	                int fill = virtualFilling.getOrDefault(destId, Integer.MAX_VALUE);
	                int size = queueSize.getOrDefault(destId, 1);
	                double loadFactorStimed = (double)(fill + ESTIMATED_PACKET) / size;

	                if (fill < minQueueFill && loadFactorStimed < loadFactor) {
	                    minQueueFill = fill;
	                    chosenDest = destId;

	                    int existingDistribution = 0;
	                    Link existingLink = sourceMote.getLinkWithDest(destId);
	                    if (existingLink != null)
	                        existingDistribution = existingLink.getDistribution();

	                    int totalDistribution = oldDistribution + existingDistribution;

	                    bestNewLink = new Link(
	                        oldLink.getLatency(), oldLink.getPower(), oldLink.getPacketLoss(),
	                        oldLink.getSource(), destId, oldLink.getSNR(),
	                        totalDistribution, oldLink.getSF()
	                    );
	                    /*
	                    System.out.println("🔍 Mote " + sourceMote.getMoteid() + " has a link to " + chosenDest);
	                    System.out.println("   ↪️ Old distribution (to " + congestedId + "): " + oldDistribution);
	                    System.out.println("   ↪️ Existing distribution to new dest (" + chosenDest + "): " + existingDistribution);
	                    System.out.println("   🔄 Total new distribution: " + totalDistribution);
	                    */
	                }
	            }

	            if (bestNewLink != null) {
	                int destId = bestNewLink.getDest();
	                virtualFilling.put(destId, virtualFilling.getOrDefault(destId, 0) + ESTIMATED_PACKET);

	                // System.out.println("✅ RedirectTraffic step: " + sourceId + " → " + destId);
	                congestionSteps.add(new PlanningStep(Step.REDIRECT_TRAFFIC, oldLink, bestNewLink));
	            } else {
	                System.out.println("⚠️ No alternative path found for mote " + sourceId);
	            }
	        }
	    }

	    return congestionSteps;
	}








	private boolean isMoteCongested(int moteId) {
	    return congestedMotes.stream().anyMatch(m -> m.getMoteid() == moteId);
	}

	void execution() {
		System.out.println("-------------------");
		System.out.println("EXECUTE STRATEGY");
		boolean addMote;
		if ((steps.stream().anyMatch(s -> s.getStep() == Step.REDIRECT_TRAFFIC) ||
				steps.stream().anyMatch(s -> s.getStep() == Step.RECOVER_CONGESTION))) {
		    executionQueueCongestion();
		    System.out.println();
		    DebuggerMethod.debugPrintLinks(motes,"After the adaptation ");
		}
		List<Mote> motesEffected = new LinkedList<Mote>();
		for (Mote mote : motes) {
			addMote = false;
			for (PlanningStep step : steps) {
				 if (step.getStep() == Step.REDIRECT_TRAFFIC || step.getStep() == Step.RECOVER_CONGESTION) continue;
				if (step.link.getSource() == mote.getMoteid()) {
					addMote = true;
					if (step.step == Step.CHANGE_POWER) {
						mote.getLinkWithDest(step.link.getDest()).setPower(step.value);
					} else if (step.step == Step.CHANGE_DIST) {
						mote.getLinkWithDest(step.link.getDest()).setDistribution(step.value);
					}
				}
			}
			motesEffected.add(mote);
		}
		List<LinkSettings> newSettings;
		
		for(Mote mote: motesEffected){
			newSettings = new LinkedList<LinkSettings>();
			for(Link link: mote.getLinks()){
				newSettings.add(new LinkSettings(mote.getMoteid(), link.getDest(), link.getPower(), link.getDistribution(), link.getSF()));
			}
			effector.setMoteSettings(mote.getMoteid(), newSettings);
		}
		
		steps.clear();
	}
	void executionQueueCongestion() {

	    List<Mote> motesEffected = new LinkedList<>();
	   
	    for (Mote mote : motes) {
	        boolean addMote = false;

	        for (PlanningStep step : steps) {
	            if (step.getStep() == Step.REDIRECT_TRAFFIC) {
	           
	            	
	                if (step.getNewLink().getSource() != mote.getMoteid()) continue;
	                System.out.println();
	                System.out.println("REDIRECT_TRAFFIC");
	                addMote = true;
	                int oldDest = step.getOldLink().getDest();
	                int newDest = step.getNewLink().getDest();
	                boolean updated = false;

	                for (Link link : mote.getLinks()) {
	                    if (link.getDest() == oldDest) {
	                        link.setDistribution(0);  // remove distribution toward congested node
	                    } else if (link.getDest() == newDest) {
	                        // add to the existing distribution if already present
	                        link.setDistribution(step.getNewLink().getDistribution());
	                        updated = true;
	                    }
	                }

	                if (!updated) {
	                    // if there wasn't already a link to newDest, add it
	                    mote.getLinks().add(step.getNewLink());
	                }

	                System.out.println("🔄 Applied REDIRECT_TRAFFIC: mote " + mote.getMoteid()
	                    + " redirected to " + newDest);
	                int congestedId = step.getOldLink().getDest();
	                if (!moteCongestAdaptedID.contains(congestedId)) {
	                    moteCongestAdaptedID.add(congestedId);
	                }
	            }

	            if (step.getStep() == Step.RECOVER_CONGESTION) {
	                int recoveringMoteId = step.getMoteId();
	                if (mote.getMoteid() != recoveringMoteId) continue;
	                System.out.println();
	                System.out.println("RECOVER CONGESTION");
	                // Replace all current links with the original ones from backup
	                mote.getLinks().clear();
	                mote.getLinks().addAll(step.getOriginalLink());

	                System.out.println("♻️ RECOVER_CONGESTION applied on mote " + recoveringMoteId);

	                addMote = true; 
	                // Ensure settings will be updated via effector
	            }
	        }

	        if (addMote) {
	            motesEffected.add(mote);
	        }
	    }
            

	    for (Mote mote : motesEffected) {
	        List<LinkSettings> newSettings = new LinkedList<>();
	        for (Link link : mote.getLinks()) {
	            newSettings.add(new LinkSettings(
	                mote.getMoteid(),
	                link.getDest(),
	                link.getPower(),
	                link.getDistribution(),
	                link.getSF()
	            ));
	        }
	        effector.setMoteSettings(mote.getMoteid(), newSettings);
	    }
	  
	}

	private Link cloneLink(Link l) { 
		return new Link(l.getLatency(), l.getPower(), l.getPacketLoss(), l.getSource(), l.getDest(), l.getSNR(), l.getDistribution(), l.getSF());
		}
	


	Mote findMote(int id) {
		for (Mote mote : motes) {
			if (mote.getMoteid() == id) return mote;
		}
		return null;
	}
}