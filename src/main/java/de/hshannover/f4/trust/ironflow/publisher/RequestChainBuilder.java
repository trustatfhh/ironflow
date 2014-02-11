package de.hshannover.f4.trust.ironflow.publisher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;

import de.hshannover.f4.trust.ironflow.Configuration;

/**
 * This class initialise the Request chain to Request the OpenflowController over the REST API.
 * In Addition it holds the list of the RequestStrategy Objects
 * 
 * 
 * @author Marius Rohde
 *
 */

public class RequestChainBuilder {
	
	 private static final Logger logger = Logger.getLogger(RequestChainBuilder.class
	            .getName());
	
    /**
     * The package path to the strategy classes.
     */	    
	private static final String PACKAGE_PATH = "de.hshannover.f4.trust.ironflow.publisher.";
	
	/**
     * the List/Chain with the different strategy objects
     */	
	private static ArrayList<RequestStrategy> requestChain;
	
    /**
     * The init methode Initiate the RequestChain and looks for the classes in packagepath
     */
	
	public static void init(){
		
		logger.info("RequestChainBuilder : looking for classes in package " + PACKAGE_PATH );
		
		RequestStrategy request;
		Iterator<Entry<Object, Object>> iteClassnames = Configuration.getClassnameMap().iterator();
		requestChain = new ArrayList<RequestStrategy>();
				
		while(iteClassnames.hasNext()){
			
			Entry<Object, Object> classname = iteClassnames.next();
			logger.info("RequestChainBuilder : found classString " + classname.getKey().toString() );
			
			if(classname.getValue().toString().equals("enabled")){	
				
				request = createNewRequestStrategie(PACKAGE_PATH+classname.getKey().toString());				
				if(request != null){
					requestChain.add(request);
				}	
			}
		}		
	}
	
    /**
     * This helper methode creates a new RequestStrategieObject
     * 
     * @param package and classname
     * @return RequestStrategy object
     */
	
	private static RequestStrategy createNewRequestStrategie(String className){
		
		RequestStrategy request = null;
		
		try {
			 Class<?> cl = Class.forName(className);
			 logger.info("RequestChainBuilder : " + cl.toString() + " instantiated" );
			 if(cl.getSuperclass() == RequestStrategy.class ){
				 request = (RequestStrategy) cl.newInstance(); 
			 }			
			 
		} catch (ClassNotFoundException e) {
			logger.severe("RequestChainBuilder: ClassNotFound");
		} catch (InstantiationException e) {
			logger.severe("RequestChainBuilder: InstantiationException");
		} catch (IllegalAccessException e) {
			logger.severe("RequestChainBuilder: IllegalAccessException");
		}
		
		return request;		
	}
	
    /**
     * The Size of the requestChain
     * 
     * @return the size
     */
	
	public static int getSize(){		
		
		return requestChain.size();
	}
	
    /**
     * This method delivers a RequestStrategyObject stored in the chain
     * 
     * @param the index of the element
     * @return an Element
     */
	
	public static RequestStrategy getElementAt(int index){		
		
		return requestChain.get(index);
	}
	
	
}
