package victor.training.oo.behavioral.template;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;

import lombok.Data;

@SpringBootApplication
public class TemplateSpringApp implements CommandLineRunner {
	public static void main(String[] args) {
		SpringApplication.run(TemplateSpringApp.class, args);
	}

	@Autowired
	private EmailSender service;
	@Autowired
	private Emails emails;
	
	public void run(String... args) throws Exception {
		service.sendEmail("a@b.com" ,emails::fillEmailReceived);
		service.sendEmail("a@b.com", emails::fillEmailShipped);
	}
}

@Service
class EmailSender {

	public void sendEmail(String emailAddress, EmailFiller filler) {
		final int MAX_RETRIES = 3;
		EmailContext context = new EmailContext(/*smtpConfig,etc*/);
		for (int i = 0; i < MAX_RETRIES; i++ ) {
			Email email = new Email(); // constructor generates new unique ID
			email.setSender("noreply@corp.com");
			email.setReplyTo("/dev/null");
			email.setTo(emailAddress);
			filler.fillEmail(email);
			boolean success = context.send(email);
			if (success) break;
		}
	}
}

interface EmailFiller {
	abstract void fillEmail(Email email);
}

@Service
class Emails {
	public void fillEmailReceived(Email email) {
		email.setSubject("Order Received");
		email.setBody("Thank you for your order");
	}
	public void fillEmailShipped(Email email) {
		email.setSubject("Order Shipped");
		email.setBody("Just sent you your order. ! Hope it gets to you (this time :p)");
	}
}



class EmailContext {
	public boolean send(Email email) {
		System.out.println("Trying to send " + email);
		return new Random(System.nanoTime()).nextBoolean();
	}
}

@Data
class Email {
	private final long id = new Random(System.nanoTime()).nextLong();
	private String sender;
	private String subject;
	private String body;
	private String replyTo;
	private String to;
}