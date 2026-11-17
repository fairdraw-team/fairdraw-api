-- 1) 시드: seed_ok가 true여야 함
select d.seed,
       d.seed = encode(digest(
         d.roster_hash || ':' || (extract(epoch from d.drawn_at) * 1000)::bigint::text,
         'sha256'), 'hex') as seed_ok
from draws d
where d.event_id = '00000000-0000-0000-0000-000000000101';

-- 2) 순서: 앞 10장이 공개 제비 목록 API의 앞 10장과 같아야 함
select row_number() over (
         order by encode(digest(d.seed || ':' || b.id::text, 'sha256'), 'hex')
       ) as draw_order,
       b.id as ballot_id, s.name as section
from draws d
join entries e on e.event_id = d.event_id and e.status = 'active'
join ballots b on b.entry_id = e.id
join sections s on s.id = b.section_id
where d.event_id = '00000000-0000-0000-0000-000000000101'
order by draw_order
limit 10;

-- 3) 결과: 구역별 당첨 수가 정원 이하여야 함
select s.name, s.capacity,
       count(*) filter (where r.result = 'won') as won,
       count(*) filter (where r.result = 'waitlisted') as waitlisted
from draw_results r
join draws d on d.id = r.draw_id
join sections s on s.id = r.section_id
where d.event_id = '00000000-0000-0000-0000-000000000101'
group by s.name, s.capacity
order by s.name;

-- 4) 결과 행 수: 합계가 살아 있는 응모 수와 같아야 함
select r.result, count(*)
from draw_results r
join draws d on d.id = r.draw_id
where d.event_id = '00000000-0000-0000-0000-000000000101'
group by r.result;