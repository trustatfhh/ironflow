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
package de.hshannover.f4.trust.ironflow.subscriber.strategies;

import java.util.HashMap;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifier;
import de.hshannover.f4.trust.ironflow.subscriber.SubscriberStrategy;
import de.hshannover.f4.trust.ironflow.utilities.IfMap;

public class BlockClientTrafficSubscriber extends SubscriberStrategy {

	private static final String SUBSCRIBERNAME = "ironflow-subscriber-" + IfMap.getSsrc().getPublisherId()
			+ "-Openflow_BLOCK";

	private static final String SUBSCRIBERFILTER = "meta:request-for-investigation[@qualifier='Openflow_BLOCK']";

	private HashMap<Identifier, Integer> mRuleids;

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

		String firewallRule = ""; // "{"src-ip": "10.0.0.3/32", "dst-ip": "10.0.0.7/32\"}";

		WebTarget webTargetStatus = getWebTarget().path("/wm/firewall/module/status/json");
		Invocation.Builder invocationBuilderStatus = webTargetStatus.request(MediaType.TEXT_PLAIN_TYPE);
		invocationBuilderStatus.header("some-header", "true");

		Response responseStatus = invocationBuilderStatus.get();

		if (responseStatus.getStatus() == 200) {
			WebTarget webTargetRules = getWebTarget().path("/wm/firewall/rules/json");
			Invocation.Builder invocationBuilderRules = webTargetRules.request(MediaType.TEXT_PLAIN_TYPE);
			invocationBuilderRules.header("some-header", "true");

			Response responseRule = invocationBuilderRules.post((Entity.entity(firewallRule, MediaType.TEXT_PLAIN)));
			responseRule.getStatus();
		}
	}

	@Override
	protected void deleteFirewallSettings(Identifier[] switchMacIp) {

		WebTarget webTarget = getWebTarget().path("/wm/firewall/rules/json");
		Invocation.Builder invocationBuilder = webTarget.request(MediaType.TEXT_PLAIN_TYPE);
		invocationBuilder.header("some-header", "true");
		Response response = invocationBuilder.delete();
		response.getStatus();

		/*
		 * curl http://localhost:8080/wm/device/
		 * 
		 * curl http://localhost:8080/wm/firewall/module/status/json
		 * 
		 * curl http://localhost:8080/wm/firewall/module/enable/json
		 * 
		 * curl http://localhost:8080/wm/firewall/module/disable/json
		 * 
		 * curl -X POST -d '{"switchid": "00:00:00:00:00:00:00:03"}'
		 * http://localhost:8080/wm/firewall/rules/json
		 * 
		 * curl -X DELETE -d '{"ruleid":"42197424"}'
		 * http://localhost:8080/wm/firewall/rules/json
		 * 
		 * curl -X POST -d '{"src-ip": "10.0.0.3/32", "dst-ip": "10.0.0.7/32"}'
		 * http://localhost:8080/wm/firewall/rules/json
		 */

	}

}
