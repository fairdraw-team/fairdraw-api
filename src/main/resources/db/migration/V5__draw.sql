-- V5: 3주차 추첨

-- 1) draws: 추첨 시드(64글자)와 추첨 시각. 추첨 전에는 비어 있음
alter table draws add column if not exists seed text;
alter table draws alter column seed drop default;
alter table draws alter column seed type text using seed::text;
alter table draws alter column seed drop not null;
alter table draws add column if not exists drawn_at timestamptz;

-- 2) draws: 추첨 상태는 두 단어만
alter table draws drop constraint if exists draws_status_check;
alter table draws add constraint draws_status_check
  check (status in ('sealed', 'drawn'));

-- 3) draw_results: 없는 칸 추가
--    대기 순번은 새로 만들지 않고 기존 rank 칸을 씀
alter table draw_results add column if not exists section_id uuid references sections(id);
alter table draw_results add column if not exists result text;
alter table draw_results add column if not exists ballot_id uuid references ballots(id);
alter table draw_results add column if not exists draw_order int;
alter table draw_results add column if not exists created_at timestamptz not null default now();

-- 4) draw_results: 당첨·낙첨은 순번이 없으므로 rank, section_id는 비어 있을 수 있게
alter table draw_results alter column rank drop not null;
alter table draw_results alter column section_id drop not null;

-- 5) draw_results: 결과 값은 세 단어만
alter table draw_results drop constraint if exists draw_results_result_check;
alter table draw_results add constraint draw_results_result_check
  check (result in ('won', 'waitlisted', 'lost'));

-- 6) draw_results: 조회가 빨라지도록
--    (draw_id, entry_id) 중복 금지는 V1에 이미 있어서 여기서 만들지 않음
create index if not exists draw_results_lookup_idx
  on draw_results (draw_id, result, section_id);
create index if not exists draw_results_entry_idx
  on draw_results (entry_id);