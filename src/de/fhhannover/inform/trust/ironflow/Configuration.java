
package de.fhhannover.inform.trust.ironflow;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;


/**
 * This class loads the configuration file from the file system and provides a
 * set of constants and a getter method to access these values.
 *
 * @author Marius Rohde
 *
 */

public class Configuration {

	   private static final Logger logger = Logger.getLogger(Configuration.class
	            .getName());

	    /**
	     * The path to the configuration file.
	     */	    
	   
	    private static final String CONFIG_FILE = "/ironflow.properties";

	    private static Properties properties;

	    private static Properties classnamesForRequestStrategie;
	    
	    // begin configuration parameter -------------------------------------------

	    private static final String IFMAP_AUTH_METHOD = "ifmap.server.auth.method";
	    private static final String IFMAP_URL_BASIC = "ifmap.server.url.basic";
	    private static final String IFMAP_URL_CERT = "ifmap.server.url.cert";
	    private static final String IFMAP_BASIC_USER = "ifmap.server.auth.basic.user";
	    private static final String IFMAP_BASIC_PASSWORD = "ifmap.server.auth.basic.password";
	    
	    private static final String KEYSTORE_PATH = "keystore.path";
	    private static final String KEYSTORE_PASSWORD = "keystore.password";
	    
	    private static final String IFMAP_KEEPALIVE = "ironflow.ifmap.interval";

	    private static final String OPENFLOW_CONTROLLER_IP = "openflow.controller.ip";
	    private static final String OPENFLOW_CONTROLLER_PORT = "openflow.controller.port";
	    
	    //publisher	    
	    private static final String OPENFLOW_CONTROLLER_POLL_INTERVAL = "ironflow.poll.interval";
	    private static final String OPENFLOW_CLASSNAME_PROPERTIES_FILENAME = "ironflow.requeststrategies.publisher";
	    private static final String IRONFLOW_DEVICE_EXPIRE_TIME = "ironflow.device.expire.time";
	    
	    
	    // subscriber
	    private static final String SUBSCRIBER_PDP = "ironflow.subscriber.pdp";

	    // end configuration parameter ---------------------------------------------

	    /**
	     * Loads the configuration file. Every time this method is called the file
	     * is read again.
	     */
	    public static void init() {
	        logger.info("reading " + CONFIG_FILE + " ...");
	        
	        properties = new Properties();
	        classnamesForRequestStrategie = new Properties();
	        
	        InputStream in = Configuration.class.getResourceAsStream(CONFIG_FILE);
	        loadPropertiesfromFile(in, properties);
	        
	        in = Configuration.class.getResourceAsStream("/"+openflowClassnamePropertiesFilename());
	        loadPropertiesfromFile(in, classnamesForRequestStrategie);
	        
	    }
	    
	    private static void loadPropertiesfromFile(InputStream in, Properties props){
	        
	    	try {
	    		props.load(in);
	        } catch (FileNotFoundException e) {
	            logger.severe("could not find " + CONFIG_FILE);
	            throw new RuntimeException(e.getMessage());
	        } catch (IOException e) {
	            logger.severe("error while reading " + CONFIG_FILE);
	            throw new RuntimeException(e.getMessage());
	        } finally {
	            try {
	                in.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }

	    /**
	     * Returns the value assigned to the given key. If the configuration has not
	     * been loaded jet this method loads it.
	     *
	     * @param key
	     * @return the value assigned to key or null if the is none
	     */
	    private static String get(String key) {
	        if (properties == null) {
	            init();
	        }
	        return properties.getProperty(key);
	    }

	    public static Set<Entry<Object, Object>> getClassnameMap() {
	        if (classnamesForRequestStrategie == null) {
	            init();
	        }
	        return classnamesForRequestStrategie.entrySet();
	    }
	    
	    
	    public static String ifmapAuthMethod() {
	        return get(IFMAP_AUTH_METHOD);
	    }

	    public static String ifmapUrlBasic() {
	        return get(IFMAP_URL_BASIC);
	    }

	    public static String ifmapUrlCert() {
	        return get(IFMAP_URL_CERT);
	    }

	    public static String ifmapBasicUser() {
	        return get(IFMAP_BASIC_USER);
	    }

	    public static String ifmapBasicPassword() {
	        return get(IFMAP_BASIC_PASSWORD);
	    }

	    public static String keyStorePath() {
	        return get(KEYSTORE_PATH);
	    }

	    public static String keyStorePassword() {
	        return get(KEYSTORE_PASSWORD);
	    }

	    public static String openFlowControllerIP() {
	        return get(OPENFLOW_CONTROLLER_IP);
	    }

	    public static int openFlowControllerPort() {
	        return Integer.parseInt(get(OPENFLOW_CONTROLLER_PORT));
	    }

	    public static int openFlowControllerPollingInterval() {
	        return Integer.parseInt(get(OPENFLOW_CONTROLLER_POLL_INTERVAL));
	    }
	    
	    public static int ironflowDeviceExpireTime() {
	        return Integer.parseInt(get(IRONFLOW_DEVICE_EXPIRE_TIME));
	    }
	    
	    public static String openflowClassnamePropertiesFilename() {
	        return get(OPENFLOW_CLASSNAME_PROPERTIES_FILENAME);
	    }
	    
	    public static int ifmapKeepalive() {
	        return Integer.parseInt(get(IFMAP_KEEPALIVE));
	    }
	    
	    public static String subscriberPdp() {
	        return get(SUBSCRIBER_PDP);
	    }
	 
	
}
