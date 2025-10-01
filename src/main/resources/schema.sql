DROP TABLE IF EXISTS
    recommendation_clothes,
    feed_clothes,
    feed_likes,
    notifications,
    comments,
    clothes_attributes,
    feeds,
    direct_messages,
    profiles,
    recommendations,
    weathers,
    clothes,
    clothes_attribute_options,
    clothes_attribute_defs,
    locations,
    grids,
    follows,
    users
    CASCADE;


-- users 테이블
CREATE TABLE IF NOT EXISTS users
(
    id UUID NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255),
    provider VARCHAR(50) DEFAULT 'local' NOT NULL,
    provider_id VARCHAR(255),
    role VARCHAR(16) DEFAULT 'USER' NOT NULL,
    is_locked BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    temporary_password_expires_at TIMESTAMPTZ,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT ck_users_role CHECK (role IN ('USER','ADMIN'))
);

CREATE TABLE IF NOT EXISTS grids
(
    id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,

    CONSTRAINT pk_grids PRIMARY KEY (id),
    CONSTRAINT uq_grids UNIQUE (x, y)
);

-- locations 테이블
CREATE TABLE IF NOT EXISTS locations
(
    id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    grid_id UUID NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    location_names TEXT[] NOT NULL,

    CONSTRAINT pk_locations PRIMARY KEY (id),
    CONSTRAINT ck_locations_lat CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_locations_lng CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT uq_locations UNIQUE (latitude, longitude)
);

ALTER TABLE locations
    ADD CONSTRAINT fk_locations_grid FOREIGN KEY (grid_id) REFERENCES grids (id) ON DELETE CASCADE;

-- clothes_attribute_defs
CREATE TABLE IF NOT EXISTS clothes_attribute_defs
(
    id UUID NOT NULL,
    name VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,

    CONSTRAINT pk_clothes_attribute_defs PRIMARY KEY (id)
);

-- clothes_attribute_options
CREATE TABLE IF NOT EXISTS clothes_attribute_options
(
    id UUID NOT NULL,
    value VARCHAR(50) NOT NULL,
    definition_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,

    CONSTRAINT pk_clothes_attribute_options PRIMARY KEY (id),
    CONSTRAINT fk_cao_def FOREIGN KEY (definition_id)
        REFERENCES clothes_attribute_defs (id) ON DELETE CASCADE,
    -- 같은 정의 내 value의 유일성 (복합 FK 대상)
    CONSTRAINT uq_cao_definition_value UNIQUE (definition_id, value),
    -- (definition_id,id)로도 유일
    CONSTRAINT uq_cao_definition_id_id UNIQUE (definition_id, id)
);

-- clothes
CREATE TABLE IF NOT EXISTS clothes
(
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    image_url TEXT,
    type VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    owner_id UUID NOT NULL,

    CONSTRAINT pk_clothes PRIMARY KEY (id),
    CONSTRAINT fk_clothes_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_clothes_type CHECK (
        type IN (
                 'TOP','BOTTOM','DRESS','OUTER','UNDERWEAR',
                 'ACCESSORY','SHOES','SOCKS','HAT','BAG','SCARF','ETC'
            )
        )
);

-- weathers 테이블
CREATE TABLE IF NOT EXISTS weathers
(
    id UUID NOT NULL,
    grid_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    forecasted_at TIMESTAMPTZ NOT NULL,
    forecast_at TIMESTAMPTZ NOT NULL,
    sky_status VARCHAR(32),
    precipitation_type VARCHAR(32),
    precipitation_amount DOUBLE PRECISION,
    precipitation_prob DOUBLE PRECISION,
    humidity_current DOUBLE PRECISION,
    humidity_compared DOUBLE PRECISION,
    temperature_current DOUBLE PRECISION,
    temperature_compared DOUBLE PRECISION,
    temperature_min DOUBLE PRECISION,
    temperature_max DOUBLE PRECISION,
    wind_speed DOUBLE PRECISION,
    wind_as_word VARCHAR(32),

    CONSTRAINT pk_weathers PRIMARY KEY (id),
    CONSTRAINT fk_weathers_grid FOREIGN KEY (grid_id) REFERENCES grids (id) ON DELETE CASCADE,
    CONSTRAINT ck_weathers_time_order CHECK (forecasted_at <= forecast_at),
    CONSTRAINT uq_weathers_grid_forecast UNIQUE (grid_id, forecast_at, forecasted_at)
);


-- recommendations 테이블
CREATE TABLE IF NOT EXISTS recommendations
(
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    weather_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_recommendations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_recommendations_weather FOREIGN KEY (weather_id) REFERENCES weathers (id) ON DELETE CASCADE
);

-- profiles 테이블
CREATE TABLE IF NOT EXISTS profiles
(
    id UUID NOT NULL,
    user_id UUID UNIQUE NOT NULL,
    location_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    name VARCHAR(32) NOT NULL,
    gender VARCHAR(32),
    birth_date DATE,
    temperature_sensitivity DOUBLE PRECISION DEFAULT 3.0,
    profile_image_url VARCHAR(255),

    CONSTRAINT pk_profiles PRIMARY KEY (id),
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_profiles_location FOREIGN KEY (location_id) REFERENCES locations (id) ON DELETE SET NULL,
    CONSTRAINT ck_profiles_gender CHECK (gender IN ('MALE','FEMALE', 'OTHER')),
    CONSTRAINT ck_profiles_temp_sensitivity CHECK (temperature_sensitivity BETWEEN 1 AND 5)
);

