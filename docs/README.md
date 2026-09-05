# RWA Shift Documentation

This folder documents the platform for three different audiences. Each doc stands alone —
read whichever matches who you are.

| Doc | Audience | What it covers |
|---|---|---|
| [`user-guide-happy-path.md`](user-guide-happy-path.md) | Anyone new to the platform, no technical background | A single guided walkthrough of the platform's one complete success path, screen by screen, in plain language. |
| [`qa-testing-guide.md`](qa-testing-guide.md) | QA / testers | Every role, every workflow, environment setup, seed accounts, edge cases and negative paths worth testing, known test coverage, and how to reset state. |
| [`business-overview.md`](business-overview.md) | Business leaders / non-technical stakeholders | What the platform does, who it's for, what problem it solves, and where it stands today (V1/demo vs. production-ready). |
| [`critical-analysis.md`](critical-analysis.md) | Anyone deciding whether to invest further, sell, or launch this | An unvarnished assessment of what's actually solid, what's a demo-only shortcut, and what would break first under real usage. Written to disagree where disagreement is warranted, not to flatter the work. |

## One-paragraph orientation

RWA Shift is a platform for turning real-world real-estate assets into on-chain, ERC-3643-compliant
security tokens, and running the full lifecycle around that: an issuer organization registers an
asset, structures it legally, creates an investment offering, gets it approved by compliance,
tokenizes it on-chain, and investors — who go through KYC and wallet verification first — buy units
either by simulated bank transfer or by paying real USDC from a connected wallet. Three portals
(Platform Admin, Issuer, Investor) sit on one Next.js frontend; a Spring Boot backend owns every
off-chain business rule; a Foundry/Solidity project owns the on-chain token and compliance
contracts. See the root [`README.md`](../README.md) for how to run all three together.
