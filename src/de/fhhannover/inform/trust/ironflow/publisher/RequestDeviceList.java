package de.fhhannover.inform.trust.ironflow.publisher;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

import javax.ws.rs.client.WebTarget;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import de.fhhannover.inform.trust.ifmapj.channel.SSRC;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapErrorResult;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapException;
import de.fhhannover.inform.trust.ifmapj.identifier.AccessRequest;
import de.fhhannover.inform.trust.ifmapj.identifier.Device;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifiers;
import de.fhhannover.inform.trust.ifmapj.identifier.IpAddress;
import de.fhhannover.inform.trust.ifmapj.identifier.MacAddress;
import de.fhhannover.inform.trust.ifmapj.messages.MetadataLifetime;
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
		
		jsonString = this.getResponse(resourceWebTarget).readEntity(String.class);

	    System.out.println(jsonString);
						    	    
		try {
			
			rootNode = mapper.readValue(jsonString, JsonNode.class);
			
			for (JsonNode node : rootNode) {
				
				JsonNode nodeLastSeen = node.path("lastSeen");
				Date date = new Date(nodeLastSeen.getLongValue());
				System.out.println(date);
				
				if(nodeLastSeen.getLongValue() >= System.currentTimeMillis() - 600000 ){
					
					JsonNode nodeMacs = node.path("mac");
					for (JsonNode nodeMacItr : nodeMacs) {					
						MacAddress mac = Identifiers.createMac(nodeMacItr.getTextValue()); //erzeugt Identifier MAC Adresse
						System.out.println(nodeMacItr.toString());
					}
					
					JsonNode nodeIps = node.path("ipv4");
					for (JsonNode nodeIpItr : nodeIps) {					
						IpAddress ip = Identifiers.createIp4(nodeIpItr.getTextValue()); // erzeugt Identifier IP Adresse
						System.out.println(nodeIpItr.toString());
					}
					
					JsonNode nodeVlan = node.path("vlan");
					for (JsonNode nodeVlanItr : nodeVlan) {					
						int port = nodeVlanItr.getIntValue(); // erzeugt Identifier IP Adresse
						System.out.println(nodeVlanItr.toString());
					}	
															
					JsonNode nodeAttachmentPoint = node.path("attachmentPoint");
					for (JsonNode nodeAttachmentPointItr : nodeAttachmentPoint) {
						
						JsonNode port = nodeAttachmentPointItr.path("port");
						if(port.getIntValue() >= 0){ // Nur Ports im positiven bereich
							Device dev = Identifiers.createDev("Switch: "+nodeAttachmentPointItr.path("switchDPID").getTextValue()); //erzeugt Identifier Device
							AccessRequest ar = Identifiers.createAr("Client: "+port.getIntValue()); //erzeugt Identifier AccessRequest
						}
					}
					
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
					//ssrc.publish(Requests.createPublishReq(Requests.createPublishUpdate(ip, mac, getMetadataFactory().createIpMac(), MetadataLifetime.forever)));
					//ssrc.publish(Requests.createPublishReq(Requests.createPublishUpdate(ar, mac, getMetadataFactory().createArMac(), MetadataLifetime.forever)));					
					//ssrc.publish(Requests.createPublishReq(Requests.createPublishUpdate(dev, ar, getMetadataFactory().createLayer2Information(null, null, port.getIntValue() , null), MetadataLifetime.forever)));	
					
					
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
		} /*catch (IfmapErrorResult e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IfmapException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		
		
		
		
	}

}
