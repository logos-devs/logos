load(":private/module.bzl", _module = "module")
load(":private/service.bzl", _service = "service")
load(":private/web.bzl", _web = "web")
load(":private/server.bzl", _server = "server")

# TODO clean up these paths
load("//dev/logos/service/storage/pg/exporter:schema_export.bzl", _schema_export = "schema_export", _schema_proto_src = "schema_proto_src", _storage_service = "java_storage_service")
load("//bzl:proto.bzl", _java_grpc_library = "java_grpc_library", _js_grpc_client = "js_grpc_client")
load("//bzl:pg_migrate.bzl", _pg_migrate = "pg_migrate")

java_grpc_library = _java_grpc_library
js_grpc_client = _js_grpc_client
module = _module
pg_migrate = _pg_migrate
schema_export = _schema_export
schema_proto_src = _schema_proto_src
service = _service
server = _server
storage_service = _storage_service
web = _web
