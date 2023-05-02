package com.api.controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

// Utility class for creating Gmail messages
class GmailUtil
{
    public static Message createEmail(String to, String from, String subject, String bodyText) throws MessagingException, IOException
    {
        Session session = Session.getDefaultInstance(new Properties(), null);
        MimeMessage email = new MimeMessage(session);
//        email.setFrom(new InternetAddress(from));
//        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
//        email.setSubject(subject);
//        email.setText(bodyText, "UTF-8");
        MimeMessageHelper helper = new MimeMessageHelper(email, false, "UTF-8");
        helper.setFrom(from, "Akash's API");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(bodyText, true);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    public static void sendMessage(Gmail service, String userId, Message message) throws MessagingException, IOException
    {
        message = service.users().messages().send(userId, message).execute();
        System.out.println("Message sent: " + message.toPrettyString());
    }
}
