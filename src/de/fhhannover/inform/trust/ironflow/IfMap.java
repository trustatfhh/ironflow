package de.fhhannover.inform.trust.ironflow;

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
     * @return
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
            e1.printStackTrace();
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
            e.printStackTrace();
            System.exit(1);
        }
        
        return ifmapSSRC;
    }
    
    public static SSRC getSSRC(){
    	return ifmapSSRC;
    }
	
	
	
}
