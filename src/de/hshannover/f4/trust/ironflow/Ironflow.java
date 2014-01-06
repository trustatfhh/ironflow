
package de.hshannover.f4.trust.ironflow;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import de.hshannover.f4.trust.ironflow.publisher.PublisherThread;
import de.hshannover.f4.trust.ironflow.publisher.RequestChainBuilder;


/**
 * This class starts the application
 * It creates the threads for publishing, subscribing and keepalives
 * It setups logging too
 *
 * @author Marius Rohde
 *
 */

public class Ironflow {

	private static final Logger logger = Logger.getLogger(Ironflow.class.getName());
	
	private static final String LOGGING_CONFIG_FILE = "/logging.properties";
	
    /**
     * The Main method initialize the Configuration and the RequestChain. After that it starts the TimerThread 
     * 
     */
	
	public static void main(String[] args) {

		setupLogging();
		Configuration.init();
		RequestChainBuilder.init();
		
		IfMap.initSSRC(Configuration.ifmapAuthMethod(),
                Configuration.ifmapUrlBasic(), Configuration.ifmapUrlCert(),
                Configuration.ifmapBasicUser(),
                Configuration.ifmapBasicPassword(),
                Configuration.keyStorePath(), Configuration.keyStorePassword());
		
        try {
        	IfMap.getSSRC().newSession();
        	IfMap.getSSRC().purgePublisher();
        } catch (Exception e) {
        	logger.severe("could not connect to ifmap server: " + e);
            System.exit(1);
        }
				
		Timer timer = new Timer();
		
		timer.schedule( new PublisherThread(), 2000, Configuration.openFlowControllerPollingInterval()*1000 );
		timer.schedule( new SsrcKeepaliveThread(), 1000, Configuration.ifmapKeepalive()*1000);
	    
		//TODO parameter for application control
		
	}
	
    /**
     * Initialize logging
     * 
     */
	
    public static void setupLogging() {
        
    	InputStream in = Ironflow.class.getResourceAsStream(LOGGING_CONFIG_FILE);

        try {
            LogManager.getLogManager().readConfiguration(in);
        } catch (Exception e) {
            Handler handler = new ConsoleHandler();
            Logger.getLogger("").addHandler(handler);
            Logger.getLogger("").setLevel(Level.INFO);
            
            logger.severe("could not read " + LOGGING_CONFIG_FILE
                    + ", using defaults");
            
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                	logger.warning("could not close log config inputstream: "+ e);
                	e.printStackTrace();
                }
            }
        }
    }
	
}
