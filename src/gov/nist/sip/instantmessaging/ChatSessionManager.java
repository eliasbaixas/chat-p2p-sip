/*
 * ChatSessionManager.java
 *
 * Created on September 25, 2002, 4:31 PM
 */

package gov.nist.sip.instantmessaging;

import javax.swing.*;
import javax.swing.border.*;

import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;

/**
 * 
 * @author deruelle
 * @version 1.0
 */
public class ChatSessionManager {

	private Vector chatSessionList;

	private InstantMessagingGUI imGUI;

	static private int MAXIMUM_SESSIONS = 3;

	private static Logger logger = Logger.getLogger(ChatSessionManager.class);

	/** Creates new ChatSessionManager */
	public ChatSessionManager(InstantMessagingGUI imGUI) {
		chatSessionList = new Vector();
		this.imGUI = imGUI;
	}

	public void closeAllActiveSessions() {
		for (int i = 0; i < chatSessionList.size(); i++) {
			ChatSession chatSession = (ChatSession) chatSessionList
					.elementAt(i);
			// chatSession.setExitedSession(true,"You are unregistered!!!!");
			chatSession.removeWindow();
		}
	}

	public void reOpenAllActiveSessions() {
		for (int i = 0; i < chatSessionList.size(); i++) {
			ChatSession chatSession = (ChatSession) chatSessionList
					.elementAt(i);
			chatSession.setExitedSession(false, "You are registered!!!!");
		}
	}

	public void addChatSession(ChatSession chatSession) {
		if (chatSessionList.size() >= MAXIMUM_SESSIONS)
			new AlertInstantMessaging("Too much activated sessions...",
					JOptionPane.ERROR_MESSAGE);
		else {
			chatSessionList.addElement(chatSession);
		}
		displayChatSession();
	}

	public void removeChatSession(String buddy) {
		if (buddy != null)
			for (int i = 0; i < chatSessionList.size(); i++) {
				ChatSession chatSession = (ChatSession) chatSessionList
						.elementAt(i);
				String bud = chatSession.getBuddy();
				if (buddy.equals(bud)) {
					// we need to send a BYE to the other peer!!!
					// chatSession.sendBye();
					chatSession.removeWindow();
					chatSessionList.remove(i);
					break;
				}
			}
	}

	public boolean hasAlreadyChatSession(String buddy) {
		logger
				.debug("Checking for an active chat session with " + buddy
						+ " :");
		if (buddy != null) {
			for (int i = 0; i < chatSessionList.size(); i++) {
				ChatSession chatSession = (ChatSession) chatSessionList
						.elementAt(i);
				String bud = chatSession.getBuddy();
				if (buddy.equals(bud)) {
					logger.debug("active session found");
					return true;
				}
			}
			logger.debug("No active session");
			return false;
		} else {
			logger.debug("No active session");
			return false;
		}
	}

	public ChatSession getChatSession(String buddy) {
		if (buddy != null) {
			for (int i = 0; i < chatSessionList.size(); i++) {
				ChatSession chatSession = (ChatSession) chatSessionList
						.elementAt(i);
				String bud = chatSession.getBuddy();
				if (buddy.equals(bud)) {
					return chatSession;
				}
			}
			return null;
		} else
			return null;
	}

	public ChatSession createChatSession(String buddy) {
		if (buddy != null) {
			ChatFrame chatFrame = new ChatFrame(imGUI, buddy);
			ChatSession chatSession = new ChatSession();
			chatSession.setChatFrame(chatFrame);
			chatFrame.setChatSession(chatSession);
			addChatSession(chatSession);
			return chatSession;
		} else
			return null;
	}

	public void displayChatSession() {
		logger.debug("*****************************************************");
		logger.debug("Active chat sessions:");
		for (int i = 0; i < chatSessionList.size(); i++) {
			ChatSession chatSession = (ChatSession) chatSessionList
					.elementAt(i);
			String buddy = chatSession.getBuddy();
			logger.debug("    buddy : " + buddy);
		}
		logger
				.debug("*****************************************************\n\n");

	}

}
