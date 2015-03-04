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
 * This file is part of ironflow, version 0.0.5, implemented by the Trust@HsH
 * research group at the Hochschule Hannover.
 * %%
 * Copyright (C) 2013 - 2015 Trust@HsH
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
package de.hshannover.f4.trust.ironflow.publisher;

import java.util.TimerTask;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import de.hshannover.f4.trust.ironflow.Configuration;
import de.hshannover.f4.trust.ironflow.utilities.IfMap;

/**
 * This class looks for the different REST request types in the list and calls
 * the function in the RequestStrategies to request the webserver
 * 
 * @author Marius Rohde
 * 
 */

public class PublisherThread extends TimerTask {

	/**
	 * This method calls the requestWebservice method of all Request Strategies
	 * in the RequestChain and defines the serverconnection to the
	 * OpenflowController
	 */

	@Override
	public void run() {

		RequestStrategy request;

		Client client = ClientBuilder.newClient(new ClientConfig().register(JacksonFeature.class));

		WebTarget webTarget = client.target("http://" + Configuration.openFlowControllerIp() + ":"
				+ Configuration.openFlowControllerPort());

		for (int i = 0; i < RequestChainBuilder.getSize(); i++) {
			request = RequestChainBuilder.getElementAt(i);
			request.requestWebservice(webTarget, IfMap.getSsrc());
		}

	}

}
