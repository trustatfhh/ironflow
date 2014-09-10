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
 * This file is part of ironflow, version 0.0.3, implemented by the Trust@HsH
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
package de.hshannover.f4.trust.ironflow.subscriber.strategies;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import de.hshannover.f4.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ifmapj.identifier.IpAddress;
import de.hshannover.f4.trust.ifmapj.identifier.MacAddress;
import de.hshannover.f4.trust.ironflow.subscriber.SubscriberStrategy;

/**
 * This class is the implementation to block client traffic if a request for
 * investigation request occurs
 * 
 * 
 * @author Marius Rohde
 * 
 */

public class BlockClientTrafficSubscriber extends SubscriberStrategy {

	private static final Logger LOGGER = Logger.getLogger(BlockClientTrafficSubscriber.class.getName());

	private static final String SUBSCRIBERNAME = "Openflow_BLOCK";

	private static final String SUBSCRIBERFILTER = "meta:request-for-investigation[@qualifier='Openflow_BLOCK']";

	private HashMap<Identifier, Integer> mRuleids = new HashMap<Identifier, Integer>();

	@Override
	protected String getSubscriberName() {
		return SUBSCRIBERNAME;
	}

	@Override
	protected String getSubscriberFilter() {
		return SUBSCRIBERFILTER;
	}

	@Override
	protected void executeFirewallSettings(Identifier[] switchMacIp) {

		Identifier ipMac = switchMacIp[1];
		String firewallRule = null;

		if (ipMac instanceof IpAddress) {
			firewallRule = "{\"src-ip\": \"" + ((IpAddress) ipMac).getValue() + "\", \"action\": \"DENY\" }";
		} else if (ipMac instanceof MacAddress) {
			firewallRule = "{\"src-mac\": \"" + ((MacAddress) ipMac).getValue() + "\", \"action\": \"DENY\" }";
		}

		if (firewallRule != null) {

			LOGGER.fine("Try to insert firewall rule: " + firewallRule);

			if (checkFirewallStatus()) {

				ObjectMapper mapper = new ObjectMapper();

				WebTarget webTargetRules = getWebTarget().path("/wm/firewall/rules/json");
				Invocation.Builder invocationBuilderRules = webTargetRules.request(MediaType.TEXT_PLAIN_TYPE);
				invocationBuilderRules.header("some-header", "true");

				Response responseRule = invocationBuilderRules.post(Entity.entity(firewallRule, MediaType.TEXT_PLAIN));

				String jsonStringResponseRule = responseRule.readEntity(String.class);

				try {

					JsonNode rootNode = mapper.readValue(jsonStringResponseRule, JsonNode.class);
					JsonNode statusNode = rootNode.path("status");

					if (statusNode.getTextValue().equals("Rule added")) {
						LOGGER.fine("Firewall rule inserted succesful!");
						mRuleids.put(ipMac, getLastFirewallId());
					} else {
						LOGGER.warning(statusNode.getTextValue());
					}

				} catch (JsonParseException e) {
					LOGGER.warning("" + e);
				} catch (JsonMappingException e) {
					LOGGER.warning("" + e);
				} catch (IOException e) {
					LOGGER.warning("" + e);
				}

			}
		} else {
			LOGGER.warning("Empty Identifier");
		}
	}

	@Override
	protected void deleteFirewallSettings(Identifier[] switchMacIp) {

		Identifier ipMac = switchMacIp[1];
		String deleteString = null;

		WebTarget webTarget = getWebTarget().path("/wm/firewall/rules/json");

		Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN_TYPE);
		invocationBuilder.header("some-header", "true");
		invocationBuilder.header("X-HTTP-Method-Override", "DELETE");

