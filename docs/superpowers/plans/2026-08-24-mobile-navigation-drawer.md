# Mobile Navigation Drawer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `subagent-driven-development` (recommended) or `executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the primary navigation usable on phones as a labelled lateral drawer that users can open, close, and navigate without changing the desktop navigation.

**Architecture:** Keep the existing desktop `collapsed` state and render the mobile drawer from a separate `isMobileNavOpen` state in `AppLayout`. The sidebar remains the single navigation component; on a phone it is always rendered with its labels and is moved on/off screen only through a mobile modifier class. A backdrop, keyboard handling, focus restoration, and scroll locking complete the drawer interaction.

**Tech Stack:** React 19, TypeScript, React Router, Vitest, Testing Library, CSS media queries, lucide-react.

## Global Constraints

- This change applies only at the existing phone breakpoint (`max-width: 767px`). At 768px and above, the current collapsible desktop sidebar and header control must retain their behaviour and visuals.
- The phone drawer must never use the desktop collapsed icon-only presentation: every navigation item and the Ledger brand remain labelled when the drawer is open.
- No new package, asset, route, API, persistence change, or backend change is required.
- The drawer begins closed on a phone. Users can close it using the header control, the backdrop, Escape, or by choosing a navigation link.
- While the phone drawer is open, document scrolling is locked. Cleanup must occur when it closes or the layout unmounts.
- The mobile control must expose its open state through `aria-expanded`; the backdrop must have an accessible close label. Keyboard focus returns to the menu control when a close action is initiated by Escape or the backdrop.
- Do not stage or modify the unrelated mobile-responsive plan already present in the worktree.

## File Responsibilities

- `frontend/src/components/AppLayout.tsx`: owns separate desktop/mobile navigation state, close behaviours, focus restoration, and mobile accessibility attributes.
- `frontend/src/components/Sidebar.tsx`: accepts mobile drawer state and invokes an optional navigation callback after a link is selected, while preserving delayed tooltips for a collapsed desktop sidebar.
- `frontend/src/mobile-navigation.css`: adds phone drawer/backdrop/scroll-lock overrides after the base styles; it is imported last by `main.tsx` so the previous phone sidebar rule is safely overridden.
- `frontend/src/components/AppLayout.test.tsx`: proves mobile open/close behaviour, labelled navigation, Escape/backdrop handling, and scroll lock.
- `frontend/src/components/Sidebar.test.tsx`: preserves the desktop tooltip regression test and verifies the mobile link callback.

## Implementation Tasks

- [ ] **1. Specify the mobile interaction contract**
  - Record the constraints above before changing production code.
  - Confirm the current issue: phone CSS turns `.ledger-sidebar--collapsed` into a visible 256px panel but the collapsed selector hides its text.

- [ ] **2. Add failing component tests first**
  - Create `AppLayout.test.tsx`, mock `MovementFilter`, and mock phone `matchMedia`.
  - Assert that activating `Abrir navega\u00e7\u00e3o` exposes `.ledger-sidebar--mobile-open`, keeps `Vis\u00e3o geral` visible, and applies a body scroll-lock class.
  - Assert that Escape and the backdrop close the drawer, remove the scroll-lock class, and restore focus to the menu button.
  - Extend `Sidebar.test.tsx` to assert selecting a link calls `onNavigate` when the mobile drawer is open.

- [ ] **3. Implement state separation and accessibility**
  - Add `isMobileNavOpen` in `AppLayout`; use it only at the phone breakpoint and leave `collapsed` for desktop.
  - Pass `collapsed={false}` on phones so label rendering is independent from desktop collapse state.
  - Add the backdrop, `aria-expanded`, contextual open/close labels, Escape listener, body lock, and focus restoration.
  - Add `isMobileOpen` and `onNavigate` support in `Sidebar`; suppress desktop-only tooltip behaviour while the drawer is open.

- [ ] **4. Add mobile-only styling**
  - At `max-width: 767px`, transform the sidebar from the left edge and reveal it through `.ledger-sidebar--mobile-open`.
  - Add a fixed backdrop beneath the sidebar and above application content.
  - Keep the drawer narrow enough to preserve an obvious close area on small screens; keep the desktop width transition untouched outside the media query.

- [ ] **5. Verify and publish**
  - Run `npx vitest run src/components/AppLayout.test.tsx src/components/Sidebar.test.tsx` from `frontend`.
  - Run `npm run build` from `frontend`.
  - Review the staged diff, commit only this specification and the relevant implementation/tests, then push `main` to `origin`.

## Acceptance Criteria

1. On a phone, the menu starts closed and opens as a left-side drawer with the Ledger name and every link label readable.
2. The drawer closes reliably through the menu button, backdrop, Escape, and link navigation, leaving users able to continue navigating.
3. Opening the drawer prevents background scrolling and closing/unmounting restores it.
4. Screen readers receive an accurate menu button name/state and can activate the backdrop close control.
5. Desktop navigation still switches between the existing expanded and icon-only collapsed modes.
6. Targeted tests and the production frontend build pass before publication.
