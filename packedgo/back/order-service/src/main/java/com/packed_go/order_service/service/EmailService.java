package com.packed_go.order_service.service;

import com.packed_go.order_service.entity.Order;
import com.packed_go.order_service.entity.OrderItem;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendOrderConfirmation(Order order, String toEmail) {
        if (toEmail == null || toEmail.isEmpty()) {
            log.warn("⚠️ Cannot send order confirmation: No email provided for order {}", order.getOrderNumber());
            return;
        }

        log.info("📧 Sending order confirmation email to {} for order {}", toEmail, order.getOrderNumber());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Confirmación de Compra - PackedGo Events - " + order.getOrderNumber());

            String htmlContent = buildOrderConfirmationHtml(order);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("✅ Order confirmation email sent successfully to {}", toEmail);

        } catch (MessagingException e) {
            log.error("❌ Failed to send order confirmation email: {}", e.getMessage(), e);
        }
    }

    private String buildOrderConfirmationHtml(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; color: #333;'>");
        sb.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px;'>");
        
        sb.append("<h2 style='color: #2c3e50; text-align: center;'>¡Gracias por tu compra!</h2>");
        sb.append("<p>Hola,</p>");
        sb.append("<p>Tu orden <strong>").append(order.getOrderNumber()).append("</strong> ha sido confirmada exitosamente.</p>");
        
        sb.append("<div style='background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 20px 0;'>");
        sb.append("<h3>Resumen de la Orden</h3>");
        sb.append("<p><strong>Fecha:</strong> ").append(order.getPaidAt() != null ? order.getPaidAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A").append("</p>");
        sb.append("<p><strong>Total:</strong> $").append(order.getTotalAmount()).append("</p>");
        sb.append("</div>");

        sb.append("<h3>Detalle de Entradas:</h3>");
        sb.append("<table style='width: 100%; border-collapse: collapse;'>");
        sb.append("<tr style='background-color: #eee;'><th style='padding: 10px; text-align: left;'>Evento</th><th style='padding: 10px; text-align: center;'>Cant.</th><th style='padding: 10px; text-align: right;'>Precio</th></tr>");

        for (OrderItem item : order.getItems()) {
            sb.append("<tr>");
            sb.append("<td style='padding: 10px; border-bottom: 1px solid #eee;'>").append("Evento ID: ").append(item.getEventId()).append("</td>"); // Idealmente buscar nombre del evento
            sb.append("<td style='padding: 10px; border-bottom: 1px solid #eee; text-align: center;'>").append(item.getQuantity()).append("</td>");
            sb.append("<td style='padding: 10px; border-bottom: 1px solid #eee; text-align: right;'>$").append(item.getUnitPrice()).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");

        sb.append("<p style='margin-top: 20px;'>Puedes ver tus tickets y códigos QR en la sección <strong>Mis Tickets</strong> de la aplicación.</p>");
        
        sb.append("<div style='text-align: center; margin-top: 30px; font-size: 12px; color: #777;'>");
        sb.append("<p>&copy; 2025 PackedGo Events. Todos los derechos reservados.</p>");
        sb.append("</div>");
        
        sb.append("</div>");
        sb.append("</body></html>");
        
        return sb.toString();
    }
}
