/** Server-only Play verification. Client purchase states never grant access. */
export const PLUS_PRODUCT = "previens_moi_plus";
export const PLUS_BASE_PLAN = "monthly";
export type PlayState =
  | "active"
  | "cancelled"
  | "grace"
  | "hold"
  | "paused"
  | "expired"
  | "pending";
export interface VerifiedSubscription {
  state: PlayState;
  active: boolean;
  expiresAt: string | null;
  orderId: string | null;
  accountId: string;
  acknowledged: boolean;
  test: boolean;
  linkedToken: string | null;
}
export async function digest(value: string): Promise<string> {
  return [
    ...new Uint8Array(
      await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value)),
    ),
  ].map((b) => b.toString(16).padStart(2, "0")).join("");
}
export const accountId = (owner: string) =>
  digest("previens-moi:play:" + owner);
export class PlayFailure extends Error {
  constructor(
    public code:
      | "purchase_invalid"
      | "purchase_account_mismatch"
      | "billing_unavailable"
      | "purchase_pending",
  ) {
    super(code);
  }
}
export function verifySubscription(
  value: any,
  expectedAccount: string,
  testOnly: boolean,
  now = Date.now(),
): VerifiedSubscription {
  const state: PlayState | undefined = ({
    SUBSCRIPTION_STATE_ACTIVE: "active",
    SUBSCRIPTION_STATE_CANCELED: "cancelled",
    SUBSCRIPTION_STATE_IN_GRACE_PERIOD: "grace",
    SUBSCRIPTION_STATE_ON_HOLD: "hold",
    SUBSCRIPTION_STATE_PAUSED: "paused",
    SUBSCRIPTION_STATE_EXPIRED: "expired",
    SUBSCRIPTION_STATE_PENDING: "pending",
    SUBSCRIPTION_STATE_PENDING_PURCHASE_CANCELED: "expired",
  } as Record<string, PlayState>)[value?.subscriptionState];
  if (
    !state || !Array.isArray(value.lineItems) || value.lineItems.length !== 1
  ) throw new PlayFailure("purchase_invalid");
  const line = value.lineItems[0];
  if (
    line.productId !== PLUS_PRODUCT ||
    line.offerDetails?.basePlanId !== PLUS_BASE_PLAN ||
    line.offerDetails?.offerId || line.prepaidPlan
  ) throw new PlayFailure("purchase_invalid");
  if (
    value.externalAccountIdentifiers?.obfuscatedExternalAccountId !==
      expectedAccount
  ) throw new PlayFailure("purchase_account_mismatch");
  const test = value.testPurchase !== null &&
    typeof value.testPurchase === "object";
  if (testOnly && !test) throw new PlayFailure("purchase_invalid");
  const expiry = typeof line.expiryTime === "string" &&
      Number.isFinite(Date.parse(line.expiryTime))
    ? line.expiryTime
    : null;
  const order = typeof line.latestSuccessfulOrderId === "string"
    ? line.latestSuccessfulOrderId
    : typeof value.latestOrderId === "string"
    ? value.latestOrderId
    : null;
  if (state !== "pending" && (!expiry || !order || order.length > 200)) {
    throw new PlayFailure("purchase_invalid");
  }
  const active = ["active", "cancelled", "grace"].includes(state) && !!expiry &&
    Date.parse(expiry) > now;
  return {
    state: expiry && Date.parse(expiry) <= now ? "expired" : state,
    active,
    expiresAt: expiry,
    orderId: order,
    accountId: expectedAccount,
    acknowledged:
      value.acknowledgementState === "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED",
    test,
    linkedToken: typeof value.linkedPurchaseToken === "string"
      ? value.linkedPurchaseToken
      : null,
  };
}
/** Notifications only identify an existing purchase to re-read from Google. */
export function notificationPurchaseToken(value: any): string | null {
  const subscription = value?.subscriptionNotification;
  const voided = value?.voidedPurchaseNotification;
  const event = subscription && !voided
    ? subscription
    : !subscription && voided?.productType === 1
    ? voided
    : null;
  const token = event?.purchaseToken;
  return typeof token === "string" && token.length >= 20 &&
      token.length <= 4096 &&
      !/[\s\x00-\x1f]/.test(token)
    ? token
    : null;
}
export interface Publisher {
  get(token: string): Promise<unknown>;
  acknowledge(token: string): Promise<void>;
}
export function publisher(
  packageName: string,
  accessToken: () => Promise<string>,
  request: typeof fetch = fetch,
): Publisher {
  if (!/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*){2,}$/.test(packageName)) {
    throw new PlayFailure("billing_unavailable");
  }
  const base =
    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/" +
    packageName + "/purchases/";
  const call = async (path: string, method: string) => {
    const response = await request(base + path, {
      method,
      headers: {
        Authorization: "Bearer " + await accessToken(),
        "Content-Type": "application/json",
      },
      ...(method === "POST" ? { body: "{}" } : {}),
      redirect: "error",
      signal: AbortSignal.timeout(10000),
    });
    if (!response.ok) {
      await response.body?.cancel();
      throw new PlayFailure(
        response.status === 404 || response.status === 410
          ? "purchase_invalid"
          : "billing_unavailable",
      );
    }
    if (method === "POST") {
      await response.body?.cancel();
      return;
    }
    // Never log raw Google responses, purchase tokens, orders or account identifiers.
    const raw = await response.text();
    if (raw.length > 65536) throw new PlayFailure("billing_unavailable");
    return JSON.parse(raw);
  };
  return {
    get: (token) =>
      call("subscriptionsv2/tokens/" + encodeURIComponent(token), "GET"),
    async acknowledge(token) {
      await call(
        "subscriptions/" + PLUS_PRODUCT + "/tokens/" +
          encodeURIComponent(token) + ":acknowledge",
        "POST",
      );
    },
  };
}
export interface PurchaseStore {
  bind(token: string, verified: VerifiedSubscription): Promise<void>;
  acknowledged(token: string): Promise<void>;
}
export async function reconcilePurchase(
  token: string,
  owner: string,
  testOnly: boolean,
  google: Publisher,
  store: PurchaseStore,
): Promise<VerifiedSubscription> {
  if (token.length < 20 || token.length > 4096 || /[\s\x00-\x1f]/.test(token)) {
    throw new PlayFailure("purchase_invalid");
  }
  const verified = verifySubscription(
    await google.get(token),
    await accountId(owner),
    testOnly,
  );
  // The atomic binding rejects cross-account reuse and older renewal snapshots.
  // Persist before acknowledgement so a network failure is recoverable.
  await store.bind(token, verified);
  if (verified.active && !verified.acknowledged) {
    await google.acknowledge(token);
    await store.acknowledged(token);
  }
  return verified;
}
