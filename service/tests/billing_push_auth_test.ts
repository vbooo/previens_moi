import { generateKeyPair, SignJWT } from "npm:jose@6.1.3";
import { authorizeBillingPush } from "../supabase/functions/_shared/billing-push-auth.ts";
const assert = (v: unknown) => {
  if (!v) throw new Error("assertion");
};
Deno.test("billing push requires Google signature, exact audience and verified service email", async () => {
  const { publicKey, privateKey } = await generateKeyPair("RS256");
  const aud = "https://service.example/functions/v1/play-notifications";
  const email = "pubsub@example.test";
  const token = async (audience = aud, mail = email, verified = true) =>
    await new SignJWT({ email: mail, email_verified: verified })
      .setProtectedHeader({ alg: "RS256" }).setIssuer(
        "https://accounts.google.com",
      ).setAudience(audience).setIssuedAt().setExpirationTime("5m").sign(
        privateKey,
      );
  const key = async () => publicKey;
  assert(
    await authorizeBillingPush("Bearer " + await token(), aud, email, key),
  );
  assert(
    !await authorizeBillingPush(
      "Bearer " + await token("https://wrong.example"),
      aud,
      email,
      key,
    ),
  );
  assert(
    !await authorizeBillingPush(
      "Bearer " + await token(aud, "wrong@example.test"),
      aud,
      email,
      key,
    ),
  );
  assert(
    !await authorizeBillingPush(
      "Bearer " + await token(aud, email, false),
      aud,
      email,
      key,
    ),
  );
  assert(!await authorizeBillingPush("Bearer tampered", aud, email, key));
  assert(!await authorizeBillingPush(null, aud, email, key));
});
