'use strict';
const $=id=>document.getElementById(id);
const dayNames=['ראשון','שני','שלישי','רביעי','חמישי','שישי','שבת'];
const key='phone-ring-controller.v1';
let state={version:1,enabled:false,pauseUntil:0,rules:[]},system=null,editing=null,deleting=null,busy=false,sequence=0;
const pending=new Map(),native=!!window.PhoneRing;
function message(text,error=false){$('message').textContent=text;$('message').classList.toggle('error',error);}
function request(action,payload){
 return new Promise((resolve,reject)=>{
  const id=String(++sequence);
  const timer=setTimeout(()=>{pending.delete(id);reject(Error('לא התקבלה תשובה מ־Android. נסו שוב.'));},8000);
  pending.set(id,{resolve,reject,timer});
  try{window.PhoneRing.postMessage(JSON.stringify({id,action,payload}));}catch(e){clearTimeout(timer);pending.delete(id);reject(e);}
 });
}
if(native)window.PhoneRing.onmessage=e=>{
 try{const r=JSON.parse(e.data),p=pending.get(r.id);if(!p)return;clearTimeout(p.timer);pending.delete(r.id);r.error?p.reject(Error(r.error)):p.resolve(r.result);}catch{message('תשובה לא תקינה מ־Android',true);}
};
async function refresh(){
 if(busy)return;
 if(native){try{const result=await request('load');state=Schedule.validate(result.state);system=result.system;render();}catch(e){message(e.message,true);$('system').textContent='אין אישור שהכללים מיושמים במערכת';}}
 else render();
}
async function mutate(change){
 if(busy)return;
 busy=true;document.body.classList.add('loading');
 try{
  const next=Schedule.validate(change(JSON.parse(JSON.stringify(state))));
  if(native){const r=await request('save',next);state=Schedule.validate(r.state);system=r.system;}
  else{localStorage.setItem(key,JSON.stringify(next));state=next;}
  render();message('ההגדרות נשמרו'+(native?' במכשיר.':' בדפדפן; זו הדגמה בלבד.'));
 }catch(e){message('השינוי לא נשמר: '+e.message,true);render();}
 finally{busy=false;document.body.classList.remove('loading');}
}
function render(){
 const now=new Date(),q=Schedule.quiet(state,now),paused=state.pauseUntil>now.getTime();
 $('enabled').checked=state.enabled;
 $('status').classList.toggle('quiet',q);$('status').replaceChildren();
 const title=document.createElement('strong');title.textContent=!state.enabled?'התזמון כבוי':paused?'התזמון מושהה':q?'☾ זמן שקט':'♫ אין בקשת שקט';
 const sub=document.createElement('p'),next=Schedule.nextChange(state,now);
 sub.className='small';sub.textContent=next?'המעבר הבא: '+new Date(next).toLocaleString('he-IL',{weekday:'long',hour:'2-digit',minute:'2-digit'}):'אין מעבר מתוכנן בשבוע הקרוב';
 if(paused)sub.textContent='השהיה עד '+new Date(state.pauseUntil).toLocaleTimeString('he-IL',{hour:'2-digit',minute:'2-digit'});
 $('status').append(title,sub);
 $('zone').textContent=Intl.DateTimeFormat().resolvedOptions().timeZone;
 $('preview').hidden=native;$('connect').hidden=!native;$('exact').hidden=!native||!!system?.exact;
 $('system').textContent=!native?'דפדפן: אין שליטה בצלצול הטלפון':!system?'החיבור עדיין לא אומת':!system.access?'חסרה הרשאת ״נא לא להפריע״':!system.ruleExists?'נדרש חיבור לכלל שקט':!system.ruleEnabled?'כלל האפליקציה כבוי בהגדרות Android':system.error?'לא ניתן ליישם: '+system.error:'הרשאה קיימת. '+(system.dnd?'מצב ״נא לא להפריע״ פעיל במערכת.':'מצב ״נא לא להפריע״ כבוי במערכת.')+(system.exact?'':' התזמון אינו מדויק ללא הרשאה נוספת.');
 $('connect').textContent=system?.access?'חיבור / הגדרות כלל השקט':'מתן הרשאת מצב שקט';
 $('rules').replaceChildren();
 state.rules.forEach(r=>{
  const card=document.createElement('article');card.className='rule';
  const row=document.createElement('div');row.className='row';
  const h=document.createElement('h3');h.textContent=r.name;
  const toggle=document.createElement('input');toggle.type='checkbox';toggle.className='switch';toggle.checked=r.enabled;toggle.setAttribute('aria-label','הפעלת '+r.name);
  toggle.onchange=()=>mutate(s=>{s.rules.find(x=>x.id===r.id).enabled=toggle.checked;return s;});
  row.append(h,toggle);const times=document.createElement('div');times.dir='ltr';times.textContent=r.start+' – '+r.end;
  const days=document.createElement('p');days.className='small';days.textContent=r.days.length===7?'כל יום':r.days.map(d=>dayNames[d]).join(' · ');
  const actions=document.createElement('div');actions.className='rule-actions';
  const edit=document.createElement('button');edit.className='secondary';edit.textContent='עריכה';edit.setAttribute('aria-label','עריכת '+r.name);edit.onclick=()=>openEditor(r);
  const del=document.createElement('button');del.className='secondary danger';del.textContent='מחיקה';del.setAttribute('aria-label','מחיקת '+r.name);del.onclick=()=>{deleting=r.id;$('deleteName').textContent=r.name;$('deleteDialog').showModal();};
  actions.append(edit,del);card.append(row,times,days,actions);$('rules').append(card);
 });
 if(!state.rules.length){const empty=document.createElement('p');empty.className='empty';empty.textContent='עדיין אין שעות שקט. הוסיפו את הכלל הראשון.';$('rules').append(empty);}
 $('pause').textContent=paused?'ביטול ההשהיה':'השהיית התזמון לשעה';$('pause').disabled=!state.enabled;
 $('add').disabled=state.rules.length>=100;
}
function openEditor(rule){
 editing=rule?.id||null;$('editorTitle').textContent=rule?'עריכת שעות שקט':'שעות שקט חדשות';
 $('name').value=rule?.name||'';$('start').value=rule?.start||'23:00';$('end').value=rule?.end||'07:00';$('error').textContent='';
 $('days').querySelectorAll('input').forEach(x=>{x.checked=rule?rule.days.includes(Number(x.value)):true;});$('editor').showModal();
}
dayNames.forEach((d,i)=>{const l=document.createElement('label'),c=document.createElement('input');c.type='checkbox';c.value=i;l.append(c,' '+d);$('days').append(l);});
$('add').onclick=()=>openEditor(null);$('cancel').onclick=()=>$('editor').close();
$('form').onsubmit=async e=>{
 e.preventDefault();const r={id:editing||('r-'+Date.now()+'-'+Math.random().toString(36).slice(2)),name:$('name').value.trim(),start:$('start').value,end:$('end').value,days:Array.from($('days').querySelectorAll('input:checked')).map(x=>Number(x.value)),enabled:editing?state.rules.find(x=>x.id===editing).enabled:true};
 try{Schedule.validate({...state,rules:[r]});}catch(err){$('error').textContent=err.message;return;}
 $('editor').close();await mutate(s=>{const i=s.rules.findIndex(x=>x.id===r.id);if(i<0)s.rules.push(r);else s.rules[i]=r;return s;});
};
$('cancelDelete').onclick=()=>$('deleteDialog').close();$('confirmDelete').onclick=()=>{$('deleteDialog').close();mutate(s=>{s.rules=s.rules.filter(x=>x.id!==deleting);return s;});};
$('enabled').onchange=()=>mutate(s=>{s.enabled=$('enabled').checked;s.pauseUntil=0;return s;});
$('pause').onclick=()=>mutate(s=>{s.pauseUntil=s.pauseUntil>Date.now()?0:Date.now()+3600000;return s;});
$('connect').onclick=async()=>{try{await request('connect');await refresh();}catch(e){message(e.message,true);}};
$('exact').onclick=async()=>{try{await request('exact');}catch(e){message(e.message,true);}};
if(!native){try{const saved=localStorage.getItem(key);if(saved)state=Schedule.validate(JSON.parse(saved));}catch{message('לא ניתן לקרוא הגדרות שמורות. בדקו שהדפדפן מאפשר אחסון מקומי.',true);}}
document.addEventListener('visibilitychange',()=>{if(!document.hidden)refresh();});
window.addEventListener('focus',()=>refresh());setInterval(refresh,30000);refresh();
