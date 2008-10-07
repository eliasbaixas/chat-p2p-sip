/*
 * PresentityList.java
 *
 * Created on October 3, 2002, 6:48 PM
 */

package gov.nist.sip.instantmessaging.presence;

import javax.sip.*;
import javax.sip.message.*;
import javax.sip.header.*;
import javax.sip.address.*;

import org.apache.log4j.Logger;

import java.util.*;
import gov.nist.sip.instantmessaging.*;

/**
 * 
 * @author deruelle
 * @version 1.0
 */
public class PresentityList {

	private Hashtable presentityList;

	private static Logger logger = Logger.getLogger(PresentityList.class);

	/** Creates new SubscriberController */
	public PresentityList() {
		presentityList = new Hashtable();
	}

	public Vector getAllPresentities() {
		if (presentityList != null) {
			Collection collection = presentityList.values();
			return new Vector(collection);
		}
		return new Vector();
	}

	public boolean hasPresentity(String presentityName) {
		Presentity presentity = (Presentity) presentityList.get(presentityName);
		if (presentity == null)
			return false;
		else
			return true;
	}

	public Presentity getPresentity(String presentityName) {
		return (Presentity) presentityList.get(presentityName);
	}

	public void updatePresentity(String presentityName, String status) {
		if (hasPresentity(presentityName)) {
			Presentity presentity = getPresentity(presentityName);
			presentity.setStatus(status);
			logger.debug("DEBUG, PresentityList, updatePresentity(), "
					+ "We change the status of " + presentityName);
			printPresentityList();
		}
	}

	public void addPresentity(Presentity presentity) {
		String presentityName = presentity.getPresentityName();
		if (hasPresentity(presentityName)) {
			logger.debug("DEBUG, PresentityList, addPresentity(), "
					+ "We add a new presentity: " + presentityName);
			presentityList.put(presentityName, presentity);
		} else {
			logger.debug("DEBUG, PresentityList, addPresentity(), "
					+ "We update the presentity: " + presentityName);
			presentityList.put(presentityName, presentity);
		}
		printPresentityList();
	}

	public void removePresentity(String presentityName) {
		Presentity presentity = (Presentity) presentityList.get(presentityName);
		if (presentity != null) {
			logger
					.debug("DEBUG: PresentityList, removePresentity(), "
							+ " the presentity " + presentityName
							+ " has been removed");
			presentityList.remove(presentityName);
		} else
			logger.debug("DEBUG: PresentityList, removePresentity(), "
					+ " the presentity: " + presentityName
					+ " was not found...");
		printPresentityList();
	}

	public void changePresentityStatus(String presentityName, String status) {
		if (hasPresentity(presentityName)) {
			logger.debug("DEBUG, PresentityList, changePresentityStatus(), "
					+ "The status of the presentity: " + presentityName
					+ " is now: " + status);
			Presentity presentity = (Presentity) presentityList
					.get(presentityName);
			presentity.setStatus(status);
		} else {
			logger.debug("DEBUG, PresentityList, changePresentityStatus(), "
					+ "The presentity " + presentityName + " was not found...");
		}
		printPresentityList();
	}

	public void printPresentityList() {
		Collection collection = presentityList.values();
		Vector presentities = new Vector(collection);
		logger
				.debug("************* DEBUG PresentityList    ************************************");
		logger
				.debug("************* Presentities record:    ************************************");
		for (int i = 0; i < presentities.size(); i++) {
			Presentity presentity = (Presentity) presentities.elementAt(i);
			logger.debug("presentity URL : " + presentity.getPresentityName());
			logger.debug("       status  : " + presentity.getStatus());

		}
		logger
				.debug("**************************************************************************");

	}

}
