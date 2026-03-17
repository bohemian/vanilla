create table product (
    id serial primary key,
    code text,
    name text,
    price_cents integer not null
);

create unique index product__code on product (code);

-- includes current
create table product_price_history (
    id serial primary key,
    product_id integer not null references product (id),
    started_at timestamp(0) not null default now(),
    ended_at timestamp(0) not null default timestamp '2111-11-11 11:11:11',
    price_cents integer not null
);

create index product_price_history__product_id on product_price_history (product_id);

create or replace function journal_product_price()
returns trigger as $$
declare _future_ended_at timestamp = timestamp '2111-11-11 11:11:11';
begin
    update product_price_history set
    ended_at = now()
    where product_id = new.id
    and ended_at = _future_ended_at;

    insert into product_price_history (product_id, started_at, ended_at, price_cents)
    values (new.id, now(), _future_ended_at, new.price_cents);

    return new;
end;
$$
language plpgsql;

create trigger product__update_insert__journal_product_price
after insert or update on product
for each row execute procedure journal_product_price();
