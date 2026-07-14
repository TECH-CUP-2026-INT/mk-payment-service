package co.edu.escuelaing.techcup.payment.config;

import co.edu.escuelaing.techcup.payment.document.PaymentOrderDocument;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentOrderAuditListener extends AbstractMongoEventListener<PaymentOrderDocument> {

    @Override
    public void onBeforeConvert(BeforeConvertEvent<PaymentOrderDocument> event) {
        PaymentOrderDocument document = event.getSource();
        LocalDateTime now = LocalDateTime.now();
        if (document.getCreatedAt() == null) {
            document.setCreatedAt(now);
        }
        document.setUpdatedAt(now);
    }
}
