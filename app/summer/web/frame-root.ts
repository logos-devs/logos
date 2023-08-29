import {EntryStorageServicePromiseClient} from "@app/summer/storage/summer/entry_grpc_web_pb.js";
import {Entry, ListEntryRequest, ListEntryResponse} from "@app/summer/storage/summer/entry_pb.js";
import "@material/web/button/outlined-button";
import "@material/web/divider/divider";
import "@material/web/fab/fab";
import "@material/web/icon/icon";
import "@material/web/list/list";
import "@material/web/list/list-item";
import {lazyInject, TYPE} from "dev/logos/stack/service/client/web/bind";
import {css, html, LitElement} from 'lit';
import {state} from "lit/decorators.js";
import {when} from "lit/directives/when.js";

class FrameRoot extends LitElement {
    @lazyInject(TYPE.EntryStorageServiceClient) private entryStorageServiceClient!: EntryStorageServicePromiseClient;
    @state() private entryList: Entry[] = [];

    connectedCallback() {
        super.connectedCallback();

        this.entryStorageServiceClient.listEntry(
            new ListEntryRequest()
        ).then((listEntryResponse: ListEntryResponse) => {
            this.entryList = listEntryResponse.getResultsList();
        });
    }

    static get styles() {
        return css`
            :host {
                --md-container-color: red;
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
              ${this.entryList.map((entry, index) => html`
                  ${when(index > 0, () => html`<md-divider></md-divider>`)}

                  <md-list-item
                          headline="${entry.getName()}"
                          multi-line-supporting-text
                          supporting-text="${entry.getBody()}"
                  >
                  </md-list-item>
              `)}
          </md-list>
        `;
    }
}

customElements.define('frame-root', FrameRoot);
