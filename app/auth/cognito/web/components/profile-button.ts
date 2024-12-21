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

import '@spectrum-web-components/action-menu/sp-action-menu.js';
import '@spectrum-web-components/icons-workflow/icons/sp-icon-login.js';
import '@spectrum-web-components/icons-workflow/icons/sp-icon-real-time-customer-profile.js';
import '@spectrum-web-components/button/sp-button.js';
import '@spectrum-web-components/menu/sp-menu.js';
import '@spectrum-web-components/menu/sp-menu-group.js';
import '@spectrum-web-components/menu/sp-menu-item.js';
import '@spectrum-web-components/menu/sp-menu-divider.js';
import '@spectrum-web-components/overlay/sp-overlay.js';

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
                <sp-action-menu
                        label="Account"
                        placement="bottom-end"
                        style="margin-inline-start: auto;"
                        quiet
                >
                    <sp-icon-real-time-customer-profile slot="icon"></sp-icon-real-time-customer-profile>
                    <sp-menu-item>Account Settings</sp-menu-item>
                    <sp-menu-item>My Profile</sp-menu-item>
                    <sp-menu-divider></sp-menu-divider>
                    <sp-menu-item>Share</sp-menu-item>
                    <sp-menu-divider></sp-menu-divider>
                    <sp-menu-item>Help</sp-menu-item>
                    <sp-menu-item>Sign Out</sp-menu-item>
                </sp-action-menu>
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
