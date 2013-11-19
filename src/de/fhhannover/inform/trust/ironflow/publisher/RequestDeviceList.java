package de.fhhannover.inform.trust.ironflow.publisher;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

import javax.ws.rs.client.WebTarget;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Document;

import de.fhhannover.inform.trust.ifmapj.channel.SSRC;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapErrorResult;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapException;
import de.fhhannover.inform.trust.ifmapj.identifier.AccessRequest;
import de.fhhannover.inform.trust.ifmapj.identifier.Device;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifiers;
import de.fhhannover.inform.trust.ifmapj.identifier.IpAddress;
import de.fhhannover.inform.trust.ifmapj.identifier.MacAddress;
import de.fhhannover.inform.trust.ifmapj.messages.MetadataLifetime;
import de.fhhannover.inform.trust.ifmapj.messages.PublishRequest;
import de.fhhannover.inform.trust.ifmapj.messages.PublishUpdate;
import de.fhhannover.inform.trust.ifmapj.messages.Requests;

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
		// TODO Auto-generated method stub
		
		String jsonString;
		JsonNode rootNode;
		
		ObjectMapper mapper = new ObjectMapper();
		WebTarget resourceWebTarget = webTarget.path("/wm/device/");
		
		ArrayList<MacAddress> macsNew = new ArrayList<MacAddress>();
		ArrayList<MacAddress> macsOld = new ArrayList<MacAddress>();
		ArrayList<IpAddress> ipsNew = new ArrayList<IpAddress>();
		ArrayList<IpAddress> ipsOld = new ArrayList<IpAddress>();
		ArrayList<Integer> vlansNew = new ArrayList<Integer>();
		ArrayList<Integer> vlansOld = new ArrayList<Integer>();
		ArrayList<Device> devicesNew = new ArrayList<Device>();
		ArrayList<Device> devicesOld = new ArrayList<Device>();
		ArrayList<Integer> portsNew = new ArrayList<Integer>();
		ArrayList<Integer> portsOld = new ArrayList<Integer>();		
		
		long expireTime = System.currentTimeMillis() - 600000;
		
		jsonString = this.getResponse(resourceWebTarget).readEntity(String.class);

		System.out.println(jsonString);
						    	    
		try {
			
			rootNode = mapper.readValue(jsonString, JsonNode.class);
			
			for (JsonNode node : rootNode) {
				
				long lastSeen = node.path("lastSeen").getLongValue();
								
				if(lastSeen >= expireTime ){ // newer than expireTime
					
					fillMacArrayList(node, macsNew); // bad c stile but no other better possibility 
					fillIpArrayList(node, ipsNew);
					fillVlanArrayList(node, vlansNew);
					fillDeviceArrayList(node, devicesNew);
					fillPortArrayList(node, portsNew);
					
					/*
					PublishDelete del = Requests.createPublishDelete(ip, mac, "meta:ip-mac");
					del.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
							IfmapStrings.STD_METADATA_NS_URI);
					ssrc.publish(Requests.createPublishReq(del));
					
					del = Requests.createPublishDelete(dev, ip, "meta:device-ip");
					del.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
							IfmapStrings.STD_METADATA_NS_URI);
					ssrc.publish(Requests.createPublishReq(del));
					
					del = Requests.createPublishDelete(ar, dev, "meta:layer2-information");
					del.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
							IfmapStrings.STD_METADATA_NS_URI);
					ssrc.publish(Requests.createPublishReq(del));
					*/
																
					
					for(int i = 0; i < portsNew.size(); i++){
						if(portsNew.get(i) > 0){ //Device publish only if port not under null
							
							AccessRequest ar = Identifiers.createAr("Client: "+portsNew.get(i));
							
							Document docLayer2 = getMetadataFactory().createLayer2Information(vlansNew.get(0), null, portsNew.get(i), null);					
							PublishUpdate publishLayer2 = Requests.createPublishUpdate(devicesNew.get(i), ar, docLayer2, MetadataLifetime.session);
							
							Document docArMac = getMetadataFactory().createArMac();
							PublishUpdate publishArMac = Requests.createPublishUpdate(ar, macsNew.get(0), docArMac, MetadataLifetime.session);
							
							ssrc.publish(Requests.createPublishReq(publishLayer2));
							ssrc.publish(Requests.createPublishReq(publishArMac));
							
							
							for(int j = 0;j< ipsNew.size();j++){
								Document docIpMac = getMetadataFactory().createIpMac();
								PublishUpdate publishIpMac = Requests.createPublishUpdate(ipsNew.get(i), macsNew.get(0), docIpMac, MetadataLifetime.session);
								ssrc.publish(Requests.createPublishReq(publishIpMac));
							}
							
						}
						
					}
					
					
					
	
				} else if(lastSeen < expireTime){ // older then expireTime
					
					fillMacArrayList(node,macsOld); // bad c stile but no other better possibility
					fillIpArrayList(node, ipsOld);
					fillVlanArrayList(node, vlansOld);
					fillDeviceArrayList(node, devicesOld);
					fillPortArrayList(node, portsOld);
					//TODO			
				}							
			
				macsNew.clear();
				macsOld.clear();
				ipsNew.clear();
				ipsOld.clear();
				vlansNew.clear();
				vlansOld.clear();								
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

	private ArrayList<MacAddress> fillMacArrayList(JsonNode node, ArrayList<MacAddress> macs){
	
		JsonNode nodeMacs = node.path("mac");
		for (JsonNode nodeMacItr : nodeMacs) {					
			macs.add(Identifiers.createMac(nodeMacItr.getTextValue()));
		}		
		return macs;		
	}
	
	private ArrayList<IpAddress> fillIpArrayList(JsonNode node, ArrayList<IpAddress> ips){
		
		JsonNode nodeIps = node.path("ipv4");
		for (JsonNode nodeIpItr : nodeIps) {					
			ips.add(Identifiers.createIp4(nodeIpItr.getTextValue()));
		}
		return ips;		
	}
	
	private ArrayList<Integer> fillVlanArrayList(JsonNode node, ArrayList<Integer> vlans){
		
		JsonNode nodeVlan = node.path("vlan");
		for (JsonNode nodeVlanItr : nodeVlan) {					
			vlans.add(nodeVlanItr.getIntValue());
		}
		return vlans;			
	}
	
	private ArrayList<Device> fillDeviceArrayList(JsonNode node, ArrayList<Device> devices){
		
		JsonNode nodeAttachmentPoint = node.path("attachmentPoint");
		for (JsonNode nodeAttachmentPointItr : nodeAttachmentPoint) {			
			devices.add(Identifiers.createDev("Switch: "+nodeAttachmentPointItr.path("switchDPID").getTextValue()));			
		}
		return devices;		
	}
	
	private ArrayList<Integer> fillPortArrayList(JsonNode node, ArrayList<Integer> ports){
		
		JsonNode nodeAttachmentPoint = node.path("attachmentPoint");
		for (JsonNode nodeAttachmentPointItr : nodeAttachmentPoint) {
			ports.add(nodeAttachmentPointItr.path("port").getIntValue()); 
		}
		return ports;		
	}
	
}
