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
import de.fhhannover.inform.trust.ifmapj.identifier.Device;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifier;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifiers;
import de.fhhannover.inform.trust.ifmapj.identifier.IpAddress;
import de.fhhannover.inform.trust.ifmapj.messages.MetadataLifetime;
import de.fhhannover.inform.trust.ifmapj.messages.PublishDelete;
import de.fhhannover.inform.trust.ifmapj.messages.PublishUpdate;
import de.fhhannover.inform.trust.ifmapj.messages.Requests;
import de.fhhannover.inform.trust.ifmapj.metadata.Cardinality;
import de.hshannover.f4.trust.ironflow.Configuration;


/**
 * This class is the Implementation to Request the OpenflowController for all connected switches 
 * and the controller himself 
 * 
 * @author Marius Rohde
 *
 */

public class RequestSwitchList extends RequestStrategy {

	public static final String IRONFLOW_METADATA_NS_URI =  "http://trust.f4.hs-hannover.de/ironflow";
	
	private static final Logger logger = Logger.getLogger(RequestSwitchList.class.getName());
	
	private HashMap<String, String> switchesAndIps = new HashMap<String, String>();
	
	@Override
	public void requestWebservice(WebTarget webTarget, SSRC ssrc) {

		String jsonString;
		JsonNode rootNode;		
		
		///wm/core/controller/switches/json
		
		ObjectMapper mapper = new ObjectMapper();
		WebTarget resourceWebTarget = webTarget.path("/wm/core/controller/switches/json");
		
		HashMap<String, String> switchesAndIpsToDelete = new HashMap<String, String>();
		switchesAndIpsToDelete.putAll(switchesAndIps);
		switchesAndIps.clear();
		
		jsonString = this.getResponse(resourceWebTarget).readEntity(String.class);
		logger.fine("json switchlist response string");
		
		try {
			
			//device-ip not specific conform
			// create controller-ip
			Device devController = Identifiers.createDev("OpenFlowController");
			IpAddress ipController = Identifiers.createIp4(Configuration.openFlowControllerIP());			
			Document devIp = getMetadataFactory().createDevIp();
			PublishUpdate publishDevIp = Requests.createPublishUpdate(
					devController,ipController,devIp,MetadataLifetime.session);			
			ssrc.publish(Requests.createPublishReq(publishDevIp));
			
			//not specific conform ...why capability here ?
			// create capability - Controller
			String filterCap = String.format(
					"meta:capability[@ifmap-publisher-id='%s']", ssrc.getPublisherId());
			PublishDelete delCap = Requests.createPublishDelete(devController,filterCap);
			delCap.addNamespaceDeclaration(    		
		        	IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
		    ssrc.publish(Requests.createPublishReq(delCap));
			
			Document capController = getMetadataFactory().createCapability("Flow Switch Controller");
			PublishUpdate publishCapController = Requests.createPublishUpdate(
					devController,capController,MetadataLifetime.session);		
			ssrc.publish(Requests.createPublishReq(publishCapController));
			
			//Create Openflow extended identifier 
			Identifier extIdOpenflowGroup = Identifiers.createExtendedIdentity(
					getClass().getResourceAsStream("/openflowGroup.xml"));
			//Create Controller-group member-of
			Document controllerMemberOfGroup = getMetadataFactory().create(
					"member-of",IfmapStrings.STD_METADATA_PREFIX,
					IRONFLOW_METADATA_NS_URI, Cardinality.singleValue);
			PublishUpdate publishSwitchGroup = Requests.createPublishUpdate(
					extIdOpenflowGroup,devController,controllerMemberOfGroup,MetadataLifetime.session);
			ssrc.publish(Requests.createPublishReq(publishSwitchGroup));	
			
			
			rootNode = mapper.readValue(jsonString, JsonNode.class);		
			
			for (JsonNode node : rootNode) {
				
				JsonNode dpidNode = node.path("dpid");								
				Device devSwitch = Identifiers.createDev("Switch: " + dpidNode.getTextValue());
				
				JsonNode inetNode = node.path("inetAddress");
				String ipStr = inetNode.getTextValue();
				ipStr = ipStr.substring(1,ipStr.indexOf(":"));
				
				if(ipStr.equals("127.0.0.1")){
					ipStr = Configuration.openFlowControllerIP();
				}
				//create ip-switch
				IpAddress ipSwitch = Identifiers.createIp4(ipStr);				
				Document switchDevIp = getMetadataFactory().createDevIp();
				PublishUpdate publishSwitchIp = Requests.createPublishUpdate(
						devSwitch,ipSwitch,switchDevIp,MetadataLifetime.session);
				ssrc.publish(Requests.createPublishReq(publishSwitchIp));
				
				//create group-switch
				Document switchMemberOfGroup = getMetadataFactory().create(
						"member-of",IfmapStrings.STD_METADATA_PREFIX,
						IRONFLOW_METADATA_NS_URI, Cardinality.singleValue);
				PublishUpdate publishSwitchMemberOfGroup = Requests.createPublishUpdate(
						devSwitch,extIdOpenflowGroup,switchMemberOfGroup,MetadataLifetime.session);
				ssrc.publish(Requests.createPublishReq(publishSwitchMemberOfGroup));	
					
				switchesAndIps.put(dpidNode.getTextValue(), ipStr);
				switchesAndIpsToDelete.remove(dpidNode.getTextValue());
				
				// del and create switch characteristics
		        String filter = String.format(
		        		"meta:device-characteristic[@ifmap-publisher-id='%s']", ssrc.getPublisherId());
				PublishDelete del = Requests.createPublishDelete(devSwitch,filter);
		        del.addNamespaceDeclaration(    		
		        		IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
		        ssrc.publish(Requests.createPublishReq(del));
				
				JsonNode descNode = node.path("attributes").path("DescriptionData");	
				DateFormat dfmt = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZ'Z'" );
								
				Date connectedSince = new Date(node.path("connectedSince").getLongValue());
				Document devDesc = getMetadataFactory().createDevChar(
						descNode.path("manufacturerDescription").getTextValue(),
						descNode.path("hardwareDescription").getTextValue(), null, 
						descNode.path("softwareDescription").getTextValue(), "l3-switch",
						dfmt.format(connectedSince), ssrc.getPublisherId(),	"scan");
				PublishUpdate publishdevDesc = Requests.createPublishUpdate(devSwitch,devDesc,MetadataLifetime.session);
				ssrc.publish(Requests.createPublishReq(publishdevDesc));				
			}
			
			//delete Old Device-IP Metadata (switches to IP) if switches disconnect from network
			// in Addition delete characteristics
			deleteDeviceSwitchAndMetaData(ssrc, switchesAndIpsToDelete);
			
						
		} catch (IfmapErrorResult e) {
			logger.severe("RequestSwitchList: "+e);
		} catch (IfmapException e) {
			logger.severe("RequestSwitchList: "+e);
		} catch (JsonParseException e) {
			logger.severe("RequestSwitchList: "+e);
		} catch (JsonMappingException e) {
			logger.severe("RequestSwitchList: "+e);
		} catch (IOException e) {
			logger.severe("RequestSwitchList: "+e);
		}	

	}
	
	/**
	* This helper method deletes old switch devices-ip, member-of and device-characteristic metadata
	*/	
	private void deleteDeviceSwitchAndMetaData(
			SSRC ssrc, HashMap<String, String> switchesAndIpsToDelete)
			throws IfmapErrorResult, IfmapException{
		
		Iterator<Entry<String, String>> itrSwitchWithIp = switchesAndIpsToDelete.entrySet().iterator();
		Identifier extIdOpenflowGroup = Identifiers.createExtendedIdentity(
				getClass().getResourceAsStream("/openflowGroup.xml"));
		
		while(itrSwitchWithIp.hasNext()){
			Entry<String, String> entrySwitchIp = itrSwitchWithIp.next();
			
			Device devSwitch = Identifiers.createDev("Switch: " + entrySwitchIp.getKey());
			IpAddress ipSwitch = Identifiers.createIp4(entrySwitchIp.getValue());			
			
	        String filterDevIp = String.format(
	        		"meta:device-ip[@ifmap-publisher-id='%s']", ssrc.getPublisherId());
			PublishDelete delDevIp = Requests.createPublishDelete(devSwitch,ipSwitch,filterDevIp);
			delDevIp.addNamespaceDeclaration(    		
	        		IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
	        ssrc.publish(Requests.createPublishReq(delDevIp));
			
	        String filterGroupOf = String.format(
	        		"meta:member-of[@ifmap-publisher-id='%s']", ssrc.getPublisherId());
			PublishDelete delGroupOf = Requests.createPublishDelete(devSwitch,extIdOpenflowGroup,filterGroupOf);
			delGroupOf.addNamespaceDeclaration(    		
	        		IfmapStrings.STD_METADATA_PREFIX, IRONFLOW_METADATA_NS_URI);
	        ssrc.publish(Requests.createPublishReq(delGroupOf));
	        
	        String filterChara = String.format(
	        		"meta:device-characteristic[@ifmap-publisher-id='%s']", ssrc.getPublisherId());
			PublishDelete delChara = Requests.createPublishDelete(devSwitch,filterChara);
			delChara.addNamespaceDeclaration(    		
	        		IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
	        ssrc.publish(Requests.createPublishReq(delChara));
		}	
	}
	
	

}
