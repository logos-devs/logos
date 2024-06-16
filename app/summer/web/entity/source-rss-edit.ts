import {SourceRssStorageServicePromiseClient} from "app/summer/storage/summer/source_rss_grpc_web_pb.js";
import {
    DeleteSourceRssRequest,
    DeleteSourceRssResponse,
    SourceRss,
    UpdateSourceRssRequest,
    UpdateSourceRssResponse
} from "app/summer/storage/summer/source_rss_pb.js";
import {lazyInject} from "dev/logos/service/client/web/module/app-module";
import {EditEntity} from "dev/logos/service/client/web/storage";
import {html} from "lit";
import {customElement} from "lit/decorators.js";

import "@material/web/textfield/filled-text-field";
import {inject} from "inversify";


@customElement('source-rss-edit')
export class SourceRssEdit extends EditEntity<
    SourceRss,
    SourceRssStorageServicePromiseClient,
    UpdateSourceRssRequest,
    UpdateSourceRssResponse,
    DeleteSourceRssRequest,
    DeleteSourceRssResponse
> {
    @lazyInject(SourceRssStorageServicePromiseClient) protected override serviceClient: SourceRssStorageServicePromiseClient;
    protected override updateRequestClass = UpdateSourceRssRequest;
    protected override deleteRequestClass = DeleteSourceRssRequest;

    override getDisplayName(sourceRss: SourceRss): string {
        return sourceRss.getName();
    }

    override getDescription(sourceRss: SourceRss): string {
        return sourceRss.getUrl();
    }

    override getThumbnailUrl(sourceRss: SourceRss): string {
        return sourceRss.getFaviconUrl();
    }

    override renderFields(sourceRss: SourceRss) {
        return html`
            <md-filled-text-field name="setName"
                                  label="Name"
                                  value=${sourceRss.getName()}></md-filled-text-field>
            <md-filled-text-field name="setUrl"
                                  label="URL"
                                  value=${sourceRss.getUrl()}></md-filled-text-field>
            <md-filled-text-field name="setFaviconUrl"
                                  label="Favicon URL"
                                  value=${sourceRss.getFaviconUrl()}></md-filled-text-field>
        `;
    }
}
