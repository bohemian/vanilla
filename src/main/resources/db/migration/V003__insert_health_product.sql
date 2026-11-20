-- Sentinel product used exclusively by PricingHealthIndicator.
-- High price (999900 cents ≈ $9999) ensures it never appears in normal price-filtered queries.
insert into product(code, name, price_cents) values
('HEALTH', 'Health Check Sentinel', 999900);
