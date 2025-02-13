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
2. You can use the SAM CLI to quickly build the project
```bash
$ cd photo-blog-app
$ sam build
```
3. To deploy the application in your AWS account, you can use the SAM CLI's guided deployment process and follow the instructions on the screen
- Parameters include:
  - FrontendDevHost: The hosted frontend
  - FrontendProdHost: The localhost of my frontend
  - PhotoBlogPrimaryBucket: Primary bucket for s3 bucket
  - PhotoBlogBackupBucket: Backup bucket for s3 bucket
  - PrimaryRegion: Primary region name
  - BackupRegion: Backup region name
  - DomainName: Domain name to be used in your primary and backup api gateway (e.g. api.photoblog.com)
  - PrimaryACMCertificate: ACM certificate arn in your primary region
  - BackupACMCertificate: ACM certificate arn in your backup region
  - DynamoDBGlobalTable: DynamoDB global table
```bash
$ sam deploy --guided
```
5. Deploy route 53 failover.
- Use this command to get domain names and its properties.
NB: Get configuration for both primary and back region. Replace <region> with the appropriate region
```
aws apigateway get-domain-names --region <region>
```
- Parameters include:
  - DOMAIN_NAME: Custom domain name used for primary api gateway (e.g. api.photoblog.com)
  - PRIMARY_REGIONAL_DOMAIN_NAME: Regional domain name for primary api gateway (e.g. xxxx.execute-api.<region>.amazonaws.com )
  - PRIMARY_HOSTED_ZONE_ID: Hosted zone ID (e.g. for eu-central-1 use Z1U9ULNL0V5AJ3)
  - BACKUP_REGIONAL_DOMAIN_NAME: Regional domain name for secondary api gateway (e.g. xxxx.execute-api.<region>.amazonaws.com )
  - BACKUP_HOSTED_ZONE_ID: Hosted zone ID (e.g. for eu-west-1 use ZLY8HYME6SFDD)
  - ADMIN_EMAIL: Email to send notification once there is a failover (e.g. name@example.com)
  - HOSTED_ZONE_NAME: The name of the Route 53 hosted zone (must end with a dot e.g. photoblog.com.)
  - REGION: Region to deploy the stack (e.g. eu-central-1)

```bash
aws cloudformation deploy \
--template-file route53-api-failover.yml \
--stack-name "route53-api-failover" \
--capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
--parameter-overrides \
  DomainName=${DOMAIN_NAME} \
  PrimaryRegionalDomainName=${PRIMARY_REGIONAL_DOMAIN_NAME} \
  BackupRegionalDomainName=${BACKUP_REGIONAL_DOMAIN_NAME} \
  AdminEmail=${ADMIN_EMAIL} \
  PrimaryHostedZoneID=${PRIMARY_HOSTED_ZONE_ID} \
  BackupHostedZoneID=${BACKUP_HOSTED_ZONE_ID} \
  HostedZoneName=${HOSTED_ZONE_NAME} \
--region ${REGION}
```
