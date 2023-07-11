import {VoiceServicePromiseClient} from "@app/digits/web/client/voice_grpc_web_pb.js";
import {CallRequest} from "@app/digits/web/client/voice_pb.js";
import {ListPhoneNumberRequest, PhoneNumber} from "@app/digits/storage/digits/phone_number_pb.js";
import {
    PhoneNumberStorageServicePromiseClient
} from "@app/digits/storage/digits/phone_number_grpc_web_pb.js";
import {DialerKeypad} from "@app/digits/web/components/dialer/Keypad";
import "@app/digits/web/components/dialer/Keypad";
import {lazyInject, TYPE} from "@logos/bind";
import {store} from "@logos/store/store";
import "@material/mwc-button";
import {Button} from "@material/mwc-button";
import "@material/mwc-dialog";
import "@material/mwc-menu";
import {Menu} from "@material/mwc-menu";
import "@material/mwc-slider";
import "@material/mwc-tab-bar";
import "@material/mwc-textfield";
import {css, html, LitElement} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import {choose} from "lit/directives/choose.js";
import {when} from "lit/directives/when.js";


// solve spam by using the ethereum wot to classify reputation of phone numbers to eliminate spam!
// use wot as the anti-spam mechanism
// when another ethereum identity proves control of a phone number, the hash of that number moves from old owner to new

enum Tabs {
    Dialpad = "dialpad",
    SMS = "sms",
    Settings = "settings"
}

@customElement("frame-digits")
export class FrameDigits extends LitElement {
    @lazyInject(TYPE.VoiceServiceClient) private voiceServiceClient!: VoiceServicePromiseClient;
    @lazyInject(TYPE.PhoneNumberStorageServiceClient) private phoneNumberStorageServiceClient!: PhoneNumberStorageServicePromiseClient;
    @property({type: String}) name!: string;
    @state() private selectedNumber!: PhoneNumber;
    @state() private ownedNumbers: PhoneNumber[] = [];
    @state() tab = Tabs.Dialpad;
    @query("mwc-button") callButton!: Button;
    @query('mwc-menu') menu!: Menu;
    @query('dialer-keypad') dialerKeypad!: DialerKeypad;

    // language=CSS
    static styles = css`
      :host {
        display: flex;
        flex-direction: column;
        height: 100%;
      }

      mwc-top-app-bar-fixed {
        --mdc-theme-primary: var(red);
        background-size: auto 100%;
        height: 100vh;
      }

      .call-control {
        display: flex;
        width: 100%;
        font-size: 2vh;
        justify-content: center;
        align-items: center;
      }

      mwc-icon.call-button {
        border-radius: 50%;
        border: 1px solid rgba(0, 0, 0, 0.25);
        font-size: 5vh;
        margin: 5vh;
        padding: 2vh;
      }
    `;

    connectedCallback() {
        super.connectedCallback();
        this.phoneNumberStorageServiceClient.listPhoneNumber(
            new ListPhoneNumberRequest()
        ).then(listPhoneNumberResponse => {
            this.ownedNumbers = listPhoneNumberResponse.getResultsList();
            this.selectedNumber = this.ownedNumbers[0];
        });
    }

    render() {
        return html`
          ${when(this.selectedNumber, () => html`
            <mwc-button label=${this.selectedNumber.getPhoneNumber()}
                        @click=${() => this.menu.open = true}>
            </mwc-button>

            <mwc-menu>
              ${this.ownedNumbers.map(ownedNumber => html`
                <mwc-list-item @click=${() => this.selectedNumber = ownedNumber}>
                  ${ownedNumber.getPhoneNumber()}
                </mwc-list-item>
              `)}
            </mwc-menu>

            ${choose(this.tab, [
              [Tabs.Dialpad, () => html`
                <dialer-keypad></dialer-keypad>
                <div class="call-control">
                  <mwc-icon class="call-button"
                            @click=${() => this.voiceServiceClient.call(
                                new CallRequest()
                                .setFromPhoneNumber(this.selectedNumber.getPhoneNumber())
                                .setToPhoneNumber(this.dialerKeypad.number)
                            )}>
                    phone
                  </mwc-icon>
                </div>
              `],
              [Tabs.SMS, () => html``],
              [Tabs.Settings, () => html``]
            ])}
          `)}

          <mwc-tab-bar>
            ${[Tabs.Dialpad, Tabs.SMS, Tabs.Settings].map(tab => html`
              <mwc-tab icon=${tab}
                       @click=${() => this.tab = tab}></mwc-tab>
            `)}
          </mwc-tab-bar>
        `;
    }
}

