/*
 * IMByeProcessing.java
 *
 * Created on September 25, 2002, 11:29 PM
 */

package gov.nist.sip.instantmessaging.presence;

import gov.nist.javax.sip.*;
import javax.sip.*;
import javax.sip.message.*;
import javax.sip.header.*;
import javax.sip.address.*;
import javax.swing.*;
import javax.swing.border.*;

import org.apache.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import gov.nist.sip.instantmessaging.*;

/**
 * 
 * @author olivier
 * @author mranga
 * @version 1.1
 */
public class IMByeProcessing {

	private IMUserAgent imUA;

	private int cseq;

	private static Logger logger = Logger.getLogger(IMByeProcessing.class);

	/** Creates new IMByeProcessing */
	public IMByeProcessing(IMUserAgent imUA) {
		this.imUA = imUA;
		cseq = 0;
	}

	public void processBye(RequestEvent requestEvent,
			ServerTransaction serverTransaction) {
		try {
			logger
					.debug("DEBUG: IMByeProcessing, Processing BYE in progress...");

			Request request = requestEvent.getRequest();

			MessageFactory messageFactory = imUA.getMessageFactory();
			InstantMessagingGUI instantMessagingGUI = imUA
					.getInstantMessagingGUI();
			ListenerInstantMessaging listenerInstantMessaging = instantMessagingGUI
					.getListenerInstantMessaging();
			ChatSessionManager chatSessionManager = listenerInstantMessaging
					.getChatSessionManager();
			String buddy = IMUtilities.getKey(request, "From");
			if (chatSessionManager.hasAlreadyChatSession(buddy)) {
				chatSessionManager.removeChatSession(buddy);
				// chatSession.setExitedSession(true,"Your contact has exited
				// the session");
			} else {
				logger
						.debug("DEBUG: IMByeProcessing, processBye(), no active chatSession");
			}

			// Send an OK
			Response response = messageFactory.createResponse(Response.OK,
					request);
			serverTransaction.sendResponse(response);
			logger
					.debug("DEBUG: IMByeProcessing, processBye(), OK replied to the BYE");

			logger.debug("DEBUG: IMByeProcessing, Processing BYE completed...");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void processOK(Response responseCloned,
			ClientTransaction clientTransaction) {
		logger.debug("Processing OK for BYE in progress...");
		logger.debug("Processing OK for BYE completed...");
	}

	public void sendBye(String localSipURL, String remoteSipURL,
			ChatSession chatSession) {
		// Send a Bye only if there were exchanged messages!!!
		if (chatSession.isEstablishedSession()) {
			try {
				logger.debug("Sending a BYE in progress to " + remoteSipURL);

				SipProvider sipProvider = imUA.getSipProvider();

				javax.sip.Dialog dialog = chatSession.getDialog();

				Request request = dialog.createRequest(Request.BYE);

				// ProxyAuthorization header if not null:
				ProxyAuthorizationHeader proxyAuthHeader = imUA
						.getProxyAuthorizationHeader();
				if (proxyAuthHeader != null)
					request.setHeader(proxyAuthHeader);

				ClientTransaction clientTransaction = sipProvider
						.getNewClientTransaction(request);

				dialog.sendRequest(clientTransaction);
				logger.debug("BYE sent:\n" + request);

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			logger.debug("BYE not sent because of no exchanged messages!!!");
		}
	}

}
