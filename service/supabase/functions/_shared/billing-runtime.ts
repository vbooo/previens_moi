import { Fault, rpc } from "./http.ts";
import { googleAccessToken, type ServiceAccount } from "./google-auth.ts";
import {
  digest,
  PlayFailure,
  type Publisher,
  publisher,
  type PurchaseStore,
  reconcilePurchase,
} from "./play-billing.ts";
interface Policy {
  enabled: boolean;
  test_only: boolean;
  monitoring_enabled: boolean;
  package_name: string;
}
let cached: Publisher | undefined;
function google(policy: Policy): Publisher {
  if (!policy.enabled) throw new Fault("billing_unavailable", 503);
  if (!cached) {
    const raw = Deno.env.get("PLAY_SERVICE_ACCOUNT_JSON");
    if (!raw) throw new Fault("billing_unavailable", 503);
    let account: ServiceAccount;
    try {
      account = JSON.parse(raw);
    } catch {
      throw new Fault("billing_unavailable", 503);
    }
    cached = publisher(
      policy.package_name,
      googleAccessToken(
        account,
        "https://www.googleapis.com/auth/androidpublisher",
      ),
    );
  }
  return cached;
}
export const billingPolicy = async () =>
  await rpc("s7_billing_policy", {}) as Policy;
export async function verifyPlayPurchase(
  token: string,
  owner: string,
  policy: Policy,
) {
  const observedAt = new Date().toISOString();
  const store: PurchaseStore = {
    async bind(purchaseToken, metadata) {
      await rpc("s7_bind_purchase", {
        owner,
        purchase_token: purchaseToken,
        metadata,
        observed_at: observedAt,
      });
    },
    async acknowledged(purchaseToken) {
      await rpc("s7_acknowledged", { token_hash: await digest(purchaseToken) });
    },
  };
  try {
    return await reconcilePurchase(
      token,
      owner,
      policy.test_only,
      google(policy),
      store,
    );
  } catch (error) {
    if (error instanceof PlayFailure) {
      throw new Fault(
        error.code,
        error.code === "billing_unavailable" ? 503 : 400,
      );
    }
    throw error;
  }
}
interface RefreshDependencies {
  policy: () => Promise<Policy>;
  candidates: (
    owner: string | null,
    forceCheck: boolean,
  ) => Promise<{ owner_id: string; token: string }[]>;
  verify: typeof verifyPlayPurchase;
}
const refreshDependencies: RefreshDependencies = {
  policy: billingPolicy,
  candidates: async (owner, forceCheck) =>
    await rpc("s7_refresh_candidates", { owner, force_check: forceCheck }) as {
      owner_id: string;
      token: string;
    }[],
  verify: verifyPlayPurchase,
};

export async function refreshSubscriptions(
  owner: string | null = null,
  forceCheck = false,
  dependencies: RefreshDependencies = refreshDependencies,
): Promise<void> {
  const policy = await dependencies.policy();
  if (!policy.enabled) {
    if (forceCheck) throw new Fault("billing_unavailable", 503);
    return;
  }
  // Accounts without a purchase do not depend on Play credentials.
  const candidates = await dependencies.candidates(owner, forceCheck);
  for (const candidate of candidates) {
    try {
      await dependencies.verify(candidate.token, candidate.owner_id, policy);
    } catch {
      // A manual check must fail explicitly instead of calling old data verified.
      if (forceCheck) throw new Fault("billing_unavailable", 503);
      console.warn(JSON.stringify({ event: "subscription_refresh_failed" }));
    }
  }
}
