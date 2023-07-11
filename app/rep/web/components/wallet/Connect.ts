import {store} from "@logos/store/store";
import {connectWallet} from "@logos/store/wallet";
import {html, LitElement} from "lit";
import {customElement} from "lit/decorators.js";

@customElement("rep-connect-wallet")
export class Connect extends LitElement {
    protected render() {
        return html`
            <mwc-dialog heading="Welcome to Rep"
                        escapeKeyAction=""
                        scrimClickAction=""
                        open>
                <div>
                    Connect your MetaMask wallet now to use Rep.
                    We won&apos;t be able to send transactions without your permission.
                    <a href="#" target="_blank">Learn more.</a>
                </div>

                <mwc-button
                        slot="primaryAction"
                        @click="${() => {
                            store.dispatch(connectWallet());
                        }}">
                    Connect
                </mwc-button>
            </mwc-dialog>
        `;
    }
}