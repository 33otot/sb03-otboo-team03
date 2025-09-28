-- UUID 생성을 위해 pgcrypto 확장 모듈을 활성화합니다.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Foreign key 제약 조건을 고려하여 테이블 생성 순서에 맞게 데이터를 삽입합니다.
-- UUID는 테스트의 일관성을 위해 미리 정의된 값을 사용합니다.

-- users (30)
INSERT INTO users (id, email, username, password, provider, provider_id, role, is_locked, created_at) VALUES
('a0000000-0000-0000-0000-000000000001', 'user1@example.com', 'user_1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'USER', false, NOW()),
('a0000000-0000-0000-0000-000000000002', 'user2@example.com', 'user_2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'USER', false, NOW() + interval '1 second'),
('a0000000-0000-0000-0000-000000000003', 'user3@example.com', 'user_3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'USER', false, NOW() + interval '2 second'),
('a0000000-0000-0000-0000-000000000004', 'user4@example.com', 'user_4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'USER', false, NOW() + interval '3 second'),
('a0000000-0000-0000-0000-000000000005', 'user5@example.com', 'user_5', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'USER', false, NOW() + interval '4 second'),
('a0000000-0000-0000-0000-000000000006', 'user6@example.com', 'user_6', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'USER', false, NOW() + interval '5 second'),
('a0000000-0000-0000-0000-000000000007', 'user7@example.com', 'user_7', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'USER', false, NOW() + interval '6 second'),
('a0000000-0000-0000-0000-000000000008', 'user8@example.com', 'user_8', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'USER', false, NOW() + interval '7 second'),
('a0000000-0000-0000-0000-000000000009', 'user9@example.com', 'user_9', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'USER', false, NOW() + interval '8 second'),
('a0000000-0000-0000-0000-000000000010', 'user10@example.com', 'user_10', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'USER', false, NOW() + interval '9 second'),
('a0000000-0000-0000-0000-000000000011', 'user11@example.com', 'user_11', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'USER', false, NOW() + interval '10 second'),
('a0000000-0000-0000-0000-000000000012', 'user12@example.com', 'user_12', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'USER', false, NOW() + interval '11 second'),
('a0000000-0000-0000-0000-000000000013', 'user13@example.com', 'user_13', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'USER', false, NOW() + interval '12 second'),
('a0000000-0000-0000-0000-000000000014', 'user14@example.com', 'user_14', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'USER', false, NOW() + interval '13 second'),
('a0000000-0000-0000-0000-000000000015', 'user15@example.com', 'user_15', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'USER', false, NOW() + interval '14 second'),
('a0000000-0000-0000-0000-000000000016', 'user16@example.com', 'user_16', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'ADMIN', false, NOW() + interval '15 second'),
('a0000000-0000-0000-0000-000000000017', 'user17@example.com', 'user_17', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'ADMIN', false, NOW() + interval '16 second'),
('a0000000-0000-0000-0000-000000000018', 'user18@example.com', 'user_18', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'ADMIN', false, NOW() + interval '17 second'),
('a0000000-0000-0000-0000-000000000019', 'user19@example.com', 'user_19', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'ADMIN', false, NOW() + interval '18 second'),
('a0000000-0000-0000-0000-000000000020', 'user20@example.com', 'user_20', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'ADMIN', false, NOW() + interval '19 second'),
('a0000000-0000-0000-0000-000000000021', 'user21@example.com', 'user_21', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'ADMIN', false, NOW() + interval '20 second'),
('a0000000-0000-0000-0000-000000000022', 'user22@example.com', 'user_22', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'ADMIN', false, NOW() + interval '21 second'),
('a0000000-0000-0000-0000-000000000023', 'user23@example.com', 'user_23', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'ADMIN', false, NOW() + interval '22 second'),
('a0000000-0000-0000-0000-000000000024', 'user24@example.com', 'user_24', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'ADMIN', false, NOW() + interval '23 second'),
('a0000000-0000-0000-0000-000000000025', 'user25@example.com', 'user_25', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'ADMIN', false, NOW() + interval '24 second'),
('a0000000-0000-0000-0000-000000000026', 'user26@example.com', 'user_26', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'ADMIN', false, NOW() + interval '25 second'),
('a0000000-0000-0000-0000-000000000027', 'user27@example.com', 'user_27', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'ADMIN', false, NOW() + interval '26 second'),
('a0000000-0000-0000-0000-000000000028', 'user28@example.com', 'user_28', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'ADMIN', false, NOW() + interval '27 second'),
('a0000000-0000-0000-0000-000000000029', 'user29@example.com', 'user_29', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'ADMIN', false, NOW() + interval '28 second'),
('a0000000-0000-0000-0000-000000000030', 'user30@example.com', 'user_30', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'LOCAL', NULL, 'ADMIN', false, NOW() + interval '29 second');

-- grids (5)
INSERT INTO grids (id, created_at, x, y) VALUES
('c0000000-0000-0000-0000-000000000001', NOW(), 60, 127), -- 서울
('c0000000-0000-0000-0000-000000000002', NOW(), 98, 76),  -- 부산
('c0000000-0000-0000-0000-000000000003', NOW(), 52, 38),  -- 제주
('c0000000-0000-0000-0000-000000000004', NOW(), 67, 100), -- 대전
('c0000000-0000-0000-0000-000000000005', NOW(), 55, 124); -- 인천

-- locations (5) - referencing grids
INSERT INTO locations (id, created_at, grid_id, latitude, longitude, location_names) VALUES
('b0000000-0000-0000-0000-000000000001', NOW(), 'c0000000-0000-0000-0000-000000000001', 37.5665, 126.9780, '{"서울시청"}'),
('b0000000-0000-0000-0000-000000000002', NOW(), 'c0000000-0000-0000-0000-000000000002', 35.1796, 129.0756, '{"부산역"}'),
('b0000000-0000-0000-0000-000000000003', NOW(), 'c0000000-0000-0000-0000-000000000003', 33.4996, 126.5312, '{"제주공항"}'),
('b0000000-0000-0000-0000-000000000004', NOW(), 'c0000000-0000-0000-0000-000000000004', 36.3504, 127.3845, '{"대전역"}'),
('b0000000-0000-0000-0000-000000000005', NOW(), 'c0000000-0000-0000-0000-000000000005', 37.4563, 126.7052, '{"인천국제공항"}');

-- clothes_attribute_defs (5)
INSERT INTO clothes_attribute_defs (id, name, created_at, updated_at) VALUES
('f0000000-0000-0000-0000-000000000001', '색상', NOW(), NOW()),
('f0000000-0000-0000-0000-000000000002', '소재', NOW(), NOW()),
('f0000000-0000-0000-0000-000000000003', '스타일', NOW(), NOW()),
('f0000000-0000-0000-0000-000000000004', '계절', NOW(), NOW()),
('f0000000-0000-0000-0000-000000000005', '사이즈', NOW(), NOW());

-- clothes_attribute_options (5 for each of 5 defs)
INSERT INTO clothes_attribute_options (id, value, definition_id, created_at, updated_at) VALUES
-- 색상 옵션
(gen_random_uuid(), '빨강', 'f0000000-0000-0000-0000-000000000001', NOW(), NOW()),
(gen_random_uuid(), '파랑', 'f0000000-0000-0000-0000-000000000001', NOW(), NOW()),
(gen_random_uuid(), '검정', 'f0000000-0000-0000-0000-000000000001', NOW(), NOW()),
(gen_random_uuid(), '흰색', 'f0000000-0000-0000-0000-000000000001', NOW(), NOW()),
(gen_random_uuid(), '초록', 'f0000000-0000-0000-0000-000000000001', NOW(), NOW()),
-- 소재 옵션
(gen_random_uuid(), '면', 'f0000000-0000-0000-0000-000000000002', NOW(), NOW()),
(gen_random_uuid(), '울', 'f0000000-0000-0000-0000-000000000002', NOW(), NOW()),
(gen_random_uuid(), '린넨', 'f0000000-0000-0000-0000-000000000002', NOW(), NOW()),
(gen_random_uuid(), '실크', 'f0000000-0000-0000-0000-000000000002', NOW(), NOW()),
(gen_random_uuid(), '폴리에스터', 'f0000000-0000-0000-0000-000000000002', NOW(), NOW()),
-- 스타일 옵션
(gen_random_uuid(), '캐주얼', 'f0000000-0000-0000-0000-000000000003', NOW(), NOW()),
(gen_random_uuid(), '포멀', 'f0000000-0000-0000-0000-000000000003', NOW(), NOW()),
(gen_random_uuid(), '스트릿', 'f0000000-0000-0000-0000-000000000003', NOW(), NOW()),
(gen_random_uuid(), '빈티지', 'f0000000-0000-0000-0000-000000000003', NOW(), NOW()),
(gen_random_uuid(), '미니멀', 'f0000000-0000-0000-0000-000000000003', NOW(), NOW()),
-- 계절 옵션
(gen_random_uuid(), '봄', 'f0000000-0000-0000-0000-000000000004', NOW(), NOW()),
(gen_random_uuid(), '여름', 'f0000000-0000-0000-0000-000000000004', NOW(), NOW()),
(gen_random_uuid(), '가을', 'f0000000-0000-0000-0000-000000000004', NOW(), NOW()),
(gen_random_uuid(), '겨울', 'f0000000-0000-0000-0000-000000000004', NOW(), NOW()),
(gen_random_uuid(), '사계절', 'f0000000-0000-0000-0000-000000000004', NOW(), NOW()),
-- 사이즈 옵션
(gen_random_uuid(), 'S', 'f0000000-0000-0000-0000-000000000005', NOW(), NOW()),
(gen_random_uuid(), 'M', 'f0000000-0000-0000-0000-000000000005', NOW(), NOW()),
(gen_random_uuid(), 'L', 'f0000000-0000-0000-0000-000000000005', NOW(), NOW()),
(gen_random_uuid(), 'XL', 'f0000000-0000-0000-0000-000000000005', NOW(), NOW()),
(gen_random_uuid(), 'FREE', 'f0000000-0000-0000-0000-000000000005', NOW(), NOW());

-- weathers (5)
INSERT INTO weathers (id, grid_id, created_at, forecasted_at, forecast_at, sky_status, temperature_current) VALUES
('d0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 'CLEAR', 25.0),
('d0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', NOW(), NOW(), NOW(), 'CLOUDY', 22.0),
('d0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', NOW(), NOW(), NOW(), 'MOSTLY_CLOUDY', 18.0),
('d0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000004', NOW(), NOW(), NOW(), 'CLOUDY', -2.0),
('d0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000005', NOW(), NOW(), NOW(), 'CLEAR', 28.0);


INSERT INTO feeds (id, author_id, weather_id, content, created_at, updated_at) VALUES
('f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', '오늘 서울 날씨 맑음! OOTD', NOW() - interval '5 day', NOW() - interval '5 day'),
('f1000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', '부산은 흐리네요', NOW() - interval '4 day', NOW() - interval '4 day'),
('f1000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', '제주도 대체로 흐림', NOW() - interval '3 day', NOW() - interval '3 day'),
('f1000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000004', '대전은 흐려요', NOW() - interval '2 day', NOW() - interval '2 day'),
('f1000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000005', '인천공항 날씨 최고', NOW() - interval '1 day', NOW() - interval '1 day');

-- comments (21 for feed f1000000-0000-0000-0000-000000000001)
INSERT INTO comments (id, feed_id, author_id, content, created_at) VALUES
('d0000000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Comment 1 for Feed 1', NOW() - interval '20 minute'),
('d0000000-0000-0000-0000-000000000002', 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', 'Comment 2 for Feed 1', NOW() - interval '19 minute'),
('d0000000-0000-0000-0000-000000000003', 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000003', 'Comment 3 for Feed 1', NOW() - interval '18 minute'),
('d0000000-0000-0000-0000-000000000004', 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000004', 'Comment 4 for Feed 1', NOW() - interval '17 minute'),
('d0000000-0000-0000-0000-000000000005', 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000005', 'Comment 5 for Feed 1', NOW() - interval '16 minute'),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Comment 6 for Feed 1', NOW() - interval '15 minute'),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', 'Comment 7 for Feed 1', NOW() - interval '14 minute'),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000003', 'Comment 8 for Feed 1', NOW() - interval '13 minute'),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000004', 'Comment 9 for Feed 1', NOW() - interval '12 minute'),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000005', 'Comment 10 for Feed 1', NOW() - interval '11 minute'),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Comment 11 for Feed 1', NOW() - interval '10 minute'),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', 'Comment 12 for Feed 1', NOW() - interval '9 minute'),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000003', 'Comment 13 for Feed 1', NOW() - interval '8 minute'),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000004', 'Comment 14 for Feed 1', NOW() - interval '7 minute'),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000005', 'Comment 15 for Feed 1', NOW() - interval '6 minute'),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Comment 16 for Feed 1', NOW() - interval '5 minute'),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', 'Comment 17 for Feed 1', NOW() - interval '4 minute'),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000003', 'Comment 18 for Feed 1', NOW() - interval '3 minute'),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000004', 'Comment 19 for Feed 1', NOW() - interval '2 minute'),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000005', 'Comment 20 for Feed 1', NOW() - interval '1 minute'),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Comment 21 for Feed 1', NOW());

-- clothes (5)
INSERT INTO clothes (id, owner_id, name, image_url, type, created_at, updated_at) VALUES
('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', '데일리 티셔츠', 'http://example.com/tshirt1.jpg', 'TOP', NOW(), NOW()),
('e0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', '슬림핏 청바지', 'http://example.com/jeans1.jpg', 'BOTTOM', NOW(), NOW()),
('e0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000002', '여름 원피스', 'http://example.com/dress1.jpg', 'DRESS', NOW(), NOW()),
('e0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000003', '가을 자켓', 'http://example.com/jacket1.jpg', 'OUTER', NOW(), NOW()),
('e0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000004', '운동화', 'http://example.com/shoes1.jpg', 'SHOES', NOW(), NOW());

-- clothes_attributes (5)
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
(gen_random_uuid(), 'e0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', '흰색', NOW(), NOW()),
(gen_random_uuid(), 'e0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000005', 'M', NOW(), NOW()),
(gen_random_uuid(), 'e0000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000001', '파랑', NOW(), NOW()),
(gen_random_uuid(), 'e0000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000003', '캐주얼', NOW(), NOW()),
(gen_random_uuid(), 'e0000000-0000-0000-0000-000000000004', 'f0000000-0000-0000-0000-000000000004', '가을', NOW(), NOW());

-- follows (5)
INSERT INTO follows (id, follower_id, followee_id, created_at) VALUES
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', NOW()),
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000003', NOW()),
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', NOW()),
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000004', NOW()),
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000005', NOW());

-- feed_likes (5)
INSERT INTO feed_likes (id, feed_id, user_id, created_at) VALUES
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', NOW()),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000003', NOW()),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', NOW()),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', NOW()),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000004', NOW());

