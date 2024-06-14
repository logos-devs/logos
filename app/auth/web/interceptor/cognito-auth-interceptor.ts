import {CognitoUser} from "app/auth/web/state";
import {ClientUnaryInterceptor} from "dev/logos/service/client/web/module/app-module";
import {User} from "dev/logos/service/client/web/module/user";
import {inject} from "inversify";


// TODO create base ClientInterceptor if it doesn't already exist in grpc-web, but it should. in either case, extend here.
export class CognitoAuthInterceptor extends ClientUnaryInterceptor {
    @inject(User) user: CognitoUser;

    override intercept(request: any, invoker: any) {
        if (this.user.isAuthenticated) {
            const metadata = request.getMetadata();
            // throw an error if somebody else is trying to set the Authorization header
            metadata.Authorization = 'Bearer ' + this.user.accessToken;
        }
        return invoker(request);
    }
}
