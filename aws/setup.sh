#!/usr/bin/env bash
# StarList AWS Setup Script
#
# All resources already exist in us-east-1. This script:
#   - Checks the live state of each resource
#   - Skips creation if it already exists
#   - Re-applies environment variables to Beanstalk if needed
#   - Prints a full status summary at the end
#
# Prerequisites:
#   - AWS CLI v2 installed and configured (aws configure)
#   - eb CLI installed  (pip install awsebcli)
#   - A built JAR at StarList-Backend/app/target/app-*.jar  (for re-deploys)
#   - jq installed (brew install jq / apt install jq)
#
# Usage:
#   chmod +x aws/setup.sh
#   ./aws/setup.sh

set -euo pipefail

# ── Known resource IDs (already live) ────────────────────────────────────────
REGION="us-east-1"

COGNITO_POOL_ID="us-east-1_sCvhvZCBu"
COGNITO_CLIENT_ID="69ofurn20p2ipes5fm9l3ct8k0"

EB_APP_NAME="StarList-App"
EB_ENV_NAME="StarList-App-env-1"
EB_URL="StarList-App-env-1.eba-zdkhtwvm.us-east-1.elasticbeanstalk.com"

RDS_INSTANCE="starlistdb"
RDS_HOST="starlistdb.cxs0b4lgjlcd.us-east-1.rds.amazonaws.com"
RDS_PORT="5432"
RDS_DB="starlist"
RDS_USER="postgres"

S3_BUCKET="starlist-media-bucket-summer"

OUTPUT_FILE="$(dirname "$0")/created-resources.env"

# ── Helpers ───────────────────────────────────────────────────────────────────
log()  { echo "[✓] $*"; }
warn() { echo "[!] $*"; }
skip() { echo "[SKIP] $*"; }
fail() { echo "[✗] $*"; }

check_cmd() { command -v "$1" &>/dev/null; }

COMPLETED=()
SKIPPED=()

mark_done() { COMPLETED+=("$1"); }
mark_skip() { SKIPPED+=("$1"); }

# ── 0. Preflight ──────────────────────────────────────────────────────────────
echo ""
echo "=== StarList AWS Status Check & Setup ==="
echo "Region: $REGION"
echo ""

if ! check_cmd aws; then
    echo "ERROR: AWS CLI not found. Install from https://aws.amazon.com/cli/"
    exit 1
fi

if ! aws sts get-caller-identity &>/dev/null; then
    echo "ERROR: AWS CLI not authenticated. Run: aws configure"
    exit 1
fi

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
log "Authenticated — account: $ACCOUNT_ID"

# ── 1. Cognito ────────────────────────────────────────────────────────────────
echo ""
echo "--- [1/4] Cognito ---"

POOL_STATUS=$(aws cognito-idp describe-user-pool \
    --user-pool-id "$COGNITO_POOL_ID" --region "$REGION" \
    --query "UserPool.Status" --output text 2>/dev/null || echo "NOT_FOUND")

if [[ "$POOL_STATUS" == "NOT_FOUND" ]]; then
    fail "Cognito pool $COGNITO_POOL_ID not found in $REGION"
    mark_skip "Cognito (pool not found — check region)"
else
    log "Cognito pool $COGNITO_POOL_ID is $POOL_STATUS"

    CLIENT_STATUS=$(aws cognito-idp describe-user-pool-client \
        --user-pool-id "$COGNITO_POOL_ID" \
        --client-id "$COGNITO_CLIENT_ID" --region "$REGION" \
        --query "UserPoolClient.ClientId" --output text 2>/dev/null || echo "NOT_FOUND")

    if [[ "$CLIENT_STATUS" == "NOT_FOUND" ]]; then
        warn "App client $COGNITO_CLIENT_ID not found — creating..."
        NEW_CLIENT=$(aws cognito-idp create-user-pool-client \
            --user-pool-id "$COGNITO_POOL_ID" \
            --client-name "starlist-web-client" \
            --region "$REGION" \
            --no-generate-secret \
            --explicit-auth-flows ALLOW_USER_PASSWORD_AUTH ALLOW_REFRESH_TOKEN_AUTH ALLOW_USER_SRP_AUTH \
            --query "UserPoolClient.ClientId" --output text)
        log "Created app client: $NEW_CLIENT"
        warn "Update aws-config.ts with new clientId: $NEW_CLIENT"
        mark_done "Cognito app client (re-created: $NEW_CLIENT)"
    else
        log "App client $COGNITO_CLIENT_ID exists"
        mark_done "Cognito (pool + client verified)"
    fi
