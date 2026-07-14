package co.edu.escuelaing.techcup.payment.config;

import co.edu.escuelaing.techcup.payment.entity.PaymentOrderEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;

public class PaymentOrderAuditListener {

    @PrePersist
    public void onPrePersist(PaymentOrderEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
    }

    @PreUpdate
    public void onPreUpdate(PaymentOrderEntity entity) {
        entity.setUpdatedAt(LocalDateTime.now());
    }
}
