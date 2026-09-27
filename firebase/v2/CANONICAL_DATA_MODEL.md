# Good4 V2 canonical data model

This document is the Phase 1 contract for `good4tr-v2`. Persisted date/time fields are Firestore `Timestamp` values. IDs that relate records are stored explicitly; display names are snapshots, never authorization inputs.

## Identity and organizations

| Domain / path | Document ID | Required fields | Optional fields / enums | Relations |
|---|---|---|---|---|
| `users/{uid}` | Firebase Auth UID | `email:string`, `displayName:string`, `role:string`, `status:string`, `university:string`, `createdAt:Timestamp`, `updatedAt:Timestamp` | `legalAcknowledgements.kvkkNotice`: `{version:string, acknowledgedAt:Timestamp}`; `legalAcknowledgements.userAgreement`: `{version:string, acceptedAt:Timestamp}`; `legalAcknowledgements.privacyPolicy`: `{version:string, presentedAt:Timestamp}`; `status`: `active`, `disabled`; `role`: `student`, `good4Admin`, `businessOwner`, `businessStaff`, `communityManager`, `communityStaff` | Auth UID |
| `organizations/{organizationId}` | Server-generated | `name:string`, `type:string`, `status:string`, `createdAt:Timestamp`, `createdBy:uid`, `updatedAt:Timestamp` | `type`: `community`, `business`; `status`: `active`, `disabled`; `university`, `description`, `logoUrl`, `coverUrl`, `followerCount:number`; temporary `legacyTestBusinessId` / `legacyTestCommunityId` links | `createdBy -> users` |
| `organizations/{organizationId}/members/{uid}` | User UID | `userId:uid`, `role:string`, `status:string`, `assignedBy:uid`, `assignedAt:Timestamp`, `updatedAt:Timestamp` | Community roles: `manager`, `staff`; business roles: `owner`, `staff`; status `active`, `disabled` | parent organization, user |
| `organizations/{organizationId}/followers/{uid}` | User UID | `userId:uid`, `followedAt:Timestamp` | none | parent organization, user |

Communities and businesses are organization types, not separate canonical top-level collections. UI-specific views may call them “community” or “business”, but ownership and membership always resolve through `organizations`.

Following is managed by the authenticated `setCommunityFollowing` callable. Its transaction checks the active actor, permits new follows only for active community organizations, writes a server timestamp, and updates `followerCount` only when the follow state changes. Users may leave an organization that was subsequently disabled or removed. Organizations without a counter initialize it from their existing followers on the first change. Direct client follower writes remain denied. `getFollowingCommunityIds` checks the active actor and runs one `followers` collection group query for their UID, returning only IDs from canonical `organizations/{id}/followers/{uid}` paths. No client collection group read permission is added; the client intersects these IDs with its already loaded active, unblocked communities. `followers.userId` needs the collection group index in `firestore.indexes.json`.

## Events, registration, and attendance

| Domain / path | Document ID | Required fields | Optional fields / enums | Relations |
|---|---|---|---|---|
| `events/{eventId}` | Server-generated | `organizationId:string`, `title:string`, `description:string`, `startsAt:Timestamp`, `endsAt:Timestamp`, `timezone:string`, `location:string`, `capacity:number`, `registrationCount:number`, `attendanceCount:number`, `status:string`, `createdAt:Timestamp`, `createdBy:uid`, `updatedAt:Timestamp` | `imageUrl:string`, `categoryId:string`; status `draft`, `published`, `cancelled`, `completed`; capacity `0` means unlimited | `organizationId -> organizations` |
| `events/{eventId}/registrations/{registrationId}` | Server-generated UUID | `eventId:string`, `organizationId:string`, `userId:uid`, `displayName:string`, `status:string`, `registeredAt:Timestamp`, `updatedAt:Timestamp` | status currently `registered` | event, organization, user |
| `events/{eventId}/checkins/{registrationId}` | Same ID as registration | `eventId:string`, `organizationId:string`, `registrationId:string`, `userId:uid`, `checkedInBy:uid`, `checkedInAt:Timestamp`, `method:string` | method `qr`, `manual` | registration, event, user, gatekeeper |

`events/{eventId}` is the sole V2 event model. `communities/{id}/entries/{id}` remains legacy-only and receives no new V2 event copy. Registration capacity and both QR/manual check-in are enforced by trusted transactions. The V2 QR payload is `good4:event:v2/{eventId}/{registrationId}`; the backend re-loads the event, registration, membership, and existing check-in instead of trusting client claims.

