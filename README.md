# Photo Blog App Serverless API
The Photo Blog App is a web application that allows users to create an account, log in, and manage their photos. Users can upload, view, delete, and share their images with others. The app ensures that all uploaded images are processed to include a watermark with the user's full name and the date of upload. The processed images are stored securely, and users can generate time-bound shareable links for non-account holders. The app also includes features like a recycling bin for deleted images and a disaster recovery mechanism to ensure high availability and data integrity.
[Frontend Application](https://github.com/Kjeff24/photo-blog-app-frontend.git)

## Technical Requirements
### Core Features
- User Authentication:
  - User sign-up and sign-in using Amazon Cognito.
  - Users are alerted via email immediately after logging in.
    Image Upload and Processing:
- Images are first staged in an S3 staging bucket.
  - Images are processed to include a watermark (user's full name and upload date) and stored in a primary S3 bucket.
  - The URL of the processed image is stored in a DynamoDB table with user-identifiable attributes.
  - Limit the size of images uploaded by users to below API Gateway limits.
  - Original unprocessed images are deleted from the staging bucket after successful processing.
- Image Processing Retry Mechanism:
  - If image processing fails, retry after 5 minutes.
  - Notify the user via email if processing fails.
  - Allow up to 2 additional retries in case of failure.
- Image Access Control:
  - Processed images are only accessible to authenticated users unless a user generates a time-bound shareable link.
  - Shareable links expire after 3 hours.
- Recycling Bin:  
  - Deleted images are moved to a recycling bin and can be restored or permanently deleted.
  - Images in the recycling bin are viewable but not shareable.
  - If an image is deleted after being shared, it becomes inaccessible via the shared link.
- Decoupling with Message Queuing:
  - Use Amazon SQS to decouple processes and prevent tight coupling.

## Functional Requirements
- User Account Management:
  - Users can sign up and create their own blog space.
  - Users can log in to upload, modify, view, or delete images.
- Image Management:
  - Only watermarked images are displayed to users.
  - Users can generate time-bound shareable links for non-account holders.
- Recycling Bin:
  - Deleted images are moved to a recycling bin and can be restored or permanently deleted.
  - Images in the recycling bin are viewable but not shareable.
- Notifications:
  - Users are notified via email immediately after logging in.
  - Users are notified if image processing fails.

## Disaster Recovery Requirements
- RPO/RTO of 10 Minutes:
  - Implement a warm standby disaster recovery solution.
- Automated Deployment:
 - Use AWS SAM to deploy all backend resources (API Gateway, Lambda, Queues, DynamoDB, etc.) in both primary and secondary (disaster recovery) regions. 
 - Ensure all resources in the disaster recovery region are idle but ready for failover.
- Data Replication:
  - Continuously back up processed images from the primary S3 bucket to a secondary bucket in the disaster recovery region.
  - Replicate DynamoDB tables in the disaster recovery region using native DynamoDB features.
- API Failover Mechanism:
  - Use AWS Route 53, CloudWatch Alarms, and Lambda to switch incoming traffic from the primary API Gateway to a secondary API Gateway in case of disaster.
  - Notify the system administrator when a failover occurs.
  - Ensure the frontend does not lose contact with the backend API Gateway for more than 10 minutes.

## Services Used
- Amazon Cognito: User authentication and management.
- Amazon S3: Staging and storage of images.
- Amazon DynamoDB: Storing metadata of processed images.
- API Gateway: Handling API requests.
- Amazon SQS: Decoupling processes.
- Amazon SNS: Sending notifications (e.g., email alerts).
- AWS Lambda: Serverless functions for image processing, failover, and other tasks.
- AWS Route 53: DNS and traffic routing for failover.
- AWS CloudWatch: Monitoring and alarms.
- AWS SAM: Automated deployment of resources.

## Pre-requisites
* [AWS CLI](https://aws.amazon.com/cli/)
* [SAM CLI](https://github.com/awslabs/aws-sam-cli)
* [Gradle](https://gradle.org/) or [Maven](https://maven.apache.org/)

## Project Workflow
1. Register a domain on AWS Route 53 (e.g. photoblog.com)
2. Create AWS ACM certificate for primary and backup region.
- Parameters include:
  - DOMAIN_NAME: The domain name for the ACM certificate (e.g., *.photoblog.com)
  - HOSTED_ZONE_ID: The Route 53 Hosted Zone (e.g. Z03224)
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
  - BACKUP_REGION_NOTIFICATION_TOPIC_ARN: Backup region notification topic arn (Subscription email are sent to users when they are created)
  - DYNAMODB_TABLE: Dynamodb global table name
  - AMPLIFY_APP_ID: Amplify app ID for hosted frontend
  - AMPLIFY_BRANCH_NAME: Branch name for hosted frontend
- NB:
  - Metrics generated by Route 53 health checks are stored in Amazon CloudWatch in us-east-1 by default.
  - This is a design constraint of AWS.
  - CloudWatch alarms that monitor Route 53 health checks must be created in us-east-1 because the underlying health check metrics are only available in that region.
```bash
aws cloudformation deploy \
--template-file route-53-record-primary.yml \
--stack-name "route-53-record-primary" \
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
  BackupRegionNotificationTopicArn=${BACKUP_REGION_NOTIFICATION_TOPIC_ARN} \
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
--template-file route-53-record-backup.yml \
--stack-name "route-53-record-backup" \
--capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
--parameter-overrides \
  DomainName=${DOMAIN_NAME} \
  HostedZoneName=${HOSTED_ZONE_NAME} \
  BackupRegionUserPoolId=${BACKUP_REGION_USER_POOL_ID} \
  BackupRegionalDomainName=${BACKUP_REGIONAL_DOMAIN_NAME} \
--region us-east-1
```
