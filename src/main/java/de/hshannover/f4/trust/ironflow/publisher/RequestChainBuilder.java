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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;

import de.hshannover.f4.trust.ironflow.Configuration;

/**
 * This class initialize the request chain to request the OpenflowController
 * over the REST API. In addition it holds the list of the RequestStrategy
 * Objects
 * 
 * 
 * @author Marius Rohde
 * 
 */

public final class RequestChainBuilder {

	private static final Logger LOGGER = Logger.getLogger(RequestChainBuilder.class.getName());

	/**
	 * The package path to the strategy classes.
	 */
	private static final String PACKAGE_PATH = "de.hshannover.f4.trust.ironflow.publisher.strategies.";

	/**
	 * the List/Chain with the different strategy objects
	 */
	private static ArrayList<RequestStrategy> requestChain;

	/**
	 * Death constructor for code convention -> final class because utility
	 * class
	 * 
	 */
	private RequestChainBuilder() {
	}

	/**
	 * The init methode Initiate the RequestChain and looks for the classes in
	 * packagepath
	 */

	public static void init() {

		LOGGER.info("RequestChainBuilder : looking for classes in package " + PACKAGE_PATH);

		RequestStrategy request;
		Iterator<Entry<Object, Object>> iteClassnames = Configuration.getRequestStrategiesClassnameMap().iterator();
		requestChain = new ArrayList<RequestStrategy>();

		while (iteClassnames.hasNext()) {

			Entry<Object, Object> classname = iteClassnames.next();
			LOGGER.info("RequestChainBuilder : found classString " + classname.getKey().toString());

			if (classname.getValue().toString().equals("enabled")) {

				request = createNewRequestStrategie(PACKAGE_PATH + classname.getKey().toString());
				if (request != null) {
					requestChain.add(request);
				}
			}
		}
	}

	/**
	 * This helper methode creates a new RequestStrategieObject
	 * 
	 * @param className
	 * @return RequestStrategy object
	 */

	private static RequestStrategy createNewRequestStrategie(String className) {

		RequestStrategy request = null;

		try {
			Class<?> cl = Class.forName(className);
			LOGGER.info("RequestChainBuilder : " + cl.toString() + " instantiated");
			if (cl.getSuperclass() == RequestStrategy.class) {
				request = (RequestStrategy) cl.newInstance();
			}

		} catch (ClassNotFoundException e) {
			LOGGER.severe("RequestChainBuilder: ClassNotFound");
		} catch (InstantiationException e) {
			LOGGER.severe("RequestChainBuilder: InstantiationException");
		} catch (IllegalAccessException e) {
			LOGGER.severe("RequestChainBuilder: IllegalAccessException");
		}

		return request;
	}

	/**
	 * The Size of the requestChain
	 * 
	 * @return the size
	 */

	public static int getSize() {

		return requestChain.size();
	}

	/**
	 * This method delivers a RequestStrategyObject stored in the chain
	 * 
	 * @param index
	 *            the index of the element
	 * @return an Element
	 */

	public static RequestStrategy getElementAt(int index) {

		return requestChain.get(index);
	}

}
