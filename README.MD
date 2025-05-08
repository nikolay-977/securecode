# OTP Service

## Описание

Этот сервис реализует систему двухфакторной аутентификации с использованием одноразовых паролей (OTP). Он включает в
себя регистрацию пользователей, аутентификацию, управление OTP-кодами, а также рассылку кодов через различные каналы:
SMS, Email и Telegram. Администраторы могут управлять конфигурацией OTP и списком пользователей.

Сервис реализован на Java с использованием JDBC для взаимодействия с базой данных PostgreSQL 17 и поддерживает следующие
операции через API.

## Требования

- Java 17 или выше
- PostgreSQL 17
- Maven или Gradle для сборки
- Библиотеки для логирования (например, SLF4J, Logback)

## Структура проекта

- **API**: Обработчики HTTP-запросов с использованием `com.sun.net.httpserver` или Spring MVC.
- **Сервисы**: Основная бизнес-логика приложения.
- **DAO**: Взаимодействие с базой данных через JDBC.
- **Логирование**: Использование библиотеки SLF4J для логирования.

## Структура Базы Данных

### 1. Пользователи

Таблица пользователей хранит логин, пароль (в зашифрованном виде) и роль пользователя (администратор или простой
пользователь).

```sql
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
```

### 2. Конфигурация OTP

Таблица хранит настройки конфигурации OTP-кодов (длина кода, время жизни).

```sql
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
```

### 3. OTP-коды

Таблица OTP-кодов хранит данные о сгенерированных кодах, включая их статус и время истечения.

```sql
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
```

## Основные команды API

### 1. Регистрация пользователя

POST /register

Регистрирует нового пользователя в системе.

Тело запроса:

```
{
   "login": "User_4a4CQ1q6qe",
   "password": "Password_4a4CQ1q6qe",
   "role": "USER",
   "phone": "+79771234567",
   "email": "test-user@mail.ru",
   "telegramId": "@test-user-977"
}
```

Ответ:

```
{
   "status": "success",
   "message": "User registered successfully."
}
```

### 2. Логин пользователя POST /login

Аутентификация пользователя. Возвращает токен для авторизации.

Тело запроса:

```
{
   "login": "User_6tqAuasrIY",
   "password": "Password_6tqAuasrIY"
}
```

Ответ:

```
{
   "userId": "857005d6-fa5a-4588-a0d4-279135c6dd8f",
   "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJVc2VyXzZ0cUF1YXNySVkiLCJyb2xlIjoiVVNFUiIsImV4cCI6MTc0NjczOTMzMX0.7JHyjBPT-dBGUhcVzeJjG29KBUmq46sNdD5kuqeAacE"
}
```

### 3. Изменение конфигурации OTP POST /admin

Изменяет настройки конфигурации OTP-кодов (длина кода и TTL).

Тело запроса:

```
{
  "action": "updateOtpConfig",
  "codeLength": 6,
  "ttlSeconds": 10
}
```

Ответ:

```
{
  "status": "success",
  "message": "OTP configuration updated successfully."
}

```

### 4. Получение списка пользователей (только для администраторов)  POST /admin

Получение списка всех пользователей, кроме администраторов.

Тело запроса:

```
{
  "action": "listUsers"
}
```

Ответ:

```
[
    {
        "phone": "+79778710977",
        "id": "487a7d68-956d-4b1a-b574-a6038df35f90",
        "login": "User_74vEkb9ZbN",
        "email": "nikolay-2708@yandex.ru"
    },
    {
        "phone": "+79778710977",
        "id": "5926ecb3-70e7-42c4-a863-8e00c83a0259",
        "login": "User_RaQ0HJJXk6",
        "email": "nikolay-2708@yandex.ru"
    },
    {
        "phone": "+79778710977",
        "id": "75b08d4d-efe7-4cf2-8f4c-060fc3f28247",
        "login": "User_7jWsuK8zhJ",
        "email": "nikolay-2708@yandex.ru"
    },
    {
        "phone": "+79778710977",
        "id": "2019032a-afb2-4e62-951f-6889e16fe353",
        "login": "User_4a4CQ1q6qe",
        "email": "nikolay-2708@yandex.ru"
    },
    {
        "phone": "+79771234567",
        "id": "857005d6-fa5a-4588-a0d4-279135c6dd8f",
        "login": "User_6tqAuasrIY",
        "email": "test-user@mail.ru"
    }
]
```

### 5. Удаление пользователя DELETE /admin

Удаляет пользователя и привязанные к нему OTP-коды.

Тело запроса:

```
{
   "action": "deleteUser",
   "userId": "857005d6-fa5a-4588-a0d4-279135c6dd8f"
}
```

Ответ:

```
{
  "status": "success",
  "message": "User deleted successfully."
}
```

### 6. Генерация OTP-кода POST /otp/generate

Генерация OTP-кода для пользователя, связанного с операцией.

Тело запроса:

```
{
  "userId": "857005d6-fa5a-4588-a0d4-279135c6dd8f",
  "operationId": "OperationId_4ySVbr767u"
}
```

Ответ:

```
{
    "operationId": "OperationId_4ySVbr767u",
    "message": "OTP code generated and saved to file."
}
```

### 7. Валидация OTP-кода POST /otp/validate

Валидация OTP-кода.

Тело запроса:

```
{
  "userId": "857005d6-fa5a-4588-a0d4-279135c6dd8f",
  "operationId": "OperationId_4ySVbr767u",
  "code": "176844"
}
```

Ответ:

```
{
    "valid": true
}
```

## Каналы рассылки OTP-кодов

Email — Используется эмулятор почтовой службы или реальная почта для отправки кодов.

SMS — Эмулятор SMS-отправки для тестирования.

Telegram — Используется Telegram-бот для отправки кодов.

Сохранение в файл — Код сохраняется в файл в корне проекта.

## Задачи фоновой обработки

Каждые 60 секунд в сервисе запускается задача, которая проверяет просроченные OTP-коды и обновляет их статус на EXPIRED.

## Логирование

Логирование осуществляется с использованием библиотеки SLF4J и Logback.

## Как запустить проект

Настройте базу данных PostgreSQL. [create_tables.sql](create_tables.sql)

Соберите проект с помощью Gradle.

Gradle:

```bash
gradle build
gradle run
```

Приложение будет доступно по адресу http://localhost:8000.

## Тестирование

Запустите Postman.

Имопортируйте Postman коллекию [SecureCodeAPI.postman_collection.json](SecureCodeAPI.postman_collection.json)

Отправьте запрос и проверьте ответ.
