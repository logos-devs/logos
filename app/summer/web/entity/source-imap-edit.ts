import {SourceImapStorageServicePromiseClient} from "app/summer/storage/summer/source_imap_grpc_web_pb.js";
import {
    DeleteSourceImapRequest,
    DeleteSourceImapResponse,
    SourceImap,
    UpdateSourceImapRequest,
    UpdateSourceImapResponse
} from "app/summer/storage/summer/source_imap_pb.js";
import {lazyInject} from "dev/logos/service/client/web/module/app-module";
import {EditEntity} from "dev/logos/service/client/web/storage";
import {html} from "lit";
import {customElement} from "lit/decorators.js";

import "@material/web/textfield/filled-text-field";


@customElement('source-imap-edit')
export class SourceImapEdit extends EditEntity<
    SourceImap,
    SourceImapStorageServicePromiseClient,
    UpdateSourceImapRequest,
    UpdateSourceImapResponse,
    DeleteSourceImapRequest,
    DeleteSourceImapResponse
> {
    @lazyInject(SourceImapStorageServicePromiseClient) protected override serviceClient: SourceImapStorageServicePromiseClient;
    protected override updateRequestClass = UpdateSourceImapRequest;
    protected override deleteRequestClass = DeleteSourceImapRequest;

    override getDisplayName(sourceImap: SourceImap): string {
        return sourceImap.getAddress();
    }

    override renderFields(sourceImap: SourceImap) {
        return html`
            <md-filled-text-field name="setAddress"
                                  label="Address"
                                  value=${sourceImap.getAddress()}></md-filled-text-field>
        `;
    }
}
