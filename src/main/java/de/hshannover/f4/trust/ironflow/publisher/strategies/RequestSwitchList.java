/*
 * #%L
 * =====================================================
 *   _____                _     ____  _   _       _   _
 *  |_   _|_ __ _   _ ___| |_  / __ \| | | | ___ | | | |
 *    | | | '__| | | / __| __|/ / _` | |_| |/ __|| |_| |
 *    | | | |  | |_| \__ \ |_| | (_| |  _  |\__ \|  _  |
 *    |_| |_|   \__,_|___/\__|\ \__,_|_| |_||___/|_| |_|
 *                             \____/
 * 
 * =====================================================
 * 
 * Hochschule Hannover
 * (University of Applied Sciences and Arts, Hannover)
 * Faculty IV, Dept. of Computer Science
 * Ricklinger Stadtweg 118, 30459 Hannover, Germany
 * 
 * Email: trust@f4-i.fh-hannover.de
 * Website: http://trust.f4.hs-hannover.de
 * 
 * This file is part of ironflow, version 0.0.1, implemented by the Trust@HsH
 * research group at the Hochschule Hannover.
 * %%
 * Copyright (C) 2013 - 2014 Trust@HsH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package de.hshannover.f4.trust.ironflow.publisher.strategies;

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

import de.hshannover.f4.trust.ifmapj.binding.IfmapStrings;
import de.hshannover.f4.trust.ifmapj.channel.SSRC;
import de.hshannover.f4.trust.ifmapj.exception.IfmapErrorResult;
import de.hshannover.f4.trust.ifmapj.exception.IfmapException;
import de.hshannover.f4.trust.ifmapj.identifier.Device;
import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.identifier.Identifiers;
import de.hshannover.f4.trust.ifmapj.identifier.IpAddress;
import de.hshannover.f4.trust.ifmapj.messages.MetadataLifetime;
import de.hshannover.f4.trust.ifmapj.messages.PublishDelete;
import de.hshannover.f4.trust.ifmapj.messages.PublishUpdate;
import de.hshannover.f4.trust.ifmapj.messages.Requests;
import de.hshannover.f4.trust.ifmapj.metadata.Cardinality;
import de.hshannover.f4.trust.ironflow.Configuration;
import de.hshannover.f4.trust.ironflow.publisher.RequestStrategy;

/**
 * This class is the Implementation to Request the OpenflowController for all
 * connected switches and the controller himself
 * 
 * @author Marius Rohde
 * 
 */

public class RequestSwitchList extends RequestStrategy {

	public static final String IRONFLOW_METADATA_NS_URI = "http://trust.f4.hs-hannover.de/ironflow";

	private static final Logger LOGGER = Logger.getLogger(RequestSwitchList.class.getName());

	private HashMap<String, String> mSwitchesAndIps = new HashMap<String, String>();

