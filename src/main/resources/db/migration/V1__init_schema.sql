-- =====================================================================
-- fishing-backend: начальная схема БД (PostgreSQL 16 + PostGIS)
-- Путь в проекте: src/main/resources/db/migration/V1__init_schema.sql
--
-- Соглашения:
--   * Все координаты: SRID 4326 (WGS 84), порядок (долгота, широта),
--     т.е. ST_MakePoint(lon, lat).
--   * Перечисления хранятся как varchar + CHECK (в JPA: @Enumerated(STRING)),
--     так их проще менять миграциями, чем нативные enum PostgreSQL.
--   * Идентификаторы: uuid для данных пользователей (id можно генерировать
--     на клиенте при офлайн-записи, повторная отправка безопасна),
--     bigint identity для справочников и событий.
--   * Время везде timestamptz (хранится в UTC).
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS postgis;

-- Общий триггер: автоматически обновляет updated_at
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------
-- users: пользователи
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         varchar(255) NOT NULL,
    password_hash varchar(255) NOT NULL,          -- BCrypt, пароль в открытом виде не храним
    display_name  varchar(100) NOT NULL,
    role          varchar(20)  NOT NULL DEFAULT 'USER'
                  CHECK (role IN ('USER', 'ADMIN')),
    is_active     boolean      NOT NULL DEFAULT true,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);
-- Уникальность email без учёта регистра (в коде email тоже приводим к lower case)
CREATE UNIQUE INDEX ux_users_email ON users (lower(email));