fi

JWT_ISSUER_URI="https://cognito-idp.${REGION}.amazonaws.com/${COGNITO_POOL_ID}"

# ── 2. RDS ────────────────────────────────────────────────────────────────────
echo ""
echo "--- [2/4] RDS ---"

RDS_STATUS=$(aws rds describe-db-instances \
    --db-instance-identifier "$RDS_INSTANCE" --region "$REGION" \
    --query "DBInstances[0].DBInstanceStatus" --output text 2>/dev/null || echo "NOT_FOUND")

if [[ "$RDS_STATUS" == "NOT_FOUND" ]]; then
    fail "RDS instance '$RDS_INSTANCE' not found."
    echo ""
    echo "  To create it, run:"
    echo "    aws rds create-db-instance \\"
    echo "      --db-instance-identifier $RDS_INSTANCE \\"
    echo "      --db-instance-class db.t3.micro \\"
    echo "      --engine postgres --engine-version 16.3 \\"
    echo "      --master-username $RDS_USER \\"
    echo "      --master-user-password '<password>' \\"
    echo "      --db-name $RDS_DB \\"
    echo "      --allocated-storage 20 --storage-type gp2 \\"
    echo "      --no-publicly-accessible --region $REGION"
    mark_skip "RDS (not found)"
elif [[ "$RDS_STATUS" == "stopped" ]]; then
    warn "RDS '$RDS_INSTANCE' is STOPPED — starting it..."
    aws rds start-db-instance --db-instance-identifier "$RDS_INSTANCE" --region "$REGION" > /dev/null
    log "Start command sent. Waiting for availability..."
    aws rds wait db-instance-available --db-instance-identifier "$RDS_INSTANCE" --region "$REGION"
    log "RDS '$RDS_INSTANCE' is now available"
    mark_done "RDS (was stopped — started)"
else
    log "RDS '$RDS_INSTANCE' is $RDS_STATUS at $RDS_HOST:$RDS_PORT"
    mark_done "RDS (verified: $RDS_STATUS)"
fi

DB_URL="jdbc:postgresql://${RDS_HOST}:${RDS_PORT}/${RDS_DB}"

# ── 3. S3 Bucket ──────────────────────────────────────────────────────────────
echo ""
echo "--- [3/4] S3 ---"

if [[ "$S3_BUCKET" == "<YOUR-S3-BUCKET-NAME>" ]]; then
    skip "S3 bucket name not set in this script — edit S3_BUCKET variable at the top"
    mark_skip "S3 (bucket name not configured in script)"
else
    S3_EXISTS=$(aws s3api head-bucket --bucket "$S3_BUCKET" --region "$REGION" 2>&1 || echo "NOT_FOUND")
    if echo "$S3_EXISTS" | grep -q "NOT_FOUND\|NoSuchBucket\|404"; then
        warn "Bucket '$S3_BUCKET' not found — creating..."
        aws s3api create-bucket \
            --bucket "$S3_BUCKET" \
            --region "$REGION" \
            --create-bucket-configuration LocationConstraint="$REGION" > /dev/null
        # Block all public access (presigned URLs don't need public access)
        aws s3api put-public-access-block \
            --bucket "$S3_BUCKET" \
            --public-access-block-configuration \
              "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"
        log "Created private S3 bucket: $S3_BUCKET"
        mark_done "S3 (created: $S3_BUCKET)"
    else
        log "S3 bucket '$S3_BUCKET' exists"
        mark_done "S3 (verified: $S3_BUCKET)"
    fi
fi

# ── 4. Elastic Beanstalk ──────────────────────────────────────────────────────
echo ""
echo "--- [4/4] Elastic Beanstalk ---"

if ! check_cmd eb; then
    skip "eb CLI not installed — skipping Beanstalk env-var sync. Install: pip install awsebcli"
    mark_skip "Beanstalk env vars (eb CLI missing)"
