# PLAYCE Launch Go / No-Go Matrix

Last reviewed: April 8, 2026

Use this as the final release snapshot. It separates what is already true in the repo from what still depends on external setup, QA, or Play Console work.

## Go Criteria

| Area | Item | Status | Notes |
|---|---|---|---|
| Product | Core app features implemented | GO | Wear scorer, phone companion, shared engine, live sync, history, stats, export, widget, onboarding, Premium, spectator mode all exist in code. |
| Build | Version code and name defined | GO | Present in `gradle.properties`. |
| Build | Shared and module structure are coherent | GO | `:app`, `:mobile`, `:shared` are consistent. |
| Website | Landing page exists | GO | Static site is present in `website/`. |
| Website | Privacy page exists | GO | Public page exists in repo. |
| Website | Terms page exists | GO | Public page added in repo. |
| Website | Basic metadata exists | GO | Title, description, favicon, OG, robots, sitemap files exist. |
| Legal | Privacy docs exist in EN/ES | GO | Repo docs and website policy exist. |
| Legal | Firebase data collection disclosed | GO | Policy text now assumes Firebase-enabled release. |

## No-Go Criteria

| Area | Item | Status | Notes |
|---|---|---|---|
| Build | Production Firebase config added | NO-GO | `google-services.json` still needs to be supplied outside the repo snapshot. |
| Build | Signed release artifacts validated | NO-GO | Needs real keystore and candidate build run. |
| QA | Real-device phone + watch QA complete | NO-GO | Still pending. |
| QA | Sync retry / ACK flow validated on devices | NO-GO | High-risk launch gate. |
| QA | Premium purchase / restore validated | NO-GO | Must be verified with test account. |
| Store | Final Play listing copy prepared | NO-GO | Short/full description still pending. |
| Store | Final screenshots exported | NO-GO | Phone and watch assets still pending. |
| Store | Content rating and category completed | NO-GO | Play Console work still pending. |
| Legal | Public privacy URL live | NO-GO | Local file exists, public production URL still pending. |
| Legal | Public terms URL live | NO-GO | Local file exists, public production URL still pending. |
| Legal | Data safety answers aligned with shipped build | NO-GO | Must be completed in Play Console after Firebase config is final. |
| Website | Production domain finalized in OG/sitemap | NO-GO | Placeholder production-domain values still need replacement. |

## External Dependencies

These cannot be closed purely from the repository:

- production website domain
- deployed HTTPS website
- public privacy URL
- public terms URL
- Play Console listing
- Play Console Data safety
- Firebase project configuration
- production signing keystore
- real-device QA evidence

## Recommended Submission Rule

Treat launch as `NO-GO` until every item in the No-Go table has an owner, evidence, and closure date.
