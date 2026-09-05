# RWA Shift — Happy Path Walkthrough

This is a plain-language walkthrough of the platform's one complete success path — from an
organization being registered to an investor owning tokens in a real estate deal. No jargon, no
error handling, just "what does a good run through the whole system look like." Every step below
has actually been run and confirmed working on this platform.

You don't need to know anything about blockchain to follow this. Where a blockchain-specific idea
comes up, it's explained in one sentence.

## The story in one paragraph

A real estate company ("the issuer") wants to sell ownership shares in a building to investors,
digitally. A platform administrator sets up the issuer's account. The issuer describes the
building, sets up the legal entity that owns it, and creates an "offering" — the terms of how many
shares exist and what they cost. A compliance reviewer checks and approves it. The system then
mints digital tokens representing ownership, one token per share. Meanwhile, an investor signs up,
proves their identity, links a payment wallet, and buys some of those shares. Once they pay, they
instantly and automatically receive their tokens — verifiable proof of ownership, forever recorded
on a shared ledger (the "blockchain") that nobody can quietly alter.

## Step by step

### 1. The platform admin creates the issuer's organization

The platform administrator logs in and registers a new organization — the real estate company that
will be listing properties (e.g. "ABC Real Estate LLC"). The admin then creates a login for that
company's staff (an "Issuer Admin" account) and hands them a one-time password.

### 2. The issuer logs in and describes their property

The issuer signs in with the credentials they were given. From their dashboard, they add a new
asset: a name, location, a description, and its appraised value (e.g. "Dubai Business Tower,"
$10,000,000). This is just a record for now — nothing has happened on the blockchain yet.

### 3. The issuer sets up the legal structure

Real ownership of a building isn't handed out directly — it's wrapped in a legal entity (an "SPV,"
short for special-purpose vehicle) that actually owns the property, and investors own shares of
*that* entity. The issuer records this entity's details (name, jurisdiction, registration number)
against the asset.

### 4. The issuer creates the offering

The issuer defines the actual investment terms: how much money they want to raise in total, how
many units (shares) that splits into, the price per unit, the minimum an investor can put in, and
what return investors are being offered. For example: "$2,000,000 raise, 20,000 units at $100
each, $5,000 minimum, 20% offered interest."

### 5. Compliance reviews and approves it

The issuer submits the offering for review. A compliance officer (or platform admin) looks it over
and approves it. This is a deliberate checkpoint — nothing goes live to investors without a human
sign-off.

### 6. The system tokenizes the offering

Once approved, the issuer clicks "Tokenize." Behind the scenes, the platform creates a real digital
token on the blockchain for this offering — this is the actual mechanism that will represent
ownership. Each organization signs this with its own dedicated on-chain wallet, not a shared
platform one, so one organization's activity is never able to touch another's. (The first time a
brand-new organization ever tokenizes something, that wallet needs a small amount of network gas
before it can transact — a one-time setup step a platform administrator handles.) This step takes a
little time (it's a real blockchain transaction), and once it's confirmed, the offering is "Open" —
ready for investors.

### 7. An investor signs up and completes verification

Meanwhile, an investor creates their own account (or the platform admin provisions one for them).
Before they can invest in anything, two things have to be true:

- **Identity verification (KYC)**: the investor submits their details, and a compliance reviewer
  verifies them. This confirms the platform knows who its investors actually are.
- **A linked wallet**: the investor connects a crypto wallet (the same idea as a bank account
  number, but for holding digital tokens) that will receive their ownership tokens and, if they
  choose, send payment.

### 8. The investor browses opportunities and picks one

Once verified, the investor sees a list of open offerings — "Dubai Business Tower SPV Units" among
them — with the key numbers up front: asset value, raise target, unit price, minimum investment,
and how much has already been raised.

### 9. The investor decides how much to invest

They pick an amount (e.g. $10,000, which works out to 100 units at $100 each) and review the terms
one more time before confirming.

### 10. The investor chooses how to pay

There are two ways to pay:

- **Pay with wallet**: send USDC (a digital dollar-equivalent) directly from their connected
  wallet. This confirms automatically, usually within seconds — no one at the company needs to do
  anything.
- **Bank transfer**: wire the money the traditional way; a platform operator manually confirms once
  it's received.

### 11. Payment confirms and tokens are issued automatically

The moment payment is confirmed (instantly for a wallet payment, or once an operator confirms a
bank transfer), the platform automatically mints the investor's ownership tokens and sends them to
their linked wallet. No one has to manually "release" anything — this step is fully automated once
payment is in.

### 12. The investor owns real, on-chain shares

The investor can now see their holding in their portfolio: how many units, what percentage of the
offering that represents, and — for anyone who wants to double-check — the token's own on-chain
balance, independent of what the company's internal records say. This dual view (the company's
record and the blockchain's own record) is the point of tokenizing in the first place: the two
should always agree, and either side can be checked by an outsider.

### 13. (Ongoing) Distributions

If the property generates income (e.g. rental income), the issuer can later create a "distribution"
— calculating each investor's pro-rata share and recording the payout run, anchored on-chain so it
can't be quietly altered after the fact.

## What "happy path" means here

This walkthrough assumes everything goes right the first time: the issuer's paperwork is in order,
compliance approves on the first pass, the investor is already eligible, and the payment succeeds.
In real use, any of these steps can also fail, be rejected, or need a retry — see
[`qa-testing-guide.md`](qa-testing-guide.md) for what those paths look like and how they're handled.
