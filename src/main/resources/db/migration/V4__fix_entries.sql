-- V4: V3에서 빠진 부분 보완
alter table entries drop constraint if exists entries_status_check;
update entries set status = 'active' where status = 'submitted';
alter table entries alter column status set default 'active';
alter table entries add constraint entries_status_check
  check (status in ('active', 'cancelled'));
alter table entries add column if not exists cancelled_at timestamptz;
alter table entries drop constraint if exists entries_event_id_user_id_key;
create unique index if not exists entries_active_unique
  on entries (event_id, user_id) where status = 'active';
create index if not exists entries_event_idx on entries (event_id, status);

alter table teams add column if not exists section_id uuid references sections(id);
alter table teams add column if not exists invite_code text;
create unique index if not exists teams_invite_code_key on teams (invite_code);

do $$
begin
    if exists (select 1 from information_schema.columns
               where table_name = 'draws' and column_name = 'executed_at') then
        alter table draws rename column executed_at to drawn_at;
    end if;
end $$;
alter table draws add column if not exists drawn_at timestamptz;
alter table draws alter column drawn_at drop not null;
alter table draws alter column drawn_at drop default;
alter table draws add column if not exists roster_hash text;
alter table draws add column if not exists ballot_count int;
alter table draws add column if not exists sealed_at timestamptz;
alter table draws add column if not exists status text not null default 'sealed';
create unique index if not exists draws_event_key on draws (event_id);

create index if not exists ballots_section_idx on ballots (section_id);
create index if not exists ballots_entry_idx on ballots (entry_id);