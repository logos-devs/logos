import {lazyInject} from "dev/logos/service/client/web/module/app-module";
import {User} from "dev/logos/service/client/web/module/user";
import cognitoPublicHostMap from "dev/logos/stack/aws/cognito_public_host_map.json";
import {css, html, LitElement} from "lit";
import {customElement} from "lit/decorators.js";
import {when} from "lit/directives/when.js";

import "@material/web/button/filled-button";
import "@material/web/iconbutton/icon-button";
import "@material/web/icon/icon";
import "@material/web/labs/card/filled-card";
import "@material/web/textfield/filled-text-field";

import "./entity/source-imap-list";
import "./entity/source-rss-list";


@customElement('view-profile')
export class ViewProfile extends LitElement {
    @lazyInject(User) user: User;

    static styles = css`
        h2 {
            text-align: center;
        }
    `;

    override render() {
        return html`
            ${when(this.user.isAuthenticated, () => html`
                <h2>Email</h2>
                <source-imap-list></source-imap-list>

                <h2>RSS</h2>
                <source-rss-list></source-rss-list>
            `, () => html`
                <md-icon-button id="login-button"
                                @click=${() => window.location.assign(cognitoPublicHostMap[location.host].loginUrl)}>
                    <md-icon>login</md-icon>
                </md-icon-button>
            `)}
        `;
    }
}