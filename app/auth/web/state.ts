import {observable} from 'mobx';

export class User {
    @observable
    public isAuthenticated = false;

    @observable
    public accessToken: string | null = null;

    @observable
    public idToken: string | null = null;

    @observable
    public refreshToken: string | null = null;
}

export const user = new User();