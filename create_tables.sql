create table users
(
    id            uuid default uuid_generate_v4() not null
        primary key,
    login         varchar(100)                    not null
        unique,
    password_hash varchar(256)                    not null,
    role          varchar(20)                     not null
        constraint users_role_check
            check ((role)::text = ANY ((ARRAY ['ADMIN'::character varying, 'USER'::character varying])::text[])),
    phone         varchar(256)                    not null,
    email         varchar(256)                    not null,
    telegram_id   varchar(256)                    not null
);

alter table users
    owner to "user";

create table otp_config
(
    id          serial
        primary key
        constraint otp_config_id_check
            check (id = 1),
    code_length integer default 6   not null
        constraint otp_config_code_length_check
            check (code_length > 0),
    ttl_seconds integer default 300 not null
        constraint otp_config_ttl_seconds_check
            check (ttl_seconds > 0)
);

alter table otp_config
    owner to "user";

create unique index otp_config_singleton
    on otp_config ((true));

create table otp_codes
(
    id           uuid      default uuid_generate_v4() not null
        primary key,
    user_id      uuid                                 not null
        references users
            on delete cascade,
    code         varchar(20)                          not null,
    status       varchar(10)                          not null
        constraint otp_codes_status_check
            check ((status)::text = ANY
                   ((ARRAY ['ACTIVE'::character varying, 'EXPIRED'::character varying, 'USED'::character varying])::text[])),
    operation_id varchar(100)                         not null,
    created_at   timestamp default CURRENT_TIMESTAMP  not null,
    expires_at   timestamp                            not null
);

alter table otp_codes
    owner to "user";

create index idx_otp_user_operation
    on otp_codes (user_id, operation_id);

create index idx_otp_status
    on otp_codes (status);