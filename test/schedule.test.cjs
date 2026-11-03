const {test}=require('node:test'),assert=require('node:assert/strict'),S=require('../web/schedule.js');
const rule=(start='23:00',end='07:00',days=[0])=>({id:'a',name:'לילה',enabled:true,start,end,days});
const state=(rules=[rule()],extra={})=>({version:1,enabled:true,pauseUntil:0,rules,...extra});
test('overnight day belongs to start; Sunday to Monday boundaries',()=>{
 const r=rule();assert.equal(S.activeAt(r,0,1380),true);assert.equal(S.activeAt(r,1,419),true);
 assert.equal(S.activeAt(r,1,420),false);assert.equal(S.activeAt(r,0,300),false);
});
test('week wrap and disabled rule',()=>{
 const r=rule('23:00','07:00',[6]);assert.equal(S.activeAt(r,0,60),true);r.enabled=false;assert.equal(S.activeAt(r,0,60),false);
});
test('overlaps form union and next transition skips inner boundaries',()=>{
 const s=state([rule('09:00','11:00'),{...rule('10:00','12:00'),id:'b'}]);
 const now=new Date(2026,10,1,9,30);assert.equal(S.quiet(s,now),true);
 assert.equal(S.nextChange(s,now),new Date(2026,10,1,12).getTime());
});
test('pause expires at exact millisecond and disable clears effect',()=>{
 const now=new Date(2026,10,1,23,30),until=now.getTime()+30123,s=state(undefined,{pauseUntil:until});
 assert.equal(S.quiet(s,now),false);assert.equal(S.nextChange(s,now),until);
 assert.equal(S.quiet(s,new Date(until)),true);s.enabled=false;assert.equal(S.nextChange(s,now),null);
});
test('empty or continuously quiet has no transition',()=>{
 const now=new Date(2026,10,1,12);
 assert.equal(S.nextChange(state([]),now),null);
 assert.equal(S.nextChange(state([rule('00:00','12:00',[0,1,2,3,4,5,6]),{...rule('12:00','00:00',[0,1,2,3,4,5,6]),id:'b'}]),now),null);
});
test('validates malformed persisted data and duplicate IDs',()=>{
 assert.throws(()=>S.validate(state([rule('12:00','12:00')])));
 assert.throws(()=>S.validate(state([rule('25:00','07:00')])));
 assert.throws(()=>S.validate(state([rule('23:00','07:00',[])])));
 assert.throws(()=>S.validate(state([rule(),rule()])));
 assert.throws(()=>S.validate(state(undefined,{pauseUntil:1.5})));
 assert.equal(S.validate(state()).rules.length,1);
});
test('DST spring skips nonexistent quiet window and fall visits repeated hour',()=>{
 const old=process.env.TZ;process.env.TZ='America/New_York';
 try {
  // Spring 02:00–03:00 does not exist on March 8, 2026.
  const s=state([rule('02:00','03:00')]),now=new Date('2026-03-08T01:59:00-05:00');
  assert.equal(S.nextChange(s,now),new Date('2026-03-15T02:00:00-04:00').getTime());
  const f=state([rule('01:30','01:45')]),before=new Date('2026-11-01T01:50:00-04:00');
  assert.equal(S.nextChange(f,before),new Date('2026-11-01T01:30:00-05:00').getTime());
 } finally {if(old===undefined)delete process.env.TZ;else process.env.TZ=old;}
});
