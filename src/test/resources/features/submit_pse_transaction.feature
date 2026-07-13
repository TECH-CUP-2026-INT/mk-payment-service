Feature: TC-PAY-02 - Enviar transacción PSE
  Como Tournament Service
  Quiero enviar los datos bancarios de un pagador para una orden de pago
  Para iniciar el pago de la inscripción vía PSE

  Scenario: Enviar exitosamente una transacción PSE para una orden pendiente
    Given existe una orden de pago pendiente para el enrollmentId "enr-010"
    And Mercado Pago acepta la transacción PSE con referencia "mp-123" y url "https://mp.test/ticket/123"
    When se envía la transacción PSE para el enrollmentId "enr-010" con institución financiera "1007" y pagador "pagador@correo.com"
    Then la transacción PSE se envía exitosamente con estado "AWAITING_BANK_CONFIRMATION"

  Scenario: Rechazar el envío si la orden no está pendiente
    Given existe una orden de pago en estado "AWAITING_BANK_CONFIRMATION" para el enrollmentId "enr-011"
    When se envía la transacción PSE para el enrollmentId "enr-011" con institución financiera "1007" y pagador "pagador@correo.com"
    Then el envío se rechaza porque la orden no está pendiente

  Scenario: Rechazar el envío si la orden ya expiró
    Given existe una orden de pago pendiente y expirada para el enrollmentId "enr-012"
    When se envía la transacción PSE para el enrollmentId "enr-012" con institución financiera "1007" y pagador "pagador@correo.com"
    Then el envío se rechaza porque la orden ya expiró

  Scenario: Rechazar el envío si no existe una orden para el enrollmentId
    Given no hay ninguna orden de pago registrada para el enrollmentId "enr-013"
    When se envía la transacción PSE para el enrollmentId "enr-013" con institución financiera "1007" y pagador "pagador@correo.com"
    Then el envío falla porque la orden no existe
