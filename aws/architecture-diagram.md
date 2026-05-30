# StarList — AWS Architecture Diagram

```mermaid
flowchart TD
    Client(["💻 Client Browser"])

    subgraph AWS_Cloud["☁️ AWS Cloud — us-east-1"]

        subgraph Frontend["Frontend Hosting"]
            Amplify["🔷 AWS Amplify\nApp: StarList\nID: d2dvwqrfms8gb6\nd2dvwqrfms8gb6.amplifyapp.com"]
        end

        subgraph Auth["Authentication"]
            Cognito["🔐 AWS Cognito\nUser pool - zpbhsk\nPool ID: us-east-1_WBlndOjnO\nIssues JWT idTokens"]
        end

        subgraph Compute["Compute"]
            Beanstalk["🌿 Elastic Beanstalk\nSpring Boot 4 / Java 25\nStarlist-env-1\nREST API :8080"]
        end

        subgraph Data["Data & Storage"]
            RDS[("🗄️ AWS RDS\nPostgreSQL\nstarlistdb\n(private subnet)")]
            S3["🪣 AWS S3\nstarlist-media-bucket\nPlant pictures"]
        end

    end

    ExternalAI["🤖 OpenAI API\ngpt-4.1-mini\n(external)"]

    %% Client loads SPA
    Client -->|"HTTPS — loads React app"| Amplify

    %% Auth flow
    Amplify -->|"signUp / signIn\nfetchAuthSession"| Cognito
    Cognito -->|"JWT idToken\n(Bearer)"| Amplify

    %% API calls
    Amplify -->|"HTTPS REST\nAuthorization: Bearer JWT"| Beanstalk

    %% JWT validation (internal)
    Beanstalk -.->|"Validate JWT\nissuer-uri check"| Cognito

    %% DB
    Beanstalk -->|"JDBC / SQL\n(inside VPC)"| RDS

    %% S3 — presigned URL flow
    Amplify -->|"Upload via presigned URL"| S3
    Amplify -->|"Fetch plant images"| S3
    Beanstalk -->|"Generate presigned URL\nfor upload"| S3

    %% AI
    Beanstalk -->|"HTTPS + API Key"| ExternalAI

    style AWS_Cloud fill:#f0f7ff,stroke:#0072bb,stroke-width:2px
    style Frontend fill:#e8f5e9,stroke:#2e7d32
    style Auth fill:#fff3e0,stroke:#e65100
    style Compute fill:#e3f2fd,stroke:#1565c0
    style Data fill:#fce4ec,stroke:#880e4f
    style ExternalAI fill:#f3e5f5,stroke:#6a1b9a
```

> Dashed arrows (-.->): internal AWS calls not visible to the client.
> All services are **live** in `us-east-1`. S3 bucket name to be filled in once confirmed.

---

## Data Flow Walkthrough

### 1. User Loads the App
```
Browser → Amplify CDN (d2dvwqrfms8gb6.amplifyapp.com) → React bundle
```

### 2. Sign Up / Sign In
```
React (authService.ts)
  → aws-amplify/auth SDK → Cognito (us-east-1_WBlndOjnO)
  ← JWT idToken  (payload: sub = cognitoUserId)
```

### 3. REST API Calls
```
React (api.ts / axios)
  → Authorization: Bearer <idToken> → Beanstalk (Starlist-env-1) :8080

Spring Boot (Spring Security OAuth2 resource server)
  -.-> validates JWT against https://cognito-idp.us-east-1.amazonaws.com/us-east-1_WBlndOjnO
  → extracts cognitoUserId → looks up User in RDS (starlistdb)
  ← JSON response
```

### 4. Plant Picture Upload
```
React → GET /upload-url → Beanstalk → generates S3 presigned PUT URL
React → PUT <file bytes> directly to S3 presigned URL  (bypasses backend)
React → renders image from S3 URL
```

### 5. AI Chat
```
Spring Boot AiConversation service
  → OpenAI API (gpt-4.1-mini)
  ← response → persisted in RDS (AiConversation entity)
```

---

## Live Resource Reference

| Service | Resource | ID / Endpoint |
|---------|----------|---------------|
| Cognito | User pool - zpbhsk | `us-east-1_WBlndOjnO` |
| Cognito | App client | `12j25ufrhgbfei0fmdq33b9e1g` |
| Cognito | JWT issuer | `https://cognito-idp.us-east-1.amazonaws.com/us-east-1_WBlndOjnO` |
| Amplify | StarList app | `d2dvwqrfms8gb6` → `d2dvwqrfms8gb6.amplifyapp.com` |
| Beanstalk | Starlist-env-1 | `starlist-env-1.eba-gjim97su.us-east-1.elasticbeanstalk.com` |
| RDS | starlistdb | `starlistdb.cdxhnilumxca.us-east-1.rds.amazonaws.com:5432` |
| S3 | starlist-media-bucket | `starlist-media-bucket` |
