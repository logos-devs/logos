import {lazyInject, TYPE} from "@logos/bind";
import '@material/mwc-button';
import {Dialog} from "@material/mwc-dialog";
import '@material/mwc-drawer';
import {Drawer} from "@material/mwc-drawer";
import '@material/mwc-icon';
import '@material/mwc-icon-button';
import {List} from "@material/mwc-list";
import '@material/mwc-list/mwc-list';
import {SingleSelectedEvent} from "@material/mwc-list/mwc-list-foundation";
import '@material/mwc-list/mwc-list-item';
import '@material/mwc-textfield';
import '@material/mwc-top-app-bar-fixed';
import {css, html, LitElement} from 'lit';
import {property, query} from 'lit/decorators.js'; // https://github.com/lit/lit/issues/1993
import {FileServicePromiseClient} from "./client/file_grpc_web_pb";
import {File, FileType, ListFilesRequest, ListFilesResponse} from "./client/file_pb";
import {ProjectServicePromiseClient} from "./client/project_grpc_web_pb";
// TODO : name grpc_web_client outputs after the rule that created them, e.g. ./client/project_grpc_web_client.js
import {ListProjectsRequest, ListProjectsResponse, Project} from "./client/project_pb";

const FILE_TYPE_ICONS: { [key: string]: string } = {
    [FileType.DIRECTORY]: 'folder',
    [FileType.REGULAR_FILE]: 'text_snippet'
};

class ReviewEditor extends LitElement {
    @lazyInject(TYPE.FileServiceClient) private fileServiceClient!: FileServicePromiseClient;
    @lazyInject(TYPE.ProjectServiceClient) private projectServiceClient!: ProjectServicePromiseClient;

    @property({type: Array}) projects: Array<Project> = [];
    @property({type: Object}) selectedProject?: Project;
    @property({type: String}) fileContents = '';
    @property({type: File}) file: File = new File();
    @property({type: Array}) files: Array<File> = [];

    @query("mwc-dialog") dialog!: Dialog;
    @query("mwc-drawer") drawer!: Drawer;
    @query("mwc-list") list!: List;
    @query(".editor") editor!: HTMLPreElement;

    static get styles() {
        return css`
            :host {
                --base03: #002b36;
                --base02: #073642;
                --base01: #586e75;
                --base00: #657b83;
                --base0: #839496;
                --base1: #93a1a1;
                --base2: #eee8d5;
                --base3: #fdf6e3;
                --yellow: #b58900;
                --orange: #cb4b16;
                --red: #dc322f;
                --magenta: #d33682;
                --violet: #6c71c4;
                --blue: #268bd2;
                --cyan: #2aa198;
                --green: #859900;

                --keyword: var(--yellow);
                --identifier: var(--blue);
                --string: var(--cyan);
                --number: var(--orange);
                --member: var(--violet);
                --method: var(--magenta);
                --comment: var(--base01);
                --input: var(--base3);
                
                background: var(--base3);
                color: var(--base0);
                --mdc-theme-primary: var(--base2);
                --mdc-theme-on-primary: var(--base02);
                --mdc-theme-secondary: var(--base1);
                --mdc-theme-on-secondary: var(--base01);
                --mdc-theme-surface: var(--base3);
                --mdc-theme-on-surface: var(--base03);
                --mdc-theme-text-primary-on-background: var(--base01);
                --mdc-theme-text-icon-on-background: var(--yellow);
                
                height: 100%;
                left: 0;
                position: absolute;
                top: 0;
                width: 100%;
            }
            @media (prefers-color-scheme: dark) {
                :host {
                    background: var(--base03);
                    color: var(--base0);
                    --mdc-theme-primary: var(--base02);
                    --mdc-theme-on-primary: var(--base2);
                    --mdc-theme-secondary: var(--base01);
                    --mdc-theme-on-secondary: var(--base1);
                    --mdc-theme-surface: var(--base03);
                    --mdc-theme-on-surface: var(--base3);
                    --mdc-theme-text-primary-on-background: var(--base1);
                    --mdc-theme-text-icon-on-background: var(--yellow);
                }
            }

            mwc-textfield {
                float: right;
            }

            .editor {
                font-family: 'Fira Code', monospace;
                margin: 0;
                padding: 0.5rem;
            }
            
            span.hover {
                border-bottom: 1px solid var(--magenta);
                cursor: pointer
            }
            
            .CATCH,
            .CLASS,
            .EXTENDS,
            .FOR,
            .IMPLEMENTS,
            .IMPORT,
            .NEW,
            .PACKAGE,
            .RETURN,
            .THROWS,
            .TRY,
            .VOID,
            .WHILE,
            .classModifier,
            .constructorModifier,
            .fieldModifier,
            .methodModifier,
            .variableModifier {
                color: var(--keyword);
            }

            .packageDeclaration {
            }

            .packageDeclaration .SEMI {
                display: none;
            }
            .packageDeclaration::after {
                content: '...';

            }

            .importDeclaration {
                display: none;
            }
            
            /*.typeName,*/
            .importDeclaration,
            .packageName,
            .unannClassOrInterfaceType {
                color: var(--identifier);
            }
            
            .constructorDeclarator > .simpleTypeName,
            .methodDeclarator > .identifier > .Identifier {
                color: var(--method);
            }

            .fieldDeclaration .variableDeclaratorId {
                color: var(--member);
            }
            .annotation {
                color: var(--orange);
            }
            
            .StringLiteral {
                color: var(--string);
            }
            
            .IntegerLiteral {
                color: var(--number);
            }
            .whitespace {
                color: var(--comment);
            }
            .singleTypeImportDeclaration > .typeName > .packageOrTypeName {
                display: none;
            }
            .singleTypeImportDeclaration > .typeName::before {
                content: " ..";
            }
            .methodModifier .annotation {
                display: none;
            }
            .methodBody > .block {
                display: none;
            }
            
            .methodBody::before {
                display: inline-block;
                width: 1rem;
                height: 1rem;
                margin-left: 0.5rem;
                line-height: 1rem;
                content: "{...}";
                text-align: center;
            }
            
            mwc-dialog textarea {
                background: rgba(0,0,0,0.2);
                box-sizing: border-box;
                color: var(--input);
                font-size: 120%;
                margin-left: -24px;
                margin-top: -20px;
                margin-bottom: -20px;
                height: 150px;
                padding: 1rem;
                outline: none;
                border: none;
                width: calc(100% + 48px);
            }
        `;
    }

