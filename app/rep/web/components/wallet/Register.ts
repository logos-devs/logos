import {store} from "@logos/store/store";
import {register} from "@logos/store/wallet";
import "@material/mwc-button";
import "@material/mwc-dialog";
import {TextField} from "@material/mwc-textfield";
import {css, html, LitElement} from "lit";
import {customElement} from "lit/decorators.js";

@customElement("rep-register-name")
export class Register extends LitElement {
    // language=CSS
    static styles = css`
        mwc-dialog {
            /*--mdc-theme-surface: transparent;*/
            /*--mdc-dialog-heading-ink-color: white;*/
            /*--mdc-dialog-content-ink-color: white;*/
            /*color: white;*/
        }

        mwc-dialog mwc-textfield {
            width: 100%;
        }
    `;

    protected render() {
        return html`
            <mwc-dialog heading="Welcome to Rep"
                        escapeKeyAction=""
                        scrimClickAction=""
                        open>
                <div>
                    The address you have selected in your wallet
                    has not registered. Choose your name now, or
                    select a registered address.
                    <a href="#" target="_blank">Learn more.</a>
                    <br>
                    <br>
                </div>

                <mwc-textfield
                        id="registerNameInput"
                        label="Name"
                        dialogInitialFocus
                        minlength="2"
                        maxlength="100"
                        required>
                </mwc-textfield>

                <mwc-button
                        slot="primaryAction"
                        @click="${() => {
                            const nameInput = <TextField>this.shadowRoot!.getElementById("registerNameInput");
                            if (nameInput.checkValidity()) {
                                store.dispatch(register(nameInput.value));
                            }
                        }}">
                    Register
                </mwc-button>
            </mwc-dialog>
        `;
    }
}