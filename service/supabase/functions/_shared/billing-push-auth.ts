import {
  createRemoteJWKSet,
  jwtVerify,
  type JWTVerifyGetKey,
} from "npm:jose@6.1.3";
const keys = createRemoteJWKSet(
  new URL("https://www.googleapis.com/oauth2/v3/certs"),
);
/** Authenticated Pub/Sub push only; a payload's email/audience is never trusted before signature verification. */
export async function authorizeBillingPush(
  header: string | null,
  audience: string,
  email: string,
  key: JWTVerifyGetKey = keys,
): Promise<boolean> {
  if (!header?.startsWith("Bearer ") || !audience || !email) return false;
  try {
    const { payload } = await jwtVerify(header.slice(7), key, {
      audience,
      issuer: ["accounts.google.com", "https://accounts.google.com"],
      algorithms: ["RS256"],
      clockTolerance: 60,
    });
    return payload.email === email && payload.email_verified === true;
  } catch {
    return false;
  }
}
