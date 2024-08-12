load("//bzl:private/grpc_web.bzl", _grpc_web_client = "grpc_web_client")
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
