import {CognitoUser} from "app/auth/web/state";
import {ClientUnaryInterceptor, lazyInject} from "dev/logos/web/module/app-module";
import {User} from "dev/logos/web/module/user";


export class CognitoAuthInterceptor extends ClientUnaryInterceptor {
    @lazyInject(User) user: CognitoUser;

    override intercept(request: any, invoker: any) {
        if (this.user.isAuthenticated) {
            const metadata = request.getMetadata();
            // TODO : throw an error if somebody else is trying to set the Authorization header
            metadata.Authorization = 'Bearer ' + this.user.accessToken;
        }
        return invoker(request);
    }
}