	@Override
	public void requestWebservice(WebTarget webTarget, SSRC ssrc) {

		String jsonString;
		JsonNode rootNode;

		// /wm/core/controller/switches/json

		ObjectMapper mapper = new ObjectMapper();
		WebTarget resourceWebTarget = webTarget.path("/wm/core/controller/switches/json");

		HashMap<String, String> switchesAndIpsToDelete = new HashMap<String, String>();
		switchesAndIpsToDelete.putAll(mSwitchesAndIps);
		mSwitchesAndIps.clear();

		jsonString = this.getResponse(resourceWebTarget).readEntity(String.class);
		LOGGER.fine("json switchlist response string");

		try {

			// device-ip not specific conform
			// create controller-ip
			Device devController = Identifiers.createDev("OpenFlowController");
			IpAddress ipController = Identifiers.createIp4(Configuration.openFlowControllerIp());
			Document devIp = getMetadataFactory().createDevIp();
			PublishUpdate publishDevIp = Requests.createPublishUpdate(devController, ipController, devIp,
					MetadataLifetime.session);
			ssrc.publish(Requests.createPublishReq(publishDevIp));

			// not specific conform ...why capability here ?
			// create capability - Controller
			String filterCap = String.format("meta:capability[@ifmap-publisher-id='%s']", ssrc.getPublisherId());
			PublishDelete delCap = Requests.createPublishDelete(devController, filterCap);
			delCap.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
			ssrc.publish(Requests.createPublishReq(delCap));

			Document capController = getMetadataFactory().createCapability("Flow Switch Controller");
			PublishUpdate publishCapController = Requests.createPublishUpdate(devController, capController,
					MetadataLifetime.session);
			ssrc.publish(Requests.createPublishReq(publishCapController));

			// Create Openflow extended identifier
			Identifier extIdOpenflowGroup = Identifiers.createExtendedIdentity(getClass().getResourceAsStream(
					"/openflowGroup.xml"));
			// Create Controller-group member-of
			Document controllerMemberOfGroup = getMetadataFactory().create("member-of",
					IfmapStrings.STD_METADATA_PREFIX, IRONFLOW_METADATA_NS_URI, Cardinality.singleValue);
			PublishUpdate publishSwitchGroup = Requests.createPublishUpdate(extIdOpenflowGroup, devController,
					controllerMemberOfGroup, MetadataLifetime.session);
			ssrc.publish(Requests.createPublishReq(publishSwitchGroup));

			rootNode = mapper.readValue(jsonString, JsonNode.class);

			for (JsonNode node : rootNode) {

				JsonNode dpidNode = node.path("dpid");
				Device devSwitch = Identifiers.createDev("Switch: " + dpidNode.getTextValue());

				JsonNode inetNode = node.path("inetAddress");
				String ipStr = inetNode.getTextValue();
				ipStr = ipStr.substring(1, ipStr.indexOf(":"));

				if (ipStr.equals("127.0.0.1")) {
					ipStr = Configuration.openFlowControllerIp();
				}
				// create ip-switch
				IpAddress ipSwitch = Identifiers.createIp4(ipStr);
				Document switchDevIp = getMetadataFactory().createDevIp();
				PublishUpdate publishSwitchIp = Requests.createPublishUpdate(devSwitch, ipSwitch, switchDevIp,
						MetadataLifetime.session);
				ssrc.publish(Requests.createPublishReq(publishSwitchIp));

				// create group-switch
				Document switchMemberOfGroup = getMetadataFactory().create("member-of",
						IfmapStrings.STD_METADATA_PREFIX, IRONFLOW_METADATA_NS_URI, Cardinality.singleValue);
				PublishUpdate publishSwitchMemberOfGroup = Requests.createPublishUpdate(devSwitch, extIdOpenflowGroup,
						switchMemberOfGroup, MetadataLifetime.session);
				ssrc.publish(Requests.createPublishReq(publishSwitchMemberOfGroup));

				mSwitchesAndIps.put(dpidNode.getTextValue(), ipStr);
				switchesAndIpsToDelete.remove(dpidNode.getTextValue());

				// del and create switch characteristics
				String filter = String.format("meta:device-characteristic[@ifmap-publisher-id='%s']",
						ssrc.getPublisherId());
				PublishDelete del = Requests.createPublishDelete(devSwitch, filter);
				del.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
				ssrc.publish(Requests.createPublishReq(del));

				JsonNode descNode = node.path("attributes").path("DescriptionData");
				DateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ'Z'");

				Date connectedSince = new Date(node.path("connectedSince").getLongValue());
				Document devDesc = getMetadataFactory().createDevChar(
						descNode.path("manufacturerDescription").getTextValue(),
						descNode.path("hardwareDescription").getTextValue(), null,
						descNode.path("softwareDescription").getTextValue(), "l3-switch", dfmt.format(connectedSince),
						ssrc.getPublisherId(), "scan");
				PublishUpdate publishdevDesc = Requests.createPublishUpdate(devSwitch, devDesc,
						MetadataLifetime.session);
				ssrc.publish(Requests.createPublishReq(publishdevDesc));
			}

			// delete Old Device-IP Metadata (switches to IP) if switches
			// disconnect from network
			// in Addition delete characteristics
			deleteDeviceSwitchAndMetaData(ssrc, switchesAndIpsToDelete);

		} catch (IfmapErrorResult e) {
			LOGGER.severe("RequestSwitchList: " + e);
		} catch (IfmapException e) {
			LOGGER.severe("RequestSwitchList: " + e);
		} catch (JsonParseException e) {
			LOGGER.severe("RequestSwitchList: " + e);
		} catch (JsonMappingException e) {
			LOGGER.severe("RequestSwitchList: " + e);
		} catch (IOException e) {
			LOGGER.severe("RequestSwitchList: " + e);
		}

	}

	/**
	 * This helper method deletes old switch devices-ip, member-of and
	 * device-characteristic metadata
	 * 
	 * @throws IfmapErrorResult
	 *             if Error comes from map server
	 * @throws IfmapException
	 *             if general execption comes from ifmapj
	 * 
	 */
	private void deleteDeviceSwitchAndMetaData(SSRC ssrc, HashMap<String, String> switchesAndIpsToDelete)
			throws IfmapErrorResult, IfmapException {

		Iterator<Entry<String, String>> itrSwitchWithIp = switchesAndIpsToDelete.entrySet().iterator();
		Identifier extIdOpenflowGroup = Identifiers.createExtendedIdentity(getClass().getResourceAsStream(
				"/openflowGroup.xml"));

		while (itrSwitchWithIp.hasNext()) {
			Entry<String, String> entrySwitchIp = itrSwitchWithIp.next();

			Device devSwitch = Identifiers.createDev("Switch: " + entrySwitchIp.getKey());
			IpAddress ipSwitch = Identifiers.createIp4(entrySwitchIp.getValue());

			String filterDevIp = String.format("meta:device-ip[@ifmap-publisher-id='%s']", ssrc.getPublisherId());
			PublishDelete delDevIp = Requests.createPublishDelete(devSwitch, ipSwitch, filterDevIp);
			delDevIp.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
			ssrc.publish(Requests.createPublishReq(delDevIp));

			String filterGroupOf = String.format("meta:member-of[@ifmap-publisher-id='%s']", ssrc.getPublisherId());
			PublishDelete delGroupOf = Requests.createPublishDelete(devSwitch, extIdOpenflowGroup, filterGroupOf);
			delGroupOf.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IRONFLOW_METADATA_NS_URI);
			ssrc.publish(Requests.createPublishReq(delGroupOf));

			String filterChara = String.format("meta:device-characteristic[@ifmap-publisher-id='%s']",
					ssrc.getPublisherId());
			PublishDelete delChara = Requests.createPublishDelete(devSwitch, filterChara);
			delChara.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
			ssrc.publish(Requests.createPublishReq(delChara));
		}
	}

}
