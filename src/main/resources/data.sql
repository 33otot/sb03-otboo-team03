-- dummy_data.sql (Updated with fixed UUIDs for testing)

-- 고정된 UUID 정의 (테스트에서 참조하기 위함)
-- 이 UUID들은 테스트 코드에서 직접 사용할 수 있습니다.
-- 예: authorId = 'a0000000-0000-0000-0000-000000000001'
-- 예: weatherId = 'c0000000-0000-0000-0000-000000000001'
-- 예: clothesId = 'd0000000-0000-0000-0000-000000000001'

-- 데이터 삽입 순서는 외래 키 제약 조건을 고려하여 정렬됩니다.

-- users 테이블 더미 데이터 (5개)
INSERT INTO users (id, email, username, password, provider, role, is_locked, created_at, updated_at) VALUES
('a0000000-0000-0000-0000-000000000001', 'user1@example.com', 'user_one', '$2a$10$somehashedpassword1', 'local', 'USER', FALSE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000002', 'user2@example.com', 'user_two', '$2a$10$somehashedpassword2', 'local', 'USER', FALSE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000003', 'admin1@example.com', 'admin_one', '$2a$10$somehashedpassword3', 'local', 'ADMIN', FALSE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000004', 'user3@example.com', 'user_three', '$2a$10$somehashedpassword4', 'local', 'USER', FALSE, NOW(), NOW()),
('a0000000-0000-0000-0000-000000000005', 'user4@example.com', 'user_four', '$2a$10$somehashedpassword5', 'local', 'USER', FALSE, NOW(), NOW());

-- locations 테이블 더미 데이터 (5개)
INSERT INTO locations (id, created_at, latitude, longitude, x, y, location_names) VALUES
('b0000000-0000-0000-0000-000000000001', NOW(), 37.5665, 126.9780, 60, 127, '["서울시청"]'),
('b0000000-0000-0000-0000-000000000002', NOW(), 35.1796, 129.0756, 98, 76, '["부산역"]'),
('b0000000-0000-0000-0000-000000000003', NOW(), 33.4996, 126.5312, 52, 38, '["제주공항"]'),
('b0000000-0000-0000-0000-000000000004', NOW(), 36.3504, 127.3845, 67, 100, '["대전역"]'),
('b0000000-0000-0000-0000-000000000005', NOW(), 37.4563, 126.7052, 55, 124, '["인천국제공항"]');

-- clothes_attribute_defs 테이블 더미 데이터 (5개)
INSERT INTO clothes_attribute_defs (id, name, created_at, updated_at) VALUES
('e0000000-0000-0000-0000-000000000001', '색상', NOW(), NOW()),
('e0000000-0000-0000-0000-000000000002', '소재', NOW(), NOW()),
('e0000000-0000-0000-0000-000000000003', '스타일', NOW(), NOW()),
('e0000000-0000-0000-0000-000000000004', '계절', NOW(), NOW()),
('e0000000-0000-0000-0000-000000000005', '사이즈', NOW(), NOW());

-- clothes_attribute_options 테이블 더미 데이터 (clothes_attribute_defs 참조)
-- 각 definition_id에 대해 5개씩 옵션 생성
INSERT INTO clothes_attribute_options (id, value, definition_id, created_at, updated_at)
SELECT gen_random_uuid(), '빨강', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '색상' UNION ALL
SELECT gen_random_uuid(), '파랑', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '색상' UNION ALL
SELECT gen_random_uuid(), '검정', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '색상' UNION ALL
SELECT gen_random_uuid(), '흰색', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '색상' UNION ALL
SELECT gen_random_uuid(), '초록', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '색상' UNION ALL

SELECT gen_random_uuid(), '면', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '소재' UNION ALL
SELECT gen_random_uuid(), '울', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '소재' UNION ALL
SELECT gen_random_uuid(), '린넨', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '소재' UNION ALL
SELECT gen_random_uuid(), '실크', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '소재' UNION ALL
SELECT gen_random_uuid(), '폴리에스터', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '소재' UNION ALL

SELECT gen_random_uuid(), '캐주얼', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '스타일' UNION ALL
SELECT gen_random_uuid(), '포멀', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '스타일' UNION ALL
SELECT gen_random_uuid(), '스트릿', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '스타일' UNION ALL
SELECT gen_random_uuid(), '빈티지', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '스타일' UNION ALL
SELECT gen_random_uuid(), '미니멀', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '스타일' UNION ALL

SELECT gen_random_uuid(), '봄', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '계절' UNION ALL
SELECT gen_random_uuid(), '여름', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '계절' UNION ALL
SELECT gen_random_uuid(), '가을', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '계절' UNION ALL
SELECT gen_random_uuid(), '겨울', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '계절' UNION ALL
SELECT gen_random_uuid(), '사계절', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '계절' UNION ALL

SELECT gen_random_uuid(), 'S', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '사이즈' UNION ALL
SELECT gen_random_uuid(), 'M', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '사이즈' UNION ALL
SELECT gen_random_uuid(), 'L', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '사이즈' UNION ALL
SELECT gen_random_uuid(), 'XL', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '사이즈' UNION ALL
SELECT gen_random_uuid(), 'FREE', id, NOW(), NOW() FROM clothes_attribute_defs WHERE name = '사이즈';


-- clothes 테이블 더미 데이터 (5개, users 참조)
INSERT INTO clothes (id, name, image_url, type, created_at, updated_at, owner_id)
VALUES
('d0000000-0000-0000-0000-000000000001', '데일리 티셔츠', 'http://example.com/tshirt1.jpg', 'TOP', NOW(), NOW(), 'a0000000-0000-0000-0000-000000000001'),
('d0000000-0000-0000-0000-000000000002', '슬림핏 청바지', 'http://example.com/jeans1.jpg', 'BOTTOM', NOW(), NOW(), 'a0000000-0000-0000-0000-000000000001'),
(gen_random_uuid(), '여름 원피스', 'http://example.com/dress1.jpg', 'DRESS', NOW(), NOW(), 'a0000000-0000-0000-0000-000000000002'),
(gen_random_uuid(), '가을 자켓', 'http://example.com/jacket1.jpg', 'OUTER', NOW(), NOW(), 'a0000000-0000-0000-0000-000000000004'),
(gen_random_uuid(), '운동화', 'http://example.com/shoes1.jpg', 'SHOES', NOW(), NOW(), 'a0000000-0000-0000-0000-000000000005');


-- weathers 테이블 더미 데이터 (5개, locations 참조)
INSERT INTO weathers (id, location_id, created_at, forecasted_at, forecast_at, sky_status, precipitation_type, precipitation_amount, precipitation_prob, humidity_current, humidity_compared, temperature_current, temperature_compared, temperature_min, temperature_max, wind_speed, wind_as_word)
VALUES
('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', NOW(), NOW() - INTERVAL '1 hour', NOW() + INTERVAL '1 hour', 'CLEAR', NULL, 0.0, 0.0, 60.0, 5.0, 25.0, 2.0, 20.0, 30.0, 3.0, 'WEAK'),
('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', NOW(), NOW() - INTERVAL '30 minutes', NOW() + INTERVAL '2 hours', 'CLEAR', NULL, 0.0, 10.0, 70.0, -3.0, 22.0, -1.0, 18.0, 26.0, 5.0, 'MODERATE'),
(gen_random_uuid(), 'b0000000-0000-0000-0000-000000000003', NOW(), NOW() - INTERVAL '2 hours', NOW() + INTERVAL '3 hours', 'MOSTLY_CLOUDY', NULL, 5.0, 80.0, 90.0, 10.0, 18.0, -5.0, 15.0, 20.0, 7.0, 'STRONG'),
(gen_random_uuid(), 'b0000000-0000-0000-0000-000000000004', NOW(), NOW() - INTERVAL '10 minutes', NOW() + INTERVAL '1 hour', 'CLOUDY', NULL, 2.0, 90.0, 85.0, 8.0, -2.0, -3.0, -5.0, 0.0, 10.0, 'WEAK'),
(gen_random_uuid(), 'b0000000-0000-0000-0000-000000000005', NOW(), NOW() - INTERVAL '5 minutes', NOW() + INTERVAL '30 minutes', 'CLEAR', NULL, 0.0, 0.0, 50.0, 0.0, 28.0, 1.0, 25.0, 32.0, 2.0, 'STRONG');


-- recommendations 테이블 더미 데이터 (5개, users, weathers 참조)
INSERT INTO recommendations (id, user_id, weather_id, created_at)
SELECT gen_random_uuid(), u.id, w.id, NOW() FROM users u, weathers w WHERE u.id = 'a0000000-0000-0000-0000-000000000001' AND w.id = 'c0000000-0000-0000-0000-000000000001' UNION ALL
SELECT gen_random_uuid(), u.id, w.id, NOW() FROM users u, weathers w WHERE u.id = 'a0000000-0000-0000-0000-000000000002' AND w.id = 'c0000000-0000-0000-0000-000000000002' UNION ALL
SELECT gen_random_uuid(), u.id, w.id, NOW() FROM users u, weathers w WHERE u.email = 'admin1@example.com' AND w.location_names = '제주공항' UNION ALL
SELECT gen_random_uuid(), u.id, w.id, NOW() FROM users u, weathers w WHERE u.email = 'user3@example.com' AND w.location_names = '대전역' UNION ALL
SELECT gen_random_uuid(), u.id, w.id, NOW() FROM users u, weathers w WHERE u.email = 'user4@example.com' AND w.location_names = '인천국제공항';


-- profiles 테이블 더미 데이터 (5개, users, locations 참조)
INSERT INTO profiles (id, user_id, location_id, created_at, updated_at, name, gender, birth_date, temperature_sensitivity)
SELECT gen_random_uuid(), u.id, l.id, NOW(), NOW(), '김철수', 'MALE', '1990-05-15', 3.5 FROM users u, locations l WHERE u.id = 'a0000000-0000-0000-0000-000000000001' AND l.id = 'b0000000-0000-0000-0000-000000000001' UNION ALL
SELECT gen_random_uuid(), u.id, l.id, NOW(), NOW(), '이영희', 'FEMALE', '1992-11-20', 4.0 FROM users u, locations l WHERE u.id = 'a0000000-0000-0000-0000-000000000002' AND l.id = 'b0000000-0000-0000-0000-000000000002' UNION ALL
SELECT gen_random_uuid(), u.id, NULL, NOW(), NOW(), '관리자', 'OTHER', '1985-01-01', 3.0 FROM users u WHERE u.id = 'a0000000-0000-0000-0000-000000000003' UNION ALL
SELECT gen_random_uuid(), u.id, l.id, NOW(), NOW(), '박민준', 'MALE', '1995-03-01', 2.5 FROM users u, locations l WHERE u.email = 'user3@example.com' AND l.location_names = '대전역' UNION ALL
SELECT gen_random_uuid(), u.id, l.id, NOW(), NOW(), '최지우', 'FEMALE', '1998-07-10', 4.5 FROM users u, locations l WHERE u.email = 'user4@example.com' AND l.location_names = '인천국제공항';


-- direct_messages 테이블 더미 데이터 (5개, users 참조)
INSERT INTO direct_messages (id, sender_id, receiver_id, created_at, message, is_read)
SELECT gen_random_uuid(), u1.id, u2.id, NOW(), '안녕하세요, 피드 잘 봤습니다!', FALSE FROM users u1, users u2 WHERE u1.id = 'a0000000-0000-0000-0000-000000000001' AND u2.id = 'a0000000-0000-0000-0000-000000000002' UNION ALL
SELECT gen_random_uuid(), u2.id, u1.id, NOW() - INTERVAL '5 minutes', '네, 감사합니다!', TRUE FROM users u1, users u2 WHERE u1.id = 'a0000000-0000-0000-0000-000000000001' AND u2.id = 'a0000000-0000-0000-0000-000000000002' UNION ALL
SELECT gen_random_uuid(), u3.id, u1.id, NOW(), '문의드립니다.', FALSE FROM users u1, users u3 WHERE u1.id = 'a0000000-0000-0000-0000-000000000001' AND u3.id = 'a0000000-0000-0000-0000-000000000004' UNION ALL
SELECT gen_random_uuid(), u1.id, u4.id, NOW(), '반갑습니다.', FALSE FROM users u1, users u4 WHERE u1.id = 'a0000000-0000-0000-0000-000000000005' AND u4.id = 'a0000000-0000-0000-0000-000000000001' UNION ALL
SELECT gen_random_uuid(), u4.id, u3.id, NOW(), '오늘 날씨 좋네요.', TRUE FROM users u3, users u4 WHERE u3.id = 'a0000000-0000-0000-0000-000000000004' AND u4.id = 'a0000000-0000-0000-0000-000000000005';


-- feeds 테이블 더미 데이터 (5개, users, weathers 참조)
INSERT INTO feeds (id, author_id, weather_id, content, like_count, comment_count, is_deleted, created_at, updated_at)
SELECT gen_random_uuid(), u.id, w.id, '오늘의 OOTD! 날씨가 좋아서 가볍게 입었어요.', 10, 2, FALSE, NOW(), NOW() FROM users u, weathers w WHERE u.id = 'a0000000-0000-0000-0000-000000000001' AND w.id = 'c0000000-0000-0000-0000-000000000001' UNION ALL
SELECT gen_random_uuid(), u.id, w.id, '부산 여행 중! 바다 바람 시원하네요.', 15, 3, FALSE, NOW(), NOW() FROM users u, weathers w WHERE u.id = 'a0000000-0000-0000-0000-000000000002' AND w.id = 'c0000000-0000-0000-0000-000000000002' UNION ALL
SELECT gen_random_uuid(), u.id, NULL, '새로운 옷 샀어요! 어떤가요?', 5, 1, FALSE, NOW(), NOW() FROM users u WHERE u.id = 'a0000000-0000-0000-0000-000000000004' UNION ALL
SELECT gen_random_uuid(), u.id, w.id, '제주도 날씨 흐림. 따뜻하게 입었어요.', 8, 0, FALSE, NOW(), NOW() FROM users u, weathers w WHERE u.id = 'a0000000-0000-0000-0000-000000000005' AND w.location_names = '제주공항' UNION ALL
SELECT gen_random_uuid(), u.id, w.id, '가을 코디 추천! 트렌치코트의 계절.', 20, 5, FALSE, NOW(), NOW() FROM users u, weathers w WHERE u.id = 'a0000000-0000-0000-0000-000000000001' AND w.location_names = '대전역';


-- clothes_attributes 테이블 더미 데이터 (5개, clothes, clothes_attribute_defs, clothes_attribute_options 참조)
-- 각 clothes_id에 대해 여러 속성 부여
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
SELECT gen_random_uuid(), c.id, cad.id, '흰색', NOW(), NOW() FROM clothes c, clothes_attribute_defs cad WHERE c.id = 'd0000000-0000-0000-0000-000000000001' AND cad.id = 'e0000000-0000-0000-0000-000000000001' UNION ALL
SELECT gen_random_uuid(), c.id, cad.id, '면', NOW(), NOW() FROM clothes c, clothes_attribute_defs cad WHERE c.id = 'd0000000-0000-0000-0000-000000000001' AND cad.id = 'e0000000-0000-0000-0000-000000000002' UNION ALL
SELECT gen_random_uuid(), c.id, cad.id, 'M', NOW(), NOW() FROM clothes c, clothes_attribute_defs cad WHERE c.id = 'd0000000-0000-0000-0000-000000000001' AND cad.id = 'e0000000-0000-0000-0000-000000000005' UNION ALL

SELECT gen_random_uuid(), c.id, cad.id, '파랑', NOW(), NOW() FROM clothes c, clothes_attribute_defs cad WHERE c.id = 'd0000000-0000-0000-0000-000000000002' AND cad.id = 'e0000000-0000-0000-0000-000000000001' UNION ALL
SELECT gen_random_uuid(), c.id, cad.id, 'L', NOW(), NOW() FROM clothes c, clothes_attribute_defs cad WHERE c.id = 'd0000000-0000-0000-0000-000000000002' AND cad.id = 'e0000000-0000-0000-0000-000000000005' UNION ALL

SELECT gen_random_uuid(), c.id, cad.id, '캐주얼', NOW(), NOW() FROM clothes c, clothes_attribute_defs cad WHERE c.name = '여름 원피스' AND cad.name = '스타일' UNION ALL
SELECT gen_random_uuid(), c.id, cad.id, '여름', NOW(), NOW() FROM clothes c, clothes_attribute_defs cad WHERE c.name = '여름 원피스' AND cad.name = '계절' UNION ALL

SELECT gen_random_uuid(), c.id, cad.id, '울', NOW(), NOW() FROM clothes c, clothes_attribute_defs cad WHERE c.name = '가을 자켓' AND cad.name = '소재' UNION ALL
SELECT gen_random_uuid(), c.id, cad.id, '가을', NOW(), NOW() FROM clothes c, clothes_attribute_defs cad WHERE c.name = '가을 자켓' AND cad.name = '계절' UNION ALL

SELECT gen_random_uuid(), c.id, cad.id, '운동화', NOW(), NOW() FROM clothes c, clothes_attribute_defs cad WHERE c.name = '운동화' AND cad.name = '스타일';


-- comments 테이블 더미 데이터 (5개, feeds, users 참조)
INSERT INTO comments (id, feed_id, author_id, content, created_at)
SELECT gen_random_uuid(), f.id, u.id, '피드 너무 예뻐요!', NOW() FROM feeds f, users u WHERE f.content LIKE '%OOTD%' AND u.id = 'a0000000-0000-0000-0000-000000000002' UNION ALL
SELECT gen_random_uuid(), f.id, u.id, '어디서 구매하셨나요?', NOW() - INTERVAL '10 minutes' FROM feeds f, users u WHERE f.content LIKE '%OOTD%' AND u.id = 'a0000000-0000-0000-0000-000000000004' UNION ALL
SELECT gen_random_uuid(), f.id, u.id, '부산 저도 가고 싶네요!', NOW() FROM feeds f, users u WHERE f.content LIKE '%부산 여행%' AND u.id = 'a0000000-0000-0000-0000-000000000001' UNION ALL
SELECT gen_random_uuid(), f.id, u.id, '새 옷 축하드려요!', NOW() FROM feeds f, users u WHERE f.content LIKE '%새로운 옷%' AND u.id = 'a0000000-0000-0000-0000-000000000005' UNION ALL
SELECT gen_random_uuid(), f.id, u.id, '트렌치코트 저도 좋아해요!', NOW() FROM feeds f, users u WHERE f.content LIKE '%트렌치코트%' AND u.id = 'a0000000-0000-0000-0000-000000000002';


-- notifications 테이블 더미 데이터 (5개, users 참조)
INSERT INTO notifications (id, receiver_id, created_at, title, content, level)
SELECT gen_random_uuid(), id, NOW(), '새로운 팔로워', 'user_two님이 회원님을 팔로우하기 시작했습니다.', 'INFO' FROM users WHERE id = 'a0000000-0000-0000-0000-000000000001' UNION ALL
SELECT gen_random_uuid(), id, NOW() - INTERVAL '5 minutes', '피드 좋아요', 'user_one님이 회원님의 피드를 좋아합니다.', 'INFO' FROM users WHERE id = 'a0000000-0000-0000-0000-000000000002' UNION ALL
SELECT gen_random_uuid(), id, NOW(), '시스템 공지', '서비스 점검 예정 안내', 'WARNING' FROM users WHERE id = 'a0000000-0000-0000-0000-000000000001' UNION ALL
SELECT gen_random_uuid(), id, NOW(), '새로운 댓글', 'user_three님이 회원님의 피드에 댓글을 남겼습니다.', 'INFO' FROM users WHERE id = 'a0000000-0000-0000-0000-000000000005' UNION ALL
SELECT gen_random_uuid(), id, NOW(), '관리자 메시지', '중요한 업데이트가 있습니다.', 'CRITICAL' FROM users WHERE id = 'a0000000-0000-0000-0000-000000000003';


-- feed_likes 테이블 더미 데이터 (5개, feeds, users 참조)
INSERT INTO feed_likes (id, feed_id, user_id, created_at)
SELECT gen_random_uuid(), f.id, u.id, NOW() FROM feeds f, users u WHERE f.content LIKE '%OOTD%' AND u.id = 'a0000000-0000-0000-0000-000000000002' UNION ALL
SELECT gen_random_uuid(), f.id, u.id, NOW() FROM feeds f, users u WHERE f.content LIKE '%OOTD%' AND u.id = 'a0000000-0000-0000-0000-000000000004' UNION ALL
SELECT gen_random_uuid(), f.id, u.id, NOW() FROM feeds f, users u WHERE f.content LIKE '%부산 여행%' AND u.id = 'a0000000-0000-0000-0000-000000000001' UNION ALL
SELECT gen_random_uuid(), f.id, u.id, NOW() FROM feeds f, users u WHERE f.content LIKE '%새로운 옷%' AND u.id = 'a0000000-0000-0000-0000-000000000005' UNION ALL
SELECT gen_random_uuid(), f.id, u.id, NOW() FROM feeds f, users u WHERE f.content LIKE '%트렌치코트%' AND u.id = 'a0000000-0000-0000-0000-000000000002';


-- feed_clothes 테이블 더미 데이터 (5개, feeds, clothes 참조)
INSERT INTO feed_clothes (id, feed_id, clothes_id, created_at)
SELECT gen_random_uuid(), f.id, c.id, NOW() FROM feeds f, clothes c WHERE f.content LIKE '%OOTD%' AND c.id = 'd0000000-0000-0000-0000-000000000001' UNION ALL
SELECT gen_random_uuid(), f.id, c.id, NOW() FROM feeds f, clothes c WHERE f.content LIKE '%OOTD%' AND c.id = 'd0000000-0000-0000-0000-000000000002' UNION ALL
SELECT gen_random_uuid(), f.id, c.id, NOW() FROM feeds f, clothes c WHERE f.content LIKE '%부산 여행%' AND c.name = '여름 원피스' UNION ALL
SELECT gen_random_uuid(), f.id, c.id, NOW() FROM feeds f, clothes c WHERE f.content LIKE '%새로운 옷%' AND c.name = '가을 자켓' UNION ALL
SELECT gen_random_uuid(), f.id, c.id, NOW() FROM feeds f, clothes c WHERE f.content LIKE '%트렌치코트%' AND c.name = '운동화';


-- recommendation_clothes 테이블 더미 데이터 (5개, recommendations, clothes 참조)
INSERT INTO recommendation_clothes (id, recommendation_id, clothes_id, created_at)
SELECT gen_random_uuid(), r.id, c.id, NOW() FROM recommendations r, clothes c WHERE r.user_id = 'a0000000-0000-0000-0000-000000000001' AND c.id = 'd0000000-0000-0000-0000-000000000001' UNION ALL
SELECT gen_random_uuid(), r.id, c.id, NOW() FROM recommendations r, clothes c WHERE r.user_id = 'a0000000-0000-0000-0000-000000000001' AND c.id = 'd0000000-0000-0000-0000-000000000002' UNION ALL
SELECT gen_random_uuid(), r.id, c.id, NOW() FROM recommendations r, clothes c WHERE r.user_id = 'a0000000-0000-0000-0000-000000000002' AND c.name = '여름 원피스' UNION ALL
SELECT gen_random_uuid(), r.id, c.id, NOW() FROM recommendations r, clothes c WHERE r.user_id = 'a0000000-0000-0000-0000-000000000004' AND c.name = '가을 자켓' UNION ALL
SELECT gen_random_uuid(), r.id, c.id, NOW() FROM recommendations r, clothes c WHERE r.user_id = 'a0000000-0000-0000-0000-000000000005' AND c.name = '운동화';


-- follows 테이블 더미 데이터 (5개, users 참조)
INSERT INTO follows (id, follower_id, followee_id, created_at)
SELECT gen_random_uuid(), u1.id, u2.id, NOW() FROM users u1, users u2 WHERE u1.id = 'a0000000-0000-0000-0000-000000000001' AND u2.id = 'a0000000-0000-0000-0000-000000000002' UNION ALL
SELECT gen_random_uuid(), u2.id, u1.id, NOW() FROM users u1, users u2 WHERE u1.id = 'a0000000-0000-0000-0000-000000000002' AND u2.id = 'a0000000-0000-0000-0000-000000000001' UNION ALL
SELECT gen_random_uuid(), u3.id, u1.id, NOW() FROM users u1, users u3 WHERE u1.id = 'a0000000-0000-0000-0000-000000000004' AND u3.id = 'a0000000-0000-0000-0000-000000000001' UNION ALL
SELECT gen_random_uuid(), u4.id, u1.id, NOW() FROM users u1, users u4 WHERE u1.id = 'a0000000-0000-0000-0000-000000000005' AND u4.id = 'a0000000-0000-0000-0000-000000000001' UNION ALL
SELECT gen_random_uuid(), u1.id, u3.id, NOW() FROM users u1, users u3 WHERE u1.id = 'a0000000-0000-0000-0000-000000000001' AND u3.id = 'a0000000-0000-0000-0000-000000000004';

