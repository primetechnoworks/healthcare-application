package com.healthcare.service;

import com.healthcare.notification.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    //private final JavaMailSender mailSender;

    @Async
    public void sendNotificationEmail(NotificationRequest request) {
        try {
            //MimeMessage message = mailSender.createMimeMessage();
            //MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            //helper.setFrom("noreply@healthcare.com", "Healthcare System");
            //helper.setSubject(request.getTitle());

            // HTML email template
            //String html = buildEmailTemplate(request.getTitle(), request.getMessage());
            //helper.setText(html, true);

            // In production: fetch user email from UserRepository
            // For now we log. Wire up helper.setTo(userEmail) when UserService is injected.
            //log.info("[EMAIL] Would send to userId={}: subject='{}', body='{}'",
              //  request.getUserId(), request.getTitle(), request.getMessage());

            // mailSender.send(message);  // uncomment when SMTP is configured

        } catch (Exception e) {
            log.error("Email send failed for userId={}: {}", request.getUserId(), e.getMessage());
        }
    }

	/*
	 * private String buildEmailTemplate(String title, String message) { return """
	 * <!DOCTYPE html> <html> <head> <meta charset="UTF-8"> <style> body {
	 * font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 0;
	 * background: #f5f5f5; } .container { max-width: 600px; margin: 40px auto;
	 * background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px
	 * 8px rgba(0,0,0,.08); } .header { background: #1565C0; padding: 32px;
	 * text-align: center; } .header h1 { color: #fff; margin: 0; font-size: 22px;
	 * font-weight: 500; } .body { padding: 32px; color: #333; line-height: 1.7;
	 * font-size: 15px; } .footer { background: #f5f5f5; padding: 16px 32px;
	 * text-align: center; color: #999; font-size: 12px; } .btn { display:
	 * inline-block; margin: 24px 0 0; padding: 12px 28px; background: #1565C0;
	 * color: #fff; text-decoration: none; border-radius: 6px; font-size: 14px; }
	 * </style> </head> <body> <div class="container"> <div class="header">
	 * <h1>&#9877; Healthcare System</h1> </div> <div class="body"> <h2
	 * style="margin-top:0;font-weight:500;">%s</h2> <p>%s</p> <a
	 * href="https://yourdomain.com/appointments" class="btn">View Appointment</a>
	 * </div> <div class="footer"> &copy; 2024 Healthcare System &bull; You received
	 * this because you have an account with us. </div> </div> </body> </html>
	 * """.formatted(title, message); }
	 */
}
