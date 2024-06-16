import {SourceRssStorageServicePromiseClient} from "app/summer/storage/summer/source_rss_grpc_web_pb.js";
import {CreateSourceRssRequest, CreateSourceRssResponse, SourceRss} from "app/summer/storage/summer/source_rss_pb.js";
import {lazyInject} from "dev/logos/service/client/web/module/app-module";
import {CreateEntity} from "dev/logos/service/client/web/storage";
import {html} from "lit";
import {customElement} from "lit/decorators.js";

import "@material/web/textfield/filled-text-field";
import {inject} from "inversify";


@customElement('source-rss-create')
export class SourceRssCreate extends CreateEntity<
    SourceRss,
    SourceRssStorageServicePromiseClient,
    CreateSourceRssRequest,
    CreateSourceRssResponse
> {
    @lazyInject(SourceRssStorageServicePromiseClient) protected override serviceClient: SourceRssStorageServicePromiseClient;
    protected override createRequestClass = CreateSourceRssRequest;
    protected override entityClass = SourceRss;

    override renderFields() {
        return html`
            <md-filled-text-field name="setName"
                                  label="Name"></md-filled-text-field>
            <md-filled-text-field name="setUrl"
                                  label="Url"></md-filled-text-field>
            <md-filled-text-field name="setFaviconUrl"
                                  label="Favicon Url"></md-filled-text-field>
        `;
    }
}