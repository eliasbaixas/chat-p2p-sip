/*
 * IMMessageProcessing.java
 *
 * Created on September 26, 2002, 12:04 AM
 */

package gov.nist.sip.instantmessaging.presence;

import gov.nist.javax.sip.*;
import javax.sip.*;
import javax.sip.message.*;
import javax.sip.header.*;
import javax.sip.address.*;

import org.apache.log4j.Logger;

import java.util.*;
import gov.nist.sip.instantmessaging.*;

/**
 * 
 * @author olivier
 * @author mranga
 * @version 1.0
 */
public class IMMessageProcessing {

	private IMUserAgent imUA;

	private int callIdCounter;

	private static Logger logger = Logger.getLogger(IMMessageProcessing.class);

	/** Creates new IMMessageProcessing */
	public IMMessageProcessing(IMUserAgent imUA) {
		this.imUA = imUA;
		callIdCounter = 0;
	}

	public void processOK(Response responseCloned,
			ClientTransaction clientTransaction) {
		logger.debug("Processing OK for MESSAGE in progress...");

		if (clientTransaction == null) {
			// This could occur if this is a retransmission of the OK.
			logger
					.debug("ERROR, IMProcessing, processOK(), the transaction is null");
			return;
		}

		InstantMessagingGUI instantMessagingGUI = imUA.getInstantMessagingGUI();
		ListenerInstantMessaging listenerInstantMessaging = instantMessagingGUI
				.getListenerInstantMessaging();
		ChatSessionManager chatSessionManager = listenerInstantMessaging
				.getChatSessionManager();
		ChatSession chatSession = null;

		Dialog dialog = clientTransaction.getDialog();
		if (dialog == null) {
			logger
					.debug("ERROR, IMProcessing, processOK(), the dialog is null");
			return;
		}

		String fromURL = IMUtilities.getKey(responseCloned, "To");
		if (chatSessionManager.hasAlreadyChatSession(fromURL)) {
			chatSession = chatSessionManager.getChatSession(fromURL);
			chatSession.displayLocalText();
			// WE remove the text typed:
			chatSession.removeSentText();

			if (chatSession.isEstablishedSession()) {
			} else {
				logger.debug("DEBUG, IMMessageProcessing, we mark the "
						+ " session established");
				chatSession.setDialog(dialog);
				chatSession.setEstablishedSession(true);
			}
		} else {
			// This is a bug!!!
			logger
					.debug("Error: IMMessageProcessing, processOK(), the chatSession is null");
		}
		logger.debug("Processing OK for MESSAGE completed...");

	}

