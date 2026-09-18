import { admin, json, normalizeEmail, publishableKey, sha256Hex } from "../_shared/rs.ts"

Deno.serve(async (req)=>{
  if(req.method!=="POST") return json({error:"Method not allowed"},405)

  const apiKey=req.headers.get("apikey") || ""
  if(apiKey!==publishableKey) return json({error:"Missing or invalid client key"},401)

  const body=await req.json().catch(()=>({}))
  const email=normalizeEmail(body.email)
  const token=String(body.token || "")
  const password=String(body.password || "")

  if(!email.includes("@") || token.length<32) return json({error:"Invalid invitation"},400)
  if(password.length<10) return json({error:"Password must be at least 10 characters"},400)

  const tokenHash=await sha256Hex(token)
  const { data:invite, error:inviteError }=await admin
    .from("rs_student_invites")
    .select("id,email,display_name,plan,expires_at,redeemed_at,revoked_at")
    .eq("token_hash",tokenHash)
    .eq("email",email)
    .maybeSingle()

  if(inviteError || !invite) return json({error:"Invitation not found"},404)
  if(invite.revoked_at) return json({error:"Invitation revoked"},410)
  if(invite.redeemed_at) return json({error:"Invitation already used"},409)
  if(new Date(invite.expires_at).getTime()<=Date.now()) return json({error:"Invitation expired"},410)

  const { count:activeCount, error:countError }=await admin
    .from("rs_profiles")
    .select("id",{count:"exact",head:true})
    .eq("role","student")
    .eq("active",true)

  if(countError) return json({error:"Could not verify student capacity"},500)
  if((activeCount || 0)>=100) return json({error:"Active student limit reached"},409)

  const { data:created, error:createError }=await admin.auth.admin.createUser({
    email,
    password,
    email_confirm:true,
    user_metadata:{display_name:invite.display_name,plan:invite.plan}
  })

  if(createError || !created.user) return json({error:"Could not create student account"},409)

  const userId=created.user.id
  const { error:profileError }=await admin
    .from("rs_profiles")
    .insert({
      id:userId,
      email,
      display_name:invite.display_name,
      role:"student",
      plan:invite.plan,
      active:true
    })

  if(profileError){
    await admin.auth.admin.deleteUser(userId)
    return json({error:"Could not create student profile"},500)
  }

  const { error:redeemError }=await admin
    .from("rs_student_invites")
    .update({redeemed_at:new Date().toISOString()})
    .eq("id",invite.id)
    .is("redeemed_at",null)
    .is("revoked_at",null)

  if(redeemError){
    await admin.from("rs_profiles").delete().eq("id",userId)
    await admin.auth.admin.deleteUser(userId)
    return json({error:"Could not finalize invitation"},500)
  }

  return json({
    ok:true,
    email,
    display_name:invite.display_name,
    plan:invite.plan,
    message:"Account created. Sign in with your email and chosen password."
  },201)
})