-- direct_messages 테이블
CREATE TABLE IF NOT EXISTS direct_messages
(
    id UUID PRIMARY KEY,
    sender_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_dm_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_dm_receiver FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_dm_no_self  CHECK (sender_id <> receiver_id) -- 본인한테 DM 금지
);

-- feeds 테이블
CREATE TABLE IF NOT EXISTS feeds
(
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL,
    weather_id UUID NOT NULL,
    content TEXT NOT NULL,
    like_count BIGINT DEFAULT 0 NOT NULL,
    comment_count BIGINT DEFAULT 0 NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,

    CONSTRAINT fk_feeds_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_feeds_weather FOREIGN KEY (weather_id) REFERENCES weathers (id) ON DELETE SET NULL,
    CONSTRAINT ck_feeds_like_count_nonneg CHECK (like_count >= 0),
    CONSTRAINT ck_feeds_comment_count_nonneg CHECK (comment_count >= 0)
);

-- clothes_attributes
-- 한 의상에 여러 정의를 가질 수 있으나, 정의별로 선택값은 1개만 가능
-- 선택값은 (definition_id, value)가 options에 반드시 존재해야 함
CREATE TABLE IF NOT EXISTS clothes_attributes
(
    id UUID NOT NULL,
    clothes_id UUID NOT NULL,
    definition_id UUID NOT NULL,
    value VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,

    CONSTRAINT pk_clothes_attributes PRIMARY KEY (id),
    CONSTRAINT fk_ca_clothes FOREIGN KEY (clothes_id) REFERENCES clothes (id) ON DELETE CASCADE,
    CONSTRAINT fk_ca_def FOREIGN KEY (definition_id) REFERENCES clothes_attribute_defs (id) ON DELETE CASCADE,

    -- 선택한 값이 해당 정의에 존재하는 옵션이어야 함
    CONSTRAINT fk_ca_definition_value FOREIGN KEY (definition_id, value)
        REFERENCES clothes_attribute_options (definition_id, value)
        ON DELETE CASCADE,

    -- 한 의상에서 같은 '정의'는 1번만 선택 가능
    CONSTRAINT uq_ca_clothes_definition UNIQUE (clothes_id, definition_id)
);

-- comments 테이블
CREATE TABLE IF NOT EXISTS comments
(
    id UUID PRIMARY KEY,
    feed_id UUID NOT NULL,
    author_id UUID NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_comments_feed FOREIGN KEY (feed_id) REFERENCES feeds (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE
);

-- notifications 테이블
CREATE TABLE IF NOT EXISTS notifications
(
    id UUID PRIMARY KEY,
    receiver_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(255) NOT NULL,
    level VARCHAR(20) NOT NULL,

    CONSTRAINT fk_notifications_receiver FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE
);

-- feed_likes 테이블
CREATE TABLE IF NOT EXISTS feed_likes
(
    id UUID PRIMARY KEY,
    feed_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_feed_likes_feed FOREIGN KEY (feed_id) REFERENCES feeds (id) ON DELETE CASCADE,
    CONSTRAINT fk_feed_likes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_feed_likes UNIQUE (feed_id, user_id)
);

-- feed_clothes 테이블
CREATE TABLE IF NOT EXISTS feed_clothes
(
    id UUID PRIMARY KEY,
    feed_id UUID NOT NULL,
    clothes_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_feed_clothes_feed FOREIGN KEY (feed_id) REFERENCES feeds (id) ON DELETE CASCADE,
    CONSTRAINT fk_feed_clothes_clothes FOREIGN KEY (clothes_id) REFERENCES clothes (id) ON DELETE CASCADE,
    CONSTRAINT uq_feed_clothes UNIQUE (feed_id, clothes_id)
);

-- recommendation_clothes 테이블
CREATE TABLE IF NOT EXISTS recommendation_clothes
(
    id UUID PRIMARY KEY,
    recommendation_id UUID NOT NULL,
    clothes_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_recommendation FOREIGN KEY (recommendation_id) REFERENCES recommendations (id) ON DELETE CASCADE,
    CONSTRAINT fk_recommendation_clothes FOREIGN KEY (clothes_id) REFERENCES clothes (id) ON DELETE CASCADE,
    CONSTRAINT uq_recommendation_clothes UNIQUE (recommendation_id, clothes_id)
);

-- follows 테이블
CREATE TABLE IF NOT EXISTS follows
(
    id UUID PRIMARY KEY,
    follower_id UUID NOT NULL,
    followee_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_follows_followee FOREIGN KEY (followee_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_follows UNIQUE (follower_id, followee_id),
    CONSTRAINT ck_follow_no_self CHECK (follower_id <> followee_id) -- 본인한테 팔로우 금지
);

-- 인덱스들

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

CREATE INDEX IF NOT EXISTS idx_ca_clothes ON clothes_attributes (clothes_id);
CREATE INDEX IF NOT EXISTS idx_cao_definition ON clothes_attribute_options (definition_id);

-- follow index
CREATE INDEX IF NOT EXISTS idx_follows_follower_created_id_desc ON follows (follower_id, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_follows_followee ON follows (followee_id);
CREATE INDEX IF NOT EXISTS idx_follows_follower ON follows (follower_id);

-- notification index
CREATE INDEX IF NOT EXISTS idx_notifications_receiver_created_at ON notifications (receiver_id, created_at DESC); --알림

-- DM index
CREATE INDEX IF NOT EXISTS idx_dm_pair_created_at ON direct_messages (sender_id, receiver_id, created_at DESC); --대화

-- Location index
CREATE INDEX IF NOT EXISTS idx_locations_coordinates ON locations (longitude, latitude); -- 경도/위도