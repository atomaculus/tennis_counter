# PLAYCE Launch Checklist

Last reviewed: April 8, 2026

This checklist is specific to the current PLAYCE repository and Google Play launch flow.

Status legend:
- `[x]` Done in repo or explicitly confirmed
- `[~]` Partially done or done locally but still needs external confirmation
- `[ ]` Pending
- `[-]` Not applicable right now

## 1. Product and build baseline

- [x] App title is defined as `PLAYCE` in both app modules.
- [x] Shared scoring engine, live score, spectator mode, widget, onboarding, history, stats, CSV export, and Premium flow exist in code.
- [x] `PLAYCE_VERSION_CODE` exists in `gradle.properties`.
- [x] `PLAYCE_VERSION_NAME` exists in `gradle.properties`.
- [x] `applicationId` is aligned across `:app` and `:mobile` as `com.playce.tenniscounter.app`.
- [ ] Bump `PLAYCE_VERSION_CODE` for the next store upload.
- [ ] Confirm `PLAYCE_VERSION_NAME` matches release notes and store copy.
- [ ] Add production `google-services.json` for the Firebase-enabled release build.
- [ ] Build signed release artifacts with the real keystore in place.

## 2. Google Play listing

- [ ] Final app title reviewed for store positioning.
- [-] Apple-style subtitle field is not applicable to Google Play.
- [ ] Short description prepared.
- [ ] Full description prepared.
- [-] Apple keyword field is not applicable to Google Play.
- [ ] Final phone screenshots exported from release candidate build.
- [ ] Final Wear OS screenshots exported from release candidate build.
- [ ] Promo video decided: publish or intentionally skip.
- [ ] Privacy policy URL is public and final.
- [ ] Support URL is public and final, or store support info is otherwise complete.
- [ ] App category selected in Play Console.
- [ ] Content rating questionnaire completed in Play Console.
- [ ] In-app product `premium_unlock` is active and linked correctly.

## 3. Website and public URLs

- [x] Landing page exists in `website/index.html`.
- [x] Privacy policy page exists in `website/privacy.html`.
- [x] Terms page exists in `website/terms.html`.
- [x] Site has responsive structure and media queries.
- [x] Meta title and description exist on landing and policy pages.
- [x] Favicon file exists and is linked.
- [~] CTA buttons work, but must be verified against the final published Play listing.
- [ ] Replace any placeholder or generic Google Play links with the final listing URL if it changes.
- [ ] Set final production domain in canonical / Open Graph / sitemap values.
- [ ] Confirm SSL is active on the public deployed domain.

## 4. SEO and discovery

- [x] `robots.txt` exists.
- [x] `sitemap.xml` exists.
- [x] Open Graph base tags exist on the landing page.
- [ ] Replace placeholder production-domain values in OG tags and sitemap.
- [ ] Connect Google Search Console.
- [ ] Connect Bing Webmaster Tools.
- [ ] Submit sitemap after production domain is live.
- [-] IndexNow is optional and not required for launch.

## 5. Legal and policy

- [x] Privacy policy is written in English and Spanish.
- [x] Terms of Service are written and linked on the website.
- [x] Data handling is documented in repo docs and public policy.
- [~] Firebase Crashlytics and Analytics are disclosed in policy text, but final Play Console answers must match the shipped build exactly.
- [ ] Review policy text once production Firebase config is added.
- [ ] Confirm support email, privacy page, and store listing use the same contact info.
- [~] GDPR review should be treated as scope check, not assumed complete.
- [-] Cookie notice is not needed unless the website later adds tracking cookies or similar web analytics tooling.

## 6. QA and release gate

- [x] Manual QA runbook exists in `docs/manual-qa-guide.md`.
- [x] Play Store release checklist exists in `docs/playstore-checklist.md`.
- [ ] Re-run full test and build command set on the release candidate.
- [ ] Complete real-device QA on phone + scorer watch.
- [ ] Complete second-watch spectator QA if that feature is shipping in this release.
- [ ] Verify watch -> phone sync retry / ACK flow on real devices.
- [ ] Verify Premium purchase and restore flow on test account.
- [ ] Verify Spanish localization on both devices.
- [ ] Archive release artifacts and the exact submitted version identifiers.

## 7. Marketing and launch ops

- [ ] Launch post drafted.
- [ ] Social assets exported.
- [ ] Friends / community / testers ready to support launch.
- [-] Email list is optional if none exists.
- [-] Product Hunt is optional and should only be added if there is an intentional launch plan for it.

## 8. Go / No-Go summary

Do not submit if any of these are still unresolved:

- [ ] Release build fails.
- [ ] `google-services.json` is missing for the intended Firebase-enabled build.
- [ ] Privacy policy URL is not public.
- [ ] Terms URL is not public.
- [ ] Data safety answers do not match the shipped build.
- [ ] Sync retry / ACK flow is flaky on real devices.
- [ ] Final store assets and descriptions are missing.
