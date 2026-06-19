---
description: "Run quality gates and fix all failures."
---
Run quality gates, fix until green: `./gradlew lint testDebugUnitTest assembleDebug` (requires Android SDK + CMake/NDK). Re-run until clean. Update docs/CHANGELOG for visible changes. Confirm `git status -sb` clean and on expected branch.
