-- dev_seed_v2.sql : 2주차 테스트용 가짜 관객 30명 + 응모 30건 + 팀 2개
do $$
declare
  v_event uuid;
  v_a uuid; v_b uuid; v_c uuid;
  v_team1 uuid; v_team2 uuid;
  v_user uuid;
  v_entry uuid;
  n int;
begin
  -- 0) 연습 공연과 구역 찾기 (제목·구역 이름은 1단계 ①에서 본 값으로)
  select id into v_event from events where title = '연습 공연';
  if v_event is null then raise exception '공연을 찾지 못했습니다. 제목을 확인하세요'; end if;
  select id into v_a from sections where event_id = v_event and name = 'A';
  select id into v_b from sections where event_id = v_event and name = 'B';
  select id into v_c from sections where event_id = v_event and name = 'C';
  if v_a is null or v_b is null or v_c is null then raise exception '구역 A/B/C를 찾지 못했습니다'; end if;

  -- 1) 가짜 관객 30명 (id 끝자리가 1~30, 번호 +821000000001 ~ +821000000030)
  for n in 1..30 loop
    v_user := ('00000000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid;
    -- [묶여 있으면 켜기] insert into auth.users (id, email, aud, role) values (v_user, 'seed' || n || '@fairdraw.test', 'authenticated', 'authenticated') on conflict (id) do nothing;
    insert into profiles (id, phone, real_name)
    values (v_user, '+82100000' || lpad(n::text, 4, '0'), '테스트관객' || n)
    on conflict (id) do update set phone = excluded.phone, real_name = excluded.real_name;
  end loop;

  -- 2) 팀 2개 (팀장 = 관객 1번, 3번)
  insert into teams (event_id, section_id, leader_id, invite_code)
  values (v_event, v_a, '00000000-0000-0000-0000-000000000001', 'SEEDA1')
  returning id into v_team1;
  insert into teams (event_id, section_id, leader_id, invite_code)
  values (v_event, v_b, '00000000-0000-0000-0000-000000000003', 'SEEDB1')
  returning id into v_team2;

  -- 3) 응모 30건 + 제비 90장
  for n in 1..30 loop
    v_user := ('00000000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid;

    insert into entries (event_id, user_id, team_id, status)
    values (v_event, v_user,
            case when n <= 2 then v_team1 when n <= 4 then v_team2 end,
            'submitted')
    returning id into v_entry;

    if n <= 2 or n between 5 and 22 then          -- A 3장
      insert into ballots (entry_id, section_id) select v_entry, v_a from generate_series(1, 3);
    elsif n <= 4 then                             -- 팀2: B 3장
      insert into ballots (entry_id, section_id) select v_entry, v_b from generate_series(1, 3);
    elsif n <= 29 then                            -- B 2장 + C 1장
      insert into ballots (entry_id, section_id) select v_entry, v_b from generate_series(1, 2);
      insert into ballots (entry_id, section_id) values (v_entry, v_c);
    else                                          -- C 3장
      insert into ballots (entry_id, section_id) select v_entry, v_c from generate_series(1, 3);
    end if;
  end loop;

  -- 4) 팀원 등록
  insert into team_members (team_id, profile_id) values
    (v_team1, '00000000-0000-0000-0000-000000000001'),
    (v_team1, '00000000-0000-0000-0000-000000000002'),
    (v_team2, '00000000-0000-0000-0000-000000000003'),
    (v_team2, '00000000-0000-0000-0000-000000000004');

  raise notice '시드 v2 완료: 공연 %', v_event;
end $$;