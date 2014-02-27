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

import de.fhhannover.inform.trust.ifmapj.identifier.Identifier;
import de.fhhannover.inform.trust.ifmapj.identifier.Identifiers;
import de.fhhannover.inform.trust.ifmapj.identifier.IpAddress;
import de.fhhannover.inform.trust.ifmapj.identifier.MacAddress;
import de.hshannover.f4.trust.ironflow.Configuration;
import de.hshannover.f4.trust.ironflow.utilities.IfMap;

public final class Test {

	/**
	 * default
	 */
	private Test() {
	};

	/**
	 * TEST IT
	 * 
	 * @author Marius Rohde
	 * 
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		IfMap.initSsrc(Configuration.ifmapAuthMethod(), Configuration.ifmapUrlBasic(), Configuration.ifmapUrlCert(),
				Configuration.ifmapBasicUser(), Configuration.ifmapBasicPassword(), Configuration.keyStorePath(),
				Configuration.keyStorePassword());

		BlockClientTrafficSubscriber testObj = new BlockClientTrafficSubscriber();

		IpAddress ip = Identifiers.createIp4("10.0.0.1");
		MacAddress mac = Identifiers.createMac("46:5e:3b:a6:22:3b");

		Identifier[] idents = new Identifier[2];

		idents[0] = null;
		idents[1] = mac;

		// testObj.executeFirewallSettings(idents);

		testObj.deleteFirewallSettings(null);
	}

}
