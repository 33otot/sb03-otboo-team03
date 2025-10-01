-- UUID 생성을 위해 pgcrypto 확장 모듈을 활성화합니다.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Foreign key 제약 조건을 고려하여 테이블 생성 순서에 맞게 데이터를 삽입합니다.
-- UUID는 테스트의 일관성을 위해 미리 정의된 값을 사용합니다.

-- users (5)
INSERT INTO users (id, email, username, password, provider, role, is_locked, created_at, updated_at) VALUES
                                                                                                         ('a0000000-0000-0000-0000-000000000001', 'user1@example.com', 'user_one', '$2a$12$IVaVY0vlOV/08y7cAkd7e.z5kDBluSSvuOJukVnnCVCjzbXBZpzwa', 'LOCAL', 'ADMIN', FALSE, NOW(), NOW()),
                                                                                                         ('a0000000-0000-0000-0000-000000000002', 'user2@example.com', 'user_two', '$2a$12$IVaVY0vlOV/08y7cAkd7e.z5kDBluSSvuOJukVnnCVCjzbXBZpzwa', 'LOCAL', 'USER', FALSE, NOW(), NOW()),
                                                                                                         ('a0000000-0000-0000-0000-000000000003', 'user3@example.com', 'user_three', '$2a$12$IVaVY0vlOV/08y7cAkd7e.z5kDBluSSvuOJukVnnCVCjzbXBZpzwa', 'LOCAL', 'USER', FALSE, NOW(), NOW()),
                                                                                                         ('a0000000-0000-0000-0000-000000000004', 'user4@example.com', 'user_four', '$2a$12$IVaVY0vlOV/08y7cAkd7e.z5kDBluSSvuOJukVnnCVCjzbXBZpzwa', 'LOCAL', 'USER', FALSE, NOW(), NOW()),
                                                                                                         ('a0000000-0000-0000-0000-000000000005', 'user5@example.com', 'user_five', '$2a$12$IVaVY0vlOV/08y7cAkd7e.z5kDBluSSvuOJukVnnCVCjzbXBZpzwa', 'LOCAL', 'USER', FALSE, NOW(), NOW()),
                                                                                                         ('a0000000-0000-0000-0000-000000000007', 'd@d.com', 'doungukKim', '$2a$12$gvauG4PHUVSIrOLjYFJTieRc8OKO.LBwCbRX2kbdXHC5dnOkCg56C', 'LOCAL', 'USER', FALSE, NOW(), NOW());

-- grids (5)
INSERT INTO grids (id, created_at, x, y) VALUES
                                             ('c0000000-0000-0000-0000-000000000001', NOW(), 60, 127), -- 서울
                                             ('c0000000-0000-0000-0000-000000000002', NOW(), 98, 76),  -- 부산
                                             ('c0000000-0000-0000-0000-000000000003', NOW(), 52, 38),  -- 제주
                                             ('c0000000-0000-0000-0000-000000000004', NOW(), 67, 100), -- 대전
                                             ('c0000000-0000-0000-0000-000000000005', NOW(), 55, 124), -- 인천
                                             ('c0000000-0000-0000-0000-000000000006', NOW(), 61, 126); -- 서울특별시 성동구 송정동

-- locations (5) - referencing grids
INSERT INTO locations (id, created_at, grid_id, latitude, longitude, location_names) VALUES
                                                                                         ('b0000000-0000-0000-0000-000000000001', NOW(), 'c0000000-0000-0000-0000-000000000001', 37.5665, 126.9780, '{"서울시청"}'),
                                                                                         ('b0000000-0000-0000-0000-000000000002', NOW(), 'c0000000-0000-0000-0000-000000000002', 35.1796, 129.0756, '{"부산역"}'),
                                                                                         ('b0000000-0000-0000-0000-000000000003', NOW(), 'c0000000-0000-0000-0000-000000000003', 33.4996, 126.5312, '{"제주공항"}'),
                                                                                         ('b0000000-0000-0000-0000-000000000004', NOW(), 'c0000000-0000-0000-0000-000000000004', 36.3504, 127.3845, '{"대전역"}'),
                                                                                         ('b0000000-0000-0000-0000-000000000005', NOW(), 'c0000000-0000-0000-0000-000000000005', 37.4563, 126.7052, '{"인천국제공항"}'),
                                                                                         ('b0000000-0000-0000-0000-000000000006', NOW(), 'c0000000-0000-0000-0000-000000000006', 37.60128, 127.0972416, '{"서울특별시", "성동구", "송정동"}');
-- clothes_attribute_defs (5)
INSERT INTO clothes_attribute_defs (id, name, created_at, updated_at) VALUES
                                                                          ('f0000000-0000-0000-0000-000000000001', '색상', NOW(), NOW()),
                                                                          ('f0000000-0000-0000-0000-000000000002', '소재', NOW(), NOW()),
                                                                          ('f0000000-0000-0000-0000-000000000003', '스타일', NOW(), NOW()),
                                                                          ('f0000000-0000-0000-0000-000000000004', '계절', NOW(), NOW()),
                                                                          ('f0000000-0000-0000-0000-000000000005', '사이즈', NOW(), NOW()),
                                                                          ('f0000000-0000-0000-0000-000000000006', '두께', NOW(), NOW());

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
-- 사이즈 옵션
(gen_random_uuid(), 'S', 'f0000000-0000-0000-0000-000000000005', NOW(), NOW()),
(gen_random_uuid(), 'M', 'f0000000-0000-0000-0000-000000000005', NOW(), NOW()),
(gen_random_uuid(), 'L', 'f0000000-0000-0000-0000-000000000005', NOW(), NOW()),
(gen_random_uuid(), 'XL', 'f0000000-0000-0000-0000-000000000005', NOW(), NOW()),
(gen_random_uuid(), 'FREE', 'f0000000-0000-0000-0000-000000000005', NOW(), NOW()),
-- 색상 옵션 추가
(gen_random_uuid(), '베이지', 'f0000000-0000-0000-0000-000000000001', NOW(), NOW()),
(gen_random_uuid(), '카키', 'f0000000-0000-0000-0000-000000000001', NOW(), NOW()),
(gen_random_uuid(), '분홍', 'f0000000-0000-0000-0000-000000000001', NOW(), NOW()),
(gen_random_uuid(), '회색', 'f0000000-0000-0000-0000-000000000001', NOW(), NOW()),
(gen_random_uuid(), '네이비', 'f0000000-0000-0000-0000-000000000001', NOW(), NOW()),
(gen_random_uuid(), '갈색', 'f0000000-0000-0000-0000-000000000001', NOW(), NOW()),
-- 소재 옵션 추가
(gen_random_uuid(), '가죽', 'f0000000-0000-0000-0000-000000000002', NOW(), NOW()),
-- 사이즈 옵션 추가
(gen_random_uuid(), '270', 'f0000000-0000-0000-0000-000000000005', NOW(), NOW()),
(gen_random_uuid(), '240', 'f0000000-0000-0000-0000-000000000005', NOW(), NOW()),
-- 두께 옵션 추가
(gen_random_uuid(), 'LIGHT', 'f0000000-0000-0000-0000-000000000006', NOW(), NOW()),
(gen_random_uuid(), 'MEDIUM', 'f0000000-0000-0000-0000-000000000006', NOW(), NOW()),
(gen_random_uuid(), 'HEAVY', 'f0000000-0000-0000-0000-000000000006', NOW(), NOW());


