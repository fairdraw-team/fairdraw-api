-- V6: 4주차 결제·승계·입장

-- 1) 예매: 어느 응모의 자리인지, 금액, 결제 기한, 구분(추첨/승계), 결제 시각
alter table bookings add column if not exists entry_id uuid references entries(id);
alter table bookings add column if not exists amount int;
alter table bookings add column if not exists pay_due_at timestamptz;
alter table bookings add column if not exists source text;
alter table bookings add column if not exists paid_at timestamptz;

alter table bookings drop constraint if exists bookings_source_check;
alter table bookings add constraint bookings_source_check
  check (source in ('draw', 'succession'));

-- 한 응모에 살아 있는 예매는 하나만
create unique index if not exists bookings_one_live_per_entry
  on bookings (entry_id) where status in ('pending', 'paid');
create index if not exists bookings_event_idx on bookings (event_id, status);
create index if not exists bookings_due_idx on bookings (status, pay_due_at);

-- 2) 결제: 결제 시각 (포트원 결제 번호는 V2의 payment_id 칸을 그대로 씀)
alter table payments add column if not exists paid_at timestamptz;
create index if not exists payments_booking_idx on payments (booking_id);

-- 3) 승계: 넘겨받은 사람에게 새로 생긴 예매. 만료된 자리 하나는 한 번만 넘어감
alter table successions add column if not exists to_booking_id uuid references bookings(id);
create unique index if not exists successions_from_key on successions (from_booking_id);

-- 4) 티켓: 입장 시각
alter table tickets add column if not exists used_at timestamptz;

-- 5) 입장 기록: 주최자도 스캔하고, 티켓을 찾지 못한 스캔도 기록하므로 비어 있어도 되게
alter table scan_logs alter column ticket_id drop not null;
alter table scan_logs alter column staff_id drop not null;
alter table scan_logs add column if not exists event_id uuid references events(id);
alter table scan_logs add column if not exists scanned_by uuid references profiles(id);
create index if not exists scan_logs_event_idx on scan_logs (event_id, scanned_at);