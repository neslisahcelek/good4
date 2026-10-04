import { createRequire } from 'node:module';
const require = createRequire(new URL('../package.json', import.meta.url));
const auth = require('firebase-tools/lib/auth.js');
const account = auth.getGlobalDefaultAccount();
if (!account) throw new Error('Firebase CLI login required');
const { access_token: token } = await auth.getAccessToken(account.tokens.refresh_token, []);
async function api(url, method = 'GET', body) {
  const response = await fetch(url, { method, headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json', 'x-goog-user-project': 'good4tr-v2' }, ...(body ? { body: JSON.stringify(body) } : {}), signal: AbortSignal.timeout(30000) });
  const result = await response.json();
  if (!response.ok) throw new Error(`${response.status}: ${result.error?.message ?? response.statusText}`);
  return result;
}
const project = 'good4tr-v2';
const projectNumber = '654697131931';
if (process.argv.includes('--enable-budget-api')) await api(`https://serviceusage.googleapis.com/v1/projects/${projectNumber}/services/billingbudgets.googleapis.com:enable`, 'POST', {});
const billing = await api(`https://cloudbilling.googleapis.com/v1/projects/${project}/billingInfo`);
console.log(JSON.stringify({ project, billingEnabled: billing.billingEnabled }));
let budgets;
const accountPath = billing.billingAccountName;
const checks = await Promise.allSettled([
  api(`https://billingbudgets.googleapis.com/v1/${accountPath}/budgets`),
  api(`https://cloudfunctions.googleapis.com/v2/projects/${project}/locations/europe-west1/functions?pageSize=100`),
  api(`https://apikeys.googleapis.com/v2/projects/${projectNumber}/locations/global/keys`),
  api(`https://recaptchaenterprise.googleapis.com/v1/projects/${project}/keys`),
]);
for (const [i, result] of checks.entries()) {
  if (result.status === 'rejected') { console.log(JSON.stringify({ check: ['budgets', 'functions', 'apiRestrictions', 'recaptcha'][i], error: result.reason.message })); continue; }
  const value = result.value;
  if (i === 0) { budgets = value.budgets ?? []; console.log(JSON.stringify({ budgets: budgets.map((b) => ({ name: b.name, displayName: b.displayName, amount: b.amount, thresholds: b.thresholdRules, notifications: b.allUpdatesRule })) })); }
  if (i === 1) console.log(JSON.stringify({ functions: (value.functions ?? []).map((f) => ({ name: f.name.split('/').at(-1), state: f.state, minInstances: f.serviceConfig?.minInstanceCount ?? 0, maxInstances: f.serviceConfig?.maxInstanceCount, appCheck: f.serviceConfig?.environmentVariables?.ENFORCE_APP_CHECK === 'true', memory: f.serviceConfig?.availableMemory })) }));
  if (i === 2) console.log(JSON.stringify({ keys: (value.keys ?? []).map((k) => ({ name: k.displayName, restrictedApis: k.restrictions?.apiTargets?.map((a) => a.service) ?? [] })) }));
  if (i === 3) console.log(JSON.stringify({ recaptcha: (value.keys ?? []).map((k) => ({ name: k.displayName, allowedDomains: k.webSettings?.allowedDomains, testing: Boolean(k.testingOptions) })) }));
}
if (process.argv.includes('--apply-budget')) {
  if (!budgets) throw new Error('Cannot safely configure budget without permission to read existing budgets');
  const topic = `projects/${project}/topics/good4-billing-budget`;
  try { await api(`https://pubsub.googleapis.com/v1/${topic}`, 'PUT', {}); }
  catch (error) { if (!error.message.startsWith('409:')) throw error; }
  const existingPolicy = await api(`https://pubsub.googleapis.com/v1/${topic}:getIamPolicy`);
  const bindings = existingPolicy.bindings ?? [];
  const publisher = 'serviceAccount:billing-budget-pubsub@system.gserviceaccount.com';
  const binding = bindings.find((b) => b.role === 'roles/pubsub.publisher' && !b.condition);
  if (binding) binding.members = [...new Set([...binding.members, publisher])];
  else bindings.push({ role: 'roles/pubsub.publisher', members: [publisher] });
  await api(`https://pubsub.googleapis.com/v1/${topic}:setIamPolicy`, 'POST', { policy: { ...existingPolicy, bindings } });
  const displayName = 'Good4 prod $10 optional work safeguard';
  const existing = budgets.find((b) => b.displayName === displayName);
  const budget = { displayName, budgetFilter: { projects: [`projects/${projectNumber}`], creditTypesTreatment: 'INCLUDE_ALL_CREDITS' },
    amount: { specifiedAmount: { currencyCode: 'USD', units: '10' } },
    thresholdRules: [.3, .5, .8, 1].map((thresholdPercent) => ({ thresholdPercent, spendBasis: 'CURRENT_SPEND' })).concat([{ thresholdPercent: 1, spendBasis: 'FORECASTED_SPEND' }]),
    allUpdatesRule: { pubsubTopic: topic, schemaVersion: '1.0', disableDefaultIamRecipients: false } };
  if (existing) await api(`https://billingbudgets.googleapis.com/v1/${existing.name}?updateMask=displayName,budgetFilter,amount,thresholdRules,allUpdatesRule`, 'PATCH', { ...budget, name: existing.name });
  else await api(`https://billingbudgets.googleapis.com/v1/${accountPath}/budgets`, 'POST', budget);
  console.log('Budget configured. Alerts and enforcement have reporting delays; this is not a hard monetary cap.');
}
