import {SourceImapStorageServicePromiseClient} from "app/summer/storage/summer/source_imap_grpc_web_pb.js";
import {SourceRssStorageServicePromiseClient} from "app/summer/storage/summer/source_rss_grpc_web_pb.js";
import {FeedServicePromiseClient} from "app/summer/proto/feed_grpc_web_pb.js";
import {AppModule, registerModule} from "dev/logos/service/client/web/module/app-module";


@registerModule
export class SummerModule extends AppModule {
    override configure() {
        this.addClient(FeedServicePromiseClient);
        this.addClient(SourceImapStorageServicePromiseClient);
        this.addClient(SourceRssStorageServicePromiseClient);
    }
}