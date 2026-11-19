import { authorizeBillingPush } from "../_shared/billing-push-auth.ts";
import { bytes, Fault, rpc, serve } from "../_shared/http.ts";
import {
  billingPolicy,
  verifyPlayPurchase,
} from "../_shared/billing-runtime.ts";
import { digest, notificationPurchaseToken } from "../_shared/play-billing.ts";
serve(async (req) => {
  if (req.method !== "POST") throw new Fault("not_found", 404);
  const audience = Deno.env.get("PLAY_RTDN_AUDIENCE"),
    email = Deno.env.get("PLAY_RTDN_EMAIL");
  if (!audience || !email) throw new Fault("billing_unavailable", 503);
  if (
    !await authorizeBillingPush(
      req.headers.get("authorization"),
      audience,
      email,
    )
  ) throw new Fault("access_denied", 403);
  let value: any;
  try {
    const envelope = JSON.parse(
      new TextDecoder().decode(await bytes(req, 16384)),
    );
    value = JSON.parse(atob(envelope.message.data));
  } catch {
    throw new Fault("invalid_input", 400);
  }
  const policy = await billingPolicy();
  if (!policy.enabled || value?.packageName !== policy.package_name) {
    throw new Fault("access_denied", 403);
  }
  const token = notificationPurchaseToken(value);
  if (token) {
    const owner = await rpc("s7_purchase_owner", {
      token_hash: await digest(token),
    }) as string | null;
    if (owner) await verifyPlayPurchase(token, owner, policy);
  }
  // Duplicates and out-of-order messages are harmless: always read Google's
  // current state, and bind renewal periods idempotently instead of trusting type.
  return new Response(null, { status: 204 });
});
