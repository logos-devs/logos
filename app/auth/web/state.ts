import {autorun, observable} from 'mobx';

export class User {
    @observable
    public isAuthenticated = localStorage.getItem("logosAccessToken") !== null;

    @observable
    public accessToken: string | null = localStorage.getItem("logosAccessToken");

    @observable
    public idToken: string | null = localStorage.getItem("logosIdToken");

    @observable
    public refreshToken: string | null = localStorage.getItem("logosRefreshToken");
}

export const user = new User();

autorun(() => {
    if (user.accessToken) {
        localStorage.setItem("logosAccessToken", user.accessToken);
    } else {
        localStorage.removeItem("logosAccessToken");
    }

    if (user.idToken) {
        localStorage.setItem("logosIdToken", user.idToken);
    } else {
        localStorage.removeItem("logosIdToken");
    }

    if (user.refreshToken) {
        localStorage.setItem("logosRefreshToken", user.refreshToken);
    } else {
        localStorage.removeItem("logosRefreshToken");
    }
});