-- weathers (5)
INSERT INTO weathers (
    id, grid_id, created_at, forecasted_at, forecast_at,
    sky_status, precipitation_type, precipitation_amount, precipitation_prob,
    humidity_current, humidity_compared,
    temperature_current, temperature_compared, temperature_min, temperature_max,
    wind_speed, wind_as_word) VALUES
                                  ('d0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', NOW(), NOW(), NOW(),
                                   'CLEAR', 'NONE', 0.0, 0.0, 40.0, 0.0, 25.0, 0.0, 18.0, 28.0, 2.5, 'WEAK'),
                                  ('d0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', NOW(), NOW(), NOW(),
                                   'MOSTLY_CLOUDY', 'RAIN', 1.2, 60.0, 55.0, 5.0, 22.0, -1.0, 16.0, 24.0, 3.0, 'MODERATE'),
                                  ('d0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', NOW(), NOW(), NOW(),
                                   'MOSTLY_MOSTLY_CLOUDY', 'NONE', 0.0, 10.0, 65.0, 2.0, 18.0, 0.5, 14.0, 20.0, 1.2, 'WEAK'),
                                  ('d0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000004', NOW(), NOW(), NOW(),
                                   'MOSTLY_CLOUDY', 'SNOW', 0.5, 80.0, 70.0, -3.0, -2.0, -1.0, -5.0, 2.0, 4.5, 'STRONG'),
                                  ('d0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000005', NOW(), NOW(), NOW(),
                                   'CLEAR', 'NONE', 0.0, 0.0, 35.0, 0.0, 28.0, 2.0, 22.0, 32.0, 2.0, 'WEAK');

-- feeds (5)
INSERT INTO feeds (id, author_id, weather_id, content, created_at, updated_at) VALUES
                                                                                   ('f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', '오늘 서울 날씨 맑음! OOTD', NOW() - interval '5 day', NOW() - interval '5 day'),
                                                                                   ('f1000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', '부산은 흐리네요', NOW() - interval '4 day', NOW() - interval '4 day'),
                                                                                   ('f1000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', '제주도 대체로 흐림', NOW() - interval '3 day', NOW() - interval '3 day'),
                                                                                   ('f1000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000004', '대전은 흐려요', NOW() - interval '2 day', NOW() - interval '2 day'),
