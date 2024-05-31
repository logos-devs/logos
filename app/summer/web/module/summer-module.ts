import {SourceImapStorageServicePromiseClient} from "@app/summer/storage/summer/source_imap_grpc_web_pb.js";
import {SourceRssStorageServicePromiseClient} from "@app/summer/storage/summer/source_rss_grpc_web_pb.js";
import {rootContainer} from "dev/logos/service/client/web/bind";
import {AppModule} from "dev/logos/service/client/web/module/app-module";


export class SummerModule extends AppModule {
    override configure() {
        this.addClient(SourceImapStorageServicePromiseClient);
        this.addClient(SourceRssStorageServicePromiseClient);
    }
}

rootContainer.load(new SummerModule())