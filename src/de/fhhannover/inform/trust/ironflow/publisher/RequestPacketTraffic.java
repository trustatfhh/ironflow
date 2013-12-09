package de.fhhannover.inform.trust.ironflow.publisher;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;

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
import de.fhhannover.inform.trust.ifmapj.identifier.Identifiers;
import de.fhhannover.inform.trust.ifmapj.identifier.MacAddress;
import de.fhhannover.inform.trust.ifmapj.messages.MetadataLifetime;
import de.fhhannover.inform.trust.ifmapj.messages.PublishDelete;
import de.fhhannover.inform.trust.ifmapj.messages.PublishUpdate;
import de.fhhannover.inform.trust.ifmapj.messages.Requests;
import de.fhhannover.inform.trust.ifmapj.metadata.EventType;
import de.fhhannover.inform.trust.ifmapj.metadata.Significance;

public class RequestPacketTraffic extends RequestStrategy {

	@Override
	public void requestWebservice(WebTarget webTarget, SSRC ssrc) {


		String jsonStringTraffic;
		String jsonStringSwitches;
		JsonNode rootNodeTraffic;
		JsonNode rootNodeSwitches;
		
		// /wm/core/switch/all/port/json  
		// /wm/device/  
		
		ObjectMapper mapper = new ObjectMapper();
		WebTarget resourceWebTargetTraffic = webTarget.path("/wm/core/switch/all/port/json");
		WebTarget resourceWebTargetDevices = webTarget.path("/wm/device/");
				
		jsonStringTraffic = this.getResponse(resourceWebTargetTraffic).readEntity(String.class);
		jsonStringSwitches = this.getResponse(resourceWebTargetDevices).readEntity(String.class);
		System.out.println(jsonStringTraffic);	
		//System.out.println(jsonStringSwitches);	
		
		DateFormat dfmt = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZ'Z'" );
		Date now = new Date(System.currentTimeMillis());
		
		try {
			
			rootNodeTraffic = mapper.readValue(jsonStringTraffic, JsonNode.class);
			rootNodeSwitches = mapper.readValue(jsonStringSwitches, JsonNode.class);
			Iterator<Entry<String, JsonNode>> itrSwitches = rootNodeTraffic.getFields();
						
			while(itrSwitches.hasNext()){
				Entry<String, JsonNode> switchEntry = itrSwitches.next();
												
				JsonNode switchNode = switchEntry.getValue();
								
				for (JsonNode node : switchNode) {
					int portNrTraffic = node.path("portNumber").getIntValue();
					if(portNrTraffic > 0){
						long rxPackets = node.path("receivePackets").getLongValue();
						long txPackets = node.path("transmitPackets").getLongValue();
												
						for (JsonNode nodeSwitches : rootNodeSwitches) {					
							
							JsonNode nodeMacs = nodeSwitches.path("mac");
							JsonNode nodeAttachmentPoint = nodeSwitches.path("attachmentPoint");
														
							for (JsonNode nodeAttachmentPointItr : nodeAttachmentPoint) {
								int portNrSwitch = nodeAttachmentPointItr.path("port").getIntValue(); 
								String dpid = nodeAttachmentPointItr.path("switchDPID").getTextValue();

								if(portNrTraffic == portNrSwitch && dpid.equals(switchEntry.getKey())){
									System.out.println(portNrSwitch+" - "+ portNrTraffic);
									System.out.println(dpid+" - "+switchEntry.getKey());
																		
									for (JsonNode nodeMacItr : nodeMacs) {					

										MacAddress macHost = Identifiers.createMac(nodeMacItr.getTextValue());
										
										String filterTXRXEvents = String.format(
												"meta:event[@ifmap-publisher-id='%s']", ssrc.getPublisherId());
										PublishDelete delTXRXEvents = Requests.createPublishDelete(macHost,filterTXRXEvents);
										delTXRXEvents.addNamespaceDeclaration(    		
									        	IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
									    ssrc.publish(Requests.createPublishReq(delTXRXEvents));
										
										Document eventTxMac = getMetadataFactory().createEvent(
												"Test", dfmt.format(now), ssrc.getPublisherId(), 1,
												100, Significance.informational, EventType.p2p, "",
												"TxPackets: "+ txPackets, "");
										PublishUpdate publishEventTxMac = Requests.createPublishUpdate(
												macHost,eventTxMac,MetadataLifetime.session);
										ssrc.publish(Requests.createPublishReq(publishEventTxMac));
										
										Document eventRxMac = getMetadataFactory().createEvent(
												"Test", dfmt.format(now), ssrc.getPublisherId(), 1,
												100, Significance.informational, EventType.p2p, "",
												"RxPackets: "+ rxPackets, "");
										PublishUpdate publishEventRxMac = Requests.createPublishUpdate(
												macHost,eventRxMac,MetadataLifetime.session);
										ssrc.publish(Requests.createPublishReq(publishEventRxMac));
										
									}	
									
								}
								
							}						
							
						}
						
					}					
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

}
