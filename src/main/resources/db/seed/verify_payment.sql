-- 1) 자리 수: 구역마다 ok가 true여야 함 (결제 완료 + 결제 대기 <= 정원)
select s.name, s.capacity,
       count(*) filter (where b.status = 'paid') as paid,
       count(*) filter (where b.status = 'pending') as pending,
       count(*) filter (where b.status = 'expired') as expired,
       count(*) filter (where b.status in ('paid', 'pending')) <= s.capacity as ok
from sections s
left join bookings b on b.section_id = s.id
where s.event_id = '<공연 id>'
group by s.name, s.capacity
order by s.name;

-- 2) 한 응모에 살아 있는 예매가 둘 이상: 0줄이어야 함
select entry_id, count(*)
from bookings
where event_id = '<공연 id>' and status in ('pending', 'paid')
group by entry_id
having count(*) > 1;

-- 3) 결제 완료 예매마다 paid 결제가 정확히 한 건이고 금액이 같음: 0줄이어야 함
--    (0원이라 결제 없이 확정된 예매는 여기에 나와도 정상)
select b.id, b.amount, count(p.id) as paid_payments, max(p.amount) as paid_amount
from bookings b
left join payments p on p.booking_id = b.id and p.status = 'paid'
where b.event_id = '<공연 id>' and b.status = 'paid'
group by b.id, b.amount
having count(p.id) <> 1 or max(p.amount) <> b.amount;

-- 4) 승계 기록: from_status는 expired, source는 succession, result는 waitlisted여야 함
--    (대기 순번 칸의 이름은 rank)
select sc.created_at, s.name as section,
       fb.status as from_status, tb.status as to_status, tb.source,
       r.result, r.rank
from successions sc
join bookings fb on fb.id = sc.from_booking_id
join bookings tb on tb.id = sc.to_booking_id
join sections s on s.id = fb.section_id
join draw_results r on r.entry_id = sc.to_entry_id
where fb.event_id = '<공연 id>'
order by sc.created_at;

-- 5) 결제 완료 예매인데 티켓이 없는 것: 0줄이어야 함
select b.id
from bookings b
left join tickets t on t.booking_id = b.id
where b.event_id = '<공연 id>' and b.status = 'paid' and t.id is null;

-- 6) 입장: used 티켓 수가 ok 스캔 수와 같아야 함
select (select count(*) from tickets t join bookings b on b.id = t.booking_id
        where b.event_id = '<공연 id>' and t.status = 'used') as used_tickets,
       (select count(*) from scan_logs
        where event_id = '<공연 id>' and result = 'ok') as ok_scans;