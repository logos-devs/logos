import {App, Environment, Stack, StackProps} from "aws-cdk-lib";
import {Bucket} from "aws-cdk-lib/aws-s3";

export class S3Stack extends Stack {
    constructor(scope: App, id: string, props?: StackProps) {
        super(scope, id, props);

        new Bucket(this, `${id}-homes`, {
            bucketName: "logos-homes",
        });
    }
}


export function makeS3Stack(app: App, id: string, env: Environment): S3Stack {
    return new S3Stack(app, id, { env })
}