-- notifications (5)
INSERT INTO notifications (id, receiver_id, created_at, title, content, level) VALUES
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', NOW(), '새로운 팔로워', 'user_two님이 회원님을 팔로우하기 시작했습니다.', 'INFO'),
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', NOW(), '새로운 댓글', 'user_three님이 회원님의 피드에 댓글을 남겼습니다.', 'INFO'),
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', NOW(), '피드 좋아요', 'user_one님이 회원님의 피드를 좋아합니다.', 'INFO'),
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', NOW(), '시스템 공지', '서비스 점검이 1시간 뒤에 시작됩니다.', 'WARNING'),
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', NOW(), '계정 관련', '비밀번호 변경이 필요합니다.', 'CRITICAL');

-- profiles (5)
INSERT INTO profiles (id, user_id, location_id, created_at, updated_at, name, gender, birth_date, temperature_sensitivity) VALUES
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', NOW(), NOW(), '김유저일', 'MALE', '1990-01-01', 3.0),
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', NOW(), NOW(), '이유저이', 'FEMALE', '1992-02-02', 4.0),
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', NOW(), NOW(), '박유저삼', 'MALE', '1993-03-03', 2.5),
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000004', NOW(), NOW(), '최유저사', 'FEMALE', '1994-04-04', 4.5),
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000005', NOW(), NOW(), '정유저오', 'OTHER', '1995-05-05', 3.5);

