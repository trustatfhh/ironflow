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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;

import de.hshannover.f4.trust.ironflow.Configuration;

/**
 * This class initialize the Subscriber chain to poll the Ifmap server for
 * request for investigation metadata. In addition it holds the list of the
 * subscriber strategy Objects
 * 
 * @author Marius Rohde
 * 
 */

public final class SubscriberChainBuilder {

	/**
	 * This class initialise the subscriber chain to react on Request for
	 * investigation metadata publisched by a pdp. In Addition it holds the list
	 * of the SubscriberStrategy Objects
	 * 
	 * @author Marius Rohde
	 * 
	 */

	private static final Logger LOGGER = Logger.getLogger(SubscriberChainBuilder.class.getName());

	/**
	 * The package path to the strategy classes.
	 */
	private static final String PACKAGE_PATH = "de.hshannover.f4.trust.ironflow.subscriber.strategies.";

	/**
	 * the List/Chain with the different strategy objects
	 */
	private static ArrayList<SubscriberStrategy> subscriberChain;

	/**
	 * Death constructor for code convention -> final class because utility
	 * class
	 * 
	 */
	private SubscriberChainBuilder() {
	}

	/**
	 * The init methode initiate the SubscriberChain and looks for the classes
	 * in packagepath
	 */

	public static void init() {

		LOGGER.info("SubscriberChainBuilder : building subscriber chain ");

		SubscriberStrategy subscriber;
		subscriberChain = new ArrayList<SubscriberStrategy>();

		Iterator<Entry<Object, Object>> iteClassnames = Configuration.getSubscriberStrategiesClassnameMap().iterator();

		while (iteClassnames.hasNext()) {

			Entry<Object, Object> classname = iteClassnames.next();
			LOGGER.info("SubscriberChainBuilder : found classString " + classname.getKey().toString());

			if (classname.getValue().toString().equals("enabled")) {

				subscriber = createNewSubscriberStrategie(PACKAGE_PATH + classname.getKey().toString());
				if (subscriber != null) {
					subscriberChain.add(subscriber);
				}
			}
		}
	}

	/**
	 * This helper methode creates a new SubscriberStrategieObject
	 * 
	 * @param className
	 * @return SubscriberStrategy object
	 */

	private static SubscriberStrategy createNewSubscriberStrategie(String className) {

		SubscriberStrategy subscriberStrategy = null;

		try {
			Class<?> cl = Class.forName(className);
			LOGGER.info("SubscriberChainBuilder : " + cl.toString() + " instantiated");
			if (cl.getSuperclass() == SubscriberStrategy.class) {
				subscriberStrategy = (SubscriberStrategy) cl.newInstance();
			}

		} catch (ClassNotFoundException e) {
			LOGGER.severe("SubscriberChainBuilder: ClassNotFound");
		} catch (InstantiationException e) {
			LOGGER.severe("SubscriberChainBuilder: InstantiationException");
		} catch (IllegalAccessException e) {
			LOGGER.severe("SubscriberChainBuilder: IllegalAccessException");
		}

		return subscriberStrategy;
	}

	/**
	 * The Size of the requestChain
	 * 
	 * @return the size
	 */

	public static int getSize() {

		return subscriberChain.size();
	}

	/**
	 * This method delivers a SubscriberStrategyObject stored in the chain
	 * 
	 * @param index
	 *            the index of the element
	 * @return an Element
	 */

	public static SubscriberStrategy getElementAt(int index) {

		return subscriberChain.get(index);
	}
}