### Event categories

Categories belong to individual events, not organizations. The callable validates any supplied `categoryId` against this fixed list; new web/mobile forms require a choice. For older clients, an omitted field is accepted and the existing value is preserved on edits. Old events are not backfilled. Missing or unknown categories are displayed as “Kategori belirtilmemiş”, included in “Tümü”, and selectable through the presentation-only `uncategorized` filter (never a stored category).

| `categoryId` | Display label |
|---|---|
| `academic-science` | Akademik ve Bilim |
| `career-entrepreneurship` | Kariyer ve Girişimcilik |
| `technology` | Teknoloji |
| `culture-arts` | Kültür ve Sanat |
| `sports-nature` | Spor ve Doğa |
| `social-entertainment` | Sosyal ve Eğlence |
| `volunteering` | Gönüllülük |
| `other` | Diğer |

The backend and web form share `functions/src/eventCategories.ts`; the mobile enum is `community/EventCategories.kt`. Event writes remain callable-only, with manager/membership/organization ownership checks. Student discovery applies category and followed-community filters before limiting the carousel to ten eligible events. Neither filter changes the community discovery search.

## Campaigns, claims, codes, and redemptions

These paths are the existing V2 target contract. Mobile migration to them is Phase 2.

| Domain / path | Document ID | Required fields | Optional fields / enums | Relations |
|---|---|---|---|---|
| `campaigns/{campaignId}` | Server-generated | `organizationId:string`, `title:string`, `description:string`, `startsAt:Timestamp`, `endsAt:Timestamp`, `status:string`, `redemptionCount:number`, `createdAt:Timestamp`, `createdBy:uid`, `updatedAt:Timestamp` | `totalLimit:number|null`; status `draft`, `published`, `cancelled`, `completed` | business organization |
| `campaignClaims/{campaignId}_{uid}` | Deterministic campaign + student | `campaignId:string`, `studentId:uid`, `code:string`, `status:string`, `issuedAt:Timestamp`, `expiresAt:Timestamp` | `redeemedAt:Timestamp`, `redeemedBy:uid`, `updatedAt:Timestamp`; status `issued`, `redeemed`, `expired` | campaign, student, code |
| `campaignCodes/{code}` | Normalized 6–12 character code | `code:string`, `campaignId:string`, `organizationId:string`, `studentId:uid`, `status:string`, `issuedAt:Timestamp`, `expiresAt:Timestamp` | `redeemedAt:Timestamp|null`, `redeemedBy:uid|null`, `updatedAt:Timestamp`; status `issued`, `redeemed`, `expired` | campaign, business organization, student |
| `redemptions/{code}` | Redeemed code | `code:string`, `campaignId:string`, `organizationId:string`, `studentId:uid`, `redeemedBy:uid`, `redeemedAt:Timestamp` | none | code, campaign, business, student |

The canonical naming is `campaignClaims`, `campaignCodes`, and `redemptions`; legacy `community_coupon_codes` and nested entry `claims` are not extended in V2.

## Media contract (design only)

V2 Storage remains deny-all in Phase 1. No public upload permission was added. Proposed object paths are:

- `organizations/{organizationId}/profile/{assetId}` for logo/cover media.
- `events/{eventId}/{assetId}` for event media.
- `campaigns/{campaignId}/{assetId}` for campaign media.

Before enabling uploads, a trusted service must create immutable metadata containing `ownerType`, `ownerId`, `organizationId`, `uploadedBy`, `contentType`, `size`, `createdAt`, and a random `assetId`. Rules or signed-upload endpoints must verify active organization membership, an allowlisted image content type, a strict size limit, and path ownership. Reads should follow the owning record's visibility; writes must never be globally authenticated/public.

## Backward compatibility

- The staging and production legacy flavors keep their current repositories and v1 QR format.
- The V2 flavor reads canonical organizations/events and uses callable transactions for registration/check-in.
- Existing legacy records remain untouched; there is no bulk rewrite or dual-write.
- Legacy numeric/string dates may still be decoded by compatibility adapters, but every new V2 server write uses Firestore `Timestamp`.
- Temporary legacy links support only functionality not migrated in this phase (notably coupons). They are not a new canonical model.
