Feature: TC-PAY-01 - Crear orden de pago
  Como Tournament Service
  Quiero crear una orden de pago para una inscripción
  Para poder cobrar el valor de la inscripción vía PSE

  Scenario: Crear exitosamente una orden de pago dentro de los límites de PSE
    Given los límites de PSE son un monto mínimo de "10000" y máximo de "500000"
    And no existe una orden de pago para el enrollmentId "enr-001"
    When se solicita crear una orden de pago para el enrollmentId "enr-001", equipo "team-1", torneo "torneo-1" y monto "50000"
    Then la orden de pago se crea exitosamente con estado "PENDING"

  Scenario: Rechazar la creación si el monto está fuera de los límites de PSE
    Given los límites de PSE son un monto mínimo de "10000" y máximo de "500000"
    And no existe una orden de pago para el enrollmentId "enr-002"
    When se solicita crear una orden de pago para el enrollmentId "enr-002", equipo "team-2", torneo "torneo-1" y monto "999999"
    Then la creación se rechaza por monto fuera de rango

  Scenario: Rechazar la creación si ya existe una orden para el enrollmentId
    Given los límites de PSE son un monto mínimo de "10000" y máximo de "500000"
    And ya existe una orden de pago para el enrollmentId "enr-003"
    When se solicita crear una orden de pago para el enrollmentId "enr-003", equipo "team-3", torneo "torneo-1" y monto "50000"
    Then la creación se rechaza por orden duplicada
