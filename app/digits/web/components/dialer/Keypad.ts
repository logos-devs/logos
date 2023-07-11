import {TextField} from "@material/mwc-textfield";
import {PhoneNumberFormat, PhoneNumberUtil} from "google-libphonenumber";
import {css, html, LitElement} from "lit";
import {customElement, query, queryAll, state} from "lit/decorators.js";
import {DialerKey} from "./Key";
import "./Key";

@customElement("dialer-keypad")
export class DialerKeypad extends LitElement {
    @query("mwc-textfield") input!: TextField;
    @queryAll("dialer-key") keys!: DialerKey[];

    private formatter = new PhoneNumberUtil();
    @state() public number = "";

    // language=CSS
    static styles = css`
        :host {
            display: flex;
            flex-wrap: wrap;
            padding: 5vw;
            gap: 1px;
            flex-grow: 1;
        }
        
        mwc-textfield {
            flex: 100%;
            margin-bottom: 1vh;
            text-align: center;
        }

        dialer-key[number="1"] {
            border-top-left-radius: 1vh;
        }
        dialer-key[number="3"] {
            border-top-right-radius: 1vh;
        }
        dialer-key[number="*"] {
            border-bottom-left-radius: 1vh;
        }
        dialer-key[number="#"] {
            border-bottom-right-radius: 1vh;
        }
    `;

    render() {
        const keyHandler = () => {
                let phoneStr = this.input.value.replace(/[^0-9+]/, "");
                try {
                    phoneStr = this.input.value = this.formatter.format(
                        this.formatter.parse(phoneStr, "US"),
                        PhoneNumberFormat.NATIONAL);
                } catch (e) {
                    console.debug(e);
                }
                this.input.value = phoneStr;
                this.number = this.input.value;
            },
            digitHandler = (ev: MouseEvent) => {
                this.input.value += (<DialerKey>ev.target).number;
                keyHandler();
            };

        return html`
            <mwc-textfield @keyup=${keyHandler} outlined type="tel"></mwc-textfield>
            <dialer-key number="1" @click=${digitHandler}></dialer-key>
            <dialer-key number="2" letters="A B C" @click=${digitHandler}></dialer-key>
            <dialer-key number="3" letters="D E F" @click=${digitHandler}></dialer-key>
            <dialer-key number="4" letters="G H I" @click=${digitHandler}></dialer-key>
            <dialer-key number="5" letters="J K L" @click=${digitHandler}></dialer-key>
            <dialer-key number="6" letters="M N O" @click=${digitHandler}></dialer-key>
            <dialer-key number="7" letters="P Q R S" @click=${digitHandler}></dialer-key>
            <dialer-key number="8" letters="T U V" @click=${digitHandler}></dialer-key>
            <dialer-key number="9" letters="W X Y Z" @click=${digitHandler}></dialer-key>
            <dialer-key number="*" @click=${digitHandler}></dialer-key>
            <dialer-key number="0" @click=${digitHandler}></dialer-key>
            <dialer-key number="#" @click=${digitHandler}></dialer-key>
        `;
    }
}