	public void processMessage(RequestEvent requestEvent,
			ServerTransaction serverTransaction) {
		try {
			Dialog dialog = requestEvent.getDialog();
			if (dialog == null) {
				logger.debug("dropping out of dialog Message");
			}
			Request request = requestEvent.getRequest();
			SipProvider sipProvider = imUA.getSipProvider();
			MessageFactory messageFactory = imUA.getMessageFactory();
			HeaderFactory headerFactory = imUA.getHeaderFactory();
			AddressFactory addressFactory = imUA.getAddressFactory();

			InstantMessagingGUI instantMessagingGUI = imUA
					.getInstantMessagingGUI();
			ListenerInstantMessaging listenerInstantMessaging = instantMessagingGUI
					.getListenerInstantMessaging();
			ChatSessionManager chatSessionManager = listenerInstantMessaging
					.getChatSessionManager();
			ChatSession chatSession = null;
			String fromURL = IMUtilities.getKey(request, "From");
			if (chatSessionManager.hasAlreadyChatSession(fromURL))
				chatSession = chatSessionManager.getChatSession(fromURL);
			else
				chatSession = chatSessionManager.createChatSession(fromURL);

			logger.debug("IMMessageProcessing, processMEssage(), ChatSession:"
					+ chatSession);
			logger.debug("Processing MESSAGE in progress...");

			// Send an OK
			Response response = messageFactory.createResponse(Response.OK,
					request);
			// Contact header:
			SipURI sipURI = addressFactory.createSipURI(null, imUA
					.getIMAddress());
			sipURI.setPort(imUA.getIMPort());
			sipURI.setTransportParam(imUA.getIMProtocol());
			Address contactAddress = addressFactory.createAddress(sipURI);
			ContactHeader contactHeader = headerFactory
					.createContactHeader(contactAddress);
			response.setHeader(contactHeader);
			ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
			if (toHeader.getTag() == null) {
				// It is the first message without a TO tag
				toHeader.setTag(new Integer((int) (Math.random() * 10000))
						.toString());
			}

			if (chatSession.isEstablishedSession()) {
				logger.debug("The Session already exists");
				serverTransaction.sendResponse(response);
				logger.debug("OK replied to the MESSAGE:\n"
						+ response.toString());
			} else {
				logger.debug("The Session does not exists yet. ");
				serverTransaction.sendResponse(response);
				logger.debug("OK replied to the MESSAGE:\n"
						+ response.toString());

				if (dialog == null) {
					logger
							.debug("ERROR, IMProcessing, processMessage(), the dialog is null");
					return;
				}
				// We need to store the dialog:
				chatSession.setDialog(dialog);
				chatSession.setEstablishedSession(true);
				logger
						.debug("The DIALOG object has been stored in the ChatSession");
			}

			Object content = request.getContent();
			String text = null;
			if (content instanceof String)
				text = (String) content;
			else if (content instanceof byte[]) {
				text = new String((byte[]) content);
			} else {
			}
			if (text != null) {
				chatSession.displayRemoteText(text);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void sendMessage(String remoteSipURL, String text,
			ChatSession chatSession) {
		try {
			logger.debug("IMMessageProcessing, ChatSession:" + chatSession);
			logger.debug("Sending a MESSAGE in progress to " + remoteSipURL);

			SipProvider sipProvider = imUA.getSipProvider();
			MessageFactory messageFactory = imUA.getMessageFactory();
			HeaderFactory headerFactory = imUA.getHeaderFactory();
			AddressFactory addressFactory = imUA.getAddressFactory();

			String proxyAddress = imUA.getProxyAddress();
			SipURI requestURI = null;
			if (proxyAddress != null) {
				requestURI = addressFactory.createSipURI(null, proxyAddress);
				requestURI.setPort(imUA.getProxyPort());
				requestURI.setTransportParam(imUA.getIMProtocol());
			} else {
				requestURI = (SipURI) addressFactory.createURI(remoteSipURL);
				requestURI.setTransportParam(imUA.getIMProtocol());
			}

			// Call-Id:
			CallIdHeader callIdHeader = null;

			// CSeq:
			CSeqHeader cseqHeader = null;

			// To header:
			ToHeader toHeader = null;

			// From Header:
			FromHeader fromHeader = null;

			// Via header
			String branchId = Utils.generateBranchId();
			ViaHeader viaHeader = headerFactory.createViaHeader(imUA
					.getIMAddress(), imUA.getIMPort(), imUA.getIMProtocol(),
					branchId);
			Vector<ViaHeader> viaList = new Vector<ViaHeader>();
			viaList.addElement(viaHeader);

			// MaxForwards header:
			MaxForwardsHeader maxForwardsHeader = headerFactory
					.createMaxForwardsHeader(70);
			if (chatSession.isEstablishedSession()) {
				logger
						.debug("DEBUG, IMMessageProcessing, sendMessage(), we get"
								+ " the DIALOG from the ChatSession");
				Dialog dialog = chatSession.getDialog();

				Address localAddress = dialog.getLocalParty();
				Address remoteAddress = dialog.getRemoteParty();
				fromHeader = headerFactory.createFromHeader(localAddress,
						dialog.getLocalTag());
				toHeader = headerFactory.createToHeader(remoteAddress, dialog
						.getRemoteTag());
				long cseq = dialog.getLocalSeqNumber();
				logger.debug("the cseq number got from the dialog:" + cseq);
				cseqHeader = headerFactory.createCSeqHeader(cseq, "MESSAGE");

				callIdHeader = dialog.getCallId();
				// Content-Type:
				ContentTypeHeader contentTypeHeader = headerFactory
						.createContentTypeHeader("text", "plain");
				contentTypeHeader.setParameter("charset", "UTF-8");

				Request request = messageFactory.createRequest(requestURI,
						"MESSAGE", callIdHeader, cseqHeader, fromHeader,
						toHeader, viaList, maxForwardsHeader,
						contentTypeHeader, text);

				// Contact header:
				SipURI sipURI = addressFactory.createSipURI(null, imUA
						.getIMAddress());
				sipURI.setPort(imUA.getIMPort());
				sipURI.setTransportParam(imUA.getIMProtocol());
				Address contactAddress = addressFactory.createAddress(sipURI);
				ContactHeader contactHeader = headerFactory
						.createContactHeader(contactAddress);
				request.setHeader(contactHeader);

				request.setHeader(imUA.getRouteToProxy());

				// ProxyAuthorization header if not null:
				ProxyAuthorizationHeader proxyAuthHeader = imUA
						.getProxyAuthorizationHeader();
				if (proxyAuthHeader != null)
					request.setHeader(proxyAuthHeader);

				ClientTransaction clientTransaction = sipProvider
						.getNewClientTransaction(request);

				dialog.sendRequest(clientTransaction);
				logger.debug("IMessageProcessing, sendMessage(), MESSAGE sent"
						+ " using the dialog:\n" + request);

			} else {
				logger
						.debug("DEBUG, IMMessageProcessing, sendMessage(), the "
								+ " session has not been established yet! Sending INVITE");

				imUA.imInviteProcessing.sendInvite(remoteSipURL, chatSession,
						text);
				return;

			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
