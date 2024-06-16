import {SourceImapStorageServicePromiseClient} from "app/summer/storage/summer/source_imap_grpc_web_pb.js";
import {
    CreateSourceImapRequest,
    CreateSourceImapResponse,
    SourceImap
} from "app/summer/storage/summer/source_imap_pb.js";
import {lazyInject} from "dev/logos/service/client/web/module/app-module";
import {CreateEntity} from "dev/logos/service/client/web/storage";
import {html} from "lit";
import {customElement} from "lit/decorators.js";

import "@material/web/textfield/filled-text-field";


@customElement('source-imap-create')
export class SourceImapCreate extends CreateEntity<
    SourceImap,
    SourceImapStorageServicePromiseClient,
    CreateSourceImapRequest,
    CreateSourceImapResponse
> {
    @lazyInject(SourceImapStorageServicePromiseClient) protected override serviceClient: SourceImapStorageServicePromiseClient;
    protected override createRequestClass = CreateSourceImapRequest;
    protected override entityClass = SourceImap;

    override renderFields() {
        return html`
            <md-filled-text-field name="setAddress"
                                  label="Address"></md-filled-text-field>
        `;
    }
}