else
    EB_STATUS=$(aws elasticbeanstalk describe-environments \
        --application-name "$EB_APP_NAME" \
        --environment-names "$EB_ENV_NAME" \
        --region "$REGION" \
        --query "Environments[0].Status" --output text 2>/dev/null || echo "NOT_FOUND")

    if [[ "$EB_STATUS" == "NOT_FOUND" || "$EB_STATUS" == "None" ]]; then
        fail "Beanstalk environment '$EB_ENV_NAME' not found under app '$EB_APP_NAME'."
        warn "If the app name is different, update EB_APP_NAME in this script."
        mark_skip "Beanstalk (env not found)"
    else
        log "Beanstalk env '$EB_ENV_NAME' is $EB_STATUS"

        # Sync environment variables
        read -rsp "Enter RDS postgres password (to sync Beanstalk env vars): " DB_PASSWORD; echo ""
        OPENAI_KEY="${OPENAI_API_KEY:-}"
        if [[ -z "$OPENAI_KEY" ]]; then
            read -rsp "Enter OpenAI API key: " OPENAI_KEY; echo ""
        fi

        aws elasticbeanstalk update-environment \
            --application-name "$EB_APP_NAME" \
            --environment-name "$EB_ENV_NAME" \
            --region "$REGION" \
            --option-settings \
                "Namespace=aws:elasticbeanstalk:application:environment,OptionName=SPRING_PROFILES_ACTIVE,Value=prod" \
                "Namespace=aws:elasticbeanstalk:application:environment,OptionName=AWS_DB_URL,Value=${DB_URL}" \
                "Namespace=aws:elasticbeanstalk:application:environment,OptionName=AWS_DB_USERNAME,Value=${RDS_USER}" \
                "Namespace=aws:elasticbeanstalk:application:environment,OptionName=AWS_DB_PASSWORD,Value=${DB_PASSWORD}" \
                "Namespace=aws:elasticbeanstalk:application:environment,OptionName=JWT_ISSUER_URI,Value=${JWT_ISSUER_URI}" \
                "Namespace=aws:elasticbeanstalk:application:environment,OptionName=OPENAI_API_KEY,Value=${OPENAI_KEY}" \
            > /dev/null
        log "Beanstalk environment variables synced"
        mark_done "Beanstalk (env vars synced to $EB_ENV_NAME)"

        # Optional: deploy a new JAR if one is present
        JAR_PATH=$(ls StarList-Backend/app/target/app-*.jar 2>/dev/null | head -1 || true)
        if [[ -n "$JAR_PATH" ]]; then
            read -rp "Found $JAR_PATH — deploy it now? [y/N]: " DEPLOY_NOW
            if [[ "$DEPLOY_NOW" =~ ^[Yy]$ ]]; then
                S3_STAGING=$(aws elasticbeanstalk create-storage-location \
                    --query S3Bucket --output text --region "$REGION")
                VERSION_LABEL="v-$(date +%Y%m%d%H%M%S)"
                aws s3 cp "$JAR_PATH" "s3://${S3_STAGING}/${EB_APP_NAME}/${VERSION_LABEL}.jar"
                aws elasticbeanstalk create-application-version \
                    --application-name "$EB_APP_NAME" \
                    --version-label "$VERSION_LABEL" \
                    --source-bundle "S3Bucket=${S3_STAGING},S3Key=${EB_APP_NAME}/${VERSION_LABEL}.jar" \
                    --region "$REGION" > /dev/null
                aws elasticbeanstalk update-environment \
                    --application-name "$EB_APP_NAME" \
                    --environment-name "$EB_ENV_NAME" \
                    --version-label "$VERSION_LABEL" \
                    --region "$REGION" > /dev/null
                log "Deployed $JAR_PATH as version $VERSION_LABEL"
                mark_done "Beanstalk JAR deploy ($VERSION_LABEL)"
            else
                skip "JAR deploy skipped by user"
                mark_skip "Beanstalk JAR deploy"
            fi
        fi
    fi
fi

# ── Save resources ────────────────────────────────────────────────────────────
cat > "$OUTPUT_FILE" <<EOF
# Generated by setup.sh on $(date)
REGION=$REGION
COGNITO_POOL_ID=$COGNITO_POOL_ID
COGNITO_CLIENT_ID=$COGNITO_CLIENT_ID
JWT_ISSUER_URI=$JWT_ISSUER_URI
RDS_HOST=$RDS_HOST
DB_URL=$DB_URL
EB_APP_NAME=$EB_APP_NAME
EB_ENV_NAME=$EB_ENV_NAME
EB_URL=$EB_URL
S3_BUCKET=$S3_BUCKET
EOF
log "Resource snapshot saved to $OUTPUT_FILE"

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════"
echo " COMPLETED (${#COMPLETED[@]})"
echo "════════════════════════════════════════"
for item in "${COMPLETED[@]}"; do echo " ✓ $item"; done

echo ""
echo "════════════════════════════════════════"
echo " SKIPPED (${#SKIPPED[@]})"
echo "════════════════════════════════════════"
for item in "${SKIPPED[@]}"; do echo " ✗ $item"; done
echo ""
