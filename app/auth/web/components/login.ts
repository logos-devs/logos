import {RootState, store} from "@logos/store/store";
import {startAssertion} from "app/auth/web/store";
import {css, html, LitElement} from 'lit';
import {customElement, query, state} from "lit/decorators.js";
import {choose} from "lit/directives/choose.js";
import {connect} from "pwa-helpers";
import "./register";

enum Tab {
    Login = 'login',
    Register = 'register'
}

@customElement("auth-login")
class AuthLogin extends LitElement {
    @query(".selector > div") main?: any;
    @state() private tab = Tab.Login;
    @state() private authenticated: boolean = false;

    static get styles() {
        // language=CSS
        return css`
          :host {
            backdrop-filter: blur(10px);
            background: rgba(38, 139, 210, 0.7);
            display: flex;
            flex-direction: column;
            align-items: center;
            height: 100%;
            gap: 1em;
            position: fixed;
            width: 100%;
            z-index: 10000;
          }

          .logo {
            margin-top: 20vh;
            color: white;
            font-size: 33vh;
            font-weight: bold;
            height: 33vh;
          }
        `;
    }

    _handleClickLogin() {
        store.dispatch(startAssertion()).then((arg) => {
            console.debug(arg);
        });
    }

    render() {
        return html`
          ${choose(this.tab, [
            [Tab.Login, () => html`
              <div class="logo">#</div>
              <mwc-button label="Sign In"
                          raised
                          @click=${this._handleClickLogin}></mwc-button>
              <mwc-button label="Create Account"
                          @click="${() => this.tab = Tab.Register}">
              </mwc-button>

            `],
            [Tab.Register, () => html`
              <auth-register @auth-tab-back=${() => this.tab = Tab.Login}>
              </auth-register>
            `]]
          )}
        `;
    }
}