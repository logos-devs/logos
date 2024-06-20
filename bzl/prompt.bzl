def _prompt_rule_impl(ctx):
    output_file = ctx.actions.declare_file("prompt.txt")
    ctx.actions.write(output_file, ctx.attr.content)
    return [DefaultInfo(files = depset([output_file]))]

prompt_rule = rule(
    implementation = _prompt_rule_impl,
    attrs = {
        "content": attr.string(mandatory = True),
    },
)

def prompt(content):
    prompt_rule(
        name = "prompt",
        content = content,
    )
