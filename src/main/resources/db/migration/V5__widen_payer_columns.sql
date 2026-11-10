-- Payer address/phone fields were too narrow for real Colombian data
-- (e.g. a 10-digit mobile phone did not fit in payer_phone_number VARCHAR(5)).
-- This caused DataIntegrityViolationException / HTTP 500 after Mercado Pago
-- had already created the PSE payment, leaving the order PENDING without an
-- mp_payment_id.
ALTER TABLE payment_orders ALTER COLUMN payer_address_zip_code TYPE VARCHAR(10);
ALTER TABLE payment_orders ALTER COLUMN payer_address_street_name TYPE VARCHAR(50);
ALTER TABLE payment_orders ALTER COLUMN payer_address_street_number TYPE VARCHAR(20);
ALTER TABLE payment_orders ALTER COLUMN payer_address_neighborhood TYPE VARCHAR(50);
ALTER TABLE payment_orders ALTER COLUMN payer_address_city TYPE VARCHAR(50);
ALTER TABLE payment_orders ALTER COLUMN payer_phone_area_code TYPE VARCHAR(5);
ALTER TABLE payment_orders ALTER COLUMN payer_phone_number TYPE VARCHAR(20);
