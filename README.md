# photo-blog-app serverless API
The photo-blog-app project, created with [`aws-serverless-java-container`](https://github.com/aws/serverless-java-container).

The starter project defines a simple `/ping` resource that can accept `GET` requests with its tests.

The project folder also includes a `template.yml` file. You can use this [SAM](https://github.com/awslabs/serverless-application-model) file to deploy the project to AWS Lambda and Amazon API Gateway or test in local with the [SAM CLI](https://github.com/awslabs/aws-sam-cli). 

## Pre-requisites
* [AWS CLI](https://aws.amazon.com/cli/)
* [SAM CLI](https://github.com/awslabs/aws-sam-cli)
* [Gradle](https://gradle.org/) or [Maven](https://maven.apache.org/)

## Project Workflow
1. Register a domain on AWS Route 53 (e.g. photoblog.com)
2. Create AWS ACM certificate for primary and backup region.
- Parameters include:
  - DOMAIN_NAME: The domain name for the ACM certificate (e.g., *.photoblog.com)
  - HOSTED_ZONE_ID: The Route 53 Hosted Zone (e.g. photoblog.com)
  - REGION: Deploy to both primary and backup region (e.g. primary region: eu-central-1, backup region: eu-west-1)
NB: The template also creates a route 53 record
```
aws cloudformation deploy \
--template-file acm-certificate.yml \
--stack-name "acm-certificate" \
--capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
--parameter-overrides \
  DomainName=${DOMAIN_NAME} \
  HostedZoneId=${HOSTED_ZONE_ID} \
--region ${REGION}
```
- To get the list of certificates (Replace <region> with the appropriate region):
```
aws acm list-certificates --region <region>
```
3. Create s3 bucket and replication
- Parameters include:
  - PrimaryBucket: Primary s3 bucket
  - BackupBucket: Backup s3 bucket
  - PrimaryStagingBucket: Primary staging s3 bucket
  - BackupStagingBucket: Backup staging s3 bucket
  - PrimaryRegion: Primary region name
  - BackupRegion: Backup region name
- NB:
  - Deploy to back-up region before the primary region, this is because back-up bucket must exist before replication of primary bucket. 
  - If any error such as "A conflicting conditional operation is currently in progress against this resource." ensure your bucket names are unique
```bash
aws cloudformation deploy \
--template-file s3-bucket.yml \
--stack-name "s3-buket" \
--capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
--parameter-overrides \
  PrimaryBucket=${PRIMARY_BUCKET} \
  BackupBucket=${BACKUP_BUCKET} \
  PrimaryStagingBucket=${PRIMARY_STAGING_BUCKET} \
  BackupStagingBucket=${PRIMARY_BACKUP_BUCKET} \
  PrimaryRegion=${PRIMARY_REGION} \
  BackupRegion=${BACKUP_REGION} \
--region us-east-1
```
4. Deploy dynamodb global table
- Parameters include:
  - BACKUP_REGION: Backup region for dynamodb (e.g. eu-west-1)
  - DYNAMODB_GLOBAL_TABLE: Global dynamodb table name (e.g. dynamodb-global-table)
```bash
aws cloudformation deploy \
--template-file global-dynamodb-table.yml \
--stack-name "global-dynamodb-table" \
--capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
--parameter-overrides \
  BackupRegion=${BACKUP_REGION} \
  DynamoDBGlobalTable=${DYNAMODB_GLOBAL_TABLE} \
--region eu-central-1
```
5. You can use the SAM CLI to quickly build the project
```bash
$ cd photo-blog-app
$ sam build
```
6. To deploy the application in your AWS account, you can use the SAM CLI's guided deployment process and follow the instructions on the screen
- Parameters include:
  - FrontendDevHost: The hosted frontend
  - FrontendProdHost: The localhost of my frontend
  - PrimaryBucket: Primary s3 bucket
  - BackupBucket: Backup s3 bucket
  - PrimaryStagingBucket: Primary staging s3 bucket
  - BackupStagingBucket: Backup staging s3 bucket
  - PrimaryRegion: Primary region name
  - BackupRegion: Backup region name
  - DomainName: Domain name to be used in your primary and backup api gateway (e.g. api.photoblog.com)
  - PrimaryACMCertificate: ACM certificate arn in your primary region
  - BackupACMCertificate: ACM certificate arn in your backup region
  - DynamoDBGlobalTable: DynamoDB global table
```bash
$ sam deploy --guided
```
7. Deploy primary record for route 53 failover.
- Use this command to get domain names and its properties.
NB: Get configuration for both primary and back region. Replace <region> with the appropriate region
```
aws apigateway get-domain-names --region <region>
```
- Parameters include:
  - DOMAIN_NAME: Custom domain name used for primary api gateway (e.g. api.photoblog.com)
  - PRIMARY_REGIONAL_DOMAIN_NAME: Regional domain name for primary api gateway (e.g. xxxx.execute-api.<region>.amazonaws.com )
  - ADMIN_EMAIL: Email to send notification once there is a failover (e.g. name@example.com)
  - PRIMARY_HOSTED_ZONE_ID: Hosted zone ID (e.g. for eu-central-1 use Z1U9ULNL0V5AJ3)
  - PRIMARY_GATEWAY_INVOKE_URL: Invoke url of your primary api gateway (e.g. <serverless-api>.execute-api.eu-central-1.amazonaws.com)
  - HOSTED_ZONE_NAME: The name of the Route 53 hosted zone (must end with a dot e.g. photoblog.com.)
  - BACKUP_REGION: Backup region (eg. eu-west-1)
  - BACKUP_REGION_USER_POOL_ID: Backup region user pool id
  - DYNAMODB_TABLE: Dynamodb global table name
  - AMPLIFY_APP_ID: Amplify app ID for hosted frontend
  - AMPLIFY_BRANCH_NAME: Branch name for hosted frontend
- NB:
  - Metrics generated by Route 53 health checks are stored in Amazon CloudWatch in us-east-1 by default.
  - This is a design constraint of AWS.  
  - CloudWatch alarms that monitor Route 53 health checks must be created in us-east-1 because the underlying health check metrics are only available in that region.
```bash
aws cloudformation deploy \
--template-file record-primary.yml \
--stack-name "record-primary" \
--capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
--parameter-overrides \
  DomainName=${DOMAIN_NAME} \
  PrimaryRegionalDomainName=${PRIMARY_REGIONAL_DOMAIN_NAME} \
  AdminEmail=${ADMIN_EMAIL} \
  PrimaryHostedZoneID=${PRIMARY_HOSTED_ZONE_ID} \
  PrimaryApiGatewayInvokeUrl=${PRIMARY_GATEWAY_INVOKE_URL]} \
  HostedZoneName=${HOSTED_ZONE_NAME} \
  BackupRegion=${BACKUP_REGION} \
  BackupRegionUserPoolId=${BACKUP_REGION_USER_POOL_ID} \
  GlobalDynamodbTable=${DYNAMODB_TABLE} \
  AmplifyAppId=${AMPLIFY_APP_ID} \
  AmplifyBranchName=${AMPLIFY_BRANCH_NAME} \
--region us-east-1
```
- Send a GET request to the health endpoint (e.g. health endpoint https://api.photoblog.com/health ) to get a response
```json
{
  "status":"UP",
  "region":"eu-central-1"
}
```
- After getting a status "UP" create a secondary record.
8. Deploy secondary record for route 53 failover.
- Parameters include:
  - DOMAIN_NAME: Custom domain name used for primary api gateway (e.g. api.photoblog.com)
  - BACKUP_REGIONAL_DOMAIN_NAME: Regional domain name for secondary api gateway (e.g. xxxx.execute-api.<region>.amazonaws.com )
  - BACKUP_HOSTED_ZONE_ID: Hosted zone ID (e.g. for eu-west-1 use ZLY8HYME6SFDD)
  - HOSTED_ZONE_NAME: The name of the Route 53 hosted zone (must end with a dot e.g. photoblog.com.)
  - DOMAIN_NAME: Custom domain name used for primary api gateway (e.g. api.photoblog.com)
```bash
aws cloudformation deploy \
--template-file record-backup.yml \
--stack-name "record-backup" \
--capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
--parameter-overrides \
  DomainName=${DOMAIN_NAME} \
  HostedZoneName=${HOSTED_ZONE_NAME} \
  BackupRegionUserPoolId=${BACKUP_REGION_USER_POOL_ID} \
  BackupRegionalDomainName=${BACKUP_REGIONAL_DOMAIN_NAME} \
--region us-east-1
```
