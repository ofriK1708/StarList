# StarList — Why Each AWS Service?

---

## AWS Cognito
**What it does:** Manages user registration, login, email verification, and issues JWTs.

**Why use it:**
- Handles the entire auth lifecycle (sign up, confirm email, sign in, token refresh) out of the box — no need to write password hashing, session management, or email sending yourself.
- The JWT it issues is verifiable by Spring Boot with a single `issuer-uri` config line — no custom auth middleware needed.
- Scales to millions of users for free up to 50,000 MAU.

**Why Cognito over rolling your own auth:**
Building auth from scratch (bcrypt, email verification, token rotation, brute-force protection) takes weeks and is a common source of security bugs. Cognito gives you all of that battle-tested, audited, and maintained by AWS. The trade-off is that it's opinionated — but for a standard email+password flow, that's a feature not a limitation.

---

## AWS RDS (PostgreSQL)
**What it does:** Managed relational database running PostgreSQL.

**Why use it:**
- Your domain model is highly relational (User owns Tasks, Habits, HabitCompletions, CoinTransactions, GalaxyItems, AiConversations all via `CascadeType.ALL`) — a relational database is the natural fit.
- RDS handles backups, automated patching, and failover automatically — no DBA work needed.
- Runs inside a VPC private subnet, so it's never reachable from the internet — only Beanstalk can talk to it.

**Why RDS over self-hosted Postgres on EC2:**
With EC2 you'd manage OS patches, disk snapshots, and WAL archiving yourself. RDS automates all of that. The cost difference on `db.t3.micro` is negligible for a student project.

**Why PostgreSQL over MySQL / Aurora:**
Spring Boot's `HabitCompletion` uses a unique constraint on `(habit_id, completed_date)` and you use `EnumType.STRING` columns — both work identically on Postgres and MySQL, but Postgres has better JSON support and more precise `TIMESTAMP WITH TIME ZONE` semantics, which matters for your UTC timezone enforcement.

---

## AWS Elastic Beanstalk
**What it does:** Deploys and runs your Spring Boot JAR on managed EC2 instances, with load balancing and auto-scaling included.

**Why use it:**
- You upload a JAR, Beanstalk provisions EC2, sets up the load balancer, manages deployments, and handles health checks — no infrastructure code needed.
- Environment variables (DB credentials, JWT issuer, OpenAI key) are injected securely at runtime — nothing hardcoded.
- Rolling deploys with zero downtime when you push a new version.

**Why Beanstalk over Lambda (serverless):**
Spring Boot is a **long-running, stateful** framework. Lambda is designed for short, stateless functions (max 15 min, cold-start latency). Running Spring Boot on Lambda requires a special adapter, adds 3–10s cold-start delay on the first request after idle, and would complicate local dev. Beanstalk runs Spring Boot exactly as-is — no code changes, no adapter, no cold starts.

**Why Beanstalk over ECS/Fargate:**
Fargate gives more control (Docker containers, fine-grained IAM per service) but requires writing task definitions, cluster config, and ECR image pipelines. Beanstalk is simpler: one `eb deploy` command and you're done. For a single-service backend, that extra control isn't worth the setup overhead.

**Why Beanstalk over a raw EC2 instance:**
EC2 would mean SSHing in to update the app, managing the Java runtime, writing your own health checks, and setting up nginx manually. Beanstalk wraps all of that — EC2 under the hood, but with a managed lifecycle on top.

---

## AWS Amplify
**What it does:** Hosts the React/Vite frontend as a static site with a global CDN, with automatic deploys on every git push.

**Why use it:**
- Connects directly to GitHub — every push to your branch triggers a build (`npm run build`) and deploys the `dist/` output automatically.
- Built-in CDN means the frontend loads fast globally, not just from `us-east-1`.
- Custom environment variables per branch (`VITE_API_URL`) make it easy to separate dev and prod builds.
- The `@aws-amplify/auth` SDK integrates natively with your Cognito User Pool — sign-in UI and token management are a few lines of code.

**Why Amplify over S3 + CloudFront manually:**
You could host a Vite build on S3 with a CloudFront distribution in front. That's actually what Amplify does internally — but to do it yourself you'd write CloudFormation, configure cache invalidations on deploy, and set up the GitHub Actions pipeline manually. Amplify bundles all of that and adds a web console to see build logs. Worth it unless you need non-standard CDN rules.

**Why Amplify over Vercel / Netlify:**
For this project, Amplify keeps everything in one AWS account and one billing line. It also avoids CORS complications — both Amplify and Beanstalk are in the same AWS account, so managing allowed origins is straightforward. The trade-off is that Amplify's CI/CD is slightly less ergonomic than Vercel, but not enough to matter here.

---

## AWS S3 (Planned — Plant Pictures)
**What it does:** Object storage for user-uploaded images (plant pictures).

**Why use it:**
- Never store binary files in PostgreSQL or on the Beanstalk EC2 disk. RDS `bytea` columns are slow and bloat your DB backups. EC2 disk is ephemeral — Beanstalk can replace instances during auto-scaling, wiping anything stored locally.
- S3 is durable (99.999999999%), cheap (~$0.023/GB/month), and integrates with presigned URLs so uploads go directly from the browser to S3 — not through your backend at all.
- Presigned URLs mean Beanstalk never handles the file bytes: it just generates a short-lived S3 URL, returns it to the frontend, and the browser uploads directly. This keeps your EC2 memory and network free.

**Why S3 over Cloudinary / external image CDN:**
S3 keeps all data in your AWS account — no third-party data sharing. You can add CloudFront in front of S3 later for image CDN delivery without changing the upload flow.

---

## OpenAI API (External)
**What it does:** Powers the AI conversation feature (`AiConversation` entity) using `gpt-4.1-mini`.

**Why external (not AWS Bedrock):**
`gpt-4.1-mini` is not available on AWS Bedrock. The `AiConversation` feature is already built against the OpenAI SDK, so switching providers would require rewriting the service layer. Bedrock would make sense if the project later needs to stay 100% within AWS (e.g., for compliance reasons), but for a student project, OpenAI is simpler and the model quality is better for general chat.
