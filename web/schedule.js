/* Pure, dependency-free engine. Day zero is Sunday. */
(function(root){
'use strict';
const timePattern=/^([01]\d|2[0-3]):[0-5]\d$/;
function minute(time){if(!timePattern.test(time))throw Error('שעה לא תקינה');return Number(time.slice(0,2))*60+Number(time.slice(3));}
function validate(state){
 if(!state||state.version!==1||typeof state.enabled!=='boolean'||!Number.isSafeInteger(state.pauseUntil)||state.pauseUntil<0||!Array.isArray(state.rules)||state.rules.length>100)throw Error('הגדרות לא תקינות');
 const ids=new Set();
 for(const r of state.rules){
  if(!r||typeof r.id!=='string'||!r.id.length||r.id.length>80||ids.has(r.id)||typeof r.name!=='string'||!r.name.trim()||r.name.length>40||typeof r.enabled!=='boolean'||!Array.isArray(r.days)||!r.days.length||r.days.length>7||new Set(r.days).size!==r.days.length||!r.days.every(d=>Number.isInteger(d)&&d>=0&&d<=6))throw Error('כלל לא תקין');
  if(minute(r.start)===minute(r.end))throw Error('שעות ההתחלה והסיום צריכות להיות שונות');ids.add(r.id);
 }
 return state;
}
function activeAt(r,day,m){
 if(!r.enabled)return false;
 const s=minute(r.start),e=minute(r.end);
 return s<e?r.days.includes(day)&&m>=s&&m<e:(r.days.includes(day)&&m>=s)||(r.days.includes((day+6)%7)&&m<e);
}
function quiet(state,date=new Date()){
 return state.enabled&&date.getTime()>=state.pauseUntil&&state.rules.some(r=>activeAt(r,date.getDay(),date.getHours()*60+date.getMinutes()));
}
function nextChange(state,now=new Date()){
 if(!state.enabled)return null;
 const current=quiet(state,now), candidates=[];
 if(state.pauseUntil>now.getTime())candidates.push(state.pauseUntil);
 // Walk real instants, not wall-clock additions: skipped and repeated DST minutes
 // therefore resolve exactly as quiet() resolves them.
 const first=Math.floor(now.getTime()/60000)*60000+60000;
 for(let t=first;t<=now.getTime()+8*86400000;t+=60000){
  if(quiet(state,new Date(t))!==current){candidates.push(t);break;}
 }
 candidates.sort((a,b)=>a-b);
 return candidates.find(t=>quiet(state,new Date(t))!==current)||null;
}
const api={minute,validate,activeAt,quiet,nextChange};
if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.Schedule=api;
})(typeof globalThis!=='undefined'?globalThis:this);
