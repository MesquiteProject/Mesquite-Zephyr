package mesquite.zephyr.lib;

import java.io.UnsupportedEncodingException;
import java.util.*;

import javax.mail.internet.*;
import javax.mail.*;

import mesquite.lib.MesquiteMessage;


public class NetworkUtil {

	public static void sendEmail(String recipientEmail, String recipientName, String senderEmail, String senderName, String subject, String message) {
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		try {
			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(senderEmail,senderName));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail, recipientName));
			msg.setSubject(subject);
			msg.setText(message);
			Transport.send(msg);

		} catch (AddressException e) {
			MesquiteMessage.warnProgrammer("AddressException: "+e.toString());
			// ...
		} catch (MessagingException e) {
			MesquiteMessage.warnProgrammer("MessagingException: "+e.toString());
			// ...
		} catch (UnsupportedEncodingException e) {
			MesquiteMessage.warnProgrammer("UnsupportedEncodingException: "+e.toString());
			// ...
		}
	}

	
	
	
	private static void sendFromGMail(String from, String pass, String[] to, String subject, String body) {
        Properties props = System.getProperties();
        String host = "smtp.gmail.com";
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.user", from);
        props.put("mail.smtp.password", pass);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");

        Session session = Session.getDefaultInstance(props);
        MimeMessage message = new MimeMessage(session);

        try {
            message.setFrom(new InternetAddress(from));
            InternetAddress[] toAddress = new InternetAddress[to.length];

            // To get the array of addresses
            for( int i = 0; i < to.length; i++ ) {
                toAddress[i] = new InternetAddress(to[i]);
            }

            for( int i = 0; i < toAddress.length; i++) {
                message.addRecipient(Message.RecipientType.TO, toAddress[i]);
            }

            message.setSubject(subject);
            message.setText(body);
            Transport transport = session.getTransport("smtp");
            transport.connect(host, from, pass);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        }
        catch (AddressException ae) {
            ae.printStackTrace();
        }
        catch (MessagingException me) {
            me.printStackTrace();
        }
    }



}
