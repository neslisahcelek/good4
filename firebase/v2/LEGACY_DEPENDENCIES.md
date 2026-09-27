# Good4 V2 mobile legacy dependency map

| File / area | Current datasource | Expected V2 datasource | Required change | Migration risk |
|---|---|---|---|---|
| `CommunityRepository.kt` community list | `communities` | `organizations` where `type=community`, `status=active` | Implemented for V2 flavor; staging/prod retain legacy path | Medium: organization profile field completeness |
| `CommunityRepository.kt` manager access | `community_access/{email}` | `organizations/{id}/members/{uid}` | Implemented V2 membership adapter | Medium: membership queries currently fan out across visible communities |
| `CommunityRepository.kt` entries | `communities/{id}/entries` | `events` by `organizationId` | Event read/create/cancel implemented for V2; no event dual-write | Low after indexes/rules deploy |
| `CommunityRepository.kt` registrations/tickets | nested legacy registrations keyed by UID, client writes | `events/{id}/registrations/{registrationId}` | Implemented V2 query + callable transaction; registration ID is ticket token | Low; requires Functions deployment with mobile release |
| `EventAdmission.*` | legacy registrations + `attendance/{uid}` client transaction | V2 registrations + `checkins/{registrationId}` trusted transaction | Implemented platform reads and one callable QR/manual operation | Low; v1/v2 QR payloads intentionally differ |
| `CommunityRepository.kt` followers | `communities/{id}/followers` | `organizations/{id}/followers` | V2 reads implemented; V2 write intentionally deferred rather than weakening rules | Medium: follow CTA must stay disabled/handled until server operation exists |
| `CommunityRepository.kt` profile editing | `communities/{id}` client update | `organizations/{id}` trusted update operation | Deferred; V2 adapter returns an explicit error | Medium |
| `CommunityRepository.kt` community coupons | nested `entries`, `claims`, `community_coupon_codes` | `campaigns`, `campaignClaims`, `campaignCodes`, `redemptions` | Not migrated in Phase 1; V2 adapter rejects legacy coupon mutation | High: status/limit/code lifecycle differs |
| `CampaignRepository.kt`, `CampaignDto.kt` | top-level legacy `campaigns` with image-oriented DTO and direct client writes | canonical V2 campaign schema and callable services | Replace DTO, reads, and admin mutation UI in Phase 2 | High: schema is not field-compatible |
| `VerifyCodeViewModel.kt` | legacy product/order codes plus legacy community coupon fallback | `redeemCampaignCode` + V2 redemption result | Route by explicit code domain in Phase 2 | High: avoid accepting stale legacy codes |
| business verification/history screens | legacy codes/orders and temporary legacy coupon bridge | `campaignCodes`, `redemptions` | Add V2 redemption repository/history query | High: legacy redemptions are absent from current V2 history |
| Android/iOS Firestore DTO adapters | Long/string/Timestamp compatibility | Timestamp-backed V2 DTOs | Event Timestamp decoding implemented; retain old decoders only for legacy flavors | Medium |

## Phase 2 campaign/coupon findings

- Stale legacy coupon codes exist and need a read-only inventory plus an explicit invalidation/migration decision before any data write.
- Legacy community coupon `totalLimit` is not atomically enforced; V2 campaign redemption must remain the authority and already checks `totalLimit` in its transaction.
- Legacy redemptions are not represented in the V2 business panel. A one-time, dry-run-first migration or historical read-only view must be approved separately.
- Mobile `CampaignDto` is image-centric and does not match V2 fields (`organizationId`, title/description, Timestamp window, status, totalLimit/redemptionCount). It must be replaced by a V2 DTO/adapter, not silently decoded into the old type.
- Migration order: finalize DTO contract → add V2 mobile reads → add callable issue/redeem integration → reconcile stale codes and limits → dry-run historical redemption mapping → obtain approval before production migration → remove temporary legacy bridge only after parity checks.
