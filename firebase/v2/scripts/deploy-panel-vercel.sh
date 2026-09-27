#!/usr/bin/env bash
# Publishes the web panel to good4tr.com (Vercel project good4-panels).
# The panel imports shared code from ../functions/src, which a plain `vercel deploy`
# does not upload, so we build here and send the prebuilt output.
set -euo pipefail

cd "$(dirname "$0")/../web"

npx --yes vercel pull --yes --environment=production
npx --yes vercel build --prod
npx --yes vercel deploy --prebuilt --prod