-- direct_messages (5)
INSERT INTO direct_messages (id, sender_id, receiver_id, created_at, message, is_read) VALUES
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', NOW() - interval '4 day', '안녕하세요', false),
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', NOW() - interval '3 day', '네 안녕하세요!', true),
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000003', NOW() - interval '2 day', '피드 잘보고 갑니다', false),
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', NOW() - interval '1 day', '질문 있습니다', false),
(gen_random_uuid(), 'a0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000002', NOW(), '오늘 날씨 어때요?', false);

-- feed_clothes (5)
INSERT INTO feed_clothes (id, feed_id, clothes_id, created_at) VALUES
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', NOW()),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', NOW()),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000003', NOW()),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000003', 'e0000000-0000-0000-0000-000000000004', NOW()),
(gen_random_uuid(), 'f1000000-0000-0000-0000-000000000004', 'e0000000-0000-0000-0000-000000000005', NOW());

-- recommendations (5)
INSERT INTO recommendations (id, user_id, weather_id, created_at) VALUES
('80000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', NOW()),
('80000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', NOW()),
('80000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', NOW()),
('80000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000004', NOW()),
('80000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000005', NOW());

-- recommendation_clothes (5)
INSERT INTO recommendation_clothes (id, recommendation_id, clothes_id, created_at) VALUES
(gen_random_uuid(), '80000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', NOW()),
(gen_random_uuid(), '80000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000002', NOW()),
(gen_random_uuid(), '80000000-0000-0000-0000-000000000003', 'e0000000-0000-0000-0000-000000000003', NOW()),
(gen_random_uuid(), '80000000-0000-0000-0000-000000000004', 'e0000000-0000-0000-0000-000000000004', NOW()),
(gen_random_uuid(), '80000000-0000-0000-0000-000000000005', 'e0000000-0000-0000-0000-000000000005', NOW());
