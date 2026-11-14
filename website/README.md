# PLAYCE Website

Static site for PLAYCE, the tennis and padel scorer for iPhone, Apple Watch, Android, Wear OS and Garmin. Deployed on Vercel with the project root set to `website/`. Production URL: https://tenniscounter.vercel.app/

There is no build step. `vercel.json` turns on clean URLs (`/privacy` serves `privacy.html`).

## Pages

- `index.html`: landing page (EN/ES toggle). Partido and the 21 training mode, tennis and padel, store links for Google Play and the App Store.
- `download.html`: one link that sends iPhone to the App Store and Android to Google Play.
- `privacy.html`: privacy policy, Spanish primary, English with the same toggle or `?lang=en`. Last updated 2026-11-14.
- `terms.html`: terms of service from the November 14, 2026 drafts. Spanish primary at `/terms`, English at `/terms?lang=en`. Covers iPhone, Apple Watch, Android, Wear OS and Garmin.
- `delete-data.html`: how to clear local data and how to email a deletion request.

## SEO and icons

- `assets/og-playce.png`: Open Graph image, 1200×630, referenced with an absolute URL.
- `favicon.svg`, `favicon.ico`, `apple-touch-icon.png`.
- `sitemap.xml` and `robots.txt`.
- Canonical URLs, Twitter card tags and `SoftwareApplication` JSON-LD on the home page.

## Store links

- Google Play: https://play.google.com/store/apps/details?id=com.playce.tenniscounter.app
- App Store: https://apps.apple.com/ar/app/playce-tennis-padel/id6769161754

TODO: there is no Garmin Connect IQ store URL in this repository. The Connect IQ app id is `c4f18a72b93e4d6fa1c8e5b2079d3a44` (`GarminConstants`, aligned with `playce_garmin`). The site names Garmin Connect IQ and does not link a store page. Do not invent an `apps.garmin.com` URL.

## Vercel

Set the project root directory to `website/`. No build command and no install command are required.
