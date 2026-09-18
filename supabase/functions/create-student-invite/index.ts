import { admin, json, normalizeEmail, randomToken, sha256Hex, userClient, validPlan } from "../_shared/rs.ts"

Deno.serve(async (req)=>{
  if(req.method!=="POST") return json({error:"Method not allowed"},405)

  const client=userClient(req)
  const { data:{ user }, error:userError }=await client.auth.getUser()
  if(userError || !user) return json({error:"Authentication required"},401)

  const { data:profile, error:profileError }=await client
    .from("rs_profiles")
    .select("role,active")
    .eq("id",user.id)
    .single()

  if(profileError || !profile?.active || !["trainer","admin"].includes(profile.role)){
    return json({error:"Trainer/admin access required"},403)
  }

  const body=await req.json().catch(()=>({}))
  const email=normalizeEmail(body.email)
  const displayName=String(body.display_name || "").trim()
  const plan=validPlan(body.plan)

  if(!email.includes("@") || !displayName) return json({error:"Valid name and email required"},400)

  const { data:activeStudents, error:countError }=await client
    .from("rs_profiles")
    .select("id")
    .eq("role","student")
    .eq("active",true)

  if(countError){
    console.error("create-student-invite capacity query failed",countError)
    return json({error:"Could not verify student capacity"},500)
  }
  if((activeStudents?.length || 0)>=100) return json({error:"Active student limit reached"},409)

  const { data:existing, error:existingError }=await client
    .from("rs_profiles")
    .select("id")
    .ilike("email",email)
    .maybeSingle()

  if(existingError){
    console.error("create-student-invite existing profile query failed",existingError)
    return json({error:"Could not verify existing student accounts"},500)
  }
  if(existing) return json({error:"A student account already exists for this email"},409)

  const token=randomToken(32)
  const tokenHash=await sha256Hex(token)
  const expiresAt=new Date(Date.now()+7*24*60*60*1000).toISOString()

  const { data:invite, error:inviteError }=await admin
    .from("rs_student_invites")
    .insert({
      email,
      display_name:displayName,
      plan,
      token_hash:tokenHash,
      created_by:user.id,
      expires_at:expiresAt
    })
    .select("id,email,display_name,plan,expires_at")
    .single()

  if(inviteError) return json({error:"Could not create invitation"},500)

  const payload=new URL("rskickbox://invite")
  payload.searchParams.set("v","2")
  payload.searchParams.set("id",invite.id)
  payload.searchParams.set("name",invite.display_name)
  payload.searchParams.set("email",invite.email)
  payload.searchParams.set("plan",invite.plan)
  payload.searchParams.set("token",token)

  return json({
    invite_id:invite.id,
    email:invite.email,
    display_name:invite.display_name,
    plan:invite.plan,
    expires_at:invite.expires_at,
    invite_uri:payload.toString()
  },201)
})
