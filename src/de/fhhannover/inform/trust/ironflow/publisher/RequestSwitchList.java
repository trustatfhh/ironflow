package de.fhhannover.inform.trust.ironflow.publisher;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
import de.fhhannover.inform.trust.ifmapj.identifier.AccessRequest;
import de.fhhannover.inform.trust.ifmapj.identifier.Device;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifiers;
import de.fhhannover.inform.trust.ifmapj.identifier.IpAddress;
import de.fhhannover.inform.trust.ifmapj.messages.MetadataLifetime;
import de.fhhannover.inform.trust.ifmapj.messages.PublishDelete;
import de.fhhannover.inform.trust.ifmapj.messages.PublishUpdate;
import de.fhhannover.inform.trust.ifmapj.messages.Requests;
import de.fhhannover.inform.trust.ironflow.Configuration;

public class RequestSwitchList extends RequestStrategy {

	@Override
	public void requestWebservice(WebTarget webTarget, SSRC ssrc) {

		String jsonString;
		JsonNode rootNode;		
		
		///wm/core/controller/switches/json
		
		ObjectMapper mapper = new ObjectMapper();
		WebTarget resourceWebTarget = webTarget.path("/wm/core/controller/switches/json");
		
		jsonString = this.getResponse(resourceWebTarget).readEntity(String.class);
		System.out.println(jsonString);		
		
		try {
			
			Device devController = Identifiers.createDev("OpenFlowController");
			IpAddress ipController = Identifiers.createIp4(Configuration.openFlowControllerIP());			
			Document devIp = getMetadataFactory().createDevIp();
			PublishUpdate publishDevIp = Requests.createPublishUpdate(devController,ipController,devIp,MetadataLifetime.session);			
			ssrc.publish(Requests.createPublishReq(publishDevIp));
						
			rootNode = mapper.readValue(jsonString, JsonNode.class);		
			
			for (JsonNode node : rootNode) {
				
				JsonNode dpidNode = node.path("dpid");								
				Device devSwitch = Identifiers.createDev("Switch: " + dpidNode.getTextValue());
				AccessRequest arSwitch= Identifiers.createAr("Switch: " + dpidNode.getTextValue());
				
				Document devAuthby = getMetadataFactory().createAuthBy();
				PublishUpdate publishAuthby = Requests.createPublishUpdate(devController,devSwitch,devAuthby,MetadataLifetime.session);
				ssrc.publish(Requests.createPublishReq(publishAuthby));
				
				
				Document devAr = getMetadataFactory().createArDev();
				PublishUpdate publishDevAr = Requests.createPublishUpdate(devSwitch,arSwitch,devAr,MetadataLifetime.session);
				ssrc.publish(Requests.createPublishReq(publishDevAr));
						
				JsonNode inetNode = node.path("inetAddress");
				String ipStr = inetNode.getTextValue();
				ipStr = ipStr.substring(1,ipStr.indexOf(":"));
				IpAddress ip = Identifiers.createIp4(ipStr);
				
				Document arIp = getMetadataFactory().createArIp();
				PublishUpdate publishArIp = Requests.createPublishUpdate(arSwitch,ip,arIp,MetadataLifetime.session);
				ssrc.publish(Requests.createPublishReq(publishArIp));
								
				JsonNode descNode = node.path("attributes").path("DescriptionData");	
				DateFormat dfmt = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZ'Z'" );

				
				PublishDelete del = Requests.createPublishDelete();
		        String filter = String.format("meta:device-characteristic[@ifmap-publisher-id='%s']",
		                ssrc.getPublisherId());

		        del.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
		        		IfmapStrings.STD_METADATA_NS_URI);
		        del.setFilter(filter);
		        del.setIdentifier1(devSwitch);
		        ssrc.publish(Requests.createPublishReq(del));
				
				
				Date connectedSince = new Date(node.path("connectedSince").getLongValue());
				Document devDesc = getMetadataFactory().createDevChar(
						descNode.path("manufacturerDescription").getTextValue(),
						descNode.path("hardwareDescription").getTextValue(), null, 
						descNode.path("softwareDescription").getTextValue(), "l3-switch",
						dfmt.format(connectedSince), 
						ssrc.getPublisherId(),
						"scan");
				PublishUpdate publishdevDesc = Requests.createPublishUpdate(devSwitch,devDesc,MetadataLifetime.session);
				ssrc.publish(Requests.createPublishReq(publishdevDesc));

				
			}	
						
		} catch (IfmapErrorResult e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IfmapException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

	}

}
