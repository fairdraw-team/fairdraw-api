-- 1. 주최자(가짜 회원)
insert into profiles (id, phone, real_name)
values ('00000000-0000-0000-0000-000000000001', '821000000001', '테스트주최자')
on conflict (id) do nothing;

-- 2. 연습 공연 (응모 중 상태)
insert into events (id, organizer_id, title, venue, starts_at, entry_opens_at,
                    entry_closes_at, payment_due_at, price, status)
values ('00000000-0000-0000-0000-000000000101',
        '00000000-0000-0000-0000-000000000001',
        '연습 공연', '학교 대강당',
        '2026-12-20 19:00+09', '2026-11-10 10:00+09',
        '2026-11-30 23:59+09', '2026-12-05 23:59+09',
        30000, 'open')
on conflict (id) do nothing;

-- 3. 구역 3개
insert into sections (event_id, name, capacity, price) values
  ('00000000-0000-0000-0000-000000000101', 'A', 50, 50000),
  ('00000000-0000-0000-0000-000000000101', 'B', 100, 30000),
  ('00000000-0000-0000-0000-000000000101', 'C', 150, 20000)
on conflict (event_id, name) do nothing;