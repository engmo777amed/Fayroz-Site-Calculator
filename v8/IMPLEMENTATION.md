# Site Calculator 9.0.0

Base: V8.1.0 from commit 4432a732157b8825067bedb0f3ecdee2c9a3b3db, v8 module.

## User-visible changes
- Independent takeoff parts and wall/surface selection; splash coats for walls and ceilings.
- Net work quantities exclude procurement waste; repetitions applied once.
- Mortar specifications, prices and per-part overrides; material consumption costs and separate labor, transport and equipment.
- Linked tile, skirting, masonry, paint, waterproofing and gypsum material calculators.
- Purchase aggregation groups equal recipes/prices/package sizes before package rounding.
- Arabic calculator library: finishing, site geometry, concrete, steel takeoff, earthworks, roads, plumbing/electrical quantities.
- Quick/detailed fields, saved input snapshots, alternative comparison, source quantities, favorites, search, hidden groups.
- Project archive/copy/delete, floor copy/rename/order, room copy/detach/order; automatic drafts and save-and-add.
- Filtered quantity/cost reports, native XLSX export, Arabic WebView PDF printing.
- ZIP backups include photos and settings, merge or replace restore, pre-import recovery snapshot.

## Scope and limitations to review
- Material rates are adjustable inputs, not a substitute for project/product specifications.
- Steel, plumbing and electrical tools estimate specified quantities; they do not design reinforcement or select cable/pipe sizes.
- Masonry calculator supports regular longitudinal courses; mixed bond layouts require separate measurements.
- Bar cutting covers repeated identical cut lengths; mixed-length optimization is not claimed.
- Floor slope volume assumes a planar linear slope and horizontal base; split multiple slopes into parts.
- PDF export uses the Android print dialog (Save as PDF).
- Existing V8 APK was a debug build and its signing key is unavailable. Release uses applicationId com.fayroz.sitecalculator.v9 and label Fayroz Site Calculator 9, so it installs alongside V8 without deleting its data. Transfer existing data through V8 backup import. Future V9 updates reuse the privately retained release key.

## Verification
GitHub Actions runs unit tests, Android lint, instrumented emulator tests, and debug/release assembly. Instrumented tests cover navigation, calculator opening, backup with photos, merge safety and XLSX contents. Artifact screenshots support visual review. Release signing is performed privately; signing keys are never committed to the public repository.
