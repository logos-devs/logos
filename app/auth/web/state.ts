import {CognitoJwtVerifier} from "aws-jwt-verify";
import {User} from "dev/logos/web/module/user";
import {injectable} from "inversify";
import {action, autorun, observable} from 'mobx';


const verifier = CognitoJwtVerifier.create({
    userPoolId: "us-east-2_0tayqImgc",
    tokenUse: "access",
    clientId: "tq7gphv26nsp3m9plqpruco0r",
});

@injectable()
export class CognitoUser extends User {
    @observable
    public isAuthenticated: boolean = localStorage.getItem("logosAccessToken") !== null;

    @observable
    public accessToken: string | null = localStorage.getItem("logosAccessToken");
    @observable
    public decodedAccessToken: any | null;

    @observable
    public idToken: string | null = localStorage.getItem("logosIdToken");
    @observable
    public decodedIdToken: any | null = this.idToken;

    @observable
    public refreshToken: string | null = localStorage.getItem("logosRefreshToken");
    @observable
    public decodedRefreshToken: any | null = this.refreshToken;

    constructor() {
        super();
        verifier.hydrate().then(
            () => {
                this.updateDecodedTokens()
                autorun(() => this.updateDecodedTokens());
            });
    }

    @action
    updateDecodedTokens() {
        if (this.accessToken) {
            verifier.verify(this.accessToken).then((token) => {
                this.decodedAccessToken = token;
                localStorage.setItem("logosAccessToken", this.accessToken);
            }).catch((reason) => this.clearAllTokens());
        } else {
            this.decodedAccessToken = null;
        }

        if (this.idToken) {
            localStorage.setItem("logosIdToken", this.idToken);
        } else {
            this.decodedIdToken = null;
        }

        if (this.refreshToken) {
            localStorage.setItem("logosRefreshToken", this.refreshToken);
        } else {
            this.decodedRefreshToken = null;
        }
    }

    @action
    checkTokenExpiry() {
        if (this.decodedAccessToken) {
            const expirationTime = this.decodedAccessToken.exp * 1000; // Convert to milliseconds
            const timeLeft = expirationTime - Date.now();
            if (timeLeft < 60000) { // Less than 1 minute
                this.clearAllTokens();
            }
        }
    }

    @action
    clearAllTokens() {
        this.accessToken = null;
        this.idToken = null;
        this.refreshToken = null;
        this.isAuthenticated = false;
        localStorage.removeItem("logosAccessToken");
        localStorage.removeItem("logosIdToken");
        localStorage.removeItem("logosRefreshToken");
    }
}
