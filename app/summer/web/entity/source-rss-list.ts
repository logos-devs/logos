import {SourceRssStorageServicePromiseClient} from "app/summer/storage/summer/source_rss_grpc_web_pb.js";
import {ListSourceRssRequest, ListSourceRssResponse, SourceRss} from "app/summer/storage/summer/source_rss_pb.js";
import {lazyInject} from "dev/logos/service/client/web/module/app-module";
import {ListEntity} from "dev/logos/service/client/web/storage";
import {html} from "lit";
import {customElement} from "lit/decorators.js";

import "./source-rss-create";
import "./source-rss-edit";
import {inject} from "inversify";


@customElement('source-rss-list')
export class SourceRssList extends ListEntity<
    SourceRss,
    SourceRssStorageServicePromiseClient,
    ListSourceRssRequest,
    ListSourceRssResponse
> {
    @lazyInject(SourceRssStorageServicePromiseClient) protected override serviceClient: SourceRssStorageServicePromiseClient;
    protected override listRequestClass = ListSourceRssRequest;

    protected override renderCreate() {
        return html`
            <source-rss-create></source-rss-create>
        `;
    }

    protected override renderEdit(entity: SourceRss) {
        return html`
            <source-rss-edit .entity=${entity}></source-rss-edit>
        `;
    }
}