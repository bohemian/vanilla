create table product_price (
    product text primary key,
    cost_cents integer not null
);

create index product_price__product on product_price (product);
