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

package de.hshannover.f4.trust.ironflow.subscriber;

import java.util.List;
import java.util.TimerTask;
import java.util.logging.Logger;

import de.hshannover.f4.trust.ifmapj.channel.ARC;
import de.hshannover.f4.trust.ifmapj.exception.CommunicationException;
import de.hshannover.f4.trust.ifmapj.exception.EndSessionException;
import de.hshannover.f4.trust.ifmapj.exception.IfmapErrorResult;
import de.hshannover.f4.trust.ifmapj.exception.IfmapException;
import de.hshannover.f4.trust.ifmapj.messages.PollResult;
import de.hshannover.f4.trust.ifmapj.messages.SearchResult;
import de.hshannover.f4.trust.ifmapj.messages.SearchResult.Type;
import de.hshannover.f4.trust.ironflow.utilities.IfMap;

/**
 * This class pools the Ifmap Server for the request for investigation metadata
 * and calls the function in the subscriber strategies to set the firewall of
 * the Floodlightcontroller
 * 
 * @author Marius Rohde
 * 
 */

public class SubscriberThread extends TimerTask {

	private static final Logger LOGGER = Logger.getLogger(SubscriberThread.class.getName());

	@Override
	public void run() {

		for (int i = 0; i < SubscriberChainBuilder.getSize(); i++) {
			SubscriberChainBuilder.getElementAt(i).initSubscriber();
		}

		ARC arc = IfMap.getArc();
		try {
			while (!Thread.currentThread().isInterrupted()) {
				LOGGER.info("polling for targets ...");

				PollResult pollResult;
				pollResult = arc.poll();

				if (pollResult.getResults().size() > 0) {
					List<SearchResult> results = pollResult.getResults();
					for (SearchResult searchResult : results) {
						if (searchResult.getType() == Type.searchResult) {
							LOGGER.finer("processing searchResult ...");
							for (int i = 0; i < SubscriberChainBuilder.getSize(); i++) {
								SubscriberChainBuilder.getElementAt(i).executeOpenflowFirewallStrategy(searchResult);
							}
						} else if (searchResult.getType() == Type.updateResult) {
							LOGGER.finer("processing updateResult ...");
							for (int i = 0; i < SubscriberChainBuilder.getSize(); i++) {
								SubscriberChainBuilder.getElementAt(i).executeOpenflowFirewallStrategy(searchResult);
							}
						} else if (searchResult.getType() == Type.notifyResult) {
							LOGGER.finer("processing notifyResult ...");
							for (int i = 0; i < SubscriberChainBuilder.getSize(); i++) {
								SubscriberChainBuilder.getElementAt(i).executeOpenflowFirewallStrategy(searchResult);
							}
						} else if (searchResult.getType() == Type.deleteResult) {
							LOGGER.finer("processing deleteResult ...");
							for (int i = 0; i < SubscriberChainBuilder.getSize(); i++) {
								SubscriberChainBuilder.getElementAt(i).deleteOpenflowFirewallStrategy(searchResult);
							}
						}
					}
				}
			}
		} catch (IfmapErrorResult e) {
			LOGGER.severe("SubscriberThread: " + e);
		} catch (EndSessionException e) {
			LOGGER.warning("SubscriberThread: session ended during poll " + e);
		} catch (CommunicationException e) {
			LOGGER.severe("SubscriberThread: " + e);
		} catch (IfmapException e) {
			LOGGER.severe("SubscriberThread: " + e);
		}

	}

}
