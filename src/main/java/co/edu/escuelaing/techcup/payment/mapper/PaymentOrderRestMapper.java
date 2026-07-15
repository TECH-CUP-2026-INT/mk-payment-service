package co.edu.escuelaing.techcup.payment.mapper;

import co.edu.escuelaing.techcup.payment.dto.request.SubmitPseTransactionRequest;
import co.edu.escuelaing.techcup.payment.dto.response.CreatePaymentOrderResponse;
import co.edu.escuelaing.techcup.payment.dto.response.PaymentOrderStatusResponse;
import co.edu.escuelaing.techcup.payment.dto.response.SubmitPseTransactionResponse;
import co.edu.escuelaing.techcup.payment.service.Payer;
import co.edu.escuelaing.techcup.payment.service.PaymentOrder;
import co.edu.escuelaing.techcup.payment.service.PaymentOrderStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PaymentOrderRestMapper {

    @Mapping(source = "id", target = "paymentOrderId")
    @Mapping(source = "status", target = "status", qualifiedByName = "statusToString")
    CreatePaymentOrderResponse toCreateResponse(PaymentOrder paymentOrder);

    @Mapping(source = "status", target = "status", qualifiedByName = "statusToString")
    SubmitPseTransactionResponse toSubmitPseResponse(PaymentOrder paymentOrder);

    /**
     * EXPIRED es interno: se expone como REJECTED al cliente.
     */
    @Mapping(source = "status", target = "status", qualifiedByName = "statusToPublicString")
    PaymentOrderStatusResponse toStatusResponse(PaymentOrder paymentOrder);

    @Mapping(source = "payerEmail", target = "email")
    @Mapping(source = "identificationType", target = "identificationType")
    @Mapping(source = "identificationNumber", target = "identificationNumber")
    @Mapping(source = "entityType", target = "entityType")
    Payer toPayer(SubmitPseTransactionRequest request);

    @Named("statusToString")
    default String statusToString(PaymentOrderStatus status) {
        return status.name();
    }

    @Named("statusToPublicString")
    default String statusToPublicString(PaymentOrderStatus status) {
        return (status == PaymentOrderStatus.EXPIRED ? PaymentOrderStatus.REJECTED : status).name();
    }
}
