/** Read only the configured Play product. No purchase, acknowledgement or catalogue change. */
import {
  googleAccessToken,
  type ServiceAccount,
} from "../service/supabase/functions/_shared/google-auth.ts";

const root = new URL("../", import.meta.url);
const account: ServiceAccount = JSON.parse(
  await Deno.readTextFile(
    new URL(".local/cloud/play-service-account.json", root),
  ),
);
if (
  account.project_id !== "previens-moi-32653" ||
  account.client_email !==
    "previens-moi-play-billing@previens-moi-32653.iam.gserviceaccount.com"
) {
  throw new Error("Unexpected Play identity; no network request performed.");
}
const report: Record<string, unknown> = {
  checkedAt: new Date().toISOString(),
  package: "fr.previensmoi.app",
  product: "previens_moi_plus",
  readOnly: true,
  newPaidProviderCalls: 0,
  realPurchaseVerified: false,
};
try {
  const token = await googleAccessToken(
    account,
    "https://www.googleapis.com/auth/androidpublisher",
  )();
  const response = await fetch(
    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/fr.previensmoi.app/subscriptions/previens_moi_plus",
    {
      headers: { Authorization: "Bearer " + token },
      redirect: "error",
      signal: AbortSignal.timeout(15000),
    },
  );
  report.httpStatus = response.status;
  const data = await response.json();
  report.catalogueAccessible = response.ok;
  if (response.ok) {
    report.productMatches = data.packageName === "fr.previensmoi.app" &&
      data.productId === "previens_moi_plus";
    const monthly = data.basePlans?.find((plan: { basePlanId: string }) =>
      plan.basePlanId === "monthly"
    );
    report.basePlanState = monthly?.state ?? "MISSING";
    report.monthlyAutoRenewing =
      monthly?.autoRenewingBasePlanType?.billingPeriodDuration === "P1M";
    const france = monthly?.regionalConfigs?.find((
      region: { regionCode: string },
    ) => region.regionCode === "FR");
    report.francePriceMatches = france?.price?.currencyCode === "EUR" &&
      String(france?.price?.units) === "6" &&
      france?.price?.nanos === 990000000;
    report.franceAvailable = france?.newSubscriberAvailability === true;
    report.passed = report.productMatches === true &&
      report.monthlyAutoRenewing === true && report.francePriceMatches === true;
  } else {
    report.googleStatus = data.error?.status ?? "UNKNOWN";
    report.reasons = data.error?.errors?.map((item: { reason: string }) =>
      item.reason
    ) ?? [];
    report.passed = false;
  }
} catch {
  report.passed = false;
  report.failure =
    "Google identity or metadata request unavailable; no token logged.";
}
await Deno.writeTextFile(
  new URL("validation/billing/play-catalogue-access.json", root),
  JSON.stringify(report, null, 2) + "\n",
);
console.log(JSON.stringify(report));
Deno.exit(report.passed ? 0 : 1);
