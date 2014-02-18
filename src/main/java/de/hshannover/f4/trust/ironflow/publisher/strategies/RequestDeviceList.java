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
import java.util.ArrayList;
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
import de.fhhannover.inform.trust.ifmapj.identifier.Identifiers;
import de.fhhannover.inform.trust.ifmapj.identifier.IpAddress;
import de.fhhannover.inform.trust.ifmapj.identifier.MacAddress;
import de.fhhannover.inform.trust.ifmapj.messages.MetadataLifetime;
import de.fhhannover.inform.trust.ifmapj.messages.PublishDelete;
import de.fhhannover.inform.trust.ifmapj.messages.PublishUpdate;
import de.fhhannover.inform.trust.ifmapj.messages.Requests;
import de.hshannover.f4.trust.ironflow.Configuration;
import de.hshannover.f4.trust.ironflow.publisher.RequestStrategy;

/**
 * This class is the Implementation to Request the OpenflowController for all
 * connected devices
 * 
 * 
 * @author Marius Rohde
 * 
 */

public class RequestDeviceList extends RequestStrategy {

	private static final Logger LOGGER = Logger.getLogger(RequestDeviceList.class.getName());

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

		long expireTime = System.currentTimeMillis() - Configuration.ironflowDeviceExpireTime() * 60000;

		jsonString = this.getResponse(resourceWebTarget).readEntity(String.class);

		LOGGER.fine("json devicelist response: " + jsonString);