-- 추가 피드 데이터 (10)
                                                                                   ('f1000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000005', '인천은 맑네요! 오늘 옷차림입니다.', NOW() - interval '1 day', NOW() - interval '1 day'),
                                                                                   ('f1000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002', '비오는 부산, 그래도 멋지게!', NOW() - interval '12 hour', NOW() - interval '12 hour'),
                                                                                   ('f1000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000003', '제주도 여행 중! 날씨 최고!', NOW() - interval '6 hour', NOW() - interval '6 hour'),
                                                                                   ('f1000000-0000-0000-0000-000000000008', 'a0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000004', '대전 눈와요! 다들 따뜻하게 입으세요.', NOW() - interval '3 hour', NOW() - interval '3 hour'),
                                                                                   ('f1000000-0000-0000-0000-000000000009', 'a0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000001', '서울에서 데일리룩 한 컷.', NOW() - interval '1 hour', NOW() - interval '1 hour'),
                                                                                   ('f1000000-0000-0000-0000-000000000010', 'a0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', '오늘의 OOTD. #데일리룩', NOW() - interval '2 day', NOW() - interval '2 day'),
                                                                                   ('f1000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', '부산 바다와 함께. #부산여행', NOW() - interval '2 day', NOW() - interval '2 day'),
                                                                                   ('f1000000-0000-0000-0000-000000000012', 'a0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', '제주도에서 힐링. #제주', NOW() - interval '3 day', NOW() - interval '3 day'),
                                                                                   ('f1000000-0000-0000-0000-000000000013', 'a0000000-0000-0000-0000-000000000004', 'd0000000-0000-0000-0000-000000000004', '추운 날씨엔 역시 패딩!', NOW() - interval '4 day', NOW() - interval '4 day'),
                                                                                   ('f1000000-0000-0000-0000-000000000014', 'a0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000005', '오늘의 출근룩. #OOTD', NOW() - interval '5 day', NOW() - interval '5 day');

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
                                                                                      ('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', '데일리 티셔츠', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/531db86b-c03a-4cff-9b7d-1048f6181827.jpg', 'TOP', NOW(), NOW()),
                                                                                      ('e0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', '슬림핏 청바지', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/5e83d9a1-67df-4803-9ac6-60debd7cd408.webp', 'BOTTOM', NOW(), NOW()),
                                                                                      ('e0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', '여름 원피스', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/a872df1f-89db-4678-a368-17f066e61616.webp', 'DRESS', NOW(), NOW()),
                                                                                      ('e0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', '가을 자켓', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/a4a29e54-3363-4c92-b792-46b60ccddda7.webp', 'OUTER', NOW(), NOW()),
                                                                                      ('e0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', '운동화', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/84f842ee-6fb6-480e-9591-0f7088aee2a6.webp', 'SHOES', NOW(), NOW()),
                                                                                      ('e0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000001', '화이트 셔츠', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/39c9a882-1721-4cd8-b36a-ef3be0423769.webp', 'TOP', NOW(), NOW()),
                                                                                      ('e0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000001', '데님 반바지', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/77615039-1260-4597-becb-0800c6ae2564.webp', 'BOTTOM', NOW(), NOW()),
                                                                                      ('e0000000-0000-0000-0000-000000000008', 'a0000000-0000-0000-0000-000000000001', '겨울 코트', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/a6b4d2ef-5bda-4157-8f6a-9cab4e55f3ad.webp', 'OUTER', NOW(), NOW()),
                                                                                      ('e0000000-0000-0000-0000-000000000009', 'a0000000-0000-0000-0000-000000000001', '여름 샌들', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/25857b8f-dfd2-41d1-b31d-32600587ae17.webp', 'SHOES', NOW(), NOW()),
                                                                                      ('e0000000-0000-0000-0000-000000000010', 'a0000000-0000-0000-0000-000000000001', '후드티', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/e894af2a-3b65-4cdf-a36d-1c3623c64651.webp', 'TOP', NOW(), NOW()),
-- 추가 의상 데이터 (26개)
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '블랙 슬랙스', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/cd9f0a23-7d3d-4c6c-abd3-dbfafbf4c596.webp', 'BOTTOM', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '베이지 면바지', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/34651e37-b04f-4509-8824-0775ed798ecc.webp', 'BOTTOM', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '체크 셔츠', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/f5b56fe8-671b-47ff-afdc-ccb4beff3fa1.webp', 'TOP', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '스트라이프 티셔츠', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/29f87970-0ffd-486e-905c-18f7364ad57d.webp', 'TOP', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '가죽 자켓', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/bc1bfc65-9a0f-4841-afb9-8ebd605b00ba.webp', 'OUTER', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '플리스 자켓', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/2487b2d0-3d7c-4213-8be9-1f58a97e11e1.webp', 'OUTER', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '롱패딩', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/2174d709-b8ee-48ae-aa46-ab076ac2ae26.webp', 'OUTER', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '니트 스웨터', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/9a7874dd-46e6-4090-8bb8-24df93371061.webp', 'TOP', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '카고 팬츠', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/56702e63-5041-4539-b67b-a3bdaf978b32.webp', 'BOTTOM', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '청치마', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/e32da07e-2cab-4656-857d-ebb5b711ba2d.jpg', 'BOTTOM', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '플라워 원피스', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/4d6091b7-cbb2-48e5-9c4b-dac16a0cc158.webp', 'DRESS', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '트레이닝 팬츠', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/426b5bec-1dbe-4ec3-982c-eac788489364.webp', 'BOTTOM', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '맨투맨', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/7c6e086f-8d63-4e2c-b3c8-31ef162d8bc0.webp', 'TOP', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '블라우스', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/48ed190a-f718-47cf-9e4e-46cf706de2ee.webp', 'TOP', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '로퍼', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/d11fd88b-3e99-4804-951c-f516613ae680.webp', 'SHOES', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '부츠', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/aa0cd948-10d1-444a-912b-3a3899a3af95.jpg', 'SHOES', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '야구 모자', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/f98620ba-4f01-4409-a771-bad994be61d1.png', 'HAT', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '비니', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/7d0e1bd9-f1f4-4cdb-9fae-61c2b2f6abe7.webp', 'HAT', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '목도리', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/790e5a97-a140-463b-ad79-47e9091d2b6c.webp', 'SCARF', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '네이비 코트', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/885042_3_500.jpg', 'OUTER', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '오렌지 셔츠', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/vibrant-orange-hawaiian-shirt_15533738.png', 'TOP', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '레인보우 후드티', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/751024_1_500.jpg', 'TOP', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '블루 진청바지', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/1653033528_image1_.webp', 'BOTTOM', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '카키 치노팬츠', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/Product_1667874964297.jpg', 'BOTTOM', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '블랙 가죽 구두', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/168c4486-56e5-490d-b4ea-673a85d1917d.jpg', 'SHOES', NOW(), NOW()),
                                                                                      (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', '장갑', 'https://samsam-otot-bucket.s3.ap-northeast-2.amazonaws.com/clothes/af34fc5e-368b-4a58-b03e-669d82a7a38c.webp', 'ETC', NOW(), NOW());

-- clothes_attributes (5)
-- 데일리 티셔츠
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', '흰색', NOW(), NOW()),   -- 색상
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000002', '면', NOW(), NOW()),   -- 소재
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000003', '캐주얼', NOW(), NOW()), -- 스타일
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000004', '여름', NOW(), NOW()), -- 계절
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000005', 'M', NOW(), NOW()),   -- 사이즈
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000006', '가능', NOW(), NOW());
-- 슬림핏 청바지
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000001', '파랑', NOW(), NOW()),   -- 색상
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000002', '면', NOW(), NOW()),   -- 소재
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000003', '캐주얼', NOW(), NOW()), -- 스타일
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000004', '봄', NOW(), NOW()),   -- 계절
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000005', 'L', NOW(), NOW()),   -- 사이즈
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000006', '불가능', NOW(), NOW());

-- 여름 원피스
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000001', '빨강', NOW(), NOW()),   -- 색상
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000002', '실크', NOW(), NOW()),   -- 소재
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000003', '포멀', NOW(), NOW()), -- 스타일
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000004', '여름', NOW(), NOW()), -- 계절
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000005', 'S', NOW(), NOW()),   -- 사이즈
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000003', 'f0000000-0000-0000-0000-000000000006', '불가능', NOW(), NOW());

-- 가을 자켓
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000004', 'f0000000-0000-0000-0000-000000000001', '검정', NOW(), NOW()),   -- 색상
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000004', 'f0000000-0000-0000-0000-000000000002', '울', NOW(), NOW()),     -- 소재
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000004', 'f0000000-0000-0000-0000-000000000003', '포멀', NOW(), NOW()),   -- 스타일
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000004', 'f0000000-0000-0000-0000-000000000004', '가을', NOW(), NOW()),   -- 계절
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000004', 'f0000000-0000-0000-0000-000000000005', 'L', NOW(), NOW()),     -- 사이즈
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000004', 'f0000000-0000-0000-0000-000000000006', '불가능', NOW(), NOW()); -- 방수

-- 운동화
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000005', 'f0000000-0000-0000-0000-000000000001', '흰색', NOW(), NOW()),   -- 색상
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000005', 'f0000000-0000-0000-0000-000000000002', '폴리에스터', NOW(), NOW()), -- 소재
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000005', 'f0000000-0000-0000-0000-000000000003', '스트릿', NOW(), NOW()), -- 스타일
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000005', 'f0000000-0000-0000-0000-000000000004', '여름', NOW(), NOW()), -- 계절
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000005', 'f0000000-0000-0000-0000-000000000005', 'FREE', NOW(), NOW()), -- 사이즈
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000005', 'f0000000-0000-0000-0000-000000000006', '가능', NOW(), NOW()); -- 방수

-- 화이트 셔츠
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000006', 'f0000000-0000-0000-0000-000000000001', '흰색', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000006', 'f0000000-0000-0000-0000-000000000002', '면', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000006', 'f0000000-0000-0000-0000-000000000003', '포멀', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000006', 'f0000000-0000-0000-0000-000000000004', '봄', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000006', 'f0000000-0000-0000-0000-000000000005', 'M', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000006', 'f0000000-0000-0000-0000-000000000006', '불가능', NOW(), NOW());

-- 데님 반바지
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000007', 'f0000000-0000-0000-0000-000000000001', '파랑', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000007', 'f0000000-0000-0000-0000-000000000002', '면', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000007', 'f0000000-0000-0000-0000-000000000003', '캐주얼', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000007', 'f0000000-0000-0000-0000-000000000004', '여름', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000007', 'f0000000-0000-0000-0000-000000000005', 'L', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000007', 'f0000000-0000-0000-0000-000000000006', '불가능', NOW(), NOW());

-- 겨울 코트
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000008', 'f0000000-0000-0000-0000-000000000001', '검정', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000008', 'f0000000-0000-0000-0000-000000000002', '울', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000008', 'f0000000-0000-0000-0000-000000000003', '포멀', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000008', 'f0000000-0000-0000-0000-000000000004', '겨울', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000008', 'f0000000-0000-0000-0000-000000000005', 'XL', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000008', 'f0000000-0000-0000-0000-000000000006', '가능', NOW(), NOW());

-- 여름 샌들
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000009', 'f0000000-0000-0000-0000-000000000001', '흰색', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000009', 'f0000000-0000-0000-0000-000000000002', '폴리에스터', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000009', 'f0000000-0000-0000-0000-000000000003', '스트릿', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000009', 'f0000000-0000-0000-0000-000000000004', '여름', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000009', 'f0000000-0000-0000-0000-000000000005', 'FREE', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000009', 'f0000000-0000-0000-0000-000000000006', '가능', NOW(), NOW());

-- 후드티
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000010', 'f0000000-0000-0000-0000-000000000001', '초록', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000010', 'f0000000-0000-0000-0000-000000000002', '폴리에스터', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000010', 'f0000000-0000-0000-0000-000000000003', '캐주얼', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000010', 'f0000000-0000-0000-0000-000000000004', '가을', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000010', 'f0000000-0000-0000-0000-000000000005', 'L', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), 'e0000000-0000-0000-0000-000000000010', 'f0000000-0000-0000-0000-000000000006', '불가능', NOW(), NOW());

-- 블랙 슬랙스
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='블랙 슬랙스'), 'f0000000-0000-0000-0000-000000000001', '검정', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='블랙 슬랙스'), 'f0000000-0000-0000-0000-000000000002', '폴리에스터', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='블랙 슬랙스'), 'f0000000-0000-0000-0000-000000000003', '포멀', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='블랙 슬랙스'), 'f0000000-0000-0000-0000-000000000004', '가을', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='블랙 슬랙스'), 'f0000000-0000-0000-0000-000000000005', 'L', NOW(), NOW());

-- 베이지 면바지
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='베이지 면바지'), 'f0000000-0000-0000-0000-000000000001', '베이지', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='베이지 면바지'), 'f0000000-0000-0000-0000-000000000002', '면', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='베이지 면바지'), 'f0000000-0000-0000-0000-000000000003', '캐주얼', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='베이지 면바지'), 'f0000000-0000-0000-0000-000000000004', '봄', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='베이지 면바지'), 'f0000000-0000-0000-0000-000000000005', 'M', NOW(), NOW());

