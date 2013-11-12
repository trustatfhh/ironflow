package de.fhhannover.inform.trust.ironflow.publisher;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.fhhannover.inform.trust.ifmapj.IfmapJ;
import de.fhhannover.inform.trust.ifmapj.channel.SSRC;
import de.fhhannover.inform.trust.ifmapj.metadata.StandardIfmapMetadataFactory;

/**
 * This abstract class is an abstract represent of the Implementation 
 * of the different REST Requests  
 *  
 * 
 * @author Marius Rohde
 *
 */

public abstract class RequestStrategy {

    /**
     * Abstract methode to request the webservice for information. Has to be implemented 
     * by the different subclass strategies 
     *
     * @param webtarget : the url for the IP/Host and Port
     */
	
	public abstract void requestWebservice(WebTarget webTarget, SSRC ssrc);
	
    /**
     * Helper Methode to request the webservice to get the response 
     * Can be used by the subclasses if needed 
     *
     * @param resourceWebTarget : The resource that will be requested
     * @return the Response of the webservice
     */
	
	public Response getResponse(WebTarget resourceWebTarget){
	
		Invocation.Builder invocationBuilder = resourceWebTarget.request(MediaType.TEXT_PLAIN_TYPE);
		invocationBuilder.header("some-header", "true");
		 
		return invocationBuilder.get();
	}
	
    /**
     * Helper Methode to get if Map MetaData factory 
     * 
     * @return the Response of the webservice
     */
	
	public StandardIfmapMetadataFactory getMetadataFactory(){
		StandardIfmapMetadataFactory mf = IfmapJ.createStandardMetadataFactory();
		return mf;
	}
	
	
}

