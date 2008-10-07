/*
 * BuddyList.java
 *
 * Created on September 25, 2002, 2:46 PM
 */

package gov.nist.sip.instantmessaging;

import javax.swing.*;
import javax.swing.border.*;

import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.*;
import gov.nist.sip.instantmessaging.presence.*;

/**
 * 
 * @author deruelle
 * @version 1.0
 */
public class BuddyList extends JList {

	private Vector buddies;

	private DefaultListModel list;

	private InstantMessagingGUI imGUI;

	private static Logger logger = Logger.getLogger(BuddyList.class);

	/** Creates new BuddyList */
	public BuddyList(InstantMessagingGUI imGUI) {
		this.imGUI = imGUI;
		list = new DefaultListModel();
		initList();
		this.setModel(list);
		buddies = new Vector();
	}

	/**
	 * ************************** Methods related to the buddies XML
	 * ******************
	 */

	public void writeToXMLFile() {
		try {
			String text = "<?xml version='1.0' encoding='us-ascii'?> \n"
					+ "<BUDDIES> \n";
			for (int i = 0; i < buddies.size(); i++) {
				BuddyTag buddyTag = (BuddyTag) buddies.elementAt(i);
				text += buddyTag.toString();
			}
			text += "</BUDDIES> \n";
			String xmlBuddiesFile = imGUI.getXMLBuddiesFile();
			if (xmlBuddiesFile != null) {
				IMUtilities.writeFile(xmlBuddiesFile, text);
			} else {
				logger
						.error("BuddyList, writeToXMLFile(), the buddies file does not exist..."
								+ " Unable to write the buddies");
			}
		} catch (Exception e) {
			logger
					.error(
							"BuddyList, writeToXMLFile(), unable to write the buddies...",
							e);
		}
	}

	public Vector getBuddies() {
		return buddies;
	}

	public void init(Vector buddies) {
		logger.debug("DebugIM, BuddyList, init(), There are " + buddies.size()
				+ " buddies to upload.");
		for (int i = 0; i < buddies.size(); i++) {
			BuddyTag buddyTag = (BuddyTag) buddies.elementAt(i);
			String buddy = buddyTag.getURI();

			logger.debug("DebugIM, BuddyList, init(), we uploaded " + buddy
					+ " to the buddy List.");
			if (!hasBuddy(buddy))
				addBuddy(buddy, "offline");
		}
	}

	public boolean hasAuthorization(String bud) {
		return hasBuddy(bud);
	}

	public boolean hasBuddy(String bud) {
		for (int i = 0; i < buddies.size(); i++) {
			BuddyTag buddyTag = (BuddyTag) buddies.elementAt(i);
			String buddy = buddyTag.getURI();

			if (buddy.equals(bud)) {
				logger.debug("DebugIM, BuddyList, hasBuddy(), the buddy is"
						+ " in the list");
				return true;
			}
		}
		return false;
	}

	public BuddyTag getBuddyTag(String bud) {
		for (int i = 0; i < buddies.size(); i++) {
			BuddyTag buddyTag = (BuddyTag) buddies.elementAt(i);
			String buddy = buddyTag.getURI();
			if (buddy.equals(bud)) {
				return buddyTag;
			}
		}
		return null;
	}

	public void addBuddy(String buddy) {
		this.removeBuddy(buddy);
		BuddyTag buddyTag = new BuddyTag();
		buddyTag.setURI(buddy);
		buddies.addElement(buddyTag);
	}

	public void removeBuddy(String bud) {
		for (int i = 0; i < buddies.size(); i++) {
			BuddyTag buddyTag = (BuddyTag) buddies.elementAt(i);
			String buddy = buddyTag.getURI();
			if (buddy.equals(bud)) {
				buddies.remove(i);
				break;
			}
		}
	}

