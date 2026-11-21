# RM Games — R8 rules for the release build (docs/04 "Release build").
# Libraries bring their own consumer rules; keep this file to what R8 cannot infer.

# Readable crash stack traces (line numbers) without exposing source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
