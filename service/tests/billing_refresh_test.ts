import { refreshSubscriptions } from "../supabase/functions/_shared/billing-runtime.ts";
import { Fault } from "../supabase/functions/_shared/http.ts";
const assert = (value: unknown) => {
  if (!value) throw new Error("assertion");
};
function fixture() {
  const calls: unknown[] = [];
  const state = {
    enabled: true,
    rows: [] as { owner_id: string; token: string }[],
    failed: false,
  };
  const dependencies = {
    policy: () =>
      Promise.resolve({
        enabled: state.enabled,
        test_only: true,
        monitoring_enabled: false,
        package_name: "fr.previensmoi.app",
      }),
    candidates: (owner: string | null, force: boolean) => {
      calls.push({ owner, force });
      return Promise.resolve(state.rows);
    },
    verify: async () => {
      calls.push("google");
      if (state.failed) throw new Error("controlled outage");
      return {} as never;
    },
  };
  return { calls, state, dependencies };
}
Deno.test("a free account with no purchase never needs Google credentials", async () => {
  const f = fixture();
  await refreshSubscriptions("owner", true, f.dependencies);
  assert(f.calls.length === 1 && !f.calls.includes("google"));
});
Deno.test("explicit verification passes force and waits for Google", async () => {
  const f = fixture();
  f.state.rows = [{ owner_id: "owner", token: "synthetic-token" }];
  await refreshSubscriptions("owner", true, f.dependencies);
  assert(
    JSON.stringify(f.calls) === '[{"owner":"owner","force":true},"google"]',
  );
});
Deno.test("Google outage fails an explicit check instead of silently succeeding", async () => {
  const f = fixture();
  f.state.rows = [{ owner_id: "owner", token: "synthetic-token" }];
  f.state.failed = true;
  let failure: unknown;
  try {
    await refreshSubscriptions("owner", true, f.dependencies);
  } catch (e) {
    failure = e;
  }
  assert(
    failure instanceof Fault && failure.code === "billing_unavailable" &&
      failure.status === 503,
  );
});
Deno.test("background reconciliation failure does not stop unrelated worker work", async () => {
  const f = fixture();
  f.state.rows = [{ owner_id: "owner", token: "synthetic-token" }];
  f.state.failed = true;
  await refreshSubscriptions(null, false, f.dependencies);
  assert(f.calls.includes("google"));
});
Deno.test("a disabled billing service cannot claim a successful explicit verification", async () => {
  const f = fixture();
  f.state.enabled = false;
  await refreshSubscriptions(null, false, f.dependencies);
  let failure: unknown;
  try {
    await refreshSubscriptions("owner", true, f.dependencies);
  } catch (e) {
    failure = e;
  }
  assert(failure instanceof Fault && f.calls.length === 0);
});
