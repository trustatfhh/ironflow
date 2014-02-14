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

package de.hshannover.f4.trust.ironflow.subscriber;

import java.util.List;
import java.util.TimerTask;
import java.util.logging.Logger;

import de.fhhannover.inform.trust.ifmapj.binding.IfmapStrings;
import de.fhhannover.inform.trust.ifmapj.channel.ARC;
import de.fhhannover.inform.trust.ifmapj.exception.CommunicationException;
import de.fhhannover.inform.trust.ifmapj.exception.EndSessionException;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapErrorResult;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapException;
import de.fhhannover.inform.trust.ifmapj.identifier.Device;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifiers;
import de.fhhannover.inform.trust.ifmapj.messages.PollResult;
import de.fhhannover.inform.trust.ifmapj.messages.Requests;
import de.fhhannover.inform.trust.ifmapj.messages.SearchResult;
import de.fhhannover.inform.trust.ifmapj.messages.SearchResult.Type;
import de.fhhannover.inform.trust.ifmapj.messages.SubscribeRequest;
import de.fhhannover.inform.trust.ifmapj.messages.SubscribeUpdate;
import de.hshannover.f4.trust.ironflow.Configuration;
import de.hshannover.f4.trust.ironflow.utilities.IfMap;

public class SubscriberThread extends TimerTask {

	private static final Logger LOGGER = Logger.getLogger(SubscriberThread.class.getName());

	/**
	 * Helper Methode to initialize the subcriber function
	 * 
	 */
	private void initSubscriber() {

		LOGGER.fine("subscribing for " + Configuration.subscriberPdp());

		Device pdpIdentifier = Identifiers.createDev(Configuration.subscriberPdp());

		SubscribeRequest subscribeRequest = Requests.createSubscribeReq();
		SubscribeUpdate subscribeUpdate = Requests.createSubscribeUpdate();
		subscribeUpdate.setName("ironflow-subscriber");
		subscribeUpdate.setMatchLinksFilter("meta:request-for-investigation");
		subscribeUpdate.setMaxDepth(1);
		subscribeUpdate.setStartIdentifier(pdpIdentifier);

		subscribeUpdate.addNamespaceDeclaration(IfmapStrings.BASE_PREFIX, IfmapStrings.BASE_NS_URI);
		subscribeUpdate.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);

		subscribeRequest.addSubscribeElement(subscribeUpdate);

		try {
			IfMap.getSsrc().subscribe(subscribeRequest);

		} catch (IfmapErrorResult e) {
			LOGGER.severe("SubscriberThread: " + e);
		} catch (IfmapException e) {
			LOGGER.severe("SubscriberThread: " + e);
		}
	}

	@Override
	public void run() {

		initSubscriber();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				LOGGER.info("polling for targets ...");

				ARC arc = IfMap.getArc();
				PollResult pollResult;
				pollResult = arc.poll();
				arc.closeTcpConnection();

				if (pollResult.getResults().size() > 0) {
					List<SearchResult> results = pollResult.getResults();
					for (SearchResult r : results) {
						if (r.getType() == Type.searchResult) {
							LOGGER.finer("processing searchResult ...");

						} else if (r.getType() == Type.updateResult) {
							LOGGER.finer("processing updateResult ...");

						} else if (r.getType() == Type.notifyResult) {
							LOGGER.finer("processing notifyResult ...");

						} else if (r.getType() == Type.deleteResult) {
							LOGGER.finer("processing deleteResult ...");

						}
					}
				}
			}
		} catch (IfmapErrorResult e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (EndSessionException e) {
			LOGGER.warning("SubscriberThread: session ended during poll " + e);
		} catch (CommunicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IfmapException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
