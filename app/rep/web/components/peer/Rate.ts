import {RootState, store} from "@logos/store/store";
import {rate} from "@logos/store/wallet";
import {Dialog} from "@material/mwc-dialog";
import {Slider} from "@material/mwc-slider";
import {TextField} from "@material/mwc-textfield";
import {css, html, LitElement} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";
import {connect} from "pwa-helpers";


@customElement("peer-rate")
export class PeerRate extends connect(store)(LitElement) {
    // language=CSS
    static styles = css`
        .currentRating {
            font-size: 100px;
            height: 70px;
            padding-top: 50px;
            text-align: center;
        }

        mwc-dialog {

        }

        mwc-dialog #title {
            text-align: center;
        }
    `;
    @property({type: String}) name: string = '';
    @state() currentRating: Number = 0;
    @state() newRating?: Number = 0;
    @query("mwc-dialog") dialog: Dialog | undefined;
    @query("#name") nameField: TextField | undefined;
    @query("#rating") ratingField: Slider | undefined;

    stateChanged(state: RootState) {
        const wallet = state.wallet;
        for (let i = 0; i < wallet.outboundPeers.length || 0; i++) {
            const [[identityId, addresses, name], outboundRating, inboundRating] = wallet.outboundPeers[i];
            if (name === this.name) {
                this.currentRating = outboundRating;
                if (this.newRating === undefined) {
                    this.newRating = this.currentRating;
                }
            }
        }
    }

    updated(changed: any) {
        if (changed.has("view")) {
            this.ratingField!.layout();
        }
    }

    save_rating() {
        const isValid = !!this.name || this.nameField!.checkValidity();
        const name = !!this.name ? this.name : this.nameField!.value;
        if (isValid) {
            store.dispatch(rate({name, rating: this.ratingField!.value}));

            return;
        }

        this.nameField!.reportValidity();
    }

    render() {
        const currentRating = 0; //this.selectedPeer ? this.selectedPeer[1] : 1;
        return html`
            <mwc-dialog
                    heading=${`Rate ${this.name ? this.name : "someone"}`}
                    scrimClickAction=""
                    escapeKeyAction=""
                    open>
                <div class="form">
                    ${when(!this.name, () => html`
                        <mwc-textfield
                                id="name"
                                label="Name"
                                required>
                        </mwc-textfield>
                    `)}
                    <div>
                        <div class="currentRating">${this.newRating}</div>
                        <mwc-slider
                                id="rating"
                                min="-10"
                                max="10"
                                step="1"
                                value="${this.newRating}"
                                discrete
                                withTickMarks
                                @change=${(ev: Event) => this.newRating = parseInt((ev.currentTarget as HTMLInputElement).value)}
                        >
                        </mwc-slider>
                    </div>
                </div>
                <mwc-button slot="primaryAction"
                            @click=${this.save_rating}
                            .disabled=${this.newRating === undefined || this.newRating == this.currentRating}>Rate
                </mwc-button>
                <mwc-button slot="secondaryAction" @click=${() => history.back()} dialogAction="cancel">Cancel
                </mwc-button>
            </mwc-dialog>
        `;
    }
}

