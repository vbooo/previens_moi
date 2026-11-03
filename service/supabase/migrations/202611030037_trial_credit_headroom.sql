-- Extend only the structural ceiling for already-available Firecrawl trial credits.
-- Runtime configuration is a separate audited operation. Preserve spent/held
-- counters, model-spending limits, slots, watches and existing reservations.
alter table private.general_budget drop constraint general_budget_credit_limit_check;
alter table private.general_budget add constraint general_budget_credit_limit_check
 check(credit_limit between 0 and 150);
