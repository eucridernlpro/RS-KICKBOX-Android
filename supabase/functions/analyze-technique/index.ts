import { json, userClient } from "../_shared/rs.ts"

const OPENAI_API_KEY=Deno.env.get("OPENAI_API_KEY") || ""
const MODEL=Deno.env.get("RS_TECHNIQUE_AI_MODEL") || "gpt-5.6-luna"
const MAX_FRAMES=4
const MAX_FRAME_BASE64=1_600_000

function extractOutputText(payload:any):string{
  const items=Array.isArray(payload?.output)?payload.output:[]
  const parts:string[]=[]
  for(const item of items){
    const content=Array.isArray(item?.content)?item.content:[]
    for(const part of content){
      if(part?.type==="output_text" && typeof part?.text==="string") parts.push(part.text)
    }
  }
  return parts.join("\n").trim()
}

Deno.serve(async(req)=>{
  if(req.method!=="POST") return json({error:"Method not allowed"},405)

  const client=userClient(req)
  const {data:{user},error:userError}=await client.auth.getUser()
  if(userError || !user) return json({error:"Authentication required"},401)

  const {data:profile,error:profileError}=await client
    .from("rs_profiles")
    .select("role,active")
    .eq("id",user.id)
    .single()

  if(profileError || !profile?.active || !["student","trainer","admin"].includes(profile.role)){
    return json({error:"Active RS KICKBOX account required"},403)
  }

  if(!OPENAI_API_KEY) return json({error:"Technique AI is not configured yet"},503)

  const body=await req.json().catch(()=>({}))
  const technique=String(body.technique || "Kickboxing technique").trim().slice(0,120)
  const language=String(body.language || "English").trim().slice(0,80)
  const rawFrames=Array.isArray(body.frames)?body.frames:[]

  const frames=rawFrames
    .filter((x:unknown)=>typeof x==="string")
    .map((x:string)=>x.trim())
    .filter((x:string)=>x.length>0 && x.length<=MAX_FRAME_BASE64)
    .slice(0,MAX_FRAMES)

  if(frames.length<2) return json({error:"At least two valid technique frames are required"},400)

  const content:any[]=[
    {
      type:"input_text",
      text:
        "You are the RS KICKBOX technique coach. Analyze only visible kickboxing mechanics from the supplied sampled frames. "+
        "Do not claim certainty about motion occurring between frames. Avoid medical diagnosis, injury assessment, or unsafe advice. "+
        "Focus on stance, balance, guard, alignment, rotation, recovery, and obvious technique-specific details. "+
        "Reply in "+language+". Give a concise practical coaching response of 90-160 words. "+
        "Start with one sentence describing what is visibly good, then list the 3 most useful corrections in priority order, "+
        "then one short safe drill suggestion. Mention that this is a sampled-frame review, not a full biomechanical measurement. "+
        "Technique selected by the athlete: "+technique+"."
    }
  ]

  for(const frame of frames){
    content.push({
      type:"input_image",
      image_url:"data:image/jpeg;base64,"+frame,
      detail:"high"
    })
  }

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
        input:[{role:"user",content}],
        max_output_tokens:650
      })
    })
  }catch(error){
    console.error("analyze-technique OpenAI request failed",error)
    return json({error:"Technique AI is temporarily unavailable"},502)
  }

  const raw=await response.text()
  if(!response.ok){
    console.error("analyze-technique OpenAI error",response.status,raw.slice(0,1000))
    return json({error:"Technique AI could not complete this review"},502)
  }

  let payload:any
  try{payload=JSON.parse(raw)}catch{
    return json({error:"Technique AI returned an invalid response"},502)
  }

  const analysis=extractOutputText(payload)
  if(!analysis) return json({error:"Technique AI returned an empty response"},502)

  return json({
    analysis,
    model:MODEL,
    sampled_frames:frames.length
  })
})