-- ---------------------------------------------------------------------
-- water_bodies: водоёмы (наполняются вручную / импортом из CSV)
-- ---------------------------------------------------------------------
CREATE TABLE water_bodies (
    id            bigint       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          varchar(200) NOT NULL,
    slug          varchar(200) NOT NULL UNIQUE,   -- для человекочитаемых ссылок
    water_type    varchar(20)  NOT NULL
                  CHECK (water_type IN ('LAKE', 'RESERVOIR', 'RIVER', 'POND', 'QUARRY', 'OTHER')),
    description   text,
    location      geometry(Point, 4326) NOT NULL, -- центральная точка для маркера на карте
    district      varchar(150),                   -- район / населённый пункт
    access_type   varchar(20)  NOT NULL DEFAULT 'UNKNOWN'
                  CHECK (access_type IN ('FREE', 'PAID', 'PERMIT', 'UNKNOWN')),
    access_notes  text,                           -- цены, где купить путёвку, режим работы
    has_parking   boolean,                        -- NULL = неизвестно
    parking_notes text,
    rules_notes   text,                           -- запреты, водоохранная зона и т.п.
    data_source   text,                           -- откуда взяли данные (ссылка / описание): "Волшебник из страны Оз"
    verified_at   timestamptz,                    -- когда данные последний раз проверяли
    is_published  boolean      NOT NULL DEFAULT false, -- черновики не показываем в приложении
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX ix_water_bodies_location ON water_bodies USING gist (location);

-- ---------------------------------------------------------------------
-- fish_species: справочник видов рыб
-- ---------------------------------------------------------------------
CREATE TABLE fish_species (
    id                bigint       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name_ru           varchar(100) NOT NULL UNIQUE,
    name_lat          varchar(150),
    slug              varchar(100) NOT NULL UNIQUE,
    description       text,
    min_catch_size_cm numeric(5,1) CHECK (min_catch_size_cm > 0), -- минимальный размер вылова по правилам
    is_protected      boolean      NOT NULL DEFAULT false,        -- охраняемый вид, вылов запрещён
    release_advice    text,                                       -- как правильно отпускать
    created_at        timestamptz  NOT NULL DEFAULT now(),
    updated_at        timestamptz  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- water_body_fish: какая рыба водится в каком водоёме (связь многие-ко-многим)
-- ---------------------------------------------------------------------
CREATE TABLE water_body_fish (
    water_body_id bigint      NOT NULL REFERENCES water_bodies (id) ON DELETE CASCADE,
    species_id    bigint      NOT NULL REFERENCES fish_species (id) ON DELETE CASCADE,
    abundance     varchar(20) CHECK (abundance IN ('RARE', 'COMMON', 'ABUNDANT')),
    PRIMARY KEY (water_body_id, species_id)
);
CREATE INDEX ix_water_body_fish_species ON water_body_fish (species_id);

-- ---------------------------------------------------------------------
-- catches: уловы
-- ---------------------------------------------------------------------
CREATE TABLE catches (
    id             uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    water_body_id  bigint       REFERENCES water_bodies (id) ON DELETE SET NULL,
    species_id     bigint       REFERENCES fish_species (id) ON DELETE SET NULL,
    species_source varchar(10)  NOT NULL DEFAULT 'USER'      -- вид указал человек или ИИ (на будущее)
                   CHECK (species_source IN ('USER', 'AI')),
    ai_confidence  numeric(4,3) CHECK (ai_confidence BETWEEN 0 AND 1),
    caught_at      timestamptz  NOT NULL,                    -- когда пойман (из EXIF или вручную)
    location       geometry(Point, 4326),                    -- точная точка; другим пользователям НЕ отдаётся
    weight_g       integer      CHECK (weight_g > 0),        -- вес в граммах, чтобы не терять точность
    length_cm      numeric(5,1) CHECK (length_cm > 0),
    bait           varchar(200),                             -- приманка
    rig            varchar(200),                             -- проводка / оснастка
    notes          text,
    visibility     varchar(10)  NOT NULL DEFAULT 'PRIVATE'   -- по умолчанию приватно (вывод из интервью)
                   CHECK (visibility IN ('PRIVATE', 'PUBLIC')),
    weather        jsonb,                                    -- снимок погоды на момент улова (Open-Meteo)
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    deleted_at     timestamptz                               -- мягкое удаление (нужно для офлайн-синхронизации)
);
-- Мой дневник: уловы пользователя по времени
CREATE INDEX ix_catches_user_time ON catches (user_id, caught_at DESC)
    WHERE deleted_at IS NULL;
-- Карточка водоёма: последние публичные уловы
CREATE INDEX ix_catches_water_body_public ON catches (water_body_id, caught_at DESC)
    WHERE visibility = 'PUBLIC' AND deleted_at IS NULL;
CREATE INDEX ix_catches_species ON catches (species_id);
CREATE INDEX ix_catches_location ON catches USING gist (location)
    WHERE location IS NOT NULL;

-- ---------------------------------------------------------------------
-- catch_photos: фото улова (сам файл лежит в хранилище, в БД только ключ)
-- ---------------------------------------------------------------------
CREATE TABLE catch_photos (
    id           uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    catch_id     uuid         NOT NULL REFERENCES catches (id) ON DELETE CASCADE,
    storage_key  varchar(500) NOT NULL UNIQUE,   -- например: catches/<catch_id>/<photo_id>.jpg
    content_type varchar(100) NOT NULL,
    size_bytes   bigint       NOT NULL CHECK (size_bytes > 0),
    width        integer,
    height       integer,
    sort_order   smallint     NOT NULL DEFAULT 0,
    created_at   timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX ix_catch_photos_catch ON catch_photos (catch_id);

-- ---------------------------------------------------------------------
-- events: события аналитики (метрики гипотезы MVP)
--   Типы событий задаются в коде (map_opened, water_body_viewed,
--   catch_created, catch_history_viewed ...), поэтому тут обычный varchar.
-- ---------------------------------------------------------------------
CREATE TABLE events (
    id            bigint       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       uuid         REFERENCES users (id) ON DELETE SET NULL,
    session_id    varchar(64),                   -- для анонимных посетителей
    event_type    varchar(50)  NOT NULL,
    water_body_id bigint       REFERENCES water_bodies (id) ON DELETE SET NULL,
    properties    jsonb        NOT NULL DEFAULT '{}'::jsonb,
    created_at    timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX ix_events_type_time ON events (event_type, created_at);
CREATE INDEX ix_events_user_time ON events (user_id, created_at);

-- ---------------------------------------------------------------------
-- Триггеры updated_at
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_users_updated_at        BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_water_bodies_updated_at BEFORE UPDATE ON water_bodies
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_fish_species_updated_at BEFORE UPDATE ON fish_species
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_catches_updated_at      BEFORE UPDATE ON catches
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
