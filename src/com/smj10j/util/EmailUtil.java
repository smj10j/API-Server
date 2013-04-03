package com.smj10j.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.poi.hwpf.extractor.WordExtractor;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.smj10j.conf.Constants;
import com.smj10j.conf.FatalException;
import com.smj10j.conf.InvalidParameterException;
import com.smj10j.servlet.GatewayServlet;

public abstract class EmailUtil {

	private static Logger logger = Logger.getLogger(EmailUtil.class);
	
	/*
	public static void email(String to, String from, String subject, String body) throws FatalException {

		String host = "localhost";
  
		Properties properties = System.getProperties();
		properties.setProperty("mail.smtp.host", host);
		Session session = Session.getDefaultInstance(properties);

		try{
			// Create a default MimeMessage object.
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			message.setSubject(subject);
			message.setText(body);
			// Send message
			Transport.send(message);
		}catch (MessagingException e) {
			throw new FatalException(e);
		}
	}
	*/
	
	public static String getInternalAdminEmail() {
		String hostname = GatewayServlet.getHostname();
		String to = "stevej@ucla.edu";
		if(hostname.equals("...")) {
			to = "...";
		}
		return to;
	}
	
	//public static void email(Customer customer, String sender, List<String> recipients, String subject, byte[] bytes, String fileFormat, Integer price) throws FatalException, InvalidParameterException {
	public static void email(Object customer, String sender, List<String> recipients, String subject, byte[] bytes, String fileFormat, Integer price) throws FatalException, InvalidParameterException {
		
		final String HOST = "host.com";
		SendEmailRequest emailRequest = new SendEmailRequest().withSource(sender + "@" + HOST);
		
		Destination dest = new Destination().withToAddresses(recipients);
		emailRequest.setDestination(dest);
	
		Content subjContent = new Content().withData(subject);
		com.amazonaws.services.simpleemail.model.Message msg = new com.amazonaws.services.simpleemail.model.Message().withSubject(subjContent);
	    
		Body emailBody;
		
		if(fileFormat.equals("html")) {
			emailBody = convertHtml(bytes);
		}
		else if(fileFormat.equals("doc")) {
			emailBody = convertDoc(bytes);
		}
		else if(fileFormat.equals("rtf")) {
			emailBody = convertRtf(bytes);
		}
		else { //text
			emailBody = convertText(bytes);
		}
		
		msg.setBody(emailBody);
	
		emailRequest.setMessage(msg);
		
		AmazonSimpleEmailServiceClient client = 
		        new AmazonSimpleEmailServiceClient(
		            new BasicAWSCredentials(
		                "AKIAIJXE7K7Z6CZ6L2QA", 
		                "6plNW5lAs/EZX92MenZqNid72QuNmffRHe3d8C/B"));
	                
	    // Call Amazon SES to send the message 
		try {
			client.sendEmail(emailRequest);
			logger.info("SES message sent successfully.");
			/*if(customer != null && price != null && price > 0) {
				StripeManager.addInvoiceItem(customer, recipients.size() * price, "email to " + recipients.size() + " recipients", "usd");
			}*/
		} catch (AmazonClientException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Body convertHtml(byte[] bytes) {
		String body = new String(bytes);
		logger.debug("body is " + body);
	    Content htmlContent = new Content().withData(body);
		return new Body().withHtml(htmlContent);
	}
	
	public static Body convertRtf(byte[] bytes) {
		String body = new String(bytes);
	    Content textContent = new Content().withData(body);
		return new Body().withText(textContent);
	}
	
	public static Body convertDoc(byte[] bytes) throws InvalidParameterException {
		WordExtractor extractor;
		try {
			extractor = new WordExtractor(new ByteArrayInputStream(bytes));
		} catch (IOException e) {
			throw new InvalidParameterException(Constants.Error.EMAIL.INVALID_BYTE_STREAM);
		}
		
	    Content textContent = new Content().withData(extractor.getText());
		return new Body().withText(textContent);
	}
	
	public static Body convertText(byte[] bytes) throws FatalException {
		String body;
		try {
			body = new String(bytes, "UTF-8");
		    Content textContent = new Content().withData(body);
			return new Body().withText(textContent);
		} catch (UnsupportedEncodingException e) {
			throw new FatalException("Bad encoding");
		}
	}
	
}
