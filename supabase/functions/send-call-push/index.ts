import { createClient } from "npm:@supabase/supabase-js@2"

const supabaseUrl=Deno.env.get("SUPABASE_URL")!

function keyMap(name:string):Record<string,string>{
  const raw=Deno.env.get(name)
  if(!raw)throw new Error("Missing "+name)
  return JSON.parse(raw)
}

const publishableKey=keyMap("SUPABASE_PUBLISHABLE_KEYS")["default"]
const secretKey=keyMap("SUPABASE_SECRET_KEYS")["default"]
const legacyServiceRoleKey=Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")||""
const adminKey=legacyServiceRoleKey||secretKey

const admin=createClient(supabaseUrl,adminKey,{
  auth:{persistSession:false,autoRefreshToken:false}
})

function userClient(req:Request){
  const authorization=req.headers.get("Authorization")||""
  return createClient(supabaseUrl,publishableKey,{
    global:{headers:{Authorization:authorization}},
    auth:{persistSession:false,autoRefreshToken:false}
  })
}

function json(body:unknown,status=200){
  return new Response(JSON.stringify(body),{
    status,
    headers:{"content-type":"application/json; charset=utf-8"}
  })
}

type ServiceAccount={
  project_id:string
  client_email:string
  private_key:string
}

function b64url(input:Uint8Array|string){
  const bytes=typeof input==="string"?new TextEncoder().encode(input):input
  let binary=""
  for(const b of bytes)binary+=String.fromCharCode(b)
  return btoa(binary).replaceAll("+","-").replaceAll("/","_").replaceAll("=","")
}

function pemToBytes(pem:string){
  const raw=pem.replace(/-----BEGIN PRIVATE KEY-----/g,"")
    .replace(/-----END PRIVATE KEY-----/g,"")
    .replace(/\s+/g,"")
  const binary=atob(raw)
  return Uint8Array.from(binary,c=>c.charCodeAt(0))
}

async function googleAccessToken(sa:ServiceAccount){
  const now=Math.floor(Date.now()/1000)
  const header=b64url(JSON.stringify({alg:"RS256",typ:"JWT"}))
  const claims=b64url(JSON.stringify({
    iss:sa.client_email,
    scope:"https://www.googleapis.com/auth/firebase.messaging",
    aud:"https://oauth2.googleapis.com/token",
    iat:now,
    exp:now+3600
  }))
  const unsigned=header+"."+claims
  const key=await crypto.subtle.importKey(
    "pkcs8",
    pemToBytes(sa.private_key),
    {name:"RSASSA-PKCS1-v1_5",hash:"SHA-256"},
    false,
    ["sign"]
  )
  const signature=new Uint8Array(await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsigned)
  ))
  const assertion=unsigned+"."+b64url(signature)
  const form=new URLSearchParams({
    grant_type:"urn:ietf:params:oauth:grant-type:jwt-bearer",
    assertion
  })
  const response=await fetch("https://oauth2.googleapis.com/token",{
    method:"POST",
    headers:{"content-type":"application/x-www-form-urlencoded"},
    body:form.toString()
  })
  if(!response.ok)throw new Error("Google OAuth failed: "+response.status)
  const payload=await response.json()
  if(!payload.access_token)throw new Error("Google OAuth token missing")
  return String(payload.access_token)
}

async function sendFcm(
  sa:ServiceAccount,
  accessToken:string,
  token:string,
  data:Record<string,string>
){
  const response=await fetch(
    "https://fcm.googleapis.com/v1/projects/"+encodeURIComponent(sa.project_id)+"/messages:send",
    {
      method:"POST",
      headers:{
        authorization:"Bearer "+accessToken,
        "content-type":"application/json"
      },
      body:JSON.stringify({
        message:{
          token,
          data,
          android:{
            priority:"high",
            ttl:"60s"
          }
        }
      })
    }
  )
  const text=await response.text()
  return {ok:response.ok,status:response.status,text}
}

