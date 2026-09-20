import { readFile } from 'node:fs/promises';
import { after, before, beforeEach, test } from 'node:test';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, updateDoc, deleteDoc, collection, getDocs, query, where } from 'firebase/firestore';
let env;
before(async () => { env = await initializeTestEnvironment({ projectId: 'demo-good4-community', firestore: { host: '127.0.0.1', port: 8185, rules: await readFile('firestore.rules', 'utf8') } }); });
after(async () => { await env?.cleanup(); });
const event = {kind:'event',title:'Kampüs buluşması',description:'Tanışma etkinliği',date:'2026-10-15',time:'14:30',location:'Kampüs',imageUrl:'',code:'',businessId:'',discountType:'percentage',discountValue:0,totalLimit:0,perUserLimit:1,status:'published'};
const coupon = {...event,kind:'coupon',businessId:'business-one',discountValue:20,totalLimit:100,status:'pending'};
const manager = () => env.authenticatedContext('manager', {email:'manager@example.com',email_verified:true}).firestore();
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async context => {
    const db=context.firestore();
    await setDoc(doc(db,'community_access/manager@example.com'), {active:true,communityIds:['one']});
    await setDoc(doc(db,'communities/one'), {name:'Bir',description:'Topluluk',logoUrl:'',coverUrl:''});
    await setDoc(doc(db,'communities/two'), {name:'İki',description:'Topluluk',logoUrl:'',coverUrl:''});
    await setDoc(doc(db,'businesses/business-one'), {name:'Kafe',ownerId:'business-owner'});
    await setDoc(doc(db,'communities/one/entries/pending'), coupon);
    await setDoc(doc(db,'communities/one/entries/public'), event);
  });
});
test('student can discover and read published entries, not pending coupons', async () => {
  const db=env.authenticatedContext('student').firestore();
  await assertSucceeds(getDocs(collection(db,'communities')));
  await assertSucceeds(getDocs(query(collection(db,'communities/one/entries'),where('status','==','published'))));
  await assertFails(getDoc(doc(db,'communities/one/entries/pending')));
  await assertFails(getDocs(collection(db,'communities/one/entries')));
});
test('manager can publish only in assigned community and cannot self-approve coupons', async () => {
  const db=manager();
  await assertSucceeds(setDoc(doc(db,'communities/one/entries/new'),event));
  await assertSucceeds(setDoc(doc(db,'communities/one/entries/new-coupon'),coupon));
  await assertFails(setDoc(doc(db,'communities/two/entries/new'),event));
  await assertFails(updateDoc(doc(db,'communities/one/entries/pending'),{status:'published'}));
  await assertFails(setDoc(doc(db,'communities/one/entries/bad'),{...coupon,status:'published'}));
});
test('unverified email and revoked admin cannot write', async () => {
  const unverified=env.authenticatedContext('manager',{email:'manager@example.com',email_verified:false}).firestore();
  await assertFails(setDoc(doc(unverified,'communities/one/entries/new'),event));
  const db=manager();
  await env.withSecurityRulesDisabled(async c => updateDoc(doc(c.firestore(),'community_access/manager@example.com'),{active:false}));
  await assertFails(setDoc(doc(db,'communities/one/entries/new'),event));
});
test('no client may assign privileges, create communities or change entry kind', async () => {
  const db=manager();
  await assertFails(updateDoc(doc(db,'community_access/manager@example.com'),{communityIds:['one','two']}));
  await assertFails(setDoc(doc(db,'communities/new'),{name:'Yeni',description:'',logoUrl:'',coverUrl:''}));
  await assertFails(updateDoc(doc(db,'communities/one/entries/pending'),{kind:'event',status:'published'}));
  await assertFails(updateDoc(doc(db,'communities/one'),{admin:true}));
});
test('manager can cancel or edit; malformed content is rejected', async () => {
  const db=manager();
  await assertSucceeds(updateDoc(doc(db,'communities/one/entries/public'),{status:'cancelled'}));
  await assertSucceeds(updateDoc(doc(db,'communities/one'),{description:'Yeni açıklama'}));
  await assertFails(setDoc(doc(db,'communities/one/entries/invalid'),{...event,title:''}));
  await assertFails(setDoc(doc(db,'communities/one/entries/invalid'),{...event,time:'99:99'}));
});
test('student can follow a community and create a claim; only the matching business can redeem it', async () => {
  await env.withSecurityRulesDisabled(async context => {
    await updateDoc(doc(context.firestore(),'communities/one/entries/pending'),{status:'published'});
  });
  const student=env.authenticatedContext('student').firestore();
  const claim={value:'123456',communityId:'one',entryId:'pending',userId:'student',businessId:'business-one',title:'Kampüs buluşması',expiresOn:'2026-10-15',status:'pending',createdAt:1,usedAt:null};
  await assertSucceeds(setDoc(doc(student,'communities/one/followers/student'),{userId:'student',followedAt:1}));
  await assertFails(setDoc(doc(student,'communities/one/followers/someone-else'),{userId:'someone-else',followedAt:1}));
  await assertSucceeds(setDoc(doc(student,'community_coupon_codes/123456'),claim));
  await assertFails(updateDoc(doc(student,'community_coupon_codes/123456'),{status:'used',usedAt:2}));
  const intruder=env.authenticatedContext('intruder').firestore();
  await assertFails(getDoc(doc(intruder,'community_coupon_codes/123456')));
  await assertFails(getDocs(collection(intruder,'community_coupon_codes')));
  const business=env.authenticatedContext('business-owner').firestore();
  await assertSucceeds(getDoc(doc(business,'community_coupon_codes/123456')));
  await assertSucceeds(updateDoc(doc(business,'community_coupon_codes/123456'),{status:'used',usedAt:2}));
});
test('student owns their event registration while manager can watch the attendee list', async () => {
  const student=env.authenticatedContext('student').firestore();
  const registration={userId:'student',displayName:'Öğrenci',registeredAt:1};
  await assertSucceeds(setDoc(doc(student,'communities/one/entries/public/registrations/student'),registration));
  await assertSucceeds(getDoc(doc(student,'communities/one/entries/public/registrations/student')));
  await assertFails(getDocs(collection(student,'communities/one/entries/public/registrations')));
  await assertFails(setDoc(doc(student,'communities/one/entries/public/registrations/other'),{...registration,userId:'other'}));
  await assertSucceeds(getDocs(collection(manager(),'communities/one/entries/public/registrations')));
  await assertSucceeds(deleteDoc(doc(student,'communities/one/entries/public/registrations/student')));
});
test('coupon claim is one per student and the matching business can mark it used', async () => {
  await env.withSecurityRulesDisabled(async context => updateDoc(doc(context.firestore(),'communities/one/entries/pending'),{status:'published'}));
  const student=env.authenticatedContext('student').firestore();
  const claim={value:'123456',userId:'student',businessId:'business-one',status:'pending',createdAt:1};
  const ref=doc(student,'communities/one/entries/pending/claims/student');
  await assertSucceeds(setDoc(ref,claim));
  await assertFails(setDoc(ref,{...claim,value:'654321'}));
  await assertFails(setDoc(doc(student,'communities/one/entries/pending/claims/other'),{...claim,userId:'other'}));
  await assertSucceeds(updateDoc(doc(env.authenticatedContext('business-owner').firestore(),'communities/one/entries/pending/claims/student'),{status:'used'}));
});
