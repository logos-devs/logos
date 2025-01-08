import {css, html, LitElement, CSSResult, TemplateResult} from "lit";
import {customElement, state} from "lit/decorators.js";

import {
    GetSignInUrlRequest,
    GetSignInUrlResponse,
    GetCurrentUserRequest,
    GetCurrentUserResponse,
    ProcessAuthCodeRequest,
    ProcessAuthCodeResponse
} from "../../proto/cognito_pb";
import {CognitoServicePromiseClient} from "../../proto/cognito_grpc_web_pb";
import {lazyInject} from '@logos/web/module/app-module';
import {when} from "lit/directives/when.js";

import '@spectrum-web-components/icons-workflow/icons/sp-icon-login.js';
import '@spectrum-web-components/icons-workflow/icons/sp-icon-real-time-customer-profile.js';
import '@spectrum-web-components/button/sp-button.js';

import '../module/auth-module';


@customElement("profile-button")
export class ProfileButton extends LitElement {
    @lazyInject(CognitoServicePromiseClient)
    private cognitoServiceClient: CognitoServicePromiseClient;

    @state()
    private isAuthenticated: boolean = false;

    @state()
    private displayName: string = null;

    @state()
    private signInUrl: string = null;

    // language=CSS
    static styles: CSSResult = css`
    `;

    connectedCallback() {
        super.connectedCallback();
        if (window.location.pathname == "/login/complete") {
            const authCode = new URLSearchParams(window.location.search).get("code");

            this.cognitoServiceClient.processAuthCode(new ProcessAuthCodeRequest().setAuthCode(authCode)).then(
                (response: ProcessAuthCodeResponse) => {
                    window.location.href = "/";
                }
            )
        } else {
            this.cognitoServiceClient.getCurrentUser(new GetCurrentUserRequest()).then((response: GetCurrentUserResponse) => {
                this.isAuthenticated = response.getIsAuthenticated();
                this.displayName = response.getDisplayName();

                if (!this.isAuthenticated) {
                    this.cognitoServiceClient.getSignInUrl(new GetSignInUrlRequest()).then((response: GetSignInUrlResponse) => {
                        this.signInUrl = response.getSignInUrl();
                    });
                }
            });

        }
    }

    render(): TemplateResult {
        return html`
            ${when(this.isAuthenticated, () => html`
                <slot></slot>
            `, () => html`
                <sp-top-nav-item @click=${(ev) => this.signInUrl || ev.preventDefault()}
                                 href=${this.signInUrl}
                                 title=${this.displayName}>
                    <sp-icon-login></sp-icon-login>
                </sp-top-nav-item>
            `)}
        `;
    }
}
