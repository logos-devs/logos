import {css, html, LitElement} from "lit";
import {customElement, property} from "lit/decorators.js";

@customElement("dialer-key")
export class DialerKey extends LitElement {
    // language=CSS
    static styles = css`
        :host {
            background: rgba(42, 161, 152, 1);
            box-sizing: border-box;
            flex: 33%;
            font-weight: bold;
            justify-content: center;
            align-items: center;
            flex-direction: column;
            display: flex;
            font-size: 4vh;
        }

        .letters {
            font-size: 1.5vh;
        }
    `;
    @property({type: String}) number: string = "";
    @property({type: String}) letters: string = "";

    render() {
        return html`
            <div class="number">${this.number}</div>
            <div class="letters">${this.letters}</div>
        `;
    }
}