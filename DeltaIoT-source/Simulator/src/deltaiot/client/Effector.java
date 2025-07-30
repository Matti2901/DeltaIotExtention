package deltaiot.client;

import java.util.List;

import deltaiot.services.LinkSettings;

public interface Effector {
//	public void setLinkSF(int src, int dest, int sf);
//	
//	public void setLinkPower(int src, int dest, int power);
//	
//	public void setLinkDistributionFactor(int src, int dest, int distributionFactor);
	
	public void setMoteSettings(int moteId, List<LinkSettings> linkSettings);
	
	public void setDefaultConfiguration();
}
