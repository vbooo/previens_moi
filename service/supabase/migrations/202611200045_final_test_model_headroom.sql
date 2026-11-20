-- A final user-requested watch needs interpretation headroom beside the funded
-- daily trial. Raise only the structural model ceiling by USD 0.02. Runtime
-- funding remains a separate guarded operation inside the cumulative EUR 2 cap.
-- Preserve used, unknown/held and trial-committed amounts; no credits are reset.
alter table private.general_budget drop constraint general_budget_model_limit_check;
alter table private.general_budget add constraint general_budget_model_limit_check
 check(model_limit between 0 and 320000);
