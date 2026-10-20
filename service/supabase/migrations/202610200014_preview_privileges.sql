-- Preview must stay behind the validated Edge entry point, including after replacement.
revoke all on function public.s3_preview(text,text,jsonb,uuid) from authenticated;
