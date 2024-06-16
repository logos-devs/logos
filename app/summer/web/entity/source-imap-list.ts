import {SourceImapStorageServicePromiseClient} from "app/summer/storage/summer/source_imap_grpc_web_pb.js";
import {ListSourceImapRequest, ListSourceImapResponse, SourceImap} from "app/summer/storage/summer/source_imap_pb.js";
import {lazyInject} from "dev/logos/service/client/web/module/app-module";
import {ListEntity} from "dev/logos/service/client/web/storage";
import {html} from "lit";
import {customElement} from "lit/decorators.js";

import "./source-imap-create";
import "./source-imap-edit";


@customElement('source-imap-list')
export class SourceImapList extends ListEntity<
    SourceImap,
    SourceImapStorageServicePromiseClient,
    ListSourceImapRequest,
    ListSourceImapResponse
> {
    @lazyInject(SourceImapStorageServicePromiseClient) protected override serviceClient: SourceImapStorageServicePromiseClient;
    protected override listRequestClass = ListSourceImapRequest;

    protected override renderCreate() {
        return html`
            <source-imap-create></source-imap-create>
        `;
    }

    protected override renderEdit(entity: SourceImap) {
        return html`
            <source-imap-edit .entity=${entity}></source-imap-edit>
        `;
    }
}
