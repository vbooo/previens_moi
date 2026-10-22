-- Credit units remain inside the provider free-plan balance. No paid top-up,
-- model-budget increase, reset of consumption, or monthly allowance.
-- The previous 40-credit qualification cap left no usable app trial.
-- Keep a hard finite ceiling; enabling/configuring a trial is separate.
alter table private.general_budget drop constraint general_budget_credit_limit_check;
alter table private.general_budget add constraint general_budget_credit_limit_check check(credit_limit between 0 and 64);