-- 체크 셔츠
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='체크 셔츠'), 'f0000000-0000-0000-0000-000000000001', '파랑', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='체크 셔츠'), 'f0000000-0000-0000-0000-000000000002', '면', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='체크 셔츠'), 'f0000000-0000-0000-0000-000000000003', '캐주얼', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='체크 셔츠'), 'f0000000-0000-0000-0000-000000000004', '여름', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='체크 셔츠'), 'f0000000-0000-0000-0000-000000000005', 'L', NOW(), NOW());

-- 스트라이프 티셔츠
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='스트라이프 티셔츠'), 'f0000000-0000-0000-0000-000000000001', '흰색', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='스트라이프 티셔츠'), 'f0000000-0000-0000-0000-000000000002', '면', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='스트라이프 티셔츠'), 'f0000000-0000-0000-0000-000000000003', '캐주얼', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='스트라이프 티셔츠'), 'f0000000-0000-0000-0000-000000000004', '여름', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='스트라이프 티셔츠'), 'f0000000-0000-0000-0000-000000000005', 'M', NOW(), NOW());

-- 가죽 자켓
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='가죽 자켓'), 'f0000000-0000-0000-0000-000000000001', '검정', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='가죽 자켓'), 'f0000000-0000-0000-0000-000000000002', '가죽', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='가죽 자켓'), 'f0000000-0000-0000-0000-000000000003', '스트릿', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='가죽 자켓'), 'f0000000-0000-0000-0000-000000000004', '가을', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='가죽 자켓'), 'f0000000-0000-0000-0000-000000000005', 'L', NOW(), NOW());

-- 플리스 자켓
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='플리스 자켓'), 'f0000000-0000-0000-0000-000000000001', '회색', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='플리스 자켓'), 'f0000000-0000-0000-0000-000000000002', '폴리에스터', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='플리스 자켓'), 'f0000000-0000-0000-0000-000000000003', '캐주얼', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='플리스 자켓'), 'f0000000-0000-0000-0000-000000000004', '겨울', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='플리스 자켓'), 'f0000000-0000-0000-0000-000000000005', 'XL', NOW(), NOW());

-- 롱패딩
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='롱패딩'), 'f0000000-0000-0000-0000-000000000001', '검정', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='롱패딩'), 'f0000000-0000-0000-0000-000000000002', '폴리에스터', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='롱패딩'), 'f0000000-0000-0000-0000-000000000003', '포멀', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='롱패딩'), 'f0000000-0000-0000-0000-000000000004', '겨울', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='롱패딩'), 'f0000000-0000-0000-0000-000000000005', 'XL', NOW(), NOW());

