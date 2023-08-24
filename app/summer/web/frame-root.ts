import {lazyInject, TYPE} from "@logos/bind";
import {css, html, LitElement} from 'lit';
import "@material/web/divider/divider";
import "@material/web/list/list";
import "@material/web/list/list-item";
import "@material/web/fab/fab";
import "@material/web/icon/icon";

import {property, query} from 'lit/decorators.js'; // https://github.com/lit/lit/issues/1993

class FrameRoot extends LitElement {
    // @lazyInject(TYPE.FileServiceClient) private fileServiceClient!: FileServicePromiseClient;

    // @property({type: Array}) projects: Array<Project> = [];
    // @property({type: Object}) selectedProject?: Project;
    // @property({type: String}) fileContents = '';
    // @property({type: File}) file: File = new File();
    // @property({type: Array}) files: Array<File> = [];

    // @query("mwc-dialog") dialog!: Dialog;
    // @query("mwc-drawer") drawer!: Drawer;
    // @query("mwc-list") list!: List;
    // @query(".editor") editor!: HTMLPreElement;

    static get styles() {
        return css`
            :host {
                display: flex;
                flex-direction: column;
                align-items: center;
            }
            md-list {
            }
            h1 {
                text-align: center;
            }
            md-fab {
                margin-top: 1em;
            }
        `;
    }

    render() {

        return html`
          <h1>☀️</h1>
          <md-list>
            <md-list-item
                headline="War with Antarctica!"
                multi-line-supporting-text
                supporting-text="Penguin leadership on the southernmost continent have broken all diplomatic channels with NATO, and appear to be massing an invasion force."
            >
            </md-list-item>
            <md-divider></md-divider>
            <md-list-item
                headline="Wife's birthday is coming up"
                multi-line-supporting-text
                supporting-text="Danielle's birthday is next Tuesday, March 3rd"
            >
            </md-list-item>
            <md-divider></md-divider>
            <md-list-item
                headline="FOMC Meeting next Wednesday"
                multi-line-supporting-text
                supporting-text="The Federal Reserve will be meeting next week. A moderate interest rate increase is expected amid high inflation."
            >
            </md-list-item>
          </md-list>
          <md-fab variant="primary" aria-label="Edit">
            <md-icon slot="icon">add</md-icon>
          </md-fab>
        `;
    }
}

customElements.define('frame-root', FrameRoot);
