import {
  accountId,
  PlayFailure,
  PLUS_PRODUCT,
  publisher,
  reconcilePurchase,
  verifySubscription,
} from "../supabase/functions/_shared/play-billing.ts";
const assert = (v: unknown) => {
  if (!v) throw new Error("assertion");
};
const now = Date.parse("2026-11-16T12:00:00Z");
function purchase(state = "SUBSCRIPTION_STATE_ACTIVE") {
  return {
    subscriptionState: state,
    acknowledgementState: "ACKNOWLEDGEMENT_STATE_PENDING",
    externalAccountIdentifiers: { obfuscatedExternalAccountId: "owner-hash" },
    testPurchase: {},
    latestOrderId: "order-1",
    lineItems: [{
      productId: PLUS_PRODUCT,
      expiryTime: "2026-12-16T12:00:00Z",
      offerDetails: { basePlanId: "monthly" },
      autoRenewingPlan: { autoRenewEnabled: true },
    }],
  };
}
function rejects(value: any, reason: string) {
  let code = "";
  try {
    verifySubscription(value, "owner-hash", true, now);
  } catch (e) {
    code = (e as PlayFailure).code;
  }
  assert(code === reason);
}
Deno.test("subscription access is based on verified Google state and expiry, not renewal flag", () => {
  for (const state of ["ACTIVE", "CANCELED", "IN_GRACE_PERIOD"]) {
    assert(
      verifySubscription(
        purchase("SUBSCRIPTION_STATE_" + state),
        "owner-hash",
        true,
        now,
      ).active,
    );
  }
  for (const state of ["ON_HOLD", "PAUSED", "EXPIRED", "PENDING"]) {
    assert(
      !verifySubscription(
        purchase("SUBSCRIPTION_STATE_" + state),
        "owner-hash",
        true,
        now,
      ).active,
    );
  }
  const old = purchase();
  old.lineItems[0].expiryTime = "2026-11-15T12:00:00Z";
  assert(!verifySubscription(old, "owner-hash", true, now).active);
});
Deno.test("wrong account, product, base plan and introductory offers cannot grant Plus", () => {
  const other = purchase();
  other.externalAccountIdentifiers.obfuscatedExternalAccountId = "other";
  rejects(other, "purchase_account_mismatch");
  const product = purchase();
  product.lineItems[0].productId = "other";
  rejects(product, "purchase_invalid");
  const plan = purchase();
  plan.lineItems[0].offerDetails.basePlanId = "annual";
  rejects(plan, "purchase_invalid");
  const offer: any = purchase();
  offer.lineItems[0].offerDetails.offerId = "automatic-trial";
  rejects(offer, "purchase_invalid");
});
Deno.test("test-only mode refuses real payments and malformed metadata", () => {
  const real: any = purchase();
  delete real.testPurchase;
  rejects(real, "purchase_invalid");
  const empty = purchase();
  empty.lineItems = [];
  rejects(empty, "purchase_invalid");
  const duplicate = purchase();
  duplicate.lineItems.push(duplicate.lineItems[0]);
  rejects(duplicate, "purchase_invalid");
  const bad = purchase();
  bad.lineItems[0].expiryTime = "bad-date";
  rejects(bad, "purchase_invalid");
});
Deno.test("a legitimate purchase is bound before acknowledgement and can recover an acknowledgement outage", async () => {
  const owner = "00000000-0000-4000-8000-000000000001", value = purchase();
  value.externalAccountIdentifiers.obfuscatedExternalAccountId =
    await accountId(owner);
  value.lineItems[0].expiryTime = "2099-01-01T00:00:00Z";
  const events: string[] = [];
  let outage = true;
  const store = {
    async bind() {
      events.push("bound");
    },
    async acknowledged() {
      events.push("recorded");
    },
  };
  const google = {
    async get() {
      events.push("verified");
      return value;
    },
    async acknowledge() {
      events.push("ack");
      if (outage) throw new Error("controlled outage");
    },
  };
  try {
    await reconcilePurchase(
      "synthetic-test-purchase-token",
      owner,
      true,
      google,
      store,
    );
  } catch { /* retry below */ }
  assert(events.join(",") === "verified,bound,ack");
  outage = false;
  await reconcilePurchase(
    "synthetic-test-purchase-token",
    owner,
    true,
    google,
    store,
  );
  assert(events.slice(-4).join(",") === "verified,bound,ack,recorded");
});
Deno.test("pending and rejected ownership never acknowledge or unlock a purchase", async () => {
  const owner = "00000000-0000-4000-8000-000000000001",
    value = purchase("SUBSCRIPTION_STATE_PENDING");
  value.externalAccountIdentifiers.obfuscatedExternalAccountId =
    await accountId(owner);
  let acks = 0, binds = 0;
  const google = {
    async get() {
      return value;
    },
    async acknowledge() {
      acks++;
    },
  };
  await reconcilePurchase(
    "synthetic-test-purchase-token",
    owner,
    true,
    google,
    {
      async bind() {
        binds++;
      },
      async acknowledged() {},
    },
  );
  assert(acks === 0 && binds === 1);
  value.subscriptionState = "SUBSCRIPTION_STATE_ACTIVE";
  value.lineItems[0].expiryTime = "2099-01-01T00:00:00Z";
  try {
    await reconcilePurchase(
      "synthetic-test-purchase-token",
      owner,
      true,
      google,
      {
        async bind() {
          throw new PlayFailure("purchase_account_mismatch");
        },
        async acknowledged() {},
      },
    );
  } catch { /* expected */ }
  assert(acks === 0);
});
Deno.test("publisher fixes the host, encodes opaque tokens, never accepts redirect or account JSON destinations", async () => {
  const calls: { url: string; method: string }[] = [];
  const google = publisher(
    "fr.previensmoi.app",
    async () => "synthetic-google-access",
    (async (url, init) => {
      calls.push({ url: String(url), method: init?.method ?? "" });
      assert(init?.redirect === "error");
      return init?.method === "POST"
        ? new Response(null, { status: 204 })
        : new Response("{}");
    }) as typeof fetch,
  );
  await google.get("opaque/token?secret");
  await google.acknowledge("opaque/token?secret");
  assert(
    calls[0].url.endsWith("/opaque%2Ftoken%3Fsecret") &&
      calls[1].url.endsWith("/opaque%2Ftoken%3Fsecret:acknowledge"),
  );
  assert(calls[0].method === "GET" && calls[1].method === "POST");
});
