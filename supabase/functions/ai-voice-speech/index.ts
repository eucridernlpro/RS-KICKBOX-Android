import { json, userClient } from "../_shared/rs.ts"

const OPENAI_API_KEY=Deno.env.get("OPENAI_API_KEY") || ""
const TTS_MODEL=Deno.env.get("RS_AI_TTS_MODEL") || "gpt-4o-mini-tts"

function bytesToBase64(bytes:Uint8Array):string{
  let binary=""
  const chunk=0x8000
  for(let i=0;i<bytes.length;i+=chunk){
    binary+=String.fromCharCode(...bytes.subarray(i,Math.min(i+chunk,bytes.length)))
  }
  return btoa(binary)
}

function languageInstruction(code:string,avatar:string):string{
  const identity=avatar==="MALE"
    ? "Use a calm adult male coaching voice."
    : "Use a warm adult female coaching voice."
  const language=(
    code==="nl" ? "Speak natural Netherlands Dutch (nl-NL), not Belgian Dutch." :
    code==="pt" ? "Speak natural European Portuguese (pt-PT)." :
    code==="es" ? "Speak natural European Spanish (es-ES)." :
    code==="fr" ? "Speak natural French from France (fr-FR)." :
    code==="de" ? "Speak natural German from Germany (de-DE)." :
    code==="it" ? "Speak natural Italian from Italy (it-IT)." :
    code==="pl" ? "Speak natural Polish (pl-PL)." :
    code==="tr" ? "Speak natural Turkish (tr-TR)." :
    "Speak natural international English."
  )
  return identity+" "+language+
    " Sound confident, friendly, premium and conversational, like a professional kickboxing coach. "+
    "Do not exaggerate emotion or use a theatrical announcer style."
}

Deno.serve(async(req)=>{
  if(req.method!=="POST")return json({error:"Method not allowed"},405)

  const client=userClient(req)
  const {data:{user},error:userError}=await client.auth.getUser()
  if(userError||!user)return json({error:"Authentication required"},401)

  const {data:profile,error:profileError}=await client
    .from("rs_profiles")
    .select("role,active,plan")
    .eq("id",user.id)
    .single()

  if(profileError||!profile?.active||!["student","trainer","admin"].includes(profile.role)){
    return json({error:"Active RS KICKBOX account required"},403)
  }
  if(!OPENAI_API_KEY)return json({error:"RS premium voice is not configured"},503)

  const plan=String(profile.plan||"BASIC").toUpperCase()
  const staff=["trainer","admin"].includes(profile.role)
  if(!staff && plan==="BASIC"){
    return json({error:"Premium cloud voice requires RS PRO or RS ELITE"},403)
  }

  const body=await req.json().catch(()=>({}))
  const text=String(body.text||"").trim().slice(0,2200)
  const languageCode=String(body.language_code||"en").trim().toLowerCase().slice(0,8)
  const avatar=String(body.avatar||"FEMALE").toUpperCase()==="MALE" ? "MALE" : "FEMALE"
  if(!text)return json({error:"Speech text required"},400)

  const voice=avatar==="MALE" ? "cedar" : "marin"
  const response=await fetch("https://api.openai.com/v1/audio/speech",{
    method:"POST",
    headers:{
      "authorization":"Bearer "+OPENAI_API_KEY,
      "content-type":"application/json"
    },
    body:JSON.stringify({
      model:TTS_MODEL,
      voice,
      input:text,
      instructions:languageInstruction(languageCode,avatar),
      response_format:"mp3"
    })
  }).catch(()=>null)

  if(!response)return json({error:"RS premium voice is temporarily unavailable"},502)
  if(!response.ok){
    const detail=(await response.text()).slice(0,600)
    console.error("ai-voice-speech OpenAI error",response.status,detail)
    return json({error:"RS premium voice could not generate speech"},502)
  }

  const bytes=new Uint8Array(await response.arrayBuffer())
  if(bytes.length<128)return json({error:"RS premium voice returned empty audio"},502)

  return json({
    audio_base64:bytesToBase64(bytes),
    mime_type:"audio/mpeg",
    model:TTS_MODEL,
    voice,
    plan
  })
})