-- 니트 스웨터
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='니트 스웨터'), 'f0000000-0000-0000-0000-000000000001', '분홍', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='니트 스웨터'), 'f0000000-0000-0000-0000-000000000002', '울', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='니트 스웨터'), 'f0000000-0000-0000-0000-000000000003', '빈티지', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='니트 스웨터'), 'f0000000-0000-0000-0000-000000000004', '겨울', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='니트 스웨터'), 'f0000000-0000-0000-0000-000000000005', 'L', NOW(), NOW());

-- 카고 팬츠
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='카고 팬츠'), 'f0000000-0000-0000-0000-000000000001', '카키', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='카고 팬츠'), 'f0000000-0000-0000-0000-000000000002', '면', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='카고 팬츠'), 'f0000000-0000-0000-0000-000000000003', '스트릿', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='카고 팬츠'), 'f0000000-0000-0000-0000-000000000004', '가을', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='카고 팬츠'), 'f0000000-0000-0000-0000-000000000005', 'M', NOW(), NOW());

-- 청치마
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='청치마'), 'f0000000-0000-0000-0000-000000000001', '파랑', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='청치마'), 'f0000000-0000-0000-0000-000000000002', '면', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='청치마'), 'f0000000-0000-0000-0000-000000000003', '캐주얼', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='청치마'), 'f0000000-0000-0000-0000-000000000004', '여름', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='청치마'), 'f0000000-0000-0000-0000-000000000005', 'S', NOW(), NOW());

-- 플라워 원피스
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='플라워 원피스'), 'f0000000-0000-0000-0000-000000000001', '분홍', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='플라워 원피스'), 'f0000000-0000-0000-0000-000000000002', '실크', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='플라워 원피스'), 'f0000000-0000-0000-0000-000000000003', '포멀', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='플라워 원피스'), 'f0000000-0000-0000-0000-000000000004', '여름', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='플라워 원피스'), 'f0000000-0000-0000-0000-000000000005', 'M', NOW(), NOW());

-- 트레이닝 팬츠
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='트레이닝 팬츠'), 'f0000000-0000-0000-0000-000000000001', '회색', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='트레이닝 팬츠'), 'f0000000-0000-0000-0000-000000000002', '폴리에스터', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='트레이닝 팬츠'), 'f0000000-0000-0000-0000-000000000003', '캐주얼', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='트레이닝 팬츠'), 'f0000000-0000-0000-0000-000000000004', '봄', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='트레이닝 팬츠'), 'f0000000-0000-0000-0000-000000000005', 'L', NOW(), NOW());

-- 맨투맨
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='맨투맨'), 'f0000000-0000-0000-0000-000000000001', '네이비', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='맨투맨'), 'f0000000-0000-0000-0000-000000000002', '면', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='맨투맨'), 'f0000000-0000-0000-0000-000000000003', '미니멀', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='맨투맨'), 'f0000000-0000-0000-0000-000000000004', '가을', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='맨투맨'), 'f0000000-0000-0000-0000-000000000005', 'XL', NOW(), NOW());

-- 블라우스
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='블라우스'), 'f0000000-0000-0000-0000-000000000001', '흰색', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='블라우스'), 'f0000000-0000-0000-0000-000000000002', '실크', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='블라우스'), 'f0000000-0000-0000-0000-000000000003', '포멀', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='블라우스'), 'f0000000-0000-0000-0000-000000000004', '봄', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='블라우스'), 'f0000000-0000-0000-0000-000000000005', 'M', NOW(), NOW());

-- 로퍼
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='로퍼'), 'f0000000-0000-0000-0000-000000000001', '갈색', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='로퍼'), 'f0000000-0000-0000-0000-000000000002', '가죽', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='로퍼'), 'f0000000-0000-0000-0000-000000000003', '포멀', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='로퍼'), 'f0000000-0000-0000-0000-000000000004', '가을', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='로퍼'), 'f0000000-0000-0000-0000-000000000005', '270', NOW(), NOW());

-- 부츠
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='부츠'), 'f0000000-0000-0000-0000-000000000001', '검정', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='부츠'), 'f0000000-0000-0000-0000-000000000002', '가죽', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='부츠'), 'f0000000-0000-0000-0000-000000000003', '스트릿', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='부츠'), 'f0000000-0000-0000-0000-000000000004', '겨울', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='부츠'), 'f0000000-0000-0000-0000-000000000005', '240', NOW(), NOW());

-- 야구 모자
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='야구 모자'), 'f0000000-0000-0000-0000-000000000001', '파랑', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='야구 모자'), 'f0000000-0000-0000-0000-000000000002', '폴리에스터', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='야구 모자'), 'f0000000-0000-0000-0000-000000000003', '캐주얼', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='야구 모자'), 'f0000000-0000-0000-0000-000000000004', '여름', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='야구 모자'), 'f0000000-0000-0000-0000-000000000005', 'FREE', NOW(), NOW());

-- 비니
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='비니'), 'f0000000-0000-0000-0000-000000000001', '검정', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='비니'), 'f0000000-0000-0000-0000-000000000002', '울', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='비니'), 'f0000000-0000-0000-0000-000000000003', '스트릿', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='비니'), 'f0000000-0000-0000-0000-000000000004', '겨울', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='비니'), 'f0000000-0000-0000-0000-000000000005', 'FREE', NOW(), NOW());

-- 목도리
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='목도리'), 'f0000000-0000-0000-0000-000000000001', '빨강', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='목도리'), 'f0000000-0000-0000-0000-000000000002', '울', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='목도리'), 'f0000000-0000-0000-0000-000000000003', '빈티지', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='목도리'), 'f0000000-0000-0000-0000-000000000004', '겨울', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='목도리'), 'f0000000-0000-0000-0000-000000000005', 'FREE', NOW(), NOW());

-- 장갑
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at) VALUES
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='장갑'), 'f0000000-0000-0000-0000-000000000001', '흰색', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='장갑'), 'f0000000-0000-0000-0000-000000000002', '울', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='장갑'), 'f0000000-0000-0000-0000-000000000003', '캐주얼', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='장갑'), 'f0000000-0000-0000-0000-000000000004', '겨울', NOW(), NOW()),
                                                                                                  (gen_random_uuid(), (SELECT id FROM clothes WHERE name='장갑'), 'f0000000-0000-0000-0000-000000000005', 'FREE', NOW(), NOW());

-- 데일리 티셔츠: LIGHT
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='데일리 티셔츠'), 'f0000000-0000-0000-0000-000000000006', 'LIGHT', NOW(), NOW());

-- 슬림핏 청바지: MEDIUM
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='슬림핏 청바지'), 'f0000000-0000-0000-0000-000000000006', 'MEDIUM', NOW(), NOW());

