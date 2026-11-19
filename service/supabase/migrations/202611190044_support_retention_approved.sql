-- Owner explicitly approved the 30-day support retention window.
select cron.schedule('previens-moi-support-retention','17 3 * * *',
 $$delete from private.support_messages where created_at < now()-interval '30 days'$$);
