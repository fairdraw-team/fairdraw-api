-- 추첨 연습 공연에 가짜 응모 30건 + 제비 90장 넣기
-- 조건: 시드 v2의 가짜 관객 30명이 있고, 공연의 구역 이름이 A, B, C
with ev as (select '00000000-0000-0000-0000-000000000101'::uuid as id),
fake as (
  select p.id as user_id, row_number() over (order by p.phone) as n
  from profiles p
  where p.phone like '8210999900%'
),
ins as (
  insert into entries (event_id, user_id, status)
  select ev.id, fake.user_id, 'active' from ev cross join fake
  returning id, user_id
)
insert into ballots (entry_id, section_id)
select ins.id, s.id
from ins
join fake on fake.user_id = ins.user_id
cross join generate_series(1, 3) as g(k)
join sections s
  on s.event_id = (select id from ev)
 and s.name = case
       when g.k = 1 then 'A'
       when g.k = 2 and fake.n % 2 = 1 then 'A'
       when g.k = 2 then 'B'
       when fake.n % 3 = 0 then 'C'
       else 'B'
     end;

-- 확인: A 45, B 35, C 10이 나와야 함 (실제 사람이 응모한 만큼 더 많을 수 있음)
select s.name, count(*) as ballots
from ballots b
join entries e on e.id = b.entry_id
join sections s on s.id = b.section_id
where e.event_id = '00000000-0000-0000-0000-000000000101' and e.status = 'active'
group by s.name order by s.name;