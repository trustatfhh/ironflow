package de.hshannover.f4.trust.ironflow;

import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import de.fhhannover.inform.trust.ifmapj.IfmapJHelper;
import de.fhhannover.inform.trust.ifmapj.channel.SSRC;
import de.fhhannover.inform.trust.ifmapj.exception.InitializationException;

public class IfMap {
  
	/**
	 * A ifmap SSRC instance.
	 */
	private static SSRC ifmapSSRC = null;
	
	private static final Logger logger = Logger.getLogger(IfMap.class.getName());
	
	   /**
     * Creates a {@link SSRC} instance with the given configuration parameters.
     *
     * @param authMethod
     * @param basicUrl
     * @param certUrl
     * @param user
     * @param pass
     * @param keypath
     * @param keypass
     * @return SSRC
     */
    public static SSRC initSSRC(String authMethod, String basicUrl,
            String certUrl, String user, String pass, String keypath,
            String keypass) {
    	
        TrustManager[] tm = null;
        KeyManager[] km = null;

        try {
            tm = IfmapJHelper.getTrustManagers(
                    Ironflow.class.getResourceAsStream(keypath), keypass);
            km = IfmapJHelper.getKeyManagers(
            		Ironflow.class.getResourceAsStream(keypath), keypass);
        } catch (InitializationException e1) {
        	
            logger.severe("could not read the security informations for the trust- and key- managers: " + e1);
            System.exit(1);
        }

        try {
            if (authMethod.equals("basic")) {
            	ifmapSSRC = new ThreadSafeSsrc(basicUrl, user, pass, tm);
            } else if (authMethod.equals("cert")) {
            	ifmapSSRC = new ThreadSafeSsrc(certUrl, km, tm);
            } else {
                throw new IllegalArgumentException(
                        "unknown authentication method '" + authMethod + "'");
            }
        } catch (InitializationException e) {
        	logger.severe("could not read the security informations for basic or cert authentication: " + e);
            System.exit(1);
        }
        
        return ifmapSSRC;
    }
    
    public static SSRC getSSRC(){
    	return ifmapSSRC;
    }
	
	
	
}
