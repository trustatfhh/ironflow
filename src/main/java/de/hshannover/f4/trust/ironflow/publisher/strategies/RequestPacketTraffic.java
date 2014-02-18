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
import de.hshannover.f4.trust.ironflow.publisher.RequestStrategy;

/**
 * This class is the implementation to request the OpenflowController for all
 * packets transferred
 * 
 * 
 * @author Marius Rohde
 * 
 */

public class RequestPacketTraffic extends RequestStrategy {

	private static final Logger LOGGER = Logger.getLogger(RequestPacketTraffic.class.getName());

	// packet count received and transmitted
	private HashMap<String, Long> mMacsAndRxData = new HashMap<String, Long>();
	private HashMap<String, Long> mMacsAndTxData = new HashMap<String, Long>();

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

		LOGGER.fine("json traffic response string:" + jsonStringTraffic);
		LOGGER.finer("json devices response string:" + jsonStringSwitches);

		DateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ'Z'");
		Date now = new Date(System.currentTimeMillis());

		long expireTime = System.currentTimeMillis() - Configuration.ironflowDeviceExpireTime() * 60000;

		HashMap<String, Long> macsAndRxDataOld = new HashMap<String, Long>();
		HashMap<String, Long> macsAndTxDataOld = new HashMap<String, Long>();

		macsAndRxDataOld.putAll(mMacsAndRxData);
		macsAndTxDataOld.putAll(mMacsAndTxData);

		// clear Rx and Tx count to fill it witch the new data
		mMacsAndRxData.clear();
		mMacsAndTxData.clear();

		try {

			rootNodeTraffic = mapper.readValue(jsonStringTraffic, JsonNode.class);
			rootNodeSwitches = mapper.readValue(jsonStringSwitches, JsonNode.class);
			Iterator<Entry<String, JsonNode>> itrTraffic = rootNodeTraffic.getFields();

			while (itrTraffic.hasNext()) {
				Entry<String, JsonNode> trafficEntry = itrTraffic.next();

				JsonNode trafficNode = trafficEntry.getValue();

				for (JsonNode node : trafficNode) {
					int portNrTraffic = node.path("portNumber").getIntValue();
					if (portNrTraffic > 0) {
						long rxPackets = node.path("receivePackets").getLongValue();
						long txPackets = node.path("transmitPackets").getLongValue();

						for (JsonNode nodeSwitches : rootNodeSwitches) {

							JsonNode nodeMacs = nodeSwitches.path("mac");
							JsonNode nodeAttachmentPoint = nodeSwitches.path("attachmentPoint");
							long lastSeen = nodeSwitches.path("lastSeen").getLongValue();

							if (lastSeen >= expireTime) {
								for (JsonNode nodeAttachmentPointItr : nodeAttachmentPoint) {
									int portNrSwitch = nodeAttachmentPointItr.path("port").getIntValue();
									String dpid = nodeAttachmentPointItr.path("switchDPID").getTextValue();

									if (portNrTraffic == portNrSwitch && dpid.equals(trafficEntry.getKey())) {

										for (JsonNode nodeMacItr : nodeMacs) {

											MacAddress macHost = Identifiers.createMac(nodeMacItr.getTextValue());

											mMacsAndRxData.put(nodeMacItr.getTextValue(), rxPackets);
											mMacsAndTxData.put(nodeMacItr.getTextValue(), txPackets);

											// TODO
											// Delete Events is Lifetime is
											// session could be modified to
											// notify Update
											// then you dont need to delete
											// events
											String filterTxRxEvents = String.format(
													"meta:event[@ifmap-publisher-id='%s']", ssrc.getPublisherId());
											PublishDelete delTxRxEvents = Requests.createPublishDelete(macHost,
													filterTxRxEvents);
											delTxRxEvents.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
													IfmapStrings.STD_METADATA_NS_URI);
											ssrc.publish(Requests.createPublishReq(delTxRxEvents));

											// Transmitted packets per interval
											long txPacketsInterval = 0;
											if (macsAndTxDataOld.containsKey(nodeMacItr.getTextValue())) {
												txPacketsInterval = txPackets
														- macsAndTxDataOld.get(nodeMacItr.getTextValue());
											}
											Document eventTxMac = getMetadataFactory().createEvent("TxTraffic",
													dfmt.format(now), ssrc.getPublisherId(), 1, 100,
													Significance.informational, EventType.p2p, "",
													"TxPackets: " + txPacketsInterval, "");
											PublishUpdate publishEventTxMac = Requests.createPublishUpdate(macHost,
													eventTxMac, MetadataLifetime.session);
											ssrc.publish(Requests.createPublishReq(publishEventTxMac));

											// Recieved packets per interval
											long rxPacketsInterval = 0;
											if (macsAndRxDataOld.containsKey(nodeMacItr.getTextValue())) {
												rxPacketsInterval = rxPackets
														- macsAndRxDataOld.get(nodeMacItr.getTextValue());
											}
											Document eventRxMac = getMetadataFactory().createEvent("RxTraffic",
													dfmt.format(now), ssrc.getPublisherId(), 1, 100,
													Significance.informational, EventType.p2p, "",
													"RxPackets: " + rxPacketsInterval, "");
											PublishUpdate publishEventRxMac = Requests.createPublishUpdate(macHost,
													eventRxMac, MetadataLifetime.session);
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
			LOGGER.severe("RequestPacketTraffic: " + e);
		} catch (JsonMappingException e) {
			LOGGER.severe("RequestPacketTraffic: " + e);
		} catch (IOException e) {
			LOGGER.severe("RequestPacketTraffic: " + e);
		} catch (IfmapErrorResult e) {
			LOGGER.severe("RequestPacketTraffic: " + e);
		} catch (IfmapException e) {
			LOGGER.severe("RequestPacketTraffic: " + e);
		}

	}

}
