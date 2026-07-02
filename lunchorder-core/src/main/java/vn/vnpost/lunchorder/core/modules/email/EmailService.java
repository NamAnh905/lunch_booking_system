package vn.vnpost.lunchorder.core.modules.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmailWithAttachment(String[] to, String subject, String body,
            byte[] attachment, String filename) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            helper.addAttachment(filename, new ByteArrayResource(attachment));

            mailSender.send(message);
            log.info("Email sent successfully to {} with attachment {}", String.join(", ", to), filename);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", String.join(", ", to), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
