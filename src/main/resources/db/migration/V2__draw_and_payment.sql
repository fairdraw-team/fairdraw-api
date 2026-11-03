-- 연석 팀
create table teams (
  id         uuid primary key default gen_random_uuid(),
  event_id   uuid not null references events(id) on delete cascade,
  leader_id  uuid not null references profiles(id),
  created_at timestamptz not null default now()
);

create table team_members (
  team_id    uuid not null references teams(id) on delete cascade,
  profile_id uuid not null references profiles(id),
  joined_at  timestamptz not null default now(),
  primary key (team_id, profile_id)
);

-- 응모: 한 사람이 같은 공연에 한 번만
create table entries (
  id         uuid primary key default gen_random_uuid(),
  event_id   uuid not null references events(id),
  user_id    uuid not null references profiles(id),
  team_id    uuid references teams(id),
  status     text not null default 'submitted'
             check (status in ('submitted', 'cancelled', 'won', 'lost')),
  created_at timestamptz not null default now(),
  unique (event_id, user_id)
);

-- 제비: 응모 1건에 여러 장, 구역별
create table ballots (
  id         uuid primary key default gen_random_uuid(),
  entry_id   uuid not null references entries(id) on delete cascade,
  section_id uuid not null references sections(id),
  created_at timestamptz not null default now()
);

-- 추첨
create table draws (
  id          uuid primary key default gen_random_uuid(),
  event_id    uuid not null references events(id),
  seed        text,                                -- 재현 가능한 추첨용 값
  executed_at timestamptz not null default now()
);

-- 추첨 결과: 같은 추첨에서 한 응모 두 번 당첨 금지
create table draw_results (
  id         uuid primary key default gen_random_uuid(),
  draw_id    uuid not null references draws(id) on delete cascade,
  entry_id   uuid not null references entries(id),
  section_id uuid references sections(id),
  rank       int not null,
  unique (draw_id, entry_id)
);

-- 예매
create table bookings (
  id              uuid primary key default gen_random_uuid(),
  event_id        uuid not null references events(id),
  user_id         uuid not null references profiles(id),
  section_id      uuid not null references sections(id),
  draw_result_id  uuid references draw_results(id),
  status          text not null default 'pending'
                  check (status in ('pending', 'paid', 'expired', 'cancelled')),
  created_at      timestamptz not null default now()
);

-- 결제
create table payments (
  id          uuid primary key default gen_random_uuid(),
  booking_id  uuid not null references bookings(id),
  payment_id  text not null unique,                -- 포트원 결제 번호
  amount      int not null check (amount >= 0),
  status      text not null default 'ready'
              check (status in ('ready', 'paid', 'failed', 'cancelled')),
  created_at  timestamptz not null default now()
);

-- 티켓
create table tickets (
  id         uuid primary key default gen_random_uuid(),
  booking_id uuid not null unique references bookings(id),
  code       text not null unique,                 -- QR에 들어갈 값
  status     text not null default 'valid'
             check (status in ('valid', 'used', 'void')),
  created_at timestamptz not null default now()
);

-- 입장 기록
create table scan_logs (
  id         uuid primary key default gen_random_uuid(),
  ticket_id  uuid not null references tickets(id),
  staff_id   uuid not null references event_staff(id),
  result     text not null check (result in ('ok', 'duplicate', 'invalid')),
  scanned_at timestamptz not null default now()
);

-- 승계 기록: 미결제로 빠진 자리를 다음 순번이 받음
create table successions (
  id               uuid primary key default gen_random_uuid(),
  from_booking_id  uuid not null references bookings(id),
  to_entry_id      uuid not null references entries(id),
  created_at       timestamptz not null default now()
);

-- 알림
create table notifications (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references profiles(id),
  type       text not null,
  body       text not null,
  read_at    timestamptz,
  created_at timestamptz not null default now()
);

alter table teams         enable row level security;
alter table team_members  enable row level security;
alter table entries       enable row level security;
alter table ballots       enable row level security;
alter table draws         enable row level security;
alter table draw_results  enable row level security;
alter table bookings      enable row level security;
alter table payments      enable row level security;
alter table tickets       enable row level security;
alter table scan_logs     enable row level security;
alter table successions   enable row level security;
alter table notifications enable row level security;