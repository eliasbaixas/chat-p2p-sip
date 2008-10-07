package gov.nist.sip.instantmessaging.presence;

import java.util.Vector;

import gov.nist.javax.sip.Utils;
import gov.nist.sip.instantmessaging.ChatSession;
import gov.nist.sip.instantmessaging.ChatSessionManager;
import gov.nist.sip.instantmessaging.IMUtilities;
import gov.nist.sip.instantmessaging.InstantMessagingGUI;
import gov.nist.sip.instantmessaging.ListenerInstantMessaging;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipProvider;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

/**
 * Process invite. This is for setting up a session for Instant messaging.
 * 
 * @author M. Ranganathan
 * 
 */
public class IMInviteProcessing {

	private static Logger logger = Logger.getLogger(IMInviteProcessing.class);

	private IMUserAgent imUserAgent;

	public IMInviteProcessing(IMUserAgent imUserAgent) {
		this.imUserAgent = imUserAgent;
	}

	public void processInvite(RequestEvent requestEvent,
			ServerTransaction serverTransaction) {
		try {
			Request request = requestEvent.getRequest();
			Response response = this.imUserAgent.getMessageFactory()
					.createResponse(Response.OK, request);
			// TODO -- check buddy list before sending the OK.
			// If the requestor is not on my buddy list fail the request
			// send a not-available.

			ContactHeader contactHeader = imUserAgent.createContactHeader();
			response.setHeader(contactHeader);

			serverTransaction.sendResponse(response);
		} catch (Exception ex) {
			logger.error("Unexpected exception", ex);
		}

	}

	/**
	 * This method is invoked when we get a response to the INVITE.
	 * 
	 * @param responseEvent
	 */

	public void processResponse(ResponseEvent responseEvent) {
		try {
			Response response = responseEvent.getResponse();
			if (response.getStatusCode() == Response.OK) {
				Dialog dialog = responseEvent.getDialog();
				Request ack = dialog.createAck(((CSeqHeader) response
						.getHeader(CSeqHeader.NAME)).getSeqNumber());
				dialog.sendAck(ack);
				ChatSession chatSession = (ChatSession) dialog
						.getApplicationData();
				String textToSend = chatSession.getTextSent();
				chatSession.setDialog(dialog);
				chatSession.setEstablishedSession(true);
				this.imUserAgent.imMessageProcessing.sendMessage(chatSession
						.getRemoteSipURL(), textToSend, chatSession);
			}
		} catch (Exception ex) {
			logger.error("Unexpected exception ", ex);
		}

	}

	public void sendInvite(String remoteSipURL, ChatSession chatSession,
			String text) {
		try {
			String localSipURL = imUserAgent.getInstantMessagingGUI()
					.getListenerInstantMessaging().getLocalSipURL();

			SipProvider sipProvider = imUserAgent.getSipProvider();
			AddressFactory addressFactory = imUserAgent.getAddressFactory();
			HeaderFactory headerFactory = imUserAgent.getHeaderFactory();
			MessageFactory messageFactory = imUserAgent.getMessageFactory();

			CallIdHeader callIdHeader = imUserAgent.getSipProvider()
					.getNewCallId();

			// To header:
			Address toAddress = addressFactory.createAddress(remoteSipURL);

			// From Header:
			Address fromAddress = addressFactory.createAddress(localSipURL);

			// We have to initiate the dialog: means to create the From tag
			String localTag = Utils.generateTag();
			FromHeader fromHeader = headerFactory.createFromHeader(fromAddress,
					localTag);
			ToHeader toHeader = headerFactory.createToHeader(toAddress, null);

			// CSeq:
			CSeqHeader cseqHeader = headerFactory.createCSeqHeader(1L,
					Request.INVITE);

			// Via header
			String branchId = Utils.generateBranchId();
			ViaHeader viaHeader = headerFactory.createViaHeader(imUserAgent
					.getIMAddress(), imUserAgent.getIMPort(), imUserAgent
					.getIMProtocol(), branchId);
			Vector<ViaHeader> viaList = new Vector<ViaHeader>();
			viaList.addElement(viaHeader);

			MaxForwardsHeader maxForwardsHeader = headerFactory
					.createMaxForwardsHeader(70);
			String proxyAddress = imUserAgent.getProxyAddress();
			SipURI requestURI;
			if (proxyAddress != null) {
				requestURI = addressFactory.createSipURI(null, proxyAddress);
				requestURI.setPort(imUserAgent.getProxyPort());
				requestURI.setTransportParam(imUserAgent.getIMProtocol());
			} else {
				requestURI = (SipURI) addressFactory.createURI(remoteSipURL);
				requestURI.setTransportParam(imUserAgent.getIMProtocol());
			}
			Request request = messageFactory.createRequest(requestURI,
					Request.INVITE, callIdHeader, cseqHeader, fromHeader,
					toHeader, viaList, maxForwardsHeader);
			ContactHeader contactHeader = this.imUserAgent
					.createContactHeader();
			request.addHeader(contactHeader);
			ClientTransaction ct = sipProvider.getNewClientTransaction(request);
			Dialog dialog = ct.getDialog();
			ct.sendRequest();
			dialog.setApplicationData(chatSession);

		} catch (Exception ex) {
			logger.error("Unexpected exception ", ex);
		}

	}

	public void processAck(RequestEvent requestEvent,
			ServerTransaction serverTransaction) {
		if (serverTransaction == null)
			return;
		Request request = requestEvent.getRequest();

		InstantMessagingGUI instantMessagingGUI = imUserAgent
				.getInstantMessagingGUI();
		ListenerInstantMessaging listenerInstantMessaging = instantMessagingGUI
				.getListenerInstantMessaging();
		ChatSessionManager chatSessionManager = listenerInstantMessaging
				.getChatSessionManager();

		String fromURL = IMUtilities.getKey(request, "From");
		ChatSession chatSession = null;
		if (chatSessionManager.hasAlreadyChatSession(fromURL))
			chatSession = chatSessionManager.getChatSession(fromURL);
		else {
			chatSession = chatSessionManager.createChatSession(fromURL);
			chatSession.setDialog(serverTransaction.getDialog());
			serverTransaction.getDialog().setApplicationData(chatSession);

		}

	}

}