    connectedCallback() {
        super.connectedCallback();

        this.projectServiceClient.listProjects(new ListProjectsRequest().setParent("dummy")).then(
            (listProjectsResponse: ListProjectsResponse) => {
                this.projects = listProjectsResponse.getProjectsList();
                this.fileServiceClient.listFiles(new ListFilesRequest().setParent("logos")).then(
                    (listFilesResponse: ListFilesResponse) => {
                        this.files = listFilesResponse.getFilesList();
                    }
                )
            }
        );

        this.openURLFile();
        window.onpopstate = () => this.openURLFile();
    }

    openURLFile() {
        const currentPath = window.location.pathname.substr(8);
        if (currentPath.length) {
            this.openFile(currentPath);
        }
    }

    openFile(fileName: string) {
        this.fileServiceClient.getFile(fileName).then(
            (file: File) => {
                this.file = file;
                this.drawer!.open = false;

                this.editor!.onmouseover = (ev) => {
                    (ev.target as HTMLSpanElement).classList.add('hover');
                }
                this.editor!.onmouseout = (ev) => {
                    (ev.target as HTMLSpanElement).classList.remove('hover');
                }
                this.editor!.onclick = ev => {
                    this.dialog!.open = true;
                }

                window.history.pushState(
                    {file: file},
                    file.getDisplayName(),
                    `/review/${file.getName()}`
                );
            }
        ).catch(console.error);
    }

    fileSelectionChanged(evt: SingleSelectedEvent) {
        if (evt.detail.index == -1) return;

        const selectedFile = this.files[evt.detail.index];
        const selectedFileName = selectedFile.getName();
        const selectedFileType = selectedFile.getType();

        if (selectedFileType === FileType.REGULAR_FILE) {
            this.title = selectedFile.getDisplayName();
            this.openFile(selectedFileName);
        } else if (selectedFileType === FileType.DIRECTORY) {
            this.fileServiceClient.listFiles(new ListFilesRequest().setParent(selectedFileName)).then(
                (listFilesResponse: ListFilesResponse) => {
                    this.files = listFilesResponse.getFilesList();
                    this.list!.select(-1);
                }
            ).catch(console.error);
        }
    }

    render() {
        const range = document.createRange();
        const fragment = range.createContextualFragment(this.file.getContents());

        return html`
            <mwc-drawer hasHeader type="modal">
                <span slot="title">Files</span>
                <span slot="subtitle">${this.files.length} results</span>
                <mwc-list @selected="${this.fileSelectionChanged.bind(this)}" activatable>
                    ${this.files.map(
                            (file: File) => html`
                                <mwc-list-item graphic="icon">
                                    <span>${file.getDisplayName()}</span>
                                    <mwc-icon slot="graphic">
                                        ${FILE_TYPE_ICONS[file.getType()]}
                                    </mwc-icon>
                                </mwc-list-item>`)}
                </mwc-list>
                <div slot="appContent">
                    <mwc-dialog>
                        <textarea></textarea>
                        <mwc-button
                                slot="primaryAction"
                                raised
                                dialogAction="save">
                            Save
                        </mwc-button>
                        <mwc-button
                                slot="secondaryAction"
                                raised
                                dialogAction="cancel">
                            Cancel
                        </mwc-button>
                    </mwc-dialog>
                    <mwc-top-app-bar-fixed dense>
                        <mwc-icon-button slot="navigationIcon" icon="menu" @click=${() => {
                            this.drawer!.open = true;
                        }}></mwc-icon-button>
                        <div slot="title">
                            ${this.file.getName()}
                        </div>
                        <pre class="editor">${fragment}</pre>
                    </mwc-top-app-bar-fixed>
                </div>
            </mwc-drawer>
        `;
    }
}

customElements.define('review-editor', ReviewEditor);