import { json, userClient } from "../_shared/rs.ts"

const OPENAI_API_KEY=Deno.env.get("OPENAI_API_KEY") || ""
const MODEL=Deno.env.get("RS_TECHNIQUE_AI_MODEL") || "gpt-5.6-luna"

function outputText(payload:any):string{
  const out=Array.isArray(payload?.output)?payload.output:[]
  const parts:string[]=[]
  for(const item of out){
    for(const part of (Array.isArray(item?.content)?item.content:[])){
      if(part?.type==="output_text" && typeof part?.text==="string")parts.push(part.text)
    }
  }
  return parts.join("\n").trim()
}

Deno.serve(async(req)=>{
  if(req.method!=="POST")return json({error:"Method not allowed"},405)

  const client=userClient(req)
  const {data:{user},error:userError}=await client.auth.getUser()
  if(userError||!user)return json({error:"Authentication required"},401)

  const {data:profile,error:profileError}=await client
    .from("rs_profiles")
    .select("role,active")
    .eq("id",user.id)
    .single()

  if(profileError||!profile?.active||!["student","trainer","admin"].includes(profile.role)){
    return json({error:"Active RS KICKBOX account required"},403)
  }

  if(!OPENAI_API_KEY)return json({error:"RS AI Coach is not configured yet"},503)

  const body=await req.json().catch(()=>({}))
  const question=String(body.question||"").trim().slice(0,1800)
  const language=String(body.language||"English").trim().slice(0,80)
  const references=String(body.references||"").trim().slice(0,3500)

  if(!question)return json({error:"Question required"},400)

  const {error:quotaError}=await client.rpc("rs_consume_ai_quota",{
    p_feature:"ai_coach_chat",
    p_limit:80
  })
  if(quotaError){
    const m=String(quotaError.message||"")
    if(m.toLowerCase().includes("daily ai limit reached")){
      return json({error:"Daily RS AI Coach limit reached"},429)
    }
    return json({error:"RS AI Coach quota could not be verified"},503)
  }

  const instructions=
    "You are Sofia or Marcus, the RS KICKBOXING platform assistant and kickboxing coach. "+
    "Always answer in "+language+". "+
    "Be warm, natural and conversational. Never restrict the conversation to kickboxing only. "+
    "You help the user operate RS KICKBOXING: navigation, music, playlists, app features, settings, support, groups, community, trainer content, and general in-app help. "+
    "When the app can perform an action, speak as an assistant who helps complete that action rather than telling the user to manually hunt for buttons. "+
    "You are also a specialist kickboxing coach with expertise in techniques, combinations, training structure, bag work, pad work, sparring preparation, footwork, defense, timing, conditioning and style differences. "+
    "Give practical coaching, but do not diagnose injuries or claim medical certainty. "+
    "For move or combination questions, explain purpose, mechanics, guard/balance/timing, common mistakes, and a useful drill. "+
    "Trainer-approved reference context is authoritative for this app when provided. Mention it naturally and do not contradict a trainer note unless there is a clear safety issue. "+
    "For greetings and casual conversation, reply naturally without forcing the topic back to training. "+
    "Keep most answers concise enough for a mobile voice/chat experience, usually 50-180 words."

  const userText=
    "User message:\n"+question+
    (references
      ? "\n\nTrainer-approved reference context:\n"+references
      : "\n\nNo trainer-approved reference matched this question.")

  let response:Response
  try{
    response=await fetch("https://api.openai.com/v1/responses",{
      method:"POST",
      headers:{
        "authorization":"Bearer "+OPENAI_API_KEY,
        "content-type":"application/json"
      },
      body:JSON.stringify({
        model:MODEL,
        instructions,
        input:userText,
        max_output_tokens:700,
        store:false
      })
    })
  }catch(error){
    console.error("ai-coach-chat request failed",error)
    return json({error:"RS AI Coach is temporarily unavailable"},502)
  }

  const raw=await response.text()
  if(!response.ok){
    console.error("ai-coach-chat OpenAI error",response.status,raw.slice(0,1000))
    return json({error:"RS AI Coach could not complete this answer"},502)
  }

  let payload:any
  try{payload=JSON.parse(raw)}catch{
    return json({error:"RS AI Coach returned an invalid response"},502)
  }

  const answer=outputText(payload)
  if(!answer)return json({error:"RS AI Coach returned an empty answer"},502)

  return json({answer,model:MODEL})
})
