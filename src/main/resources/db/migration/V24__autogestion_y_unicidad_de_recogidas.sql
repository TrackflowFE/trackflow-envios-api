alter table pickups add column management_code_hash varchar(100);

create unique index uq_pickups_active_shipment on pickups (shipment_tracking_number)
    where status <> 'CANCELLED' and shipment_tracking_number is not null;