Deno.serve(async(req)=>{
  if(req.method!=="POST")return json({error:"Method not allowed"},405)

  const client=userClient(req)
  const {data:{user},error:userError}=await client.auth.getUser()
  if(userError||!user)return json({error:"Authentication required"},401)

  const rawSecret=Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON")
  if(!rawSecret)return json({error:"FCM server credential is not configured"},503)

  let sa:ServiceAccount
  try{
    sa=JSON.parse(rawSecret)
  }catch{
    return json({error:"FCM server credential is invalid"},503)
  }
  if(!sa.project_id||!sa.client_email||!sa.private_key){
    return json({error:"FCM server credential is incomplete"},503)
  }

  const body=await req.json().catch(()=>({}))
  const kind=String(body.kind||"")

  let targetIds:string[]=[]
  let payload:Record<string,string>={}

  if(kind==="direct_call"){
    const callId=String(body.call_id||"")
    const {data:call,error}=await admin
      .from("rs_calls")
      .select("id,caller_id,callee_id,call_type,status,created_at")
      .eq("id",callId)
      .single()
    if(error||!call)return json({error:"Call not found"},404)
    if(call.caller_id!==user.id)return json({error:"Only the caller may send this push"},403)
    if(call.status!=="RINGING")return json({error:"Call is no longer ringing"},409)

    const {data:caller}=await admin
      .from("rs_profiles")
      .select("display_name,email")
      .eq("id",user.id)
      .single()

    targetIds=[call.callee_id]
    payload={
      kind:"direct_call",
      call_id:call.id,
      caller_id:user.id,
      caller_name:String(caller?.display_name||caller?.email||"RS Member"),
      caller_email:String(caller?.email||""),
      call_type:String(call.call_type||"AUDIO"),
      created_at:String(call.created_at||"")
    }
  }else if(kind==="video_room"){
    const roomId=String(body.room_id||"")
    const {data:room,error}=await admin
      .from("rs_video_rooms")
      .select("id,host_id,title,status,created_at")
      .eq("id",roomId)
      .single()
    if(error||!room)return json({error:"Video room not found"},404)
    if(room.host_id!==user.id)return json({error:"Only the host may send this push"},403)
    if(room.status!=="OPEN")return json({error:"Video room is no longer open"},409)

    const {data:members}=await admin
      .from("rs_video_room_members")
      .select("user_id,role,status")
      .eq("room_id",roomId)

    targetIds=(members||[])
      .filter((m:any)=>m.role==="STUDENT"&&m.status==="INVITED")
      .map((m:any)=>String(m.user_id))

    const {data:host}=await admin
      .from("rs_profiles")
      .select("display_name,email")
      .eq("id",user.id)
      .single()

    payload={
      kind:"video_room",
      room_id:room.id,
      host_id:user.id,
      host_name:String(host?.display_name||host?.email||"RS Trainer"),
      title:String(room.title||"RS Video Session"),
      participant_count:String((members||[]).length),
      created_at:String(room.created_at||"")
    }
  }else{
    return json({error:"Unsupported push kind"},400)
  }

  if(targetIds.length===0)return json({sent:0})

  const {data:tokens,error:tokenError}=await admin
    .from("rs_fcm_device_tokens")
    .select("id,user_id,token")
    .in("user_id",targetIds)
    .eq("active",true)

  if(tokenError)return json({error:"Could not load FCM targets"},500)
  if(!tokens?.length)return json({sent:0})

  const accessToken=await googleAccessToken(sa)
  let sent=0
  const invalidIds:string[]=[]

  for(const row of tokens){
    const result=await sendFcm(sa,accessToken,String(row.token),payload)
    if(result.ok){
      sent++
    }else if(result.text.includes("UNREGISTERED")||result.text.includes("registration-token-not-registered")){
      invalidIds.push(String(row.id))
    }else{
      console.error("FCM send failed",result.status,result.text)
    }
  }

  if(invalidIds.length){
    await admin.from("rs_fcm_device_tokens")
      .update({active:false,updated_at:new Date().toISOString()})
      .in("id",invalidIds)
  }

  return json({sent,targets:tokens.length})
})
