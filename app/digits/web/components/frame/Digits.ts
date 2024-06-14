import "@app/digits/web/components/dialer/Keypad";
import "@material/web/button/outlined-button";
import "@material/web/dialog/dialog";
import "@material/web/fab/fab";
import "@material/web/icon/icon";
import "@material/web/iconbutton/icon-button";
import "@material/web/labs/card/filled-card";
import "@material/web/labs/navigationbar/navigation-bar";
import "@material/web/labs/navigationtab/navigation-tab";
import "@material/web/list/list";
import "@material/web/list/list-item";
import "@material/web/menu/menu";
import "@material/web/menu/menu-item";
import {PhoneNumberStorageServicePromiseClient} from "app/digits/storage/digits/phone_number_grpc_web_pb.js";
import {ListPhoneNumberRequest, PhoneNumber} from "app/digits/storage/digits/phone_number_pb.js";
import {VoiceServicePromiseClient} from "app/digits/proto/voice_grpc_web_pb.js";
import {CallRequest} from "app/digits/proto/voice_pb.js";
import {DialerKeypad} from "app/digits/web/components/dialer/Keypad";
import {MdMenu} from "@material/web/menu/menu";
import {css, html, LitElement} from "lit";
import {customElement, property, query, state} from "lit/decorators.js";
import {choose} from "lit/directives/choose.js";
import {when} from "lit/directives/when.js";
import {inject} from "inversify";


enum Tabs {
    Dialpad = "dialpad",
    SMS = "sms",
    Settings = "settings"
}

@customElement("frame-digits")
export class FrameDigits extends LitElement {
    @inject(VoiceServicePromiseClient) private voiceServiceClient!: VoiceServicePromiseClient;
    @inject(PhoneNumberStorageServicePromiseClient) private phoneNumberStorageServiceClient!: PhoneNumberStorageServicePromiseClient;
    @property({type: String}) name!: string;
    @state() private selectedNumber!: PhoneNumber;
    @state() private ownedNumbers: PhoneNumber[] = [];
    @state() tab = Tabs.Dialpad;
    @query("md-button") callButton;
    @query('md-menu') menu: MdMenu;
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
        this.phoneNumberStorageServiceClient.list(
            new ListPhoneNumberRequest()
        ).then(listPhoneNumberResponse => {
            this.ownedNumbers = listPhoneNumberResponse.getResultsList();
            this.selectedNumber = this.ownedNumbers[0];
        });
    }

    render() {
        return html`
            ${when(this.selectedNumber, () => html`
                <h1 id="selectNumber" @click=${() => this.menu.open = true}>
                    ${this.selectedNumber.getPhoneNumber()}
                </h1>

                <md-menu anchor="selectNumber">
                    ${this.ownedNumbers.map(ownedNumber => html`
                        <md-menu-item @click=${() => this.selectedNumber = ownedNumber}>
                            ${ownedNumber.getPhoneNumber()}
                        </md-menu-item>
                    `)}
                </md-menu>

                ${choose(this.tab, [
                    [Tabs.Dialpad, () => html`
                        <dialer-keypad></dialer-keypad>
                        <div class="call-control">
                            <md-icon class="call-button"
                                     @click=${() => this.voiceServiceClient.call(
                                             new CallRequest()
                                                     .setFromPhoneNumber(this.selectedNumber.getPhoneNumber())
                                                     .setToPhoneNumber(this.dialerKeypad.number)
                                     )}>
                                phone
                            </md-icon>
                        </div>
                    `],
                    [Tabs.SMS, () => html``],
                    [Tabs.Settings, () => html``]
                ])}
            `)}

            <md-navigation-bar>
                ${[Tabs.Dialpad, Tabs.SMS, Tabs.Settings].map(tab => html`
                    <md-navigation-tab @click=${() => this.tab = tab}>
                        <md-icon slot="active-icon">${tab}</md-icon>
                        <md-icon slot="inactive-icon">${tab}</md-icon>
                    </md-navigation-tab>
                `)}
            </md-navigation-bar>
        `;
    }
}

