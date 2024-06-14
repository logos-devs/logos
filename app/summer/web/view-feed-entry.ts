import "@material/web/labs/card/filled-card";
import "@material/web/chips/chip-set";
import "@material/web/chips/filter-chip";
import {Source} from "app/summer/proto/feed_pb.js";
import {Entry} from "app/summer/storage/summer/entry_pb.js";
import {css, html, LitElement} from 'lit';
import {customElement, property, state} from "lit/decorators.js";

@customElement('view-feed-entry')
export class ViewFeedEntry extends LitElement {
    @property({type: Object}) entry: Entry;
    @property({type: Object}) source: Source;

    @state() private x1 = 0;

    static get styles() {
        return css`
            :host {
            }

            md-filled-card {
                padding: 1em;
                position: relative;
                margin-top: 1em;
                backdrop-filter: blur(50px);
            }


            md-filled-card h3 {
                margin-top: 0;
            }

            md-filter-chip {
                margin-top: 1em;
            }
        `;
    }

    // connectedCallback() {
    //     super.connectedCallback();
    //
    //     this.x1 = 0;
    //     this.addEventListener('mousedown', this.startDrag);
    //     this.addEventListener('touchstart', this.startDrag);
    // }
    //
    //
    // startDrag(e) {
    //     e.preventDefault(); // This prevents unwanted scrolling on touch devices
    //     this.x1 = (e.type === 'touchstart') ? e.touches[0].clientX : e.clientX;
    //     console.log(this.x1);
    //     document.addEventListener('mousemove', this.dragMove);
    //     document.addEventListener('touchmove', this.dragMove, {passive: false});
    //     document.addEventListener('mouseup', this.stopDrag);
    //     document.addEventListener('touchend', this.stopDrag);
    // }
    //
    // dragMove(e) {
    //     e.preventDefault();
    //     this.x1 = (e.type === 'touchmove') ? e.touches[0].clientX : e.clientX - this.x1;
    //     console.log(this.x1);
    // }
    //
    // stopDrag() {
    //     document.removeEventListener('mousemove', this.dragMove);
    //     document.removeEventListener('touchmove', this.dragMove);
    //     document.removeEventListener('mouseup', this.stopDrag);
    //     document.removeEventListener('touchend', this.stopDrag);
    // }

    render() {
        return html`
            <md-filled-card .style=${`left: ${this.x1}`} draggable="true">
                <h3>${this.source.getName()} - ${this.entry.getName()}</h3>
                ${this.entry.getBody()}
                <md-chip-set>
                    ${this.entry.getTagsList().map(
                            tag => html`
                                <md-filter-chip label="${tag}"
                                                @click=${ev => {
                                                    ev.stopPropagation()
                                                    //this.toggleTag(tag);
                                                }}>
                                </md-filter-chip>
                            `)}
                </md-chip-set>
            </md-filled-card>
        `;
    }
}


// html`
//     <div class="mdc-card entry-card">
//         <div @click=${() => window.location.href = entry.getLinkUrl()}
//              class="mdc-card__primary-action entry-card__primary-action" tabindex="0">
//             ${when(entry.getImageUrl(), () => html`
//                 <div class="mdc-card__media mdc-card__media--16-9 entry-card__media"
//                      style="background-image: url(&quot;${entry.getImageUrl()}&quot;);"></div>
//             `)}
//             <div class="entry-card__primary">
//                 <h2 class="entry-card__title mdc-typography mdc-typography--headline6">
//                     ${entry.getName()}</h2>
//                 <h3 class="entry-card__subtitle mdc-typography mdc-typography--subtitle2">
//                     ${this.formattedDate(entry.getPublishedAt())}
//                 </h3>
//             </div>
//             <div class="entry-card__secondary mdc-typography mdc-typography--body2">
//                 ${entry.getBody()}
//             </div>
//         </div>
//         <div class="mdc-card__actions">
//             <div class="mdc-card__action-buttons">
//                 <img class="source-icon"
//                      src="${this.getSourceIcon(entry.getSourceRssId())}">
//             </div>
//             <div class="mdc-card__action-icons">
//                 <!--                                            <button class="mdc-icon-button material-icons mdc-card__action mdc-card__action&#45;&#45;icon&#45;&#45;unbounded"-->
//                 <!--                                                    title="Share" data-mdc-ripple-is-unbounded="true">-->
//                 <!--                                                push_pin-->
//                 <!--                                            </button>-->
//                 ${when(navigator['share'], () => html`
//                     <button class="mdc-icon-button material-icons mdc-card__action mdc-card__action--icon--unbounded"
//                             title="Share" data-mdc-ripple-is-unbounded="true"
//                             @click=${() => navigator.share({
//                                 title: entry.getName(),
//                                 text: entry.getBody(),
//                                 url: entry.getLinkUrl()
//                             })}>
//                         share
//                     </button>
//                 `)}
//             </div>
//         </div>
//     </div>
// `)
