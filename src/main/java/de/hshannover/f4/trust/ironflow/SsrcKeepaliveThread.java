package de.hshannover.f4.trust.ironflow;

import java.util.TimerTask;
import java.util.logging.Logger;

import de.fhhannover.inform.trust.ifmapj.exception.IfmapErrorResult;
import de.fhhannover.inform.trust.ifmapj.exception.IfmapException;

/**
 * A {@link Keepalive} can be used to keep an IF-MAP connection alive, by
 * continuously sending a re-new session request to the MAPS.
 *
 * @author Marius Rohde
 *
 */

public class SsrcKeepaliveThread extends TimerTask {
	
	private static final Logger logger = Logger.getLogger(SsrcKeepaliveThread.class
	            .getName());
	
	@Override
	public void run() {
		
		try {
             IfMap.getSSRC().renewSession();     
             
        } catch (IfmapException e) {
            logger.severe("renewSession failed: " + e.getMessage());
            try {
            	IfMap.getSSRC().endSession();
            } catch (Exception ex) {
                logger.warning("error while ending the session");
            }
        } catch (IfmapErrorResult e) {
            logger.severe("renewSession failed: " + e.getMessage());
            try {
            	IfMap.getSSRC().endSession();
            } catch (Exception ex) {
                logger.warning("error while ending the session");
            }
        }
	}

}
