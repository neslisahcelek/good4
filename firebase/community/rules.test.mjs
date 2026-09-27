import { readFile } from 'node:fs/promises';
import { after, before, beforeEach, test } from 'node:test';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, updateDoc, deleteDoc, collection, getDocs, query, where, runTransaction } from 'firebase/firestore';
import { ref as storageRef, uploadBytes } from 'firebase/storage';
let env;
before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-good4-community',
    firestore: { host: '127.0.0.1', port: 8185, rules: await readFile('firestore.rules', 'utf8') },
    storage: { host: '127.0.0.1', port: 9195, rules: await readFile('storage.rules', 'utf8') }
  });
});
after(async () => { await env?.cleanup(); });
const event = {kind:'event',title:'Kampüs buluşması',description:'Tanışma etkinliği',date:'2026-10-15',time:'14:30',location:'Kampüs',imageUrl:'',code:'',businessId:'',discountType:'percentage',discountValue:0,capacity:0,totalLimit:0,perUserLimit:1,status:'published'};
const coupon = {...event,kind:'coupon',businessId:'business-one',discountValue:20,totalLimit:100,status:'pending'};
const manager = () => env.authenticatedContext('manager', {email:'manager@example.com',email_verified:true}).firestore();
beforeEach(async () => {
  await env.clearFirestore();
  await env.clearStorage();
  await env.withSecurityRulesDisabled(async context => {
    const db=context.firestore();
    await setDoc(doc(db,'community_access/manager@example.com'), {active:true,communityIds:['one']});
    await setDoc(doc(db,'communities/one'), {name:'Bir',university:'Akdeniz Üniversitesi',description:'Topluluk',logoUrl:'',coverUrl:''});
    await setDoc(doc(db,'communities/two'), {name:'İki',university:'Akdeniz Üniversitesi',description:'Topluluk',logoUrl:'',coverUrl:''});
    await setDoc(doc(db,'businesses/business-one'), {name:'Kafe',ownerId:'business-owner'});
    await setDoc(doc(db,'communities/one/entries/pending'), coupon);
    await setDoc(doc(db,'communities/one/entries/public'), event);
  });
});
test('verified manager can upload a community image even when iOS omits MIME metadata', async () => {
  const managerStorage = env.authenticatedContext('manager', {email:'manager@example.com',email_verified:true}).storage();
  const bytes = new Uint8Array([0xff, 0xd8, 0xff, 0xd9]);
  await assertSucceeds(uploadBytes(storageRef(managerStorage, 'community_images/one/test.jpg'), bytes));
  await assertFails(uploadBytes(storageRef(managerStorage, 'community_images/two/test.jpg'), bytes));
  const studentStorage = env.authenticatedContext('student', {email:'student@example.com',email_verified:true}).storage();
  await assertFails(uploadBytes(storageRef(studentStorage, 'community_images/one/test.jpg'), bytes));
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
test('signed-in users can create only their own validated feedback', async () => {
  const db=env.authenticatedContext('student',{email:'student@example.com'}).firestore();
  const valid={
    userId:'student',userEmail:'student@example.com',userDisplayName:'Demo Öğrenci',
    subject:'Ana sayfa önerisi',message:'Kartların sıralaması daha anlaşılır olabilir.',
    source:'mobile',environment:'legacyTest',status:'new',createdAt:1_800_000_000
  };
  await assertSucceeds(setDoc(doc(db,'feedbackSubmissions/valid'),valid));
  await assertFails(setDoc(doc(db,'feedbackSubmissions/spoofed'),{...valid,userId:'another-user'}));
  await assertFails(setDoc(doc(db,'feedbackSubmissions/short'),{...valid,message:'kısa'}));
  await assertFails(getDoc(doc(db,'feedbackSubmissions/valid')));
});
test('no client may assign privileges, create communities or change entry kind', async () => {
  const db=manager();
  await assertFails(updateDoc(doc(db,'community_access/manager@example.com'),{communityIds:['one','two']}));
  await assertFails(setDoc(doc(db,'communities/new'),{name:'Yeni',university:'Akdeniz Üniversitesi',description:'',logoUrl:'',coverUrl:''}));
  await assertFails(updateDoc(doc(db,'communities/one/entries/pending'),{kind:'event',status:'published'}));
  await assertFails(updateDoc(doc(db,'communities/one'),{admin:true}));
});
test('student cannot escalate privileges or overwrite another user', async () => {
  await env.withSecurityRulesDisabled(async context => {
    const db=context.firestore();
    await setDoc(doc(db,'users/student'), {email:'student@example.com',role:'student',fullName:'Ada',credit:3});
    await setDoc(doc(db,'users/other'), {email:'other@example.com',role:'student',fullName:'Ece',credit:3});
  });
  const db=env.authenticatedContext('student',{email:'student@example.com'}).firestore();
  await assertFails(updateDoc(doc(db,'users/student'),{role:'admin'}));
  await assertFails(updateDoc(doc(db,'users/student'),{credit:999}));
  await assertFails(setDoc(doc(db,'users/other'),{email:'student@example.com',role:'admin',fullName:'Sahte'}));
  await assertFails(setDoc(doc(db,'community_access/student@example.com'),{active:true,communityIds:['one']}));
  await assertSucceeds(updateDoc(doc(db,'users/student'),{fullName:'Ada Güncel'}));
  await assertFails(deleteDoc(doc(db,'users/student')));
  await assertSucceeds(setDoc(doc(db,'userTombstones/student'),{userId:'student',deletedAt:1}));
  await assertSucceeds(deleteDoc(doc(db,'users/student')));
  await assertFails(setDoc(doc(db,'users/student'),{email:'student@example.com',role:'business'}));
  await assertFails(setDoc(doc(db,'businesses/student-business'),{name:'Sahte',ownerId:'student'}));
});
test('student cannot take over businesses or write campaign management data', async () => {
  await env.withSecurityRulesDisabled(async context => {
    const db=context.firestore();
    await setDoc(doc(db,'users/student'), {email:'student@example.com',role:'student'});
    await setDoc(doc(db,'campaigns/campaign-one'), {image:'https://example.com/a.jpg'});
  });
  const db=env.authenticatedContext('student',{email:'student@example.com'}).firestore();
  await assertFails(updateDoc(doc(db,'businesses/business-one'),{ownerId:'student'}));
  await assertFails(updateDoc(doc(db,'businesses/business-one'),{name:'Ele geçirildi'}));
  await assertFails(setDoc(doc(db,'businesses/forged'),{name:'Sahte',ownerId:'student'}));
  await assertFails(setDoc(doc(db,'campaigns/forged'),{image:'https://example.com/fake.jpg'}));
  await assertFails(deleteDoc(doc(db,'campaigns/campaign-one')));
  await assertFails(setDoc(doc(db,'arbitrary/private'),{role:'admin'}));
});
test('business signup and owner-scoped profile updates remain allowed', async () => {
  const owner=env.authenticatedContext('new-owner',{email:'owner@example.com'}).firestore();
  await assertSucceeds(setDoc(doc(owner,'businesses/new-business'),{
    name:'Yeni İşletme',ownerId:'new-owner',phone:'',address:'Adres',addressUrl:'',city:'Antalya',district:''
  }));
  await assertSucceeds(setDoc(doc(owner,'users/new-owner'),{
    email:'owner@example.com',fullName:'İşletme Sahibi',role:'business',verified:false
  }));
  await assertSucceeds(updateDoc(doc(owner,'businesses/new-business'),{name:'Yeni İsim'}));
  await assertFails(updateDoc(doc(owner,'businesses/new-business'),{ownerId:'someone-else'}));

  await env.withSecurityRulesDisabled(async context => {
    const db=context.firestore();
    await setDoc(doc(db,'users/other-owner'), {email:'other-owner@example.com',role:'business'});
  });
  const other=env.authenticatedContext('other-owner',{email:'other-owner@example.com'}).firestore();
  await assertFails(updateDoc(doc(other,'businesses/new-business'),{name:'Başka İşletme'}));
});
test('business cannot mutate another business product or order', async () => {
  await env.withSecurityRulesDisabled(async context => {
    const db=context.firestore();
    await setDoc(doc(db,'users/business-owner'), {email:'owner@example.com',role:'business'});
    await setDoc(doc(db,'users/other-owner'), {email:'other@example.com',role:'business'});
    await setDoc(doc(db,'businesses/business-two'), {name:'Başka',ownerId:'other-owner'});
    await setDoc(doc(db,'products/product-two'), {name:'Ürün',businessId:'business-two',pendingCount:2});
    await setDoc(doc(db,'orders/order-two'), {businessId:'business-two',supporterId:'supporter',status:'pending'});
  });
  const db=env.authenticatedContext('business-owner',{email:'owner@example.com'}).firestore();
  await assertFails(updateDoc(doc(db,'products/product-two'),{name:'Ele geçirildi'}));
  await assertFails(updateDoc(doc(db,'products/product-two'),{pendingCount:99}));
  await assertFails(updateDoc(doc(db,'orders/order-two'),{status:'confirmed'}));
});
test('legacy counter changes are bounded and finalized codes or orders cannot be replayed', async () => {
  await env.withSecurityRulesDisabled(async context => {
    const db=context.firestore();
    await setDoc(doc(db,'users/student'), {email:'student@example.com',role:'student',credit:3});
    await setDoc(doc(db,'products/product-one'), {name:'Ürün',businessId:'business-one',pendingCount:2});
    await setDoc(doc(db,'codes/used-code'), {userId:'student',businessId:'business-one',productId:'product-one',status:'used'});
    await setDoc(doc(db,'orders/confirmed-order'), {supporterId:'student',businessId:'business-one',status:'confirmed'});
  });
  const db=env.authenticatedContext('student',{email:'student@example.com'}).firestore();
  await assertSucceeds(updateDoc(doc(db,'products/product-one'),{pendingCount:1}));
  await assertFails(updateDoc(doc(db,'products/product-one'),{pendingCount:5}));
  await assertFails(updateDoc(doc(db,'codes/used-code'),{status:'expired',usedAt:2}));
  await assertFails(updateDoc(doc(db,'orders/confirmed-order'),{status:'cancelled'}));
});
test('community manager cannot mutate another community management data', async () => {
  const db=manager();
  await assertFails(updateDoc(doc(db,'communities/two'),{description:'Yetkisiz'}));
  await assertFails(setDoc(doc(db,'communities/two/entries/forged'),event));
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
  const registration={userId:'student',displayName:'Öğrenci',registeredAt:1,ticketToken:'aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa'};
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

test('QR admission requires the assigned manager and current event ticket; entry locks student cancellation', async () => {
  const student = env.authenticatedContext('student').firestore();
  const token = 'aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa';
  const path = 'communities/one/entries/public';
  await setDoc(doc(student, `${path}/registrations/student`), {userId:'student',displayName:'Ada',registeredAt:1,ticketToken:token});
  const admission = {userId:'student',checkedInBy:'manager',checkedInAt:2,method:'qr',ticketToken:token};
  await assertFails(setDoc(doc(student, `${path}/attendance/student`), {...admission, checkedInBy:'student'}));
  await assertFails(setDoc(doc(manager(), `${path}/attendance/student`), {...admission,ticketToken:'bbbbbbbb-bbbb-4bbb-bbbb-bbbbbbbbbbbb'}));
  await assertFails(setDoc(doc(manager(), 'communities/two/entries/public/attendance/student'), admission));
  await assertFails(setDoc(doc(manager(), `${path}/attendance/absent`), {...admission,userId:'absent'}));
  await assertSucceeds(setDoc(doc(manager(), `${path}/attendance/student`), admission));
  await assertFails(updateDoc(doc(manager(), `${path}/attendance/student`), {checkedInAt:3}));
  await assertFails(deleteDoc(doc(student, `${path}/registrations/student`)));
  await assertFails(updateDoc(doc(student, `${path}/registrations/student`), {ticketToken:'bbbbbbbb-bbbb-4bbb-bbbb-bbbbbbbbbbbb'}));
  await assertSucceeds(deleteDoc(doc(manager(), `${path}/attendance/student`)));
  await assertSucceeds(deleteDoc(doc(student, `${path}/registrations/student`)));
  await assertFails(setDoc(doc(manager(), `${path}/attendance/student`), admission));
});

test('simultaneous gatekeepers admit a student once; manual fallback is permitted', async () => {
  const path = 'communities/one/entries/public';
  await env.withSecurityRulesDisabled(async context => setDoc(doc(context.firestore(), `${path}/registrations/student`),
    {userId:'student',displayName:'Ada',registeredAt:1}));
  const db = manager();
  const ref = doc(db, `${path}/attendance/student`);
  const admit = () => runTransaction(db, async tx => {
    const existing = await tx.get(ref);
    if (existing.exists()) return false;
    tx.set(ref, {userId:'student',checkedInBy:'manager',checkedInAt:2,method:'manual',ticketToken:''});
    return true;
  });
  const results = await Promise.all([admit(), admit()]);
  if (results.filter(Boolean).length !== 1) throw new Error('Duplicate admission');
  await assertSucceeds(getDocs(collection(db, `${path}/attendance`)));
  await assertFails(getDocs(collection(env.authenticatedContext('other').firestore(), `${path}/attendance`)));
});

test('only assigned manager can observe follower totals; old tickets cannot be reused after re-registration', async () => {
  const student = env.authenticatedContext('student').firestore();
  await setDoc(doc(student,'communities/one/followers/student'),{userId:'student',followedAt:1});
  await assertSucceeds(getDocs(collection(manager(),'communities/one/followers')));
  await assertFails(getDocs(collection(student,'communities/one/followers')));
  await assertFails(getDocs(collection(manager(),'communities/two/followers')));
  const path='communities/one/entries/public';
  const registration={userId:'student',displayName:'Ada',registeredAt:1,ticketToken:'aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa'};
  await setDoc(doc(student,`${path}/registrations/student`),registration);
  await deleteDoc(doc(student,`${path}/registrations/student`));
  await setDoc(doc(student,`${path}/registrations/student`),{...registration,ticketToken:'bbbbbbbb-bbbb-4bbb-bbbb-bbbbbbbbbbbb'});
  await assertFails(setDoc(doc(manager(),`${path}/attendance/student`),{userId:'student',checkedInBy:'manager',checkedInAt:2,method:'qr',ticketToken:registration.ticketToken}));
  await updateDoc(doc(manager(),path),{status:'cancelled'});
  await assertFails(setDoc(doc(manager(),`${path}/attendance/student`),{userId:'student',checkedInBy:'manager',checkedInAt:2,method:'manual',ticketToken:''}));
});
