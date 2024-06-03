import {user} from "app/auth/web/state";
import {rootContainer} from "dev/logos/service/client/web/bind";
import {DevEndpoint} from "dev/logos/service/client/web/endpoint";
import {ContainerModule, interfaces} from "inversify";
import getDecorators from "inversify-inject-decorators";

const enableDevTools = window.__GRPCWEB_DEVTOOLS__ || ((_: any) => {
});

type ClientConstructor = new (hostname: string, credentials: any, options: any) => any;

const endpoint = new DevEndpoint();
const endpointUrl = endpoint.getURL();


class AuthInterceptor {
    intercept(request: any, invoker: any) {
        if (user.isAuthenticated) {
            const metadata = request.getMetadata();
            metadata.Authorization = 'Bearer ' + user.accessToken;
        }
        return invoker(request);
    }
}

export abstract class AppModule extends ContainerModule {
    abstract configure(): void;

    protected clients: ClientConstructor[] = [];

    static {
        console.debug(this);
    }

    constructor() {
        super((bind: interfaces.Bind) => {
            this.configure();
            this.clients.forEach(clientClass => {
                const
                    interceptors = [new AuthInterceptor()],
                    client = new clientClass(endpointUrl, null, {
                        unaryInterceptors: interceptors,
                        streamInterceptors: interceptors,
                    });

                enableDevTools([client]);
                bind(clientClass).toDynamicValue(() => client);
            });
        });
    }

    protected addClient(clientClass: ClientConstructor): void {
        this.clients.push(clientClass);
    }
}

export function registerModule(target: new () => AppModule) {
    rootContainer.load(new target())
}

export const {lazyInject} = getDecorators(rootContainer);
