export interface ServiceAccount {
  project_id: string;
  client_email: string;
  private_key: string;
  token_uri?: string;
}
/** Fixed Google endpoint and a closed scope set; account JSON cannot set destinations. */
export function googleAccessToken(
  account: ServiceAccount,
  scope:
    | "https://www.googleapis.com/auth/androidpublisher"
    | "https://www.googleapis.com/auth/firebase.messaging",
  request: typeof fetch = fetch,
): () => Promise<string> {
  let cached = "";
  let until = 0;
  const enc = new TextEncoder();
  const b64 = (v: Uint8Array) =>
    btoa(String.fromCharCode(...v)).replaceAll("+", "-").replaceAll("/", "_")
      .replace(/=+$/, "");
  return async () => {
    if (cached && Date.now() < until) return cached;
    const now = Math.floor(Date.now() / 1000),
      uri = "https://oauth2.googleapis.com/token";
    const header = b64(
      enc.encode(JSON.stringify({ alg: "RS256", typ: "JWT" })),
    );
    const claims = b64(
      enc.encode(
        JSON.stringify({
          iss: account.client_email,
          scope,
          aud: uri,
          iat: now,
          exp: now + 3600,
        }),
      ),
    );
    const pem = account.private_key.replace(/-----[^-]+-----/g, "").replaceAll(
      /\s/g,
      "",
    );
    const key = await crypto.subtle.importKey(
      "pkcs8",
      Uint8Array.from(atob(pem), (v) => v.charCodeAt(0)),
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["sign"],
    );
    const signature = new Uint8Array(
      await crypto.subtle.sign(
        "RSASSA-PKCS1-v1_5",
        key,
        enc.encode(header + "." + claims),
      ),
    );
    const response = await request(uri, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
        assertion: header + "." + claims + "." + b64(signature),
      }),
      redirect: "error",
      signal: AbortSignal.timeout(10000),
    });
    if (!response.ok) {
      await response.body?.cancel();
      throw new Error("Google identity unavailable");
    }
    const value = await response.json();
    if (
      typeof value.access_token !== "string" ||
      !Number.isFinite(value.expires_in) || value.expires_in <= 60
    ) throw new Error("Invalid Google identity response");
    cached = value.access_token;
    until = Date.now() + Math.min(value.expires_in, 3600) * 1000 - 60000;
    return cached;
  };
}
