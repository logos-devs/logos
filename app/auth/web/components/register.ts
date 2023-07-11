import {store} from "@logos/store/store";
import "@material/mwc-button";
import "@material/mwc-textfield";
import {TextField} from "@material/mwc-textfield";
import {Credential, StartRegistrationRequest} from "../client/auth_pb";
import {css, html, LitElement} from 'lit';
import {customElement, query} from "lit/decorators.js";
import {startRegistration} from "../store";


@customElement("auth-register")
class AuthRegister extends LitElement {
    @query("#username") username: TextField;
    @query("#display_name") display_name: TextField;

    static get styles() {
        // language=CSS
        return css`
          :host {
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            height: 100%;
            position: fixed;
            width: 100%;
            z-index: 10000;
          }

          mwc-textfield {
            padding-bottom: 0.5em;
          }

          .buttons {
            display: flex;
            gap: 0.5em;
          }

          mwc-button.back {
            --mdc-theme-primary: gray;
            --mdc-theme-on-primary: white;
          }
        `;
    }

    _clickSignUp() {
        if ([this.username, this.display_name]
        .map(field => field.reportValidity())
        .every(fieldValid => fieldValid)
        ) {
            void store.dispatch(startRegistration(
                new StartRegistrationRequest().setDesiredCredential(
                    new Credential().setUsername(this.username.value)
                                    .setDisplayName(this.display_name.value))))
        }
    }

    render() {
        return html`
          <mwc-textfield label="Username"
                         id="username"
                         placeholder="digitsuser"
                         required
                         shaped-filled>
          </mwc-textfield>
          <mwc-textfield label="Display Name"
                         id="display_name"
                         placeholder="Digits User"
                         required
                         shaped-filled>
          </mwc-textfield>
          <div class="buttons">
            <mwc-button label="Back"
                        class="back"
                        raised
                        @click=${() => this.dispatchEvent(new CustomEvent('auth-tab-back'))}>
            </mwc-button>
            <mwc-button label="Sign Up"
                        raised
                        @click=${this._clickSignUp}></mwc-button>
          </div>
        `;
    }
}
