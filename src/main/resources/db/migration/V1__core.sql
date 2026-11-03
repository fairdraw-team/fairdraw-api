-- 회원: Supabase 로그인 사용자 1명 = 1행
create table profiles (
  id          uuid primary key,                  -- Supabase 사용자 번호(토큰의 sub)와 같은 값
  phone       text unique,                       -- 821012345678 모양(+ 없이). 같은 번호 두 계정 금지
  real_name   text,                              -- 실명
  role        text not null default 'user'
              check (role in ('user', 'admin')),
  created_at  timestamptz not null default now()
);

-- 공연
create table events (
  id               uuid primary key default gen_random_uuid(),
  organizer_id     uuid not null references profiles(id),  -- 만든 사람 = 주최자
  title            text not null,
  venue            text not null,                           -- 장소
  starts_at        timestamptz not null,                    -- 공연 일시
  entry_opens_at   timestamptz not null,                    -- 응모 시작
  entry_closes_at  timestamptz not null,                    -- 응모 마감
  payment_due_at   timestamptz not null,                    -- 결제 마감
  price            int not null check (price >= 0),         -- 기본 티켓 가격(원)
  ballots_per_user int not null default 3 check (ballots_per_user between 1 and 10),
  max_team_size    int not null default 2 check (max_team_size between 1 and 4),
  status           text not null default 'draft'
                   check (status in ('draft', 'open', 'closed', 'drawn', 'paying', 'done')),
  created_at       timestamptz not null default now(),
  -- 날짜 순서: 응모 시작, 응모 마감, 결제 마감, 공연 순
  check (entry_opens_at < entry_closes_at
     and entry_closes_at < payment_due_at
     and payment_due_at < starts_at)
);

-- 좌석 구역
create table sections (
  id         uuid primary key default gen_random_uuid(),
  event_id   uuid not null references events(id) on delete cascade,
  name       text not null,                          -- A, B, C
  capacity   int not null check (capacity > 0),      -- 정원
  price      int check (price >= 0),                 -- 비면 공연 기본 가격
  created_at timestamptz not null default now(),
  unique (event_id, name)                            -- 한 공연에 같은 구역 이름 금지
);

-- 입장 스태프: 휴대폰 번호로 등록, 아직 가입 안 한 사람도 가능
create table event_staff (
  id         uuid primary key default gen_random_uuid(),
  event_id   uuid not null references events(id) on delete cascade,
  phone      text not null,                          -- 821012345678 모양
  profile_id uuid references profiles(id),           -- 가입한 사람이면 연결
  created_at timestamptz not null default now(),
  unique (event_id, phone)
);

-- 자물쇠(RLS): 화면 쪽 열쇠로는 못 보고 백엔드만 보게
alter table profiles    enable row level security;
alter table events      enable row level security;
alter table sections    enable row level security;
alter table event_staff enable row level security;