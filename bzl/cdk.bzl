def cdk_output_jq_filter(stack_id, key_prefix):
    return '."{}" | to_entries | map(select(.key | startswith("{}")) | .key |= sub("^{}"; "")) | from_entries'.format(
        stack_id,
        key_prefix,
        key_prefix,
    )
