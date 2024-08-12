import {injectable} from "inversify";

enum Protocol {
    HTTP = 'http',
    HTTPS = 'https'
}

@injectable()
export abstract class Endpoint {
    abstract protocol: Protocol;
    abstract hostname: string;
    abstract port: number;
    abstract path: string;

    getURL(): string {
        return `${this.protocol}://${this.hostname}:${this.port}/${this.path}`;
    }

    // FIXME : Generate a union type which includes all generated GRPC service clients for this build, and use here.
    serviceClient(serviceClientClass: any) {
        const client = new serviceClientClass(this.getURL()),
            // @ts-ignore
            enableDevTools = window.__GRPCWEB_DEVTOOLS__ || (() => {
            });

        enableDevTools([client]);
        return client;
    }
}