-- 여름 원피스: LIGHT
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='여름 원피스'), 'f0000000-0000-0000-0000-000000000006', 'LIGHT', NOW(), NOW());

-- 가을 자켓: HEAVY
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='가을 자켓'), 'f0000000-0000-0000-0000-000000000006', 'HEAVY', NOW(), NOW());

-- 운동화: LIGHT
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='운동화'), 'f0000000-0000-0000-0000-000000000006', 'LIGHT', NOW(), NOW());

-- 화이트 셔츠: LIGHT
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='화이트 셔츠'), 'f0000000-0000-0000-0000-000000000006', 'LIGHT', NOW(), NOW());

-- 데님 반바지: LIGHT
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='데님 반바지'), 'f0000000-0000-0000-0000-000000000006', 'LIGHT', NOW(), NOW());

-- 겨울 코트: HEAVY
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='겨울 코트'), 'f0000000-0000-0000-0000-000000000006', 'HEAVY', NOW(), NOW());

-- 여름 샌들: LIGHT
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='여름 샌들'), 'f0000000-0000-0000-0000-000000000006', 'LIGHT', NOW(), NOW());

-- 후드티: MEDIUM
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='후드티'), 'f0000000-0000-0000-0000-000000000006', 'MEDIUM', NOW(), NOW());

-- 블랙 슬랙스: MEDIUM
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='블랙 슬랙스'), 'f0000000-0000-0000-0000-000000000006', 'MEDIUM', NOW(), NOW());

-- 베이지 면바지: MEDIUM
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='베이지 면바지'), 'f0000000-0000-0000-0000-000000000006', 'MEDIUM', NOW(), NOW());

-- 체크 셔츠: LIGHT
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='체크 셔츠'), 'f0000000-0000-0000-0000-000000000006', 'LIGHT', NOW(), NOW());

-- 스트라이프 티셔츠: LIGHT
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='스트라이프 티셔츠'), 'f0000000-0000-0000-0000-000000000006', 'LIGHT', NOW(), NOW());

-- 가죽 자켓: HEAVY
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='가죽 자켓'), 'f0000000-0000-0000-0000-000000000006', 'HEAVY', NOW(), NOW());

-- 플리스 자켓: HEAVY
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='플리스 자켓'), 'f0000000-0000-0000-0000-000000000006', 'HEAVY', NOW(), NOW());

-- 롱패딩: HEAVY
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='롱패딩'), 'f0000000-0000-0000-0000-000000000006', 'HEAVY', NOW(), NOW());

-- 니트 스웨터: MEDIUM
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='니트 스웨터'), 'f0000000-0000-0000-0000-000000000006', 'MEDIUM', NOW(), NOW());

-- 카고 팬츠: MEDIUM
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='카고 팬츠'), 'f0000000-0000-0000-0000-000000000006', 'MEDIUM', NOW(), NOW());

-- 청치마: LIGHT
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='청치마'), 'f0000000-0000-0000-0000-000000000006', 'LIGHT', NOW(), NOW());

-- 플라워 원피스: LIGHT
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='플라워 원피스'), 'f0000000-0000-0000-0000-000000000006', 'LIGHT', NOW(), NOW());

-- 트레이닝 팬츠: MEDIUM
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='트레이닝 팬츠'), 'f0000000-0000-0000-0000-000000000006', 'MEDIUM', NOW(), NOW());

-- 맨투맨: MEDIUM
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='맨투맨'), 'f0000000-0000-0000-0000-000000000006', 'MEDIUM', NOW(), NOW());

-- 블라우스: LIGHT
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='블라우스'), 'f0000000-0000-0000-0000-000000000006', 'LIGHT', NOW(), NOW());

-- 로퍼: MEDIUM
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='로퍼'), 'f0000000-0000-0000-0000-000000000006', 'MEDIUM', NOW(), NOW());

-- 부츠: HEAVY
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='부츠'), 'f0000000-0000-0000-0000-000000000006', 'HEAVY', NOW(), NOW());

-- 야구 모자: LIGHT
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='야구 모자'), 'f0000000-0000-0000-0000-000000000006', 'LIGHT', NOW(), NOW());

-- 비니: MEDIUM
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='비니'), 'f0000000-0000-0000-0000-000000000006', 'MEDIUM', NOW(), NOW());

-- 목도리: MEDIUM
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='목도리'), 'f0000000-0000-0000-0000-000000000006', 'MEDIUM', NOW(), NOW());

