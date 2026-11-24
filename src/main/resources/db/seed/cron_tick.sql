-- 5분마다 백엔드의 tick을 부르는 알람. 실행할 때만 <CRON_SECRET>을 실제 값으로 바꿈
select cron.schedule(
  'fairdraw-tick',
  '*/5 * * * *',
  $$
  select net.http_post(
    url := 'https://fairdraw-api.vercel.app/api/internal/tick',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'X-Cron-Secret', '<CRON_SECRET>'
    ),
    body := '{}'::jsonb,
    timeout_milliseconds := 30000
  );
  $$
);

-- 멈출 때만 아래 줄의 앞 두 글자(--)를 지우고 그 줄만 실행
-- select cron.unschedule('fairdraw-tick');