		if (ipMac instanceof IpAddress) {
			IpAddress ip = (IpAddress) ipMac;
			Iterator<Entry<Identifier, Integer>> itrRuleIds = mRuleids.entrySet().iterator();
			while (itrRuleIds.hasNext()) {
				Entry<Identifier, Integer> ruleIdsEntry = itrRuleIds.next();
				if (ruleIdsEntry.getKey() instanceof IpAddress) {
					IpAddress ipEntry = (IpAddress) ruleIdsEntry.getKey();
					if (ipEntry.getValue().equals(ip.getValue())) {
						deleteString = "{\"ruleid\":\"" + ruleIdsEntry.getValue() + "\"}";
						itrRuleIds.remove();
					}
				}
			}
		} else if (ipMac instanceof MacAddress) {
			MacAddress ip = (MacAddress) ipMac;
			Iterator<Entry<Identifier, Integer>> itrRuleIds = mRuleids.entrySet().iterator();

			while (itrRuleIds.hasNext()) {
				Entry<Identifier, Integer> ruleIdsEntry = itrRuleIds.next();
				if (ruleIdsEntry.getKey() instanceof MacAddress) {
					MacAddress ipEntry = (MacAddress) ruleIdsEntry.getKey();
					if (ipEntry.getValue().equals(ip.getValue())) {
						deleteString = "{\"ruleid\":\"" + ruleIdsEntry.getValue() + "\"}";
						itrRuleIds.remove();
					}
				}
			}
		}

		if (deleteString != null) {
			LOGGER.fine("Try to delete Rule");
			Response response = invocationBuilder.post(Entity.entity(deleteString, MediaType.TEXT_PLAIN));
			if (response.getStatus() == 200) {
				LOGGER.fine("Rule deleted");
			}
		} else {
			LOGGER.warning("No rule id for this Identifier found");
		}

	}

	/**
	 * helper methode to request the floodlight controller if the firewall is
	 * enabled
	 * 
	 * @return boolean enabled(true) or disabled(false) or failure(false)
	 */
	private boolean checkFirewallStatus() {

		ObjectMapper mapper = new ObjectMapper();

		WebTarget webTargetStatus = getWebTarget().path("/wm/firewall/module/status/json");
		Invocation.Builder invocationBuilderStatus = webTargetStatus.request(MediaType.TEXT_PLAIN_TYPE);
		invocationBuilderStatus.header("some-header", "true");

		Response responseStatus = invocationBuilderStatus.get();
		String jsonStringResponseStatus = responseStatus.readEntity(String.class);

		try {
			JsonNode rootNode = mapper.readValue(jsonStringResponseStatus, JsonNode.class);

			JsonNode resultNode = rootNode.path("result");
			LOGGER.fine("Firewall status: " + resultNode.getTextValue());

			if (resultNode.getTextValue().equals("firewall enabled")) {
				return true;
			}

		} catch (JsonParseException e) {
			LOGGER.warning("" + e);
		} catch (JsonMappingException e) {
			LOGGER.warning("" + e);
		} catch (IOException e) {
			LOGGER.warning("" + e);
		}
		return false;
	}

	/**
	 * Dreckige methode zum rausfinden der letzten hinzugefuegten Regel im
	 * floodlight controller, da floodlight die rule id nicht im post response
	 * zurueck schickt!!!!! Hierdurch koennen raceconditions entstehen wenn
	 * viele andere programme/menschen regeln in den controller einfuegen
	 * 
	 * Sofort umbauen wenn ruleid in response enthalten!!!!!!!!
	 * 
	 * @return int value of last rule id in openflow controller
	 */
	private int getLastFirewallId() {

		ObjectMapper mapper = new ObjectMapper();

		WebTarget webTargetRules = getWebTarget().path("/wm/firewall/rules/json");
		Invocation.Builder invocationBuilderRules = webTargetRules.request(MediaType.TEXT_PLAIN_TYPE);
		invocationBuilderRules.header("some-header", "true");

		Response responseRules = invocationBuilderRules.get();
		String jsonStringResponseRules = responseRules.readEntity(String.class);

		try {
			JsonNode rootNode = mapper.readValue(jsonStringResponseRules, JsonNode.class);

			JsonNode firstNode = rootNode.get(0);
			JsonNode ruleIdNode = firstNode.path("ruleid");

			return ruleIdNode.getIntValue();

		} catch (JsonParseException e) {
			LOGGER.warning("" + e);
		} catch (JsonMappingException e) {
			LOGGER.warning("" + e);
		} catch (IOException e) {
			LOGGER.warning("" + e);
		}

		return 0;
	}

}
