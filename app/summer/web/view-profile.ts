import {SourceRssStorageServicePromiseClient} from "@app/summer/storage/summer/source_rss_grpc_web_pb.js";
import {
    CreateSourceRssRequest,
    CreateSourceRssResponse,
    DeleteSourceRssRequest,
    DeleteSourceRssResponse,
    ListSourceRssRequest,
    ListSourceRssResponse,
    SourceRss,
    UpdateSourceRssRequest,
    UpdateSourceRssResponse
} from "@app/summer/storage/summer/source_rss_pb.js";
import cognitoPublicHostMap from "@infra/cognito_public_host_map.json";
import {user} from "app/auth/web/state";
import {lazyInject} from "dev/logos/service/client/web/bind";
import {CreateEntity, EditEntity, ListEntity} from "dev/logos/service/client/web/storage";
import {css, html, LitElement} from "lit";
import {customElement} from "lit/decorators.js";

import "@material/web/button/filled-button";
import "@material/web/iconbutton/icon-button";
import "@material/web/icon/icon";
import "@material/web/labs/card/filled-card";
import "@material/web/textfield/filled-text-field";
import {when} from "lit/directives/when.js";


@customElement('edit-source-rss')
export class EditSourceRss extends EditEntity<
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


@customElement('create-source-rss')
export class CreateSourceRss extends CreateEntity<
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


@customElement('list-source-rss')
export class ListSourceRss extends ListEntity<
    SourceRss,
    SourceRssStorageServicePromiseClient,
    ListSourceRssRequest,
    ListSourceRssResponse
> {
    @lazyInject(SourceRssStorageServicePromiseClient) protected override serviceClient: SourceRssStorageServicePromiseClient;
    protected override listRequestClass = ListSourceRssRequest;

    protected override renderCreate() {
        return html`
            <create-source-rss></create-source-rss>
        `;
    }

    protected override renderEdit(entity: SourceRss) {
        return html`
            <edit-source-rss .entity=${entity}></edit-source-rss>
        `;
    }
}


@customElement('view-profile')
export class ViewProfile extends LitElement {
    static styles = css`
        h2 {
            text-align: center;
        }
    `;

    override render() {
        return html`
            ${when(user.isAuthenticated, () => html`
                <h2>RSS Feeds</h2>
                <list-source-rss></list-source-rss>

                <h2>Email</h2>
            `, () => html`
                <md-icon-button id="login-button"
                                @click=${() => window.location.assign(cognitoPublicHostMap[location.host].loginUrl)}>
                    <md-icon>login</md-icon>
                </md-icon-button>
            `)}
        `;
    }
}