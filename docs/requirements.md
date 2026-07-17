# Requirements

## Functional Requirements

External actors of the service (the service itself is not modeled as an actor):

- **Tournament Service**: external system that originates enrollments and queries payment status.
- **Payer**: person who pays for an enrollment via PSE.
- **Mercado Pago**: external payment gateway that processes the PSE transaction and notifies its result.

| ID | Actor | Requirement | Priority | Status |
|----|-------|-------------|----------|--------|
| RF-01 | Tournament Service | Create a payment order for an enrollment (enrollmentId, teamId, tournamentId, amount), validating that the amount is within the current PSE limits and that no order already exists for that enrollment | High | Implemented |
| RF-02 | Tournament Service / Payer | Query the minimum and maximum amount limits enabled for the PSE payment method | High | Implemented |
| RF-03 | Payer | Initiate a PSE transaction on a pending order, providing the financial institution and payer details (email, identification type/number, entity type), leaving the order awaiting bank confirmation | High | Implemented |
| RF-04 | Mercado Pago | Notify the service of a PSE transaction result (approved, rejected, or cancelled) via a webhook, so the associated order is approved or rejected | High | Implemented |
| RF-05 | Tournament Service | Query the current status of a payment order by enrollmentId | High | Implemented |
| RF-06 | Mercado Pago (timer-triggered) | Daily sync of PSE amount limits reported by Mercado Pago to keep amount validation up to date | Medium | Implemented |
| RF-07 | Payer / Tournament Service (timer-triggered) | Automatically expire pending or awaiting-bank-confirmation payment orders that exceeded their time limit (60 minutes), checking every 5 minutes | High | Implemented |
| RF-08 | Payer | Be rejected when attempting to initiate a PSE transaction on an already-expired order, even if the periodic expiration sweep has not yet marked it as such | Medium | Implemented |

## Non-Functional Requirements

| ID | Requirement | Acceptance Criteria |
|----|-------------|---------------------|
| RNF-01 | Availability | The service must respond in under 500 ms under normal load |
| RNF-02 | Scalability | Stateless design compatible with horizontal scaling |
| RNF-03 | Maintainability | Hexagonal architecture with decoupled layers |
| RNF-04 | Observability | Structured logs and transaction traceability |
| RNF-05 | Security | Input validation and centralized exception handling |
| RNF-06 | Documentation | API and architecture documented in MkDocs |