		try {

			rootNode = mapper.readValue(jsonString, JsonNode.class);

			for (JsonNode node : rootNode) {

				long lastSeen = node.path("lastSeen").getLongValue();

				if (lastSeen >= expireTime) { // newer than expireTime

					macsNew = fillMacArrayList(node);
					ipsNew = fillIpArrayList(node);
					vlansNew = fillVlanArrayList(node);
					devicesNew = fillDeviceArrayList(node);
					portsNew = fillPortArrayList(node);

					// At this time a Device can only have 1 mac and one port to
					// be connected
					if (macsNew.size() > 0 && devicesNew.size() > 0 && portsNew.size() > 0) {
						if (portsNew.get(0) > 0) { // Device publish only if
													// port not under null

							// only one mac -> only one discoverd by
							Document docDiscoByMac = getMetadataFactory().createDiscoveredBy();
							PublishUpdate publishDiscoByMac = Requests.createPublishUpdate(devicesNew.get(0),
									macsNew.get(0), docDiscoByMac, MetadataLifetime.session);
							ssrc.publish(Requests.createPublishReq(publishDiscoByMac));

							updateDeleteIps(ssrc, devicesNew, ipsNew, macsNew);

							updateDeleteVlans(ssrc, devicesNew, vlansNew, portsNew, macsNew);

						}
					}

					// clear data structures for next node
					macsNew.clear();
					ipsNew.clear();
					vlansNew.clear();
					devicesNew.clear();
					portsNew.clear();

				} else if (lastSeen < expireTime) { // older then expireTime

					macsOld = fillMacArrayList(node);
					ipsOld = fillIpArrayList(node);
					vlansOld = fillVlanArrayList(node);
					devicesOld = fillDeviceArrayList(node);
					portsOld = fillPortArrayList(node);

					if (macsOld.size() > 0 && devicesOld.size() > 0 && portsOld.size() > 0) {
						if (portsOld.get(0) > 0) {
							// Device publish only if
							// port not under null
							// del discovered by
							PublishDelete delDiscoByMac = Requests.createPublishDelete(devicesOld.get(0),
									macsOld.get(0), "meta:discovered-by[@ifmap-publisher-id='" + ssrc.getPublisherId()
											+ "']");
							delDiscoByMac.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX,
									IfmapStrings.STD_METADATA_NS_URI);
							ssrc.publish(Requests.createPublishReq(delDiscoByMac));

							deleteIps(ssrc, devicesOld, ipsOld, macsOld);

							deleteVlans(ssrc, devicesOld, vlansOld, portsOld, macsOld);

						}
					}

					// clear data structures for next node
					macsOld.clear();
					ipsOld.clear();
					vlansOld.clear();
					devicesOld.clear();
					portsOld.clear();
				}
			}

		} catch (JsonParseException e) {
			LOGGER.severe("RequestDeviceList: " + e);
		} catch (JsonMappingException e) {
			LOGGER.severe("RequestDeviceList: " + e);
		} catch (IOException e) {
			LOGGER.severe("RequestDeviceList: " + e);
		} catch (IfmapErrorResult e) {
			LOGGER.severe("RequestDeviceList: " + e);
		} catch (IfmapException e) {
			LOGGER.severe("RequestDeviceList: " + e);
		}

	}

	/**
	 * This helper method fills the mac adresses of the rest response in an
	 * arraylist of IFMAP mac adresses.
	 * 
	 * @return Arraylist with the if-map mac-adresses
	 */
	private ArrayList<MacAddress> fillMacArrayList(JsonNode node) {

		ArrayList<MacAddress> macs = new ArrayList<MacAddress>();

		JsonNode nodeMacs = node.path("mac");
		for (JsonNode nodeMacItr : nodeMacs) {
			macs.add(Identifiers.createMac(nodeMacItr.getTextValue()));
		}
		return macs;
	}

	/**
	 * This helper method fills the ip adresses of the rest response in an
	 * arraylist of IFMAP ip adresses
	 * 
	 * @return Arraylist with the if-map ip-adresses
	 */
	private ArrayList<IpAddress> fillIpArrayList(JsonNode node) {

		ArrayList<IpAddress> ips = new ArrayList<IpAddress>();

		JsonNode nodeIps = node.path("ipv4");
		for (JsonNode nodeIpItr : nodeIps) {
			ips.add(Identifiers.createIp4(nodeIpItr.getTextValue()));
		}
		return ips;
	}

	/**
	 * This helper method fills the vlan numbers of the rest response in an
	 * integer arraylist
	 * 
	 * @return Arraylist with the vlan ids
	 */
	private ArrayList<Integer> fillVlanArrayList(JsonNode node) {

		ArrayList<Integer> vlans = new ArrayList<Integer>();

		JsonNode nodeVlan = node.path("vlan");
		for (JsonNode nodeVlanItr : nodeVlan) {
			vlans.add(nodeVlanItr.getIntValue());
		}
		return vlans;
	}

	/**
	 * This helper method fills the dpid numbers of the rest response in an
	 * arraylist of IFMAP devices
	 * 
	 * @return Arraylist with the if-map devices
	 */
	private ArrayList<Device> fillDeviceArrayList(JsonNode node) {

		ArrayList<Device> devices = new ArrayList<Device>();

		JsonNode nodeAttachmentPoint = node.path("attachmentPoint");
		for (JsonNode nodeAttachmentPointItr : nodeAttachmentPoint) {
			devices.add(Identifiers.createDev("Switch: " + nodeAttachmentPointItr.path("switchDPID").getTextValue()));
		}
		return devices;
	}

	/**
	 * This helper method fills the port numbers of the rest response in an
	 * interger arraylist
	 * 
	 * @return Arraylist with the port numbers
	 */
	private ArrayList<Integer> fillPortArrayList(JsonNode node) {

		ArrayList<Integer> ports = new ArrayList<Integer>();

		JsonNode nodeAttachmentPoint = node.path("attachmentPoint");
		for (JsonNode nodeAttachmentPointItr : nodeAttachmentPoint) {
			ports.add(nodeAttachmentPointItr.path("port").getIntValue());
		}
		return ports;
	}

	/**
	 * This helper method deletes double ip-mac and discovered-by metadata and
	 * creates new ones
	 * 
	 * @throws IfmapErrorResult
	 *             if Error comes from map server
	 * @throws IfmapException
	 *             if general execption comes from ifmapj
	 * 
	 */
	private void updateDeleteIps(SSRC ssrc, ArrayList<Device> devicesNew, ArrayList<IpAddress> ipsNew,
			ArrayList<MacAddress> macsNew) throws IfmapErrorResult, IfmapException {

		for (int j = 0; j < ipsNew.size(); j++) {

			// del ip-mac
			PublishDelete del = Requests.createPublishDelete(ipsNew.get(j), macsNew.get(0),
					"meta:ip-mac[@ifmap-publisher-id='" + ssrc.getPublisherId() + "']");
			del.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
			ssrc.publish(Requests.createPublishReq(del));
			// del discovered by
			PublishDelete delDiscoByIp = Requests.createPublishDelete(ipsNew.get(j), devicesNew.get(0),
					"meta:discovered-by[@ifmap-publisher-id='" + ssrc.getPublisherId() + "']");
			delDiscoByIp.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
			ssrc.publish(Requests.createPublishReq(delDiscoByIp));
			// add ip-mac
			Document docIpMac = getMetadataFactory().createIpMac();
			PublishUpdate publishIpMac = Requests.createPublishUpdate(ipsNew.get(j), macsNew.get(0), docIpMac,
					MetadataLifetime.session);
			ssrc.publish(Requests.createPublishReq(publishIpMac));
			// add discovered by
			Document docDiscoByIp = getMetadataFactory().createDiscoveredBy();
			PublishUpdate publishDiscoByIp = Requests.createPublishUpdate(devicesNew.get(0), ipsNew.get(j),
					docDiscoByIp, MetadataLifetime.session);
			ssrc.publish(Requests.createPublishReq(publishDiscoByIp));
		}

	}

	/**
	 * This helper method deletes double vlan numbers (layer2) metadata and
	 * creates new ones
	 * 
	 * @throws IfmapErrorResult
	 *             if Error comes from map server
	 * @throws IfmapException
	 *             if general execption comes from ifmapj
	 */
	private void updateDeleteVlans(SSRC ssrc, ArrayList<Device> devicesNew, ArrayList<Integer> vlansNew,
			ArrayList<Integer> portsNew, ArrayList<MacAddress> macsNew) throws IfmapErrorResult, IfmapException {

		if (vlansNew.size() == 0) {
			// del layer2
			String filter = String.format("meta:layer2-information[@ifmap-publisher-id='%s' and port='%s']",
					ssrc.getPublisherId(), portsNew.get(0));
			PublishDelete del = Requests.createPublishDelete(devicesNew.get(0), macsNew.get(0), filter);
			del.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
			ssrc.publish(Requests.createPublishReq(del));

			// add layer2 not specific conform
			Document docLayer2 = getMetadataFactory().createLayer2Information(null, null, portsNew.get(0), null);
			PublishUpdate publishLayer2 = Requests.createPublishUpdate(devicesNew.get(0), macsNew.get(0), docLayer2,
					MetadataLifetime.session);
			ssrc.publish(Requests.createPublishReq(publishLayer2));
		}

		// with vlans
		for (int j = 0; j < vlansNew.size(); j++) {
			// del layer 2
			String filter = String.format(
					"meta:layer2-information[@ifmap-publisher-id='%s' and vlan='%s' and port='%s']",
					ssrc.getPublisherId(), vlansNew.get(j), portsNew.get(j));
			PublishDelete del = Requests.createPublishDelete(devicesNew.get(0), macsNew.get(0), filter);
			del.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
			ssrc.publish(Requests.createPublishReq(del));
			// add layer2 not specific conform
			Document docLayer2 = getMetadataFactory().createLayer2Information(vlansNew.get(0), null, portsNew.get(j),
					null);
			PublishUpdate publishLayer2 = Requests.createPublishUpdate(devicesNew.get(0), macsNew.get(0), docLayer2,
					MetadataLifetime.session);
			ssrc.publish(Requests.createPublishReq(publishLayer2));
		}

	}

	/**
	 * This helper method deletes old ip-mac and discovered-by metadata
	 * 
	 * @throws IfmapErrorResult
	 *             if Error comes from map server
	 * @throws IfmapException
	 *             if general execption comes from ifmapj
	 */
	private void deleteIps(SSRC ssrc, ArrayList<Device> devicesOld, ArrayList<IpAddress> ipsOld,
			ArrayList<MacAddress> macsOld) throws IfmapErrorResult, IfmapException {

		for (int j = 0; j < ipsOld.size(); j++) {

			// del ip-mac
			PublishDelete del = Requests.createPublishDelete(ipsOld.get(j), macsOld.get(0),
					"meta:ip-mac[@ifmap-publisher-id='" + ssrc.getPublisherId() + "']");
			del.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
			ssrc.publish(Requests.createPublishReq(del));

			// del discovered by
			String filter = String.format("meta:discovered-by[@ifmap-publisher-id='%s']", ssrc.getPublisherId());
			PublishDelete delDiscoByIp = Requests.createPublishDelete(ipsOld.get(j), devicesOld.get(0), filter);
			delDiscoByIp.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
			ssrc.publish(Requests.createPublishReq(delDiscoByIp));
		}

	}

	/**
	 * This helper method deletes old layer 2 metadata
	 * 
	 * @throws IfmapErrorResult
	 *             if Error comes from map server
	 * @throws IfmapException
	 *             if general execption comes from ifmapj
	 */
	private void deleteVlans(SSRC ssrc, ArrayList<Device> devicesOld, ArrayList<Integer> vlansOld,
			ArrayList<Integer> portsOld, ArrayList<MacAddress> macsOld) throws IfmapErrorResult, IfmapException {

		// with vlans delete
		for (int j = 0; j < vlansOld.size(); j++) {

			String filter = String.format(
					"meta:layer2-information[@ifmap-publisher-id='%s' and vlan='%s' and port='%s']",
					ssrc.getPublisherId(), vlansOld.get(j), portsOld.get(j));
			PublishDelete del = Requests.createPublishDelete(devicesOld.get(0), macsOld.get(0), filter);
			del.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
			ssrc.publish(Requests.createPublishReq(del));
		}

		// no vlans delete
		if (vlansOld.size() == 0) {

			String filter = String.format("meta:layer2-information[@ifmap-publisher-id='%s' and port='%s']",
					ssrc.getPublisherId(), portsOld.get(0));
			PublishDelete del = Requests.createPublishDelete(devicesOld.get(0), macsOld.get(0), filter);
			del.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);
			ssrc.publish(Requests.createPublishReq(del));
		}

	}

}