-- 장갑: HEAVY
INSERT INTO clothes_attributes (id, clothes_id, definition_id, value, created_at, updated_at)
VALUES (gen_random_uuid(), (SELECT id FROM clothes WHERE name='장갑'), 'f0000000-0000-0000-0000-000000000006', 'HEAVY', NOW(), NOW());


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
                                                              (gen_random_uuid(), 'f1000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000004', NOW()),
                                                              (gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000004', NOW()),
                                                              (gen_random_uuid(), 'f1000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000005', NOW()),
                                                              (gen_random_uuid(), 'f1000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000003', NOW()),
                                                              (gen_random_uuid(), 'f1000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000004', NOW()),
                                                              (gen_random_uuid(), 'f1000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000002', NOW()),
                                                              (gen_random_uuid(), 'f1000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000005', NOW()),
                                                              (gen_random_uuid(), 'f1000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', NOW()),
                                                              (gen_random_uuid(), 'f1000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000002', NOW()),
                                                              (gen_random_uuid(), 'f1000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000003', NOW()),
                                                              (gen_random_uuid(), 'f1000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000005', NOW());


-- notifications (5)
INSERT INTO notifications (id, receiver_id, created_at, title, content, level) VALUES
                                                                                   (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', NOW(), '새로운 팔로워', 'user_two님이 회원님을 팔로우하기 시작했습니다.', 'INFO'),
                                                                                   (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', NOW(), '새로운 댓글', 'user_three님이 회원님의 피드에 댓글을 남겼습니다.', 'INFO'),
                                                                                   (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', NOW(), '피드 좋아요', 'user_one님이 회원님의 피드를 좋아합니다.', 'INFO'),
                                                                                   (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', NOW(), '시스템 공지', '서비스 점검이 1시간 뒤에 시작됩니다.', 'WARNING'),
                                                                                   (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', NOW(), '계정 관련', '비밀번호 변경이 필요합니다.', 'CRITICAL');

-- profiles (5)
INSERT INTO profiles (id, user_id, location_id, created_at, updated_at, name, gender, birth_date, temperature_sensitivity, profile_image_url) VALUES
                                                                                                                                                  (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', NOW(), NOW(), 'user_one', 'MALE', '1990-01-01', 3.0, null),
                                                                                                                                                  (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000006', NOW(), NOW(), 'user_two', 'FEMALE', '1992-02-02', 4.0, null),
                                                                                                                                                  (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', NOW(), NOW(), 'user_three', 'MALE', '1993-03-03', 2.5, null),
                                                                                                                                                  (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000004', NOW(), NOW(), 'user_four', 'FEMALE', '1994-04-04', 4.5, null),
                                                                                                                                                  (gen_random_uuid(), 'a0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000005', NOW(), NOW(), 'user_five', 'OTHER', '1995-05-05', 3.5, null);

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
                                                                   (gen_random_uuid(), 'f1000000-0000-0000-0000-000000000004', 'e0000000-0000-0000-0000-000000000005', NOW()),
-- feed_clothes for new feeds
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '인천은 맑네요! 오늘 옷차림입니다.'), 'e0000000-0000-0000-0000-000000000001', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '인천은 맑네요! 오늘 옷차림입니다.'), 'e0000000-0000-0000-0000-000000000002', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '비오는 부산, 그래도 멋지게!'), 'e0000000-0000-0000-0000-000000000003', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '비오는 부산, 그래도 멋지게!'), 'e0000000-0000-0000-0000-000000000004', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '제주도 여행 중! 날씨 최고!'), 'e0000000-0000-0000-0000-000000000005', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '제주도 여행 중! 날씨 최고!'), 'e0000000-0000-0000-0000-000000000006', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '대전 눈와요! 다들 따뜻하게 입으세요.'), 'e0000000-0000-0000-0000-000000000007', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '대전 눈와요! 다들 따뜻하게 입으세요.'), 'e0000000-0000-0000-0000-000000000008', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '서울에서 데일리룩 한 컷.'), 'e0000000-0000-0000-0000-000000000009', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '서울에서 데일리룩 한 컷.'), 'e0000000-0000-0000-0000-000000000010', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '오늘의 OOTD. #데일리룩'), 'e0000000-0000-0000-0000-000000000001', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '오늘의 OOTD. #데일리룩'), 'e0000000-0000-0000-0000-000000000002', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '부산 바다와 함께. #부산여행'), 'e0000000-0000-0000-0000-000000000003', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '부산 바다와 함께. #부산여행'), 'e0000000-0000-0000-0000-000000000004', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '제주도에서 힐링. #제주'), 'e0000000-0000-0000-0000-000000000005', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '제주도에서 힐링. #제주'), 'e0000000-0000-0000-0000-000000000006', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '추운 날씨엔 역시 패딩!'), 'e0000000-0000-0000-0000-000000000007', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '추운 날씨엔 역시 패딩!'), 'e0000000-0000-0000-0000-000000000008', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '오늘의 출근룩. #OOTD'), 'e0000000-0000-0000-0000-000000000009', NOW()),
                                                                   (gen_random_uuid(), (SELECT id FROM feeds WHERE content = '오늘의 출근룩. #OOTD'), 'e0000000-0000-0000-0000-000000000010', NOW());

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


-- 좋아요 카운트 백필
UPDATE feeds f
SET like_count = COALESCE(sub.cnt, 0),
    updated_at = NOW()
FROM (
         SELECT feed_id, COUNT(*) AS cnt
         FROM feed_likes
         GROUP BY feed_id
     ) sub
WHERE f.id = sub.feed_id;

-- 댓글 카운트 백필
UPDATE feeds f
SET comment_count = COALESCE(sub.cnt, 0),
    updated_at = NOW()
FROM (
         SELECT feed_id, COUNT(*) AS cnt
         FROM comments
         GROUP BY feed_id
     ) sub
WHERE f.id = sub.feed_id;

-- 혹시 남아있을 NULL 정리
UPDATE feeds SET like_count = 0 WHERE like_count IS NULL;
UPDATE feeds SET comment_count = 0 WHERE comment_count IS NULL;

