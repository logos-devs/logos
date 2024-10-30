load("//bzl:private/grpc_web.bzl", _grpc_web_client = "grpc_web_client")
load("//bzl:private/proto_json_schema.bzl", _proto_json_schema = "proto_json_schema")
load("@io_grpc_grpc_java//:java_grpc_library.bzl", _java_grpc_library = "java_grpc_library")

def proto_library(name, srcs, deps = None, visibility = None):
    native.proto_library(
        name = name,
        srcs = srcs,
        deps = deps,
        visibility = visibility,
    )

def java_proto_library(name, deps, visibility = None):
    native.java_proto_library(
        name = name,
        deps = deps,
        visibility = visibility,
    )

def java_grpc_library(name, srcs, deps, visibility = None):
    _java_grpc_library(
        name = name,
        srcs = srcs,
        deps = deps,
        visibility = visibility,
    )

def js_grpc_client(name, proto, deps = None, visibility = None):
    _grpc_web_client(
        name = name,
        proto = proto,
        visibility = visibility,
        deps = deps,
    )

def proto_json_schema(name, proto, entrypoints, deps = None, visibility = None):
    _proto_json_schema(
        name = name,
        proto = proto,
        entrypoints = entrypoints,
        deps = deps,
        visibility = visibility,
    )
