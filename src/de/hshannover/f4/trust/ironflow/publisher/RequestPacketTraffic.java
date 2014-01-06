package de.hshannover.f4.trust.ironflow.publisher;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;

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
import de.hshannover.f4.trust.ironflow.Configuration;

/**
 * This class is the Implementation to Request the OpenflowController for all Packets transferred 
 * 
 * 
 * @author Marius Rohde
 *
 */

public class RequestPacketTraffic extends RequestStrategy {

	private static final Logger logger = Logger.getLogger(RequestPacketTraffic.class.getName());
	
	
	
	// packet count received and transmitted
	private HashMap<String, Long> macsAndRxData = new HashMap<String, Long>();
	private HashMap<String, Long> macsAndTxData = new HashMap<String, Long>();
	
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
		
		
		logger.fine("json traffic response string:" + jsonStringTraffic);
		logger.finer("json devices response string:" + jsonStringSwitches);

		
		DateFormat dfmt = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZ'Z'" );
		Date now = new Date(System.currentTimeMillis());
		
		long expireTime = System.currentTimeMillis() - Configuration.ironflowDeviceExpireTime()*60000;
		
		HashMap<String, Long> macsAndRxDataOld = new HashMap<String, Long>();
		HashMap<String, Long> macsAndTxDataOld = new HashMap<String, Long>();
		
		macsAndRxDataOld.putAll(macsAndRxData);
		macsAndTxDataOld.putAll(macsAndTxData);
		
		//clear Rx and Tx count to fill it witch the new data 
		macsAndRxData.clear();
		macsAndTxData.clear();
		
		try {
			
			rootNodeTraffic = mapper.readValue(jsonStringTraffic, JsonNode.class);
			rootNodeSwitches = mapper.readValue(jsonStringSwitches, JsonNode.class);
			Iterator<Entry<String, JsonNode>> itrTraffic = rootNodeTraffic.getFields();
						
			while(itrTraffic.hasNext()){
				Entry<String, JsonNode> trafficEntry = itrTraffic.next();
												
				JsonNode trafficNode = trafficEntry.getValue();
								
				for (JsonNode node : trafficNode) {
					int portNrTraffic = node.path("portNumber").getIntValue();
					if(portNrTraffic > 0){
						long rxPackets = node.path("receivePackets").getLongValue();
						long txPackets = node.path("transmitPackets").getLongValue();
												
						for (JsonNode nodeSwitches : rootNodeSwitches) {					
							
							JsonNode nodeMacs = nodeSwitches.path("mac");
							JsonNode nodeAttachmentPoint = nodeSwitches.path("attachmentPoint");
							long lastSeen = nodeSwitches.path("lastSeen").getLongValue();		

							if(lastSeen >= expireTime){
								for (JsonNode nodeAttachmentPointItr : nodeAttachmentPoint) {
									int portNrSwitch = nodeAttachmentPointItr.path("port").getIntValue(); 
									String dpid = nodeAttachmentPointItr.path("switchDPID").getTextValue();
	
									if(portNrTraffic == portNrSwitch && dpid.equals(trafficEntry.getKey())){
																			
										for (JsonNode nodeMacItr : nodeMacs) {					
	
											MacAddress macHost = Identifiers.createMac(nodeMacItr.getTextValue());
											
											macsAndRxData.put(nodeMacItr.getTextValue(), rxPackets);
											macsAndTxData.put(nodeMacItr.getTextValue(), txPackets);
											
											//TODO
											// Delete Events is Lifetime is session could be modified to notify Update
											// then you dont need to delete events
											String filterTXRXEvents = String.format(
													"meta:event[@ifmap-publisher-id='%s']", ssrc.getPublisherId());
											PublishDelete delTXRXEvents = Requests.createPublishDelete(
													macHost,filterTXRXEvents);
											delTXRXEvents.addNamespaceDeclaration(    		
										        	IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
										    ssrc.publish(Requests.createPublishReq(delTXRXEvents));
											
										    //Transmitted packets per interval
										    long txPacketsInterval = 0;
										    if(macsAndTxDataOld.containsKey(nodeMacItr.getTextValue())){
											    txPacketsInterval = txPackets - macsAndTxDataOld.get(
											    		nodeMacItr.getTextValue());
										    }
											Document eventTxMac = getMetadataFactory().createEvent(
													"TxTraffic", dfmt.format(now), ssrc.getPublisherId(), 1,
													100, Significance.informational, EventType.p2p, "",
													"TxPackets: "+ txPacketsInterval, "");
											PublishUpdate publishEventTxMac = Requests.createPublishUpdate(
													macHost,eventTxMac,MetadataLifetime.session);
											ssrc.publish(Requests.createPublishReq(publishEventTxMac));
											
											//Recieved packets per interval
										    long rxPacketsInterval = 0;
										    if(macsAndRxDataOld.containsKey(nodeMacItr.getTextValue())){
											    rxPacketsInterval = rxPackets - macsAndRxDataOld.get(
											    		nodeMacItr.getTextValue());
										    }
											Document eventRxMac = getMetadataFactory().createEvent(
													"RxTraffic", dfmt.format(now), ssrc.getPublisherId(), 1,
													100, Significance.informational, EventType.p2p, "",
													"RxPackets: "+ rxPacketsInterval, "");
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
			}

		} catch (JsonParseException e) {
			logger.severe("RequestPacketTraffic: "+e);
		} catch (JsonMappingException e) {
			logger.severe("RequestPacketTraffic: "+e);
		} catch (IOException e) {
			logger.severe("RequestPacketTraffic: "+e);
		} catch (IfmapErrorResult e) {
			logger.severe("RequestPacketTraffic: "+e);
		} catch (IfmapException e) {
			logger.severe("RequestPacketTraffic: "+e);
		}
		
		
	}

}