	public void changeAllBuddiesStatus(String status) {
		for (int i = 0; i < buddies.size(); i++) {
			BuddyTag buddyTag = (BuddyTag) buddies.elementAt(i);
			String buddy = buddyTag.getURI();
			changeBuddyStatus(buddy, status);
		}
	}

	/**
	 * ************************** Methods related to the buddy list
	 * ******************
	 */

	public void initList() {
		list.removeAllElements();
		list.addElement("(empty)");
	}

	public int getBuddyIndex(String buddy) {
		try {
			for (int i = 0; i < list.size(); i++) {
				String bud = (String) list.elementAt(i);
				// logger.debug("bud:"+bud);
				if (bud.equals("(empty)")) {
					return -1;
				} else {
					int beginIndex = bud.indexOf("(");
					int endIndex = bud.indexOf(")");
					String realBud = bud.substring(0, beginIndex - 1).trim();
					if (realBud.equals(buddy))
						return i;
				}
			}
			return -1;
		} catch (Exception e) {
			logger.debug("BuddyList, getBuddyIndex(), error:");
			e.printStackTrace();
			return -1;
		}
	}

	public String getBuddyStatus(String buddy) {
		try {
			int index = getBuddyIndex(buddy);
			if (index != -1) {
				String bud = (String) list.get(index);
				int beginIndex = bud.indexOf("(");
				int endIndex = bud.indexOf(")");
				return bud.substring(beginIndex + 1, endIndex).trim();
			} else {
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.debug("BuddyList, getBuddyStatus(), the " + buddy
					+ " is not" + " in the buddy list: no status!!!");
			return null;
		}
	}

	public void changeBuddyStatus(String buddy, String status) {
		try {
			String stat = getBuddyStatus(buddy);
			if (stat == null) {
				logger.debug("BuddyList, changeBuddyStatus(), the " + buddy
						+ " is not"
						+ " in the buddy list: no changed status!!!");
			} else if (stat.equals(status)) {
				// we do nothing
				logger.debug("BuddyList, changeBuddyStatus(), the status ("
						+ stat + ") is" + " already the same for the buddy: "
						+ buddy + ", we do nothing!!!");
			} else {
				logger.debug("BuddyList, changeBuddyStatus(), the status ("
						+ stat + ") has" + " to be changed to " + status
						+ " for the buddy: " + buddy);
				removeFromBuddyList(buddy);
				addBuddy(buddy, status);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addBuddy(String buddy, String status) {
		if (list.size() == 1) {
			String bud = (String) list.get(0);
			if (bud.equals("(empty)")) {
				list.remove(0);
			}
		}
		addBuddy(buddy);
		list.addElement(buddy + " (" + status + ")");
	}

	public String getBuddy(int index) {
		try {
			String buddy = (String) list.get(index);
			if (buddy.equals("(empty)"))
				return null;

			int beginIndex = buddy.indexOf("(");
			return buddy.substring(0, beginIndex).trim();

		} catch (Exception e) {
			return null;
		}
	}

	public void removeSelectedBuddy() {
		if (isSelectedBuddy()) {
			String buddy = getBuddy(this.getSelectedIndex());

			removeBuddy(buddy);
			removeFromBuddyList(buddy);
			if (list.size() == 0)
				initList();
		} else {
			new AlertInstantMessaging("You must select a contact to remove!",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public void removeFromBuddyList(String buddy) {
		logger.debug("BuddyList, removeFromBuddyList(), the " + buddy
				+ " is going " + " to be removed ");

		int index = getBuddyIndex(buddy);
		logger.debug("BuddyList, removeFromBuddyList(), index: " + index);
		if (index != -1) {
			list.remove(index);
		}
	}

	public boolean isSelectedBuddy() {
		String buddy = null;
		if (this.getSelectedIndex() == -1)
			return false;
		else {
			buddy = getBuddy(this.getSelectedIndex());
			// logger.debug(buddy);
			if (buddy.equals("(empty)"))
				return false;
			else
				return true;
		}
	}

}
