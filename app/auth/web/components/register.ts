import {Credential, StartRegistrationRequest} from "@app/auth/proto/auth_pb.js";
import {store} from "@logos/store/store";
import "@material/web/button/filled-button";
import {MdOutlinedTextField} from "@material/web/textfield/outlined-text-field";
import "@material/web/textfield/outlined-text-field";
import {css, html, LitElement} from 'lit';
import {customElement, query} from "lit/decorators.js";
import {startRegistration} from "../store";


@customElement("auth-register")
class AuthRegister extends LitElement {
    @query("#username") username: MdOutlinedTextField;
    @query("#display_name") display_name: MdOutlinedTextField;

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
                <md-filled-button
                        class="back"
                        @click=${() => this.dispatchEvent(new CustomEvent('auth-tab-back'))}>
                    Back
                </md-filled-button>
                <md-filled-button
                        @click=${this._clickSignUp}>
                    Sign Up
                </md-filled-button>
            </div>
        `;
    }
}
