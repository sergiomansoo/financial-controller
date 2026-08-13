# Frontend Test Stability Spec

## Trigger

The final `npm test` run produced six unrelated five-second timeouts while focused tests passed immediately. This must be diagnosed before delivery.

## Required investigation

1. Reproduce sequentially with `npm test -- --maxWorkers=1` and compare to the default run.
2. Inspect Vitest configuration and test isolation/cleanup for shared timers, fetch mocks, localStorage, portals and Recharts mocks.
3. Identify one evidenced root cause; do not increase timeouts as a workaround.
4. Add a regression test or configuration assertion where applicable.
5. Verify default `npm test` and `npm run build` pass, then document the finding.