INSERT INTO weathers (
    id, created_at, grid_id, forecast_at, forecasted_at,
    temperature_current, temperature_compared, humidity_current, humidity_compared, precipitation_prob,
    precipitation_amount, precipitation_type, sky_status,
    wind_as_word, wind_speed, temperature_max, temperature_min
) VALUES
-- 예보 발표 시각: 2025-09-30 11:00:00
-- 2025-09-30 (최고: 25, 최저: 18)
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-09-30 12:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 23.0, 1.0, 65.0, -5.0, 20.0, 0.1, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 1.4, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-09-30 13:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 23.0, 0.5, 60.0, -8.0, 30.0, 0.5, 'SHOWER', 'CLOUDY', 'WEAK', 2.0, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-09-30 14:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 24.0, 1.2, 55.0, -10.0, 20.0, 0.2, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 2.3, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-09-30 15:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 24.0, 1.5, 55.0, -12.0, 20.0, 0.1, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 2.6, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-09-30 16:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 24.0, 1.8, 50.0, -15.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 2.6, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-09-30 17:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 24.0, 2.0, 60.0, -10.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 2.2, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-09-30 18:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 23.0, 1.5, 65.0, -5.0, 20.0, 0.2, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 1.6, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-09-30 19:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 22.0, 1.0, 70.0, 0.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 1.4, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-09-30 20:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 22.0, 0.8, 80.0, 5.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 1.1, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-09-30 21:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 21.0, 0.5, 85.0, 8.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.7, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-09-30 22:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 21.0, 0.5, 85.0, 10.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.7, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-09-30 23:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 20.0, 0.2, 85.0, 12.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.7, 25.0, 18.0),
-- 2025-10-01 (최고: 27, 최저: 17)
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 00:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 20.0, 0.0, 90.0, 15.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.8, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 01:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 19.0, -0.5, 95.0, 18.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.2, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 02:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 19.0, -0.8, 90.0, 15.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.3, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 03:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 18.0, -1.0, 95.0, 20.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.2, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 04:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 18.0, -1.2, 95.0, 22.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.3, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 05:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 18.0, -1.5, 95.0, 25.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.7, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 06:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 18.0, -1.5, 95.0, 25.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.8, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 07:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 18.0, -1.0, 95.0, 20.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.7, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 08:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 19.0, -0.5, 90.0, 15.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 1.2, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 09:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 20.0, 0.0, 80.0, 10.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 1.6, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 10:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 22.0, 1.0, 75.0, 5.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 1.5, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 11:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 23.0, 1.5, 65.0, 0.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 1.0, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 12:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 24.0, 1.0, 60.0, -5.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.6, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 13:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 25.0, 2.0, 55.0, -5.0, 20.0, 0.2, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 0.6, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 14:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 26.0, 2.0, 50.0, -5.0, 20.0, 0.3, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 1.4, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 15:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 26.0, 2.0, 50.0, -5.0, 20.0, 0.1, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 2.4, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 16:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 26.0, 2.0, 50.0, 0.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 2.5, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 17:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 25.0, 1.0, 60.0, 0.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 2.4, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 18:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 24.0, 1.0, 70.0, 5.0, 20.0, 0.4, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 1.9, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 19:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 23.0, 1.0, 75.0, 5.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 1.3, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 20:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 22.0, 0.0, 85.0, 5.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.9, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 21:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 21.0, 0.0, 85.0, 0.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.6, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 22:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 21.0, 0.0, 90.0, 5.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.6, 27.0, 17.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-01 23:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 21.0, 1.0, 85.0, 0.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.6, 27.0, 17.0),
-- 2025-10-02 (최고: 26, 최저: 18)
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 00:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 20.0, 0.0, 90.0, 0.0, 20.0, 0.1, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 0.6, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 01:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 20.0, 1.0, 90.0, -5.0, 20.0, 0.2, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 0.7, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 02:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 20.0, 1.0, 90.0, 0.0, 20.0, 0.1, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 0.8, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 03:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 19.0, 1.0, 90.0, -5.0, 30.0, 1.0, 'SHOWER', 'CLOUDY', 'WEAK', 0.4, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 04:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 19.0, 1.0, 90.0, -5.0, 20.0, 0.5, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 1.1, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 05:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 19.0, 1.0, 95.0, 0.0, 20.0, 0.2, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 1.0, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 06:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 19.0, 1.0, 95.0, 0.0, 20.0, 0.1, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 1.3, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 07:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 19.0, 1.0, 95.0, 0.0, 20.0, 0.1, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 1.1, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 08:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 20.0, 1.0, 85.0, -5.0, 20.0, 0.0, 'NONE', 'MOSTLY_CLOUDY', 'WEAK', 1.3, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 09:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 21.0, 1.0, 80.0, 0.0, 20.0, 0.0, 'NONE', 'MOSTLY_CLOUDY', 'WEAK', 1.2, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 10:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 22.0, 0.0, 75.0, 0.0, 30.0, 0.5, 'SHOWER', 'CLOUDY', 'WEAK', 0.8, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 11:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 23.0, 0.0, 65.0, 0.0, 30.0, 0.8, 'SHOWER', 'CLOUDY', 'WEAK', 1.5, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 12:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 24.0, 1.0, 60.0, -5.0, 20.0, 0.2, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 2.1, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 13:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 25.0, 2.0, 60.0, 5.0, 30.0, 1.2, 'SHOWER', 'CLOUDY', 'WEAK', 0.5, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 14:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 25.0, -1.0, 60.0, 10.0, 30.0, 1.0, 'SHOWER', 'CLOUDY', 'WEAK', 1.0, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 15:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 25.0, -1.0, 60.0, 10.0, 30.0, 0.8, 'SHOWER', 'CLOUDY', 'WEAK', 1.0, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 16:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 25.0, -1.0, 60.0, 10.0, 20.0, 0.1, 'RAIN', 'MOSTLY_CLOUDY', 'WEAK', 0.8, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 17:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 25.0, 0.0, 65.0, 5.0, 30.0, 0.4, 'RAIN', 'CLOUDY', 'WEAK', 0.9, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 18:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 24.0, 0.0, 75.0, 5.0, 30.0, 0.5, 'RAIN', 'CLOUDY', 'WEAK', 1.1, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 19:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 23.0, 0.0, 75.0, 0.0, 20.0, 0.0, 'NONE', 'MOSTLY_CLOUDY', 'WEAK', 0.9, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 20:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 23.0, 1.0, 85.0, 0.0, 20.0, 0.0, 'NONE', 'MOSTLY_CLOUDY', 'WEAK', 0.8, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 21:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 22.0, 1.0, 90.0, 5.0, 20.0, 0.0, 'NONE', 'MOSTLY_CLOUDY', 'WEAK', 0.7, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 22:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 22.0, 1.0, 90.0, 0.0, 20.0, 0.0, 'NONE', 'MOSTLY_CLOUDY', 'WEAK', 0.6, 26.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-02 23:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 21.0, 0.0, 90.0, 5.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 0.2, 26.0, 18.0),
-- 2025-10-03 (최고: 25, 최저: 18)
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-03 00:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 21.0, 1.0, 90.0, 0.0, 60.0, 1.5, 'RAIN', 'CLOUDY', 'WEAK', 0.6, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-03 03:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 20.0, 1.0, 90.0, 0.0, 70.0, 2.0, 'RAIN', 'CLOUDY', 'WEAK', 1.0, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-03 06:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 19.0, 0.0, 80.0, -15.0, 80.0, 5.0, 'RAIN', 'CLOUDY', 'WEAK', 1.0, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-03 09:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 20.0, -1.0, 75.0, -5.0, 70.0, 3.0, 'RAIN', 'CLOUDY', 'WEAK', 1.0, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-03 12:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 22.0, -2.0, 65.0, 5.0, 60.0, 1.0, 'RAIN', 'CLOUDY', 'WEAK', 1.0, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-03 15:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 25.0, 0.0, 60.0, 0.0, 60.0, 0.5, 'RAIN', 'CLOUDY', 'WEAK', 2.0, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-03 18:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 23.0, -1.0, 65.0, -10.0, 70.0, 2.5, 'RAIN', 'CLOUDY', 'WEAK', 2.0, 25.0, 18.0),
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-03 21:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 22.0, 0.0, 70.0, -20.0, 60.0, 1.0, 'RAIN', 'CLOUDY', 'WEAK', 1.0, 25.0, 18.0),
-- 2025-10-04 (최고: 24, 최저: 17)
(GEN_RANDOM_UUID(), NOW(), 'c0000000-0000-0000-0000-000000000006', '2025-10-04 00:00:00'::TIMESTAMPTZ, '2025-09-30 11:00:00'::TIMESTAMPTZ, 21.0, 0.0, 75.0, -15.0, 0.0, 0.0, 'NONE', 'CLEAR', 'WEAK', 1.0, 24.0, 17.0);
