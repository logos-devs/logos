load("@bazel_skylib//rules:common_settings.bzl", "string_flag")

infra_provider = string_flag

def _escape_java(value):
    return value.replace("\\", "\\\\").replace('"', '\\"')

def env_class_cmd(package, class_name, fields):
    lines = [
        "cat <<'EOF' > $@",
        "package %s;" % package,
        "",
        "public final class %s {" % class_name,
        "    private %s() { }" % class_name,
    ]

    for name, value in fields:
        lines.append('    public static final String %s = "%s";' % (name, _escape_java(value)))

    lines.extend([
        "}",
        "EOF",
    ])

    return "\n".join(lines)

def provider_class_cmd(value):
    return env_class_cmd(
        "dev.logos.config.infra",
        "InfrastructureProvider",
        [("VALUE", value)],
    )
