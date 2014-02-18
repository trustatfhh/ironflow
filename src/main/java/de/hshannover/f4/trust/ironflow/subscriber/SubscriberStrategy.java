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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import de.fhhannover.inform.trust.ifmapj.binding.IfmapStrings;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapErrorResult;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapException;
import de.fhhannover.inform.trust.ifmapj.identifier.Device;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifier;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifiers;
import de.fhhannover.inform.trust.ifmapj.identifier.IpAddress;
import de.fhhannover.inform.trust.ifmapj.identifier.MacAddress;
import de.fhhannover.inform.trust.ifmapj.messages.Requests;
import de.fhhannover.inform.trust.ifmapj.messages.ResultItem;
import de.fhhannover.inform.trust.ifmapj.messages.SearchRequest;
import de.fhhannover.inform.trust.ifmapj.messages.SearchResult;
import de.fhhannover.inform.trust.ifmapj.messages.SubscribeRequest;
import de.fhhannover.inform.trust.ifmapj.messages.SubscribeUpdate;
import de.fhhannover.inform.trust.ifmapj.messages.SearchResult.Type;
import de.hshannover.f4.trust.ironflow.Configuration;
import de.hshannover.f4.trust.ironflow.utilities.IfMap;

public abstract class SubscriberStrategy {

	private static final Logger LOGGER = Logger.getLogger(SubscriberStrategy.class.getName());

	/**
	 * Helper method to initialize the subscriber function
	 * 
	 */
	public void initSubscriber() {

		LOGGER.fine("subscribing for " + Configuration.subscriberPdp());

		Device pdpIdentifier = Identifiers.createDev(Configuration.subscriberPdp());

		SubscribeRequest subscribeRequest = Requests.createSubscribeReq();
		SubscribeUpdate subscribeUpdate = Requests.createSubscribeUpdate();
		subscribeUpdate.setName("ironflow-subscriber-Openflow_BLOCK");
		subscribeUpdate.setMatchLinksFilter("meta:request-for-investigation[@qualifier='Openflow_BLOCK']");
		subscribeUpdate.setMaxDepth(1);
		subscribeUpdate.setStartIdentifier(pdpIdentifier);

		subscribeUpdate.addNamespaceDeclaration(IfmapStrings.BASE_PREFIX, IfmapStrings.BASE_NS_URI);
		subscribeUpdate.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);

		subscribeRequest.addSubscribeElement(subscribeUpdate);

		try {
			IfMap.getSsrc().subscribe(subscribeRequest);
		} catch (IfmapErrorResult e) {
			LOGGER.severe("SubscriberStrategy: " + e);
		} catch (IfmapException e) {
			LOGGER.severe("SubscriberStrategy: " + e);
		}
	}

	/**
	 * Method to execute the OpenflowFirewallStrategie
	 * 
	 */
	public void executeOpenflowFirewallStrategy(List<ResultItem> resultItems) {

		for (ResultItem resultItem : resultItems) {
			Identifier[] switchMacIp = searchMacIpDiscoveredBySwitch(resultItem);
		}

	}

	/**
	 * Method to delete the OpenflowFirewallStrategie
	 * 
	 */
	public void deleteOpenflowFirewallStrategy(List<ResultItem> resultItems) {

		for (ResultItem resultItem : resultItems) {
			Identifier[] switchMacIp = searchMacIpDiscoveredBySwitch(resultItem);
		}

	}

	/**
	 * Helper method to clean the searchResult from the empty ResultItems
	 * 
	 * @return the ArrayList of ResultItems from a SearchResult
	 */
	private List<ResultItem> cleanEmptySearchResult(SearchResult searchResults) {
		List<ResultItem> resultItems = new ArrayList<ResultItem>();

		for (ResultItem resultItem : searchResults.getResultItems()) {
			if (!resultItem.getMetadata().isEmpty()) {
				resultItems.add(resultItem);
			}
		}

		return resultItems;
	}

	/**
	 * Search for the discoverer switch of the discovered mac or ip
	 * 
	 * @return the Device Switch which discovered the mac or ip Adress
	 */
	private Identifier[] searchMacIpDiscoveredBySwitch(ResultItem resultItem) {

		Identifier[] switchMacIp = new Identifier[2]; // Index 0 = Device Index
														// 1 = Mac or Ip
		Identifier identPosi1;
		Identifier identPosi2;

		SearchRequest searchRequest = Requests.createSearchReq();
		searchRequest.setMatchLinksFilter("meta:discovered-by[@ifmap-publisher-id='" + IfMap.getSsrc().getPublisherId()
				+ "']");
		searchRequest.setMaxDepth(1);

		identPosi1 = resultItem.getIdentifier1();
		identPosi2 = resultItem.getIdentifier2();

		if (identPosi1 instanceof MacAddress || identPosi1 instanceof IpAddress) {
			searchRequest.setStartIdentifier(identPosi1);
			switchMacIp[1] = identPosi1;
		} else if (identPosi2 instanceof MacAddress || identPosi2 instanceof IpAddress) {
			searchRequest.setStartIdentifier(identPosi2);
			switchMacIp[1] = identPosi2;
		}

		if (switchMacIp[1] != null) {

			searchRequest.addNamespaceDeclaration(IfmapStrings.BASE_PREFIX, IfmapStrings.BASE_NS_URI);
			searchRequest.addNamespaceDeclaration(IfmapStrings.STD_METADATA_PREFIX, IfmapStrings.STD_METADATA_NS_URI);

			try {
				SearchResult searchResult = IfMap.getSsrc().search(searchRequest);
				if (searchResult.getType() == Type.searchResult) {
					LOGGER.finer("processing searchResult ...");
					cleanEmptySearchResult(searchResult);
					if (searchResult.getResultItems().size() == 1) {
						ResultItem resultItemDiscoBySwitch = searchResult.getResultItems().get(0);
						if (resultItemDiscoBySwitch.getIdentifier1() instanceof Device) {
							switchMacIp[0] = resultItemDiscoBySwitch.getIdentifier1();
						} else if (resultItemDiscoBySwitch.getIdentifier2() instanceof Device) {
							switchMacIp[0] = resultItemDiscoBySwitch.getIdentifier2();
						}
					}
				}
			} catch (IfmapErrorResult e) {
				LOGGER.severe("SubscriberThread: " + e);
			} catch (IfmapException e) {
				LOGGER.severe("SubscriberThread: " + e);
			}
		}

		return switchMacIp;
	}
}
