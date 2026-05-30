#!/bin/bash

echo "=== 1. Elastic Beanstalk Environments ==="
aws elasticbeanstalk describe-environments --query "Environments[*].{Name:EnvironmentName,Status:Status,Health:Health}" --output table

echo "=== 2. RDS Databases (Including Stopped) ==="
aws rds describe-db-instances --query "DBInstances[*].{DBInstanceIdentifier:DBInstanceIdentifier,Engine:Engine,Status:DBInstanceStatus}" --output table

echo "=== 3. AWS Amplify Apps ==="
aws amplify list-apps --query "apps[*].{Name:name,Id:appId,DefaultDomain:defaultDomain}" --output table

echo "=== 4. S3 Buckets ==="
aws s3api list-buckets --query "Buckets[*].Name" --output table

echo "=== 5. Cognito User Pools ==="
aws idp list-user-pools --max-results 10 --query "UserPools[*].{Name:Name,Id:Id}" --output table