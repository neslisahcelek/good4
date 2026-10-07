#!/usr/bin/env node
// Inventory by default; --apply revokes old tokens while Social is disabled.
import {createRequire} from "node:module";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";
const require=createRequire(import.meta.url);
const root=resolve(dirname(fileURLToPath(import.meta.url)),"..");
const functionsRequire=createRequire(resolve(root,"functions/package.json"));
const projectId=process.argv[process.argv.indexOf("--project")+1];
if(!process.argv.includes("--project") || !["demo-good4-v2","good4tr-test","good4tr-v2"].includes(projectId)) throw new Error("Specify an authorized --project.");
const local=projectId==="demo-good4-v2";
if(local !== Boolean(process.env.FIREBASE_STORAGE_EMULATOR_HOST && process.env.FIRESTORE_EMULATOR_HOST)) throw new Error("Demo requires both emulators; live migration must not use emulators.");
const {Firestore}=functionsRequire("@google-cloud/firestore");
const {Storage}=functionsRequire("@google-cloud/storage");
let credentials;
if(!local && !process.env.GOOGLE_APPLICATION_CREDENTIALS){
 const auth=require("firebase-tools/lib/auth.js"); const api=require("firebase-tools/lib/api.js");
 const account=auth.getProjectDefaultAccount(root);
 if(!account?.tokens?.refresh_token) throw new Error("Firebase CLI login or application credentials required.");
 credentials={type:"authorized_user",client_id:api.clientId(),client_secret:api.clientSecret(),refresh_token:account.tokens.refresh_token};
}
const options={projectId,...(credentials?{credentials}:{})};
const database=new Firestore(options);
const bucket=new Storage({...options,...(local?{apiEndpoint:`http://${process.env.FIREBASE_STORAGE_EMULATOR_HOST}`,useAuthWithCustomEndpoint:false}:{})})
 .bucket(local?`${projectId}.appspot.com`:`${projectId}.firebasestorage.app`);
const apply=process.argv.includes("--apply");
const config=await database.doc("app_config/social_activities").get();
if(apply && config.get("enabled")===true) throw new Error("Disable Social before migration.");
const {privatizeLegacySocialPhotos}=await import("../functions/lib/socialProfile.js");
const result=await privatizeLegacySocialPhotos(bucket,apply);
const remaining=apply?await privatizeLegacySocialPhotos(bucket):null;
console.log(JSON.stringify({projectId,apply,enabled:config.get("enabled")===true,...result,remainingChanges:remaining?.changed??null}));
await database.terminate();
if(remaining?.changed) throw new Error("Private photo migration incomplete.");
