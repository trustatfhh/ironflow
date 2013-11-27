package de.fhhannover.inform.trust.ironflow.publisher;

import java.io.IOException;
import java.util.ArrayList;

import javax.ws.rs.client.WebTarget;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Document;

import de.fhhannover.inform.trust.ifmapj.binding.IfmapStrings;
import de.fhhannover.inform.trust.ifmapj.channel.SSRC;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapErrorResult;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapException;
import de.fhhannover.inform.trust.ifmapj.identifier.Device;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifiers;
import de.fhhannover.inform.trust.ifmapj.identifier.IpAddress;
import de.fhhannover.inform.trust.ifmapj.identifier.MacAddress;
import de.fhhannover.inform.trust.ifmapj.messages.MetadataLifetime;
import de.fhhannover.inform.trust.ifmapj.messages.PublishDelete;
import de.fhhannover.inform.trust.ifmapj.messages.PublishUpdate;
import de.fhhannover.inform.trust.ifmapj.messages.Requests;
import de.fhhannover.inform.trust.ironflow.Configuration;

/**
 * This class is the Implementation to Request the OpenflowController for all connected devices
 * 
 * 
 * @author Marius Rohde
 *
 */

public class RequestDeviceList extends RequestStrategy {

	@Override
	public void requestWebservice(WebTarget webTarget, SSRC ssrc) {
		
		String jsonString;
		JsonNode rootNode;
		
		ObjectMapper mapper = new ObjectMapper();
		WebTarget resourceWebTarget = webTarget.path("/wm/device/");
		
		ArrayList<MacAddress> macsNew; 
		ArrayList<MacAddress> macsOld;
		ArrayList<IpAddress> ipsNew;
		ArrayList<IpAddress> ipsOld;
		ArrayList<Integer> vlansNew;
		ArrayList<Integer> vlansOld;
		ArrayList<Device> devicesNew;
		ArrayList<Device> devicesOld;
		ArrayList<Integer> portsNew;
		ArrayList<Integer> portsOld;
		
		long expireTime = System.currentTimeMillis() - Configuration.ironflowDeviceExpireTime()*60000;
		
		jsonString = this.getResponse(resourceWebTarget).readEntity(String.class);

		System.out.println(jsonString);
						    	    
		try {
			
			rootNode = mapper.readValue(jsonString, JsonNode.class);
			
			for (JsonNode node : rootNode) {
				
				long lastSeen = node.path("lastSeen").getLongValue();
								
				if(lastSeen >= expireTime ){ // newer than expireTime
					
					macsNew = fillMacArrayList(node); 
					ipsNew = fillIpArrayList(node);
					vlansNew = fillVlanArrayList(node);
					devicesNew = fillDeviceArrayList(node);
					portsNew = fillPortArrayList(node);

					// At this time a Device can only have 1 mac and one port to be connected
					if(macsNew.size() > 0 && devicesNew.size() > 0 && portsNew.size() > 0){
						if(portsNew.get(0) > 0){ //Device publish only if port not under null
																
							Document docDiscoByMac = getMetadataFactory().createDiscoveredBy();					
							PublishUpdate publishDiscoByMac = Requests.createPublishUpdate(
									devicesNew.get(0),macsNew.get(0), docDiscoByMac, MetadataLifetime.session);															
							ssrc.publish(Requests.createPublishReq(publishDiscoByMac));	
														
							// update delete with ips
							for(int j = 0;j< ipsNew.size();j++){
								
								//del ip-mac
								PublishDelete del = Requests.createPublishDelete(
										ipsNew.get(j), macsNew.get(0), "meta:ip-mac");
								del.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
										IfmapStrings.STD_METADATA_NS_URI);
								ssrc.publish(Requests.createPublishReq(del));
								//del discovered by		
								PublishDelete delDiscoByIp = Requests.createPublishDelete(
										ipsNew.get(j), devicesNew.get(0), "meta:discovered-by");
								delDiscoByIp.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
										IfmapStrings.STD_METADATA_NS_URI);
								ssrc.publish(Requests.createPublishReq(delDiscoByIp));
								// add ip-mac
								Document docIpMac = getMetadataFactory().createIpMac();
								PublishUpdate publishIpMac = Requests.createPublishUpdate(
										ipsNew.get(j), macsNew.get(0), docIpMac, MetadataLifetime.session);
								ssrc.publish(Requests.createPublishReq(publishIpMac));								
								//add discovered by
								Document docDiscoByIp = getMetadataFactory().createDiscoveredBy();	
								PublishUpdate publishDiscoByIp = Requests.createPublishUpdate(
										devicesNew.get(0),ipsNew.get(j), docDiscoByIp, MetadataLifetime.session);															
								ssrc.publish(Requests.createPublishReq(publishDiscoByIp));
							}							
							// update delete with vlans
							for(int j = 0;j< vlansNew.size();j++){
								//del layer 2 
						        String filter = String.format(
						        		"meta:layer2-information[@ifmap-publisher-id='%s' and vlan='%s' and port='%s']",
						                ssrc.getPublisherId(), vlansNew.get(j), portsNew.get(j));
								PublishDelete del = Requests.createPublishDelete(
										devicesNew.get(0),macsNew.get(0),filter);
						        del.addNamespaceDeclaration(
						        		IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
						        ssrc.publish(Requests.createPublishReq(del));
								//add layer2 not specific conform		
								Document docLayer2 = getMetadataFactory().createLayer2Information(
										vlansNew.get(0), null, portsNew.get(j), null);					
								PublishUpdate publishLayer2 = Requests.createPublishUpdate(
										devicesNew.get(0), macsNew.get(0), docLayer2, MetadataLifetime.session);															
								ssrc.publish(Requests.createPublishReq(publishLayer2));								
							}
							// update delete with no vlans
							if(vlansNew.size() == 0){
								//del layer2
						        String filter = String.format(
						        		"meta:layer2-information[@ifmap-publisher-id='%s' and port='%s']",
						                ssrc.getPublisherId(), portsNew.get(0));
								PublishDelete del = Requests.createPublishDelete(
										devicesNew.get(0),macsNew.get(0),filter);
						        del.addNamespaceDeclaration(
						        		IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
						        ssrc.publish(Requests.createPublishReq(del));
								
						        //add layer2 not specific conform
								Document docLayer2 = getMetadataFactory().createLayer2Information(
										null, null, portsNew.get(0), null);					
								PublishUpdate publishLayer2 = Requests.createPublishUpdate(
										devicesNew.get(0),macsNew.get(0), docLayer2, MetadataLifetime.session);															
								ssrc.publish(Requests.createPublishReq(publishLayer2));									
							}							
						}
					}
					
					macsNew.clear();
					ipsNew.clear();
					vlansNew.clear();
					devicesNew.clear();
					portsNew.clear();					
	
				} else if(lastSeen < expireTime){ // older then expireTime
					
					macsOld = fillMacArrayList(node);
					ipsOld = fillIpArrayList(node);
					vlansOld = fillVlanArrayList(node);
					devicesOld = fillDeviceArrayList(node);
					portsOld = fillPortArrayList(node);
										
					if(macsOld.size() > 0 && devicesOld.size() > 0 && portsOld.size() > 0){
						if(portsOld.get(0) > 0){ //Device publish only if port not under null
							//del discovered by								
							PublishDelete delDiscoByMac = Requests.createPublishDelete(
									devicesOld.get(0), macsOld.get(0), "meta:discovered-by");
							delDiscoByMac.addNamespaceDeclaration(
									IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
							ssrc.publish(Requests.createPublishReq(delDiscoByMac));
															
							// with ips delete
							for(int j = 0;j< ipsOld.size();j++){
								
								//del ip-mac
								PublishDelete del = Requests.createPublishDelete(
										ipsOld.get(j), macsOld.get(0), "meta:ip-mac");
								del.addNamespaceDeclaration(
										IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
								ssrc.publish(Requests.createPublishReq(del));
								
								//del discovered by		
								String filter = String.format(
										"meta:discovered-by[@ifmap-publisher-id='%s']", ssrc.getPublisherId());
								PublishDelete delDiscoByIp = Requests.createPublishDelete(
										ipsOld.get(j), devicesOld.get(0),filter);										 
								delDiscoByIp.addNamespaceDeclaration(
										IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
								ssrc.publish(Requests.createPublishReq(delDiscoByIp));					
							}	
							
							//with vlans delete
							for(int j = 0;j< vlansOld.size();j++){
								
						        String filter = String.format(
						        		"meta:layer2-information[@ifmap-publisher-id='%s' and vlan='%s' and port='%s']",
						        		ssrc.getPublisherId(), vlansOld.get(j), portsOld.get(j)	);						        
								PublishDelete del = Requests.createPublishDelete(
										devicesOld.get(0),macsOld.get(0),filter	);								
						        del.addNamespaceDeclaration(
						        		IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI );						        
						        ssrc.publish(Requests.createPublishReq(del));					
							}
							
							// no vlans delete
							if(vlansOld.size() == 0){
																
						        String filter = String.format(
						        		"meta:layer2-information[@ifmap-publisher-id='%s' and port='%s']",
						                ssrc.getPublisherId(), portsOld.get(0));
						        PublishDelete del = Requests.createPublishDelete(
						        		devicesOld.get(0), macsOld.get(0), filter);
						        del.addNamespaceDeclaration(
						        		IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
						        ssrc.publish(Requests.createPublishReq(del));
							}							
						}
					}				
					
					macsOld.clear();
					ipsOld.clear();
					vlansOld.clear();
					devicesOld.clear();
					portsOld.clear();
				}							
			}			
				
			
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IfmapErrorResult e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IfmapException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		
	}

	private ArrayList<MacAddress> fillMacArrayList(JsonNode node){
	
		ArrayList<MacAddress> macs = new ArrayList<MacAddress>();
		
		JsonNode nodeMacs = node.path("mac");
		for (JsonNode nodeMacItr : nodeMacs) {					
			macs.add(Identifiers.createMac(nodeMacItr.getTextValue()));
		}		
		return macs;		
	}
	
	private ArrayList<IpAddress> fillIpArrayList(JsonNode node){
		
		ArrayList<IpAddress> ips = new ArrayList<IpAddress>();
		
		JsonNode nodeIps = node.path("ipv4");
		for (JsonNode nodeIpItr : nodeIps) {					
			ips.add(Identifiers.createIp4(nodeIpItr.getTextValue()));
		}
		return ips;		
	}
	
	private ArrayList<Integer> fillVlanArrayList(JsonNode node){
		
		ArrayList<Integer> vlans = new ArrayList<Integer>();
		
		JsonNode nodeVlan = node.path("vlan");
		for (JsonNode nodeVlanItr : nodeVlan) {					
			vlans.add(nodeVlanItr.getIntValue());
		}
		return vlans;			
	}
	
	private ArrayList<Device> fillDeviceArrayList(JsonNode node){
		
		ArrayList<Device> devices = new ArrayList<Device>();
		
		JsonNode nodeAttachmentPoint = node.path("attachmentPoint");
		for (JsonNode nodeAttachmentPointItr : nodeAttachmentPoint) {			
			devices.add(Identifiers.createDev("Switch: "+nodeAttachmentPointItr.path("switchDPID").getTextValue()));			
		}
		return devices;		
	}
	
	private ArrayList<Integer> fillPortArrayList(JsonNode node){
		
		ArrayList<Integer> ports = new ArrayList<Integer>();
		
		JsonNode nodeAttachmentPoint = node.path("attachmentPoint");
		for (JsonNode nodeAttachmentPointItr : nodeAttachmentPoint) {
			ports.add(nodeAttachmentPointItr.path("port").getIntValue()); 
		}
		return ports;		
	}
	
}
