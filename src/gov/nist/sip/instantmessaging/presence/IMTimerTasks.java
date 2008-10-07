/*
 * Created on 8.3.2007
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package gov.nist.sip.instantmessaging.presence;

import gov.nist.sip.instantmessaging.BuddyList;
import gov.nist.sip.instantmessaging.InstantMessagingGUI;

import java.awt.event.*;

/**
 * @author niepin
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class IMTimerTasks implements ActionListener {

	private IMUserAgent imUA;

	private int taskCode;

	/** Creates new IMRegisterProcessing */
	public IMTimerTasks(IMUserAgent imUA, int code) {
		this.imUA = imUA;
		this.taskCode = code;
	}

	public int getCurrentTaskCode() {
		return this.taskCode;
	}

	public void setTaskCode(int code) {
		this.taskCode = code;
	}

	public void actionPerformed(ActionEvent evt) {
		if (imUA != null) {
			if (taskCode == 0) {
				// do nothing
			}
			if (taskCode == 1) {
				// send SUBSCRIBE to poll the presence of the buddies in the
				// list
				IMRegisterProcessing imRegisterProcessing = imUA
						.getIMRegisterProcessing();
				InstantMessagingGUI imGUI = imUA.getInstantMessagingGUI();

				if (imRegisterProcessing.isRegistered()) {
					BuddyList buddyList = imGUI.getBuddyList();
					if (buddyList != null)
						imUA.subscribeBuddiesPresentity(buddyList.getBuddies());
				}
			}
			if (taskCode == 2) {
				// something else
			}
		}
	}

}
