import "@material/web/labs/card/outlined-card";
import {Entry} from "@app/summer/storage/summer/entry_pb.js";
import {css, html, LitElement} from 'lit';
import {customElement, property, state} from "lit/decorators.js";

@customElement('view-feed-entry')
export class ViewFeedEntry extends LitElement {
    @property({type: Object}) entry: Entry;
    @state() private x1 = 0;

    static get styles() {
        return css`
            :host {
            }

            md-outlined-card {
                position: relative;
                padding: 1em;
                margin: 1em;
                font-family: monospace;
            }
        `;
    }

    // connectedCallback() {
    //     super.connectedCallback();
    //
    //     this.x1 = 0;
    //     this.addEventListener('mousedown', this.startDrag);
    //     this.addEventListener('touchstart', this.startDrag);
    // }
    //
    //
    // startDrag(e) {
    //     e.preventDefault(); // This prevents unwanted scrolling on touch devices
    //     this.x1 = (e.type === 'touchstart') ? e.touches[0].clientX : e.clientX;
    //     console.log(this.x1);
    //     document.addEventListener('mousemove', this.dragMove);
    //     document.addEventListener('touchmove', this.dragMove, {passive: false});
    //     document.addEventListener('mouseup', this.stopDrag);
    //     document.addEventListener('touchend', this.stopDrag);
    // }
    //
    // dragMove(e) {
    //     e.preventDefault();
    //     this.x1 = (e.type === 'touchmove') ? e.touches[0].clientX : e.clientX - this.x1;
    //     console.log(this.x1);
    // }
    //
    // stopDrag() {
    //     document.removeEventListener('mousemove', this.dragMove);
    //     document.removeEventListener('touchmove', this.dragMove);
    //     document.removeEventListener('mouseup', this.stopDrag);
    //     document.removeEventListener('touchend', this.stopDrag);
    // }

    render() {
        return html`
            <md-outlined-card .style=${`left: ${this.x1}`} draggable="true">
                ${this.entry.getBody()}
            </md-outlined-card>
        `;
    }
}
