# Proto JSON Schema Generation Fix

## Issue Description

We discovered a bug where the JSON schema files for protocol buffer messages were not being regenerated when the proto files changed. This caused inconsistencies between the proto definitions and their corresponding JSON schemas, leading to validation failures in tools that rely on these schemas.

Specifically, in the `proto_json_schema.bzl` file:
1. The build action did not properly track dependencies on the proto source files
2. When proto fields were renamed or types were changed, the JSON schemas weren't regenerated
3. This created a mismatch between the implementation code and validation schemas

## Fix Implemented

We've enhanced the `_proto_json_schema_impl` function to:

1. **Explicitly include proto source files as inputs**: Now the proto direct sources are added to `cmd_inputs` list
   ```python
   # Add all proto source files as inputs to ensure rebuilds when they change
   for source in ctx.attr.proto[ProtoInfo].direct_sources:
       cmd_inputs.append(source)
   ```

2. **Consider transitive imports**: When available, transitive imports are added as inputs

3. **Add better error handling and logging**: The shell commands now include:
   - Set `-e` to exit immediately if a command fails
   - Echo which proto is being processed
   - Clear error messages if schema generation fails

4. **Fix indentation issues**: Fixed an indentation problem with the `outputs` list

## Testing the Fix

After implementing this fix, the build system will correctly detect changes to proto files and regenerate the JSON schemas accordingly. 

To verify this is working:
1. Make a change to a proto file (rename a field, change a type)
2. Run `bazel build //...` 
3. Verify the JSON schema file has been regenerated with the updated field names/types

## Apply This Fix

To apply this fix:
1. Replace the current `proto_json_schema.bzl` with the fixed version
2. Run `bazel clean`
3. Rebuild your project

## Prevention

This issue highlights the importance of properly declaring all inputs to build actions. When writing custom Bazel rules:
1. Always explicitly declare all source files as inputs to actions
2. Add proper error handling and logging in shell commands
3. Consider adding tests that verify build dependency correctness