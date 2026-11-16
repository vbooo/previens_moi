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
export async function refreshSubscriptions(
  owner: string | null = null,
): Promise<void> {
  const policy = await billingPolicy();
  if (!policy.enabled) return;
  // No Google request unless credentials exist. Refresh at most one row per wakeup.
  google(policy);
  const candidates = await rpc("s7_refresh_candidates", { owner }) as {
    owner_id: string;
    token: string;
  }[];
  for (const candidate of candidates) {
    try {
      await verifyPlayPurchase(candidate.token, candidate.owner_id, policy);
    } catch {
      console.warn(JSON.stringify({ event: "subscription_refresh_failed" }));
    }
  }
}
