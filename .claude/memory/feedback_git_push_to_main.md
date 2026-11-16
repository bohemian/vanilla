---
name: force push allowed to main
description: Force push to main is permitted even with branch protection rules active
type: feedback
---

Force push to main is allowed on this repo even with branch protection enabled.

**Why:** Branch protection is configured to allow force pushes, so direct pushes to main don't need a PR.

**How to apply:** When a normal `git push origin main` is rejected due to branch protection, use `git push --force-with-lease origin main` rather than creating a PR just to work around it.
