package de.hshannover.f4.trust.ironflow.publisher;

import java.util.TimerTask;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import de.hshannover.f4.trust.ironflow.Configuration;
import de.hshannover.f4.trust.ironflow.IfMap;

/**
 * This class looks for the different REST request types in the list 
 * and calls the function in the RequestStrategies to request the webserver
 *  
 * @author Marius Rohde
 *
 */


public class PublisherThread extends TimerTask{

	/**
	 * This method calls the requestWebservice method of all Request Strategies in the RequestChain 
	 * and defines the serverconnection to the OpenflowController
	 */
	
	@Override
	public void run() {

		RequestStrategy request;
		 
		Client client = ClientBuilder.newClient(new ClientConfig().register(JacksonFeature.class));
		
		WebTarget webTarget = client.target("http://"+ Configuration.openFlowControllerIP()	+":"+ 
				Configuration.openFlowControllerPort());
				
		for(int i = 0; i < RequestChainBuilder.getSize(); i++){
			request = RequestChainBuilder.getElementAt(i);			
			request.requestWebservice(webTarget, IfMap.getSSRC());		
		}
				
		
	}

}
