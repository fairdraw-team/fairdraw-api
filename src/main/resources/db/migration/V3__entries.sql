-- 1) 응모: 취소 시각
alter table entries add column if not exists cancelled_at timestamptz;

-- 2) 팀: 구역 하나 + 초대 코드
alter table teams add column if not exists section_id uuid references sections(id);
alter table teams add column if not exists invite_code text;
create unique index if not exists teams_invite_code_key on teams (invite_code);

-- 3) 팀원 중복 금지 (V1 이름 그대로 profile_id)
create unique index if not exists team_members_unique on team_members (team_id, profile_id);

-- 4) 봉인 정보 (seed, executed_at은 3주차 추첨 때 채우므로 비어 있어도 되게)
alter table draws add column if not exists roster_hash text;
alter table draws add column if not exists ballot_count int;
alter table draws add column if not exists sealed_at timestamptz;
alter table draws add column if not exists status text not null default 'sealed'
  check (status in ('sealed','drawn'));
alter table draws alter column seed drop not null;
alter table draws alter column executed_at drop not null;
create unique index if not exists draws_event_key on draws (event_id);

-- 5) 현황 조회가 빨라지도록
create index if not exists ballots_section_idx on ballots (section_id);
create index if not exists entries_event_idx on entries (event_id, status);

-- 6) 취소한 응모는 빼고, 한 사람 한 공연 1건 (DB 4번)
alter table entries drop constraint entries_event_id_user_id_key;
create unique index if not exists entries_one_per_user
  on entries (event_id, user_id) where status <> 'cancelled';