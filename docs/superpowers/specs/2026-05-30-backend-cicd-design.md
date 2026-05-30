# Backend CI/CD Pipeline — Design Spec

## Overview

A GitHub Actions workflow that automatically tests and deploys the StarList Spring Boot backend to AWS Elastic Beanstalk on every push to `main`, with a manual trigger option as well.

## Trigger

- **Automatic:** push to `main` branch, only when files under `StarList-Backend/**` change
- **Manual:** `workflow_dispatch` (button in GitHub Actions UI)

The `paths` filter prevents a frontend-only commit from triggering an unnecessary backend deploy.

## Jobs

### Job 1: `test`

Runs on every trigger before any deployment occurs.

- Checks out the repo
- Sets up Java 25 (matches the project's `java.version` in `pom.xml`)
- Runs `mvn test` from the `StarList-Backend/` directory
- If any test fails, the workflow stops — `deploy` never runs

### Job 2: `deploy`

Only runs if `test` passes (`needs: test`).

- Checks out the repo
- Sets up Java 25
- Builds the JAR: `mvn package -DskipTests` (tests already passed in job 1)
- Artifact: `StarList-Backend/app/target/app-*.jar`
- Configures AWS credentials via `aws-actions/configure-aws-credentials@v4`
- Deploys via `einaregilsson/beanstalk-deploy@v22`:
  - Application: `Starlist`
  - Environment: `Starlist-env-1`
  - Region: `us-east-1`
  - Version label: `starlist-{github.sha}` (unique per commit)
  - Deployment package: the built JAR

## Secrets Required

Three secrets must be added to the GitHub repository (`Settings → Secrets and variables → Actions`):

| Secret | Where to get it |
|--------|----------------|
| `AWS_ACCESS_KEY_ID` | IAM console → create user with EB deploy permissions → access key |
| `AWS_SECRET_ACCESS_KEY` | Same IAM user creation flow |
| `AWS_REGION` | `us-east-1` |

### IAM Permissions needed for the deploy user

The IAM user needs these AWS managed policies:
- `AWSElasticBeanstalkFullAccess`
- `AmazonS3FullAccess` (EB uses S3 to stage the deployment package)

## Workflow File Location

`.github/workflows/deploy-backend.yml`

## Pre-Deployment Note

The Elastic Beanstalk environment (`Starlist-env-1`) is currently **Suspended**. It must be restarted in the EB console before any deployment will succeed. This is a one-time manual action unrelated to the CI/CD setup.

## What Is Not In Scope

- Frontend deployment (Amplify handles this via git push)
- Database migrations (no Flyway in this project)
- Notifications (Slack, email) on deploy success/failure
