# Windows Maven Wrapper Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `backend\\mvnw.cmd` run Maven commands from PowerShell on Windows so contributors can execute the documented backend tests without installing Maven globally.

**Architecture:** This is a wrapper-only compatibility fix. The Windows hybrid batch/PowerShell script must treat a normal `.m2` directory as having no symbolic-link target, without indexing a null `Target` value. The Linux wrapper (`backend/mvnw`), Spring Boot application, Dockerfile, and Render configuration remain unchanged.

**Tech Stack:** Apache Maven Wrapper 3.3.4, PowerShell, Java 21, Maven 3.9.16.

## Global Constraints

- Modify only `backend/mvnw.cmd` for the functional fix.
- Do not modify Java source, `pom.xml`, Docker files, Flyway configuration, or Render configuration.
- Preserve the wrapper's existing Maven distribution URL and target Maven version: `3.9.16`.
- Do not require a globally installed Maven binary after this change.
- Validate with `.\mvnw.cmd -v` and `.\mvnw.cmd test` from `backend` in PowerShell.

---

## Acceptance Criteria

- On Windows PowerShell, `.\mvnw.cmd -v` no longer terminates with `Cannot start maven from wrapper` or `NullArray`.
- The command reports Apache Maven 3.9.16.
- `.\mvnw.cmd test` starts the Maven test lifecycle and exits successfully.
- The Linux wrapper invocation used by Docker remains `./mvnw package -DskipTests -q`; no Dockerfile change is introduced.

### Task 1: Make the Windows wrapper null-safe for a normal Maven home

**Files:**

- Modify: `backend/mvnw.cmd:91-96`
- Test: PowerShell wrapper smoke command from `backend`

**Interfaces:**

- Consumes: `$MAVEN_M2_PATH`, populated from `MAVEN_USER_HOME` or `$HOME/.m2`.
- Produces: `$MAVEN_WRAPPER_DISTS`, the directory used by the wrapper to find or download Maven.

- [ ] **Step 1: Reproduce the current Windows failure**

From a PowerShell terminal:

```powershell
Set-Location backend
.\mvnw.cmd -v
```

Expected before the fix in the affected environment:

```text
Cannot start maven from wrapper
```

The preceding PowerShell output may contain `Cannot index into a null array`, caused by indexing `.Target[0]` for a normal directory.

- [ ] **Step 2: Replace the unsafe target lookup**

In `backend/mvnw.cmd`, replace this block:

```powershell
$MAVEN_WRAPPER_DISTS = $null
if ((Get-Item $MAVEN_M2_PATH).Target[0] -eq $null) {
  $MAVEN_WRAPPER_DISTS = "$MAVEN_M2_PATH/wrapper/dists"
} else {
  $MAVEN_WRAPPER_DISTS = (Get-Item $MAVEN_M2_PATH).Target[0] + "/wrapper/dists"
}
```

with:

```powershell
$mavenHome = Get-Item $MAVEN_M2_PATH
$mavenHomeTarget = $mavenHome.Target
$MAVEN_WRAPPER_DISTS = if ($null -eq $mavenHomeTarget) {
  "$MAVEN_M2_PATH/wrapper/dists"
} else {
  "$mavenHomeTarget/wrapper/dists"
}
```

This preserves symbolic-link support while avoiding an array index on a null target.

- [ ] **Step 3: Verify wrapper bootstrap**

```powershell
.\mvnw.cmd -v
```

Expected: exit code 0 and output containing:

```text
Apache Maven 3.9.16
```

The first successful invocation can download Maven into the local `.m2` cache. This download is expected and must not be committed.

- [ ] **Step 4: Verify the backend suite through the documented Windows command**

```powershell
.\mvnw.cmd test
```

Expected: exit code 0 and Maven `BUILD SUCCESS`.

- [ ] **Step 5: Review the exact change**

```powershell
git diff --check
git diff -- backend/mvnw.cmd
```

Expected: no whitespace errors and a diff restricted to the null-safe Maven home target lookup.

- [ ] **Step 6: Commit the wrapper fix**

```powershell
git add -- backend/mvnw.cmd
git commit -m "fix(build): support Maven wrapper in PowerShell"
```

## Self-Review

- **Spec coverage:** the plan reproduces the reported Windows failure, applies one wrapper-only change, validates Maven bootstrap and the backend suite, and confirms Docker/Render remain out of scope.
- **Placeholder scan:** no implementation, command, path, expected result, or commit message is left unspecified.
- **Type consistency:** the PowerShell variables used in the replacement are defined within the same wrapper block and produce the existing `$MAVEN_WRAPPER_DISTS` contract.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-08-27-windows-maven-wrapper.md`. Two execution options:

1. **Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration.
2. **Inline Execution** - Execute the task in this session using executing-plans, with a verification checkpoint.

Which approach?
