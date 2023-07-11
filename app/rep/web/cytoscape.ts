// import avsdf from 'cytoscape-avsdf';
// import cytoscape from 'cytoscape';
// import {toSvg} from "jdenticon";
//
// cytoscape.use(avsdf);
//
// firstUpdated() {
//     // this.cytoscape = cytoscape({
//     //
//     //     container: this.graph,
//     //
//     //     style: [
//     //         {
//     //             selector: 'node',
//     //             style: {
//     //                 'background-color': '#066',
//     //                 'label': 'data(label)'
//     //             }
//     //         },
//     //
//     //         {
//     //             selector: 'edge',
//     //             style: {
//     //                 'width': 3,
//     //                 'line-color': '#ccc',
//     //                 'target-arrow-color': '#ccc',
//     //                 'target-arrow-shape': 'triangle',
//     //                 'curve-style': 'bezier',
//     //                 'label': 'data(label)'
//     //             }
//     //         }
//     //     ]
//     // });
// }
//
// identityNode(identity) {
//     return {
//         data: {
//             id: identity.id,
//             label: identity.name
//         }
//     };
// }
//
// identityNodeCss(identity) {
//     return {
//         'background-color': 'white',
//         'background-image': `url("data:image/svg+xml;utf8,${encodeURIComponent('<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE svg>' + toSvg(identity.name, 30))}")`,
//         'border-width': '1px',
//         'border-color': '#444',
//         'text-valign': 'bottom',
//         'font-size': '6px'
//     };
// }
//
// relationshipEdge(sourceIdentity, targetIdentity, rating) {
//     return {
//         data: {
//             id: `${sourceIdentity.id}-${targetIdentity.id}`,
//             source: sourceIdentity.id,
//             target: targetIdentity.id,
//             label: rating
//         }
//     };
// }
//
// relationshipEdgeStyle(sourceIdentity, targetIdentity, rating) {
//     const color = rating > 0 ? '#999' : '#c77';
//     return {
//         'width': Math.abs(rating),
//         'line-color': color,
//         'target-arrow-color': color,
//         'font-size': '4px'
//     };
// }
//
// // per edge
// this.cytoscape.remove('*');
// this.cytoscape
//     .add(this.identityNode(identity))
//     .css(this.identityNodeCss(identity));
//
// this.cytoscape
//     .add(this.identityNode(outbound_peer_identity))
//     .css(this.identityNodeCss(outbound_peer_identity));
//
// if (rating_given != 0) {
//     this.cytoscape
//         .add(this.relationshipEdge(identity, outbound_peer_identity, rating_given))
//         .css(this.relationshipEdgeStyle(identity, outbound_peer_identity, rating_given));
// }
// if (rating_received != 0) {
//     this.cytoscape
//         .add(this.relationshipEdge(outbound_peer_identity, identity, rating_received))
//         .css(this.relationshipEdgeStyle(identity, outbound_peer_identity, rating_received));
// }
// for (const outbound_peer of outbound_peers) {
//     const [outbound_peer_identity, rating_given, rating_received] = outbound_peer;
//
//     // this.cytoscape
//     //     .add(this.identityNode(outbound_peer_identity))
//     //     .css(this.identityNodeCss(outbound_peer_identity));
//     //
//     // if (rating_given != 0) {
//     //     this.cytoscape
//     //         .add(this.relationshipEdge(identity, outbound_peer_identity, rating_given))
//     //         .css(this.relationshipEdgeStyle(identity, outbound_peer_identity, rating_given));
//     // }
//     // if (rating_received != 0) {
//     //     this.cytoscape
//     //         .add(this.relationshipEdge(outbound_peer_identity, identity, rating_received))
//     //         .css(this.relationshipEdgeStyle(identity, outbound_peer_identity, rating_received));
//     // }
// }
//
// // this.cytoscape.layout({
// //     name: 'avsdf',
// //     nodeSeparation: 75,
// //     animate: false
// // }).run();
