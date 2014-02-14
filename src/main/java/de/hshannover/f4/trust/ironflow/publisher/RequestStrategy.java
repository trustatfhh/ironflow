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
package de.hshannover.f4.trust.ironflow.publisher;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.fhhannover.inform.trust.ifmapj.IfmapJ;
import de.fhhannover.inform.trust.ifmapj.channel.SSRC;
import de.fhhannover.inform.trust.ifmapj.metadata.StandardIfmapMetadataFactory;

/**
 * This abstract class is an abstract represent of the Implementation of the
 * different REST Requests
 * 
 * 
 * @author Marius Rohde
 * 
 */

public abstract class RequestStrategy {

	/**
	 * Abstract methode to request the webservice for information. Has to be
	 * implemented by the different subclass strategies
	 * 
	 * @param webTarget
	 *            : the url for the IP/Host and Port
	 */

	public abstract void requestWebservice(WebTarget webTarget, SSRC ssrc);

	/**
	 * Helper Methode to request the webservice to get the response Can be used
	 * by the subclasses if needed
	 * 
	 * @param resourceWebTarget
	 *            : The resource that will be requested
	 * @return the Response of the webservice
	 */

	public Response getResponse(WebTarget resourceWebTarget) {

		Invocation.Builder invocationBuilder = resourceWebTarget.request(MediaType.TEXT_PLAIN_TYPE);
		invocationBuilder.header("some-header", "true");

		return invocationBuilder.get();
	}

	/**
	 * Helper Methode to get if Map MetaData factory
	 * 
	 * @return the Response of the webservice
	 */

	public StandardIfmapMetadataFactory getMetadataFactory() {
		StandardIfmapMetadataFactory mf = IfmapJ.createStandardMetadataFactory();
		return mf;
	}

}
