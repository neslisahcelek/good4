# Good4 V2 Firebase

This directory is isolated from the legacy `firebase/community` setup and is
bound only to the new Firebase project `good4tr-v2`.

## Roles

- `student`: mobile-only student account
- `businessOwner`: business owner
- `businessStaff`: unrestricted code-verification staff for one business
- `communityManager`: community manager
- `communityStaff`: community event staff
- `good4Admin`: Good4 system administrator

Organization scope is stored under
`organizations/{organizationId}/members/{uid}`. Email addresses are identity
attributes, not authorization keys.

## Security boundary

Clients can read only the data required by their role. Critical mutations are
denied in Firestore rules and will be implemented as authenticated backend
operations:

- campaign code issuance and redemption
- organization and membership changes
- event creation/status changes
- event registration and check-in
- role assignment
- audit log creation

Storage is deny-by-default until the upload functions and scoped media paths
are implemented.

## Implemented callable backend operations

- `createOrganization`: Good4 admin creates a business or community
- `assignOrganizationMember`: Good4 admin assigns an owner, manager, or staff member
- `createCampaign`: Good4 admin creates a draft or published campaign
- `issueCampaignCode`: a student with a verified `.edu.tr` address receives one short-lived code per campaign
- `requestEduVerification` / `confirmEduVerification`: a student proves a `.edu.tr` address with a six-digit e-mailed code
- `redeemCampaignCode`: unlimited business staff members manually redeem a code
- `getBusinessContext`: an authenticated business user receives only their own business context
- `getCommunityPortalDashboard`: a community manager receives their linked Good4Test community, entries and live attendance totals
- `saveCommunityPortalEntry`: a community manager creates or updates an event or a pending coupon
- `cancelCommunityPortalEntry`: a community manager removes an event or coupon from publication without deleting history

Campaign redemption is transactional and idempotent. The same code cannot be
consumed twice, even when two requests arrive concurrently. Authentication,
active-role checks, and organization membership checks protect callable access.

### Temporary Good4Test coupon bridge

The current Good4Test mobile build still uses the separate `good4tr-test`
database and issues six-digit `community_coupon_codes`. The business web panel
routes **only six-digit** codes to `redeemLegacyTestCoupon`; V2 campaign codes
continue through `redeemCampaignCode`. The bridge reads the legacy coupon,
published community entry and matching student claim, checks the business and
expiry, then consumes the legacy code and claim in one transaction. A V2 audit
record is written after successful redemption. Existing students' codes are
not copied or altered until a business explicitly verifies one.

Only the V2 business organization with a trusted `legacyTestBusinessId` field
may redeem codes for that Good4Test business. The bridge never accesses the
old production `good4tr` project. Cross-project access for the V2 runtime is
limited to the `good4tr-test` custom IAM role
`good4LegacyCouponBridge` (Firestore transaction, document get/update). This
is a compatibility step, not a migration of student accounts or a permanent
second source of truth. New mobile releases should use V2 Auth and the V2
`issueCampaignCode` flow; remove this bridge after the test app is migrated.

## Web panel

The `web/` directory contains one role-aware login. Businesses receive manual
campaign-code verification, community managers receive their event, coupon,
registration and attendance panel, and Good4 administrators receive the system
administration area. The administrator
can review pending community coupons, create businesses and communities, and
assign business or community accounts. There is no student web interface and
no campaign performance dashboard.

```sh
npm install --prefix web
npm run test:web
npx firebase deploy --only hosting --project good4tr-v2
```

## Local rules tests

```sh
npm install
npm run test:rules
npm run test:functions
npm run test:web
```

## University e-mail verification

Suspended meals (`issueCampaignCode`) require `users/{uid}.eduVerified == true`.
Students get there in one of two ways:

- signing in with a verified `.edu.tr` address (`ensureStudentProfile` marks it), or
- entering a `.edu.tr` address on the Askıda Yemek screen and confirming the
  six-digit code sent to it (`requestEduVerification` → `confirmEduVerification`).

Codes are stored only as SHA-256 hashes in `eduVerifications/{uid}`, expire
after 10 minutes, allow 5 attempts, and can be resent once a minute and 5 times
an hour. `eduEmailClaims/{sha256(email)}` keeps one address per account. None of
these collections, nor `mail`, is readable by clients.

The callable only queues the message in the `mail` collection. Delivery needs
the **Trigger Email from Firestore** extension (`firebase/firestore-send-email`)
installed on `good4tr-v2` with collection `mail`, an SMTP connection URI and a
default FROM address:

```sh
firebase ext:install firebase/firestore-send-email --project good4tr-v2
```

## Campus weather

`refreshCampusWeather` runs every 30 minutes (Cloud Scheduler, europe-west1),
fetches the MET Norway Locationforecast for the Akdeniz campus and stores the
current temperature and a Turkish label in `app_config/campus_weather`. The app
only reads that document, so user devices never call the weather provider.
MET Norway data is free for commercial use under CC BY 4.0; the home card shows
"Veri: MET Norway" linking to their licensing page.

## Deployment

Deploy only after authenticating the Firebase CLI and verifying the selected
project:

```sh
firebase use good4tr-v2
firebase deploy --only firestore:rules,firestore:indexes,storage
```
