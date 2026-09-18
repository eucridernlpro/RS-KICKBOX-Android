import { createClient } from "npm:@supabase/supabase-js@2"

export const supabaseUrl = Deno.env.get("SUPABASE_URL")!

function keyMap(name: string): Record<string,string> {
  const raw = Deno.env.get(name)
  if (!raw) throw new Error("Missing "+name)
  return JSON.parse(raw)
}

export const publishableKey = keyMap("SUPABASE_PUBLISHABLE_KEYS")["default"]
export const secretKey = keyMap("SUPABASE_SECRET_KEYS")["default"]
const legacyServiceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || ""
export const adminKey = legacyServiceRoleKey || secretKey

export const admin = createClient(supabaseUrl, adminKey, {
  auth: { persistSession: false, autoRefreshToken: false }
})

export function userClient(req: Request) {
  const authorization = req.headers.get("Authorization") || ""
  return createClient(supabaseUrl, publishableKey, {
    global: { headers: { Authorization: authorization } },
    auth: { persistSession: false, autoRefreshToken: false }
  })
}

export function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" }
  })
}

export function normalizeEmail(value: unknown) {
  return String(value || "").trim().toLowerCase()
}

export async function sha256Hex(value: string) {
  const bytes = new TextEncoder().encode(value)
  const digest = await crypto.subtle.digest("SHA-256", bytes)
  return Array.from(new Uint8Array(digest)).map(b=>b.toString(16).padStart(2,"0")).join("")
}

export function randomToken(bytes = 32) {
  const data = new Uint8Array(bytes)
  crypto.getRandomValues(data)
  return btoa(String.fromCharCode(...data))
    .replaceAll("+","-")
    .replaceAll("/","_")
    .replaceAll("=","")
}

export function validPlan(value: unknown) {
  const plan=String(value || "PRO").toUpperCase()
  return ["BASIC","PRO","ELITE"].includes(plan) ? plan : "PRO